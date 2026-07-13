# Pinpoint 분석 → 자체 APM 에이전트 설계 노트

본 문서는 Pinpoint 오픈소스(https://github.com/pinpoint-apm/pinpoint)의
JVM 모니터링/메트릭 파이프라인을 코드 레벨로 분석하고, 그 결과를 우리
`apm-agent` / `apm-collector` 프로젝트 설계에 어떻게 반영할지 정리한
설계 노트입니다.

분석 기준 코드: `C:/Users/SSAFY/seok/pinpoint`

---

## 0. 한눈에 보는 Pinpoint 메트릭 파이프라인

```
[Agent JVM]
   ScheduledExecutor (1 daemon thread, 5s 주기)
     └ AgentStatCollector.collect()      ← 12종 MXBean snapshot
        └ N cycle 누적 후 batch flush
           └ gRPC bidirectional stream (StatService.sendAgentStat)
                                                │
                                                ▼
[Collector]
   StatService          (PStatMessage receiver)
     └ AgentMetricBatchHandler
        └ GrpcAgentStatBatchMapper       proto → AgentStatBo
           └ AgentStatGroupService       fan-out to backends
              └ PinotAgentStatService.save()
                 ├ validateTime(10분)    Pinot segment 보호
                 ├ AgentStatModelConverter.convertXxx
                 │     도메인 → EAV row (long-form)
                 ├ DAO.insertAgentStat        → Kafka topic (hashed by app)
                 └ DAO.insertApplicationStat  → Kafka topic (single)
                                                │
                                                ▼
[Apache Pinot real-time]
   agent_stat / application_stat 테이블 segment 적재
                                                │
                                                ▼
[Web]
   AgentInspectorStatController
     └ TimeWindowSlotCentricSampler(10s, 200) 다운샘플링
        └ PinotAgentStatDao (AVG / MAX / SUM SQL)
           └ React 차트 (200 포인트 고정)
```

---

## 1. 측정 대상 — Pinpoint가 모니터링하는 12종 JVM 리소스

`AgentStatCollector` (`agent-module/profiler/.../monitor/collector/AgentStatCollector.java:86`)
가 한 cycle에 호출하는 11+1개의 sub-collector:

| # | Collector | 측정 항목 | 핵심 MXBean |
|---|-----------|-----------|-------------|
| 1 | BasicJvmGcMetricCollector | heap/non-heap used·max, GC old count·time, gcType | MemoryMXBean, GarbageCollectorMXBean |
| 2 | DetailedJvmGcMetricCollector | Eden/Survivor/Old/CodeCache/Perm/Metaspace 사용률, GC new count·time | MemoryPoolMXBean × 6 |
| 3 | DefaultCpuLoadMetric | jvm CPU load, system CPU load (0~1) | com.sun.management.OperatingSystemMXBean |
| 4 | DefaultTransactionMetric | sampled new/continuation, unsampled, skipped (delta) | TransactionCounter (내부) |
| 5 | DefaultActiveTraceMetricCollector | fast/normal/slow/very-slow 활성 트레이스 분포 | ActiveTraceHistogram (내부) |
| 6 | DefaultDataSourceMetricCollector | pool별 active/max connection | 등록된 DataSourceMonitor |
| 7 | DefaultResponseTimeMetric | 윈도우 avg, max | ResponseTimeCollector.resetAndGetValue |
| 8 | DefaultDeadlockMetric | deadlock 스레드 ID set | DeadlockThreadLocator (별도 monitor 스레드) |
| 9 | OracleFileDescriptorMetric | open file descriptor count | com.sun.management.UnixOperatingSystemMXBean |
| 10 | DefaultBufferMetric | direct/mapped buffer count·memoryUsed | BufferPoolMXBean |
| 11 | DefaultTotalThreadMetric | thread count | ThreadMXBean |
| 12 | DefaultLoadedClassMetric | loaded class count, unloaded class count | ClassLoadingMXBean |

### JMX로 추가 가능한 metric (Pinpoint가 안 쓰지만 우리는 검토 대상)

- `MemoryMXBean.getObjectPendingFinalizationCount()` — finalizer 적체 감지
- `ThreadMXBean.getPeakThreadCount()` / `getDaemonThreadCount()` — 스레드 leak 감지에 유용
- `ThreadMXBean.getThreadCpuTime(id)` — 스레드별 CPU 핫스팟
- `CompilationMXBean.getTotalCompilationTime()` — JIT 활동
- `RuntimeMXBean.getUptime()` — 단순한데 dashboard용으로 자주 필요
- `com.sun.management.OperatingSystemMXBean.getFreePhysicalMemorySize()` — 컨테이너 OOM 예측
- `GarbageCollectionNotificationInfo` (push 모델) — 각 GC 이벤트의 cause/duration/before·after — pause time 분포 측정
- `HotSpotDiagnosticMXBean.dumpHeap()` — on-demand heap dump

JMX로 **불가능한 것**: GC pause 분포(이벤트 listener는 가능), allocation rate, lock contention 상세, async stack profiling, JIT inlining. 이건 JFR / JVMTI native agent / async-profiler 영역.

---

## 2. 에이전트 측 수집 로직 (Pinpoint 패턴)

### 2-1. 스케줄러 — `DefaultAgentStatMonitor`

`agent-module/profiler/.../monitor/DefaultAgentStatMonitor.java:51`

```java
private static final long MIN_COLLECTION_INTERVAL_MS = 1000;
private static final long MAX_COLLECTION_INTERVAL_MS = 1000 * 10;

private final ScheduledExecutorService executor =
    new ScheduledThreadPoolExecutor(1,
        new PinpointThreadFactory("Pinpoint-stat-monitor", true));  // daemon

@Override
public void start() {
    executor.scheduleAtFixedRate(statMonitorJob,
        this.collectionIntervalMs, this.collectionIntervalMs, TimeUnit.MILLISECONDS);
}
```

핵심:
- **단일 데몬 스레드** — 비즈니스 스레드와 격리.
- 인터벌은 [1s, 10s]로 클램프, 기본 5s.
- **pre-load class 트릭**: `start()` 전에 `EmptyDataSender`로 `CollectJob.run()`을 2번 미리 호출 → JDK 6 시절 classloader 순환 deadlock(#2881) 방지. 우리 프로젝트도 안전 보험으로 유지 권장.

### 2-2. 한 cycle의 흐름 — `CollectJob`

`agent-module/profiler/.../monitor/CollectJob.java:62`

```java
@Override
public void run() {
    final long currentCollectionTimestamp = System.currentTimeMillis();
    final long collectInterval = currentCollectionTimestamp - this.prevCollectionTimestamp;
    try {
        final AgentStatMetricSnapshot agentStat = agentStatCollector.collect();
        agentStat.setTimestamp(currentCollectionTimestamp);
        agentStat.setCollectInterval(collectInterval);   // 측정된 wall-clock interval
        this.agentStats.add(agentStat);
        if (++this.collectCount >= numCollectionsPerBatch) {
            sendAgentStats();
            this.collectCount = 0;
        }
    } catch (Exception ex) {
        logger.warn("AgentStat collect failed. Caused:{}", ex.getMessage(), ex);
    } finally {
        this.prevCollectionTimestamp = currentCollectionTimestamp;
    }
}

private void sendAgentStats() {
    final AgentStatMetricSnapshotBatch agentStatBatch = new AgentStatMetricSnapshotBatch();
    agentStatBatch.setAgentId(agentId);
    agentStatBatch.setStartTimestamp(agentStartTimestamp);
    agentStatBatch.setAgentStats(this.agentStats);
    this.agentStats = new ArrayList<>(numCollectionsPerBatch);  // list swap (lock-free)
    dataSender.send(agentStatBatch);
}
```

핵심 4가지:
1. **`collectInterval`은 측정값**: 스케줄 인터벌(5초)이 아니라 실제 경과 시간. GC pause로 8초가 됐으면 8초로 보냄. → 컬렉터가 정확한 TPS 계산 가능.
2. **N cycle 누적 후 batch flush** (default 6) → gRPC 호출 1/6로 절감.
3. **list swap**: `sendAgentStats()`가 새 ArrayList를 할당하고 기존 리스트는 dataSender 스레드에 넘김 → lock 없이 producer/consumer 분리.
4. **try-catch 격리**: 한 cycle 실패해도 throw 안 하고 다음 cycle 진행.

### 2-3. MXBean 호출은 단순 polling

대표 예시 (`agent-module/profiler/.../monitor/metric/memory/DefaultMemoryMetric.java:34`):

```java
@Override
public MemoryMetricSnapshot getSnapshot() {
    final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
    return new MemoryMetricSnapshot(
        heapMemoryUsage.getMax(),     heapMemoryUsage.getUsed(),
        nonHeapMemoryUsage.getMax(),  nonHeapMemoryUsage.getUsed());
}
```

DetailedMemory의 ratio 변환 (`DefaultDetailedMemoryMetric.java:78`):

```java
private double calculateUsage(MemoryUsage memoryUsage) {
    if (memoryUsage == null) return UNCOLLECTED_USAGE;
    long max = memoryUsage.getMax() == -1 ? memoryUsage.getCommitted() : memoryUsage.getMax();
    if (max == -1 || max == 0) return UNCOLLECTED_USAGE;
    return memoryUsage.getUsed() / (double) max;   // 0~1 비율
}
```

→ ZGC/Metaspace처럼 `max == -1`(unbounded)인 pool을 `committed`로 fallback. 절댓값 byte가 아니라 **사용률(0~1)**로 보내서 차트 친화적.

### 2-4. GC 알고리즘 자동 감지

`agent-module/profiler/.../context/provider/stat/jvmgc/GarbageCollectorMetricProvider.java:46`

```java
@Override
public GarbageCollectorMetric get() {
    Map<String, GarbageCollectorMXBean> map = createGarbageCollectorMap();
    for (GarbageCollectorType garbageCollectorType : GarbageCollectorType.values()) {
        if (map.containsKey(garbageCollectorType.oldGenName())) {
            return new DefaultGarbageCollectorMetric(garbageCollectorType,
                map.get(garbageCollectorType.oldGenName()));
        }
    }
    return new UnknownGarbageCollectorMetric();   // ← graceful fallback
}
```

`GarbageCollectorType.java:23`:

```java
SERIAL  (JvmGcType.SERIAL,   "MarkSweepCompact",       "Copy"),
PARALLEL(JvmGcType.PARALLEL, "PS MarkSweep",           "PS Scavenge"),
CMS     (JvmGcType.CMS,      "ConcurrentMarkSweep",    "ParNew"),
G1      (JvmGcType.G1,       "G1 Old Generation",      "G1 Young Generation"),
ZGC     (JvmGcType.ZGC,      "ZGC Major Pauses",       "ZGC Minor Pauses");
```

`GarbageCollectorMXBean.getName()`을 보고 5종 GC 알고리즘 자동 매칭. 매칭 실패 시 `UnknownGarbageCollectorMetric`.

### 2-5. 값의 종류별 처리 (snapshot vs delta vs window)

| 종류 | Pinpoint 예시 | 처리 위치 |
|------|--------------|-----------|
| Gauge (snapshot) | heap used, threadCount, CPU, fd | 폴링값 그대로 |
| Cumulative counter | GC count, GC time, classes loaded | agent 누적값 그대로 → 서버에서 차분 |
| Delta gauge | transaction count | agent에서 prev 보관 (`TransactionGauge`) |
| Window reset | response time avg/max | cycle마다 reset |

`TransactionGauge` 구현 (`DefaultTransactionMetric.java:96`):

```java
private long getTransactionCount() {
    final long transactionCount = longCounter.getCount();
    if (transactionCount < 0) return UNCOLLECTED;
    if (this.prevTransactionCount == UNINITIALIZED) {
        this.prevTransactionCount = transactionCount;
        return 0L;       // 첫 호출은 0 (spike 방지)
    }
    final long delta = transactionCount - this.prevTransactionCount;
    this.prevTransactionCount = transactionCount;
    return delta;
}
```

### 2-6. graceful degradation (필수)

`profiler-optional/.../DefaultCpuLoadMetric.java:44`:

```java
CpuUsageProvider jvmCpuUsageProvider = new JvmCpuUsageProvider(operatingSystemMXBean);
try {
    jvmCpuUsageProvider.getCpuUsage();
} catch (NoSuchMethodError e) {
    logger.warn("Expected method not found ...");
    jvmCpuUsageProvider = CpuUsageProvider.UNSUPPORTED;
}
```

→ JDK 메서드가 없으면 `UNSUPPORTED` provider로 교체. 메트릭 하나가 죽어도 다른 메트릭에 영향 없음.

---

## 3. 컬렉터 측 수신 로직 (참고용)

### 3-1. gRPC bidirectional stream

`collector/.../receiver/grpc/service/StatService.java:75`

```java
@Override
public StreamObserver<PStatMessage> sendAgentStat(StreamObserver<Empty> responseStream) {
    long streamId = serverStreamId.incrementAndGet();
    UidFetcher fetcher = uidFetcherStreamService.newUidFetcher();
    return new ServerCallStream<>(logger, streamId, fetcher, responseObserver,
        this::onNext, streamCloseOnError, Empty::getDefaultInstance);
}
```

요청-응답이 아니라 **에이전트가 한 번 stream 열고 stat을 push**. trace stream과 분리되어 한쪽이 폭주해도 다른 쪽이 영향 없음.

### 3-2. 도메인 매핑 — Visitor 패턴

`collector/.../mapper/grpc/stat/GrpcAgentStatMapper.java:59`

```java
void map(PAgentStat agentStat, AgentStatBo.Builder builder) {
    final long timestamp = agentStat.getTimestamp();
    AgentStatBo.Builder.StatBuilder statBuilder = builder.newStatBuilder(timestamp);
    for (GrpcStatMapper mapper : mappers) {     // 12개 매퍼가 같은 builder 방문
        mapper.map(statBuilder, agentStat);
    }
}
```

각 매퍼는 자기가 책임지는 stat만 추가 (`GrpcCpuLoadBoMapper.java:37`):

```java
@Override
public void map(AgentStatBo.Builder.StatBuilder builder, PAgentStat agentStat) {
    if (agentStat.hasCpuLoad()) {                // proto에 필드 있을 때만
        DataPoint point = builder.getDataPoint();
        builder.addPoint(this.map(point, agentStat.getCpuLoad()));
    }
}
```

→ 매퍼 추가/제거가 다른 매퍼에 영향 없음.

### 3-3. EAV 변환 — 핵심 디자인

`inspector-collector/.../model/kafka/AgentStatModelConverter.java:78` (JVM GC 1건 → 7 row):

```java
public List<AgentStat> convertJvmGc(List<JvmGcBo> boList, String tenantId) {
    AgentStatList list = new AgentStatList(boList.size() * 7);
    for (JvmGcBo jvmGcBo : boList) {
        final AgentStatList.Collector builder = list.newCollect(tenantId, jvmGcBo);
        builder.collect(AgentStatField.JVM_GC_TYPE,            jvmGcBo.getGcType().getTypeCode());
        builder.collect(AgentStatField.JVM_GC_HEAP_USED,       jvmGcBo.getHeapUsed());
        builder.collect(AgentStatField.JVM_GC_HEAP_MAX,        jvmGcBo.getHeapMax());
        builder.collect(AgentStatField.JVM_GC_NONHEAP_USED,    jvmGcBo.getNonHeapUsed());
        builder.collect(AgentStatField.JVM_GC_NONHEAP_MAX,     jvmGcBo.getNonHeapMax());
        builder.collect(AgentStatField.JVM_GC_NONHEAP_GC_OLD_COUNT, jvmGcBo.getGcOldCount());
        builder.collect(AgentStatField.JVM_GC_NONHEAP_GC_OLD_TIME,  jvmGcBo.getGcOldTime());
    }
    return list.build();
}
```

`AgentStat` 객체 (`AgentStat.java:30`):

```java
public class AgentStat {
    private final String tenantId;
    private final long timestamp;
    private final String applicationName;
    private final String agentId;
    private final String sortKey;
    private final String metricName;       // 예: "JVM_GC"
    private final String fieldName;        // 예: "JVM_GC_HEAP_USED"
    private final double fieldValue;
    private final List<Tag> tags;
}
```

**EAV(Entity-Attribute-Value) 형태**: 12종 stat이 모두 같은 스키마를 공유 → 단일 Pinot 테이블에 적재, 새 metric 추가 시 스키마 변경 0.

### 3-4. PinotMappers — Reflection 기반 매퍼 자동 등록

`inspector-collector/.../service/PinotMappers.java:39`

```java
public List<PinotTypeMapper<StatDataPoint>> getMapper() {
    List<Method> methods = findDeclaredMethods(this.getClass(), this::isTypeMapper);
    List<PinotTypeMapper<StatDataPoint>> mappers = new ArrayList<>();
    for (Method method : methods) {
        logger.info("Found PinotTypeMapper : {}", method.getName());
        mappers.add(newMapper(method));
    }
    return mappers;
}

private boolean isTypeMapper(Method method) {
    return method.getName().startsWith("get") &&
           PinotTypeMapper.class.isAssignableFrom(method.getReturnType());
}

public PinotTypeMapper<CpuLoadBo> getCpuLoad() {
    return new PinotTypeMapper<>(AgentStatBo::getCpuLoadBos, agentMapper::convertCpuLoad);
}
```

→ 새 metric 추가 = `getXxx()` 메서드 한 개 + `convertXxx` 한 개. **자동 등록**. 우리 라우터에도 그대로 적용 가능.

### 3-5. 시간 검증 (Pinot segment 보호)

`PinotAgentStatService.java:87`:

```java
private boolean validateTime(List<? extends StatDataPoint> agentStatData) {
    Instant collectedTime = Instant.ofEpochMilli(point.getTimestamp());
    Instant validTime = Instant.now().minus(Duration.ofMinutes(10));
    if (!validTime.isBefore(collectedTime)) {
        logger.info("AgentStat data is invalid. ...");
        return false;
    }
    return true;
}
```

→ 10분 이상 늦은 데이터는 **통째 drop**. real-time 시간 파티션 오염 방지. 클럭 스큐 방어를 겸함.

---

## 4. 자체 에이전트 설계 가이드

### 4-1. 7가지 핵심 결정사항

설계 진입 전에 이걸 먼저 못 박으세요:

| # | 결정 | Pinpoint 선택 | 우리 권장 |
|---|------|---------------|-----------|
| 1 | Pull vs Push | Pull (단일 스레드 폴링) + GC notification은 push | 동일 |
| 2 | 값의 종류 표시 | enum 없음 (서버가 metric 이름으로 추론) | **enum으로 강제 (`GAUGE/CUMULATIVE/DELTA/WINDOW`)** |
| 3 | EAV vs Typed | proto는 typed, Pinot는 EAV | 동일 (송신 typed, 저장 EAV) |
| 4 | 스레드 모델 | 단일 데몬 스레드 + 별도 sender 큐 | 동일 |
| 5 | 배치 정책 | N cycle 누적 후 flush (default 6) | 동일 + payload size 임계치 추가 |
| 6 | 실패 정책 | per-cycle try-catch, NoSuchMethodError 캐치 | **per-collector try-catch까지 한 단계 더** |
| 7 | 4-튜플 키 | (applicationName, agentId, agentStartTime, timestamp) | + tenantId 미리 박기 |

### 4-2. Core 데이터 모델 (권장 스케치)

```java
// 1. 식별자 4-tuple
public record DataPoint(
    String tenantId,            // 멀티테넌시는 처음부터
    String applicationName,
    String agentId,
    long agentStartTime,        // 재시작 인스턴스 구분
    long timestamp
) {}

// 2. 값의 종류 (enum으로 강제)
public enum MetricValueType {
    GAUGE,              // heap, threadCount
    CUMULATIVE,         // GC count
    DELTA,              // transaction
    WINDOW              // response time
}

// 3. 한 cycle snapshot (송신 모델, typed)
public final class MetricSnapshot {
    private DataPoint origin;
    private long collectIntervalMs;
    private final List<MetricEntry> entries = new ArrayList<>();
}

public record MetricEntry(
    String metricName,          // "jvm.gc"
    String fieldName,           // "heap_used"
    double value,
    MetricValueType type,
    List<Tag> tags
) {}

// 4. 저장 row (EAV)
public record MetricRow(
    String tenantId, long timestamp,
    String applicationName, String agentId,
    String sortKey,
    String metricName, String fieldName,
    double value, List<Tag> tags,
    MetricValueType type
) {}

// 5. Collector 인터페이스
public interface MetricCollector<S> {
    String name();
    boolean isAvailable();      // graceful degradation을 인터페이스에 박음
    S collect();
}
```

### 4-3. Plugin 아키텍처

#### A. SPI

```java
public interface MetricPlugin {
    String id();                                  // "jvm.core", "hikari", "tomcat"
    int order();
    boolean canActivate(Environment env);          // probe-by-class
    List<MetricCollector<?>> register(PluginContext ctx);
}
```

`META-INF/services/com.your.MetricPlugin` 파일에 구현체 등록 → `ServiceLoader.load(MetricPlugin.class)`로 자동 발견.

#### B. probe-by-class 패턴

```java
public class HikariPlugin implements MetricPlugin {
    @Override
    public boolean canActivate(Environment env) {
        return ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource",
                                    env.classLoader());
    }
    @Override
    public List<MetricCollector<?>> register(PluginContext ctx) {
        return List.of(new HikariPoolCollector(ctx.mbeanServer()));
    }
}
```

→ HikariCP 안 쓰는 앱에서 `NoClassDefFoundError` 안 남.

#### C. 모듈 분리 권장 구조

```
apm-agent/
  agent-core/              ← 의존성 0 (JMX만)
  agent-bootstrap/         ← premain 진입점
  plugins/
    jvm-core/              ← heap, gc, threads, class, fd, buffer
    jvm-detailed/          ← MemoryPool 분해
    vendor-oracle/         ← com.sun.management.*
    vendor-ibm/
    hikari/
    tomcat/
    redis-lettuce/
    http-okhttp/
  agent-dist/              ← 최종 jar 빌드 (plugin 묶음 선택)
```

`vendor-oracle` 같은 경우 `<scope>provided</scope>` + reflection이나 `-Aprocessor` 컴파일 분기로 메인 jar에는 안 들어가게.

#### D. Composite + per-collector 격리

```java
public class CompositeCollector implements MetricCollector<MetricSnapshot> {
    private final List<MetricCollector<?>> children;
    @Override
    public MetricSnapshot collect() {
        var snap = new MetricSnapshot();
        for (var child : children) {
            if (!child.isAvailable()) continue;
            try {
                snap.merge(child.name(), child.collect());
            } catch (Throwable t) {
                logger.warn("collector failed: {}", child.name(), t);
            }
        }
        return snap;
    }
}
```

한 collector 실패가 다른 collector를 막지 않게 **try-catch는 composite 레이어에 박음**.

### 4-4. PinotMappers 패턴 적용 (선택)

새 metric 종류 추가의 마찰을 줄이고 싶으면 reflection 기반 자동 등록을 적용:

```java
public class MetricRouter {
    public List<TypeMapper<?>> getMappers() {
        return Arrays.stream(getClass().getDeclaredMethods())
            .filter(m -> m.getName().startsWith("get") &&
                         TypeMapper.class.isAssignableFrom(m.getReturnType()))
            .map(this::invoke)
            .toList();
    }

    public TypeMapper<HeapBo> getHeap() {
        return new TypeMapper<>(Snapshot::getHeapBos, converter::convertHeap);
    }

    public TypeMapper<GcBo> getGc() {
        return new TypeMapper<>(Snapshot::getGcBos, converter::convertGc);
    }
}
```

→ 새 metric 추가 = `getXxx()` 메서드 + `convertXxx()` 메서드 두 개만. 다른 코드 손대지 않아도 자동 적재.

### 4-5. graceful degradation 체크리스트

자체 에이전트에 반드시 구현:

- [ ] `NoSuchMethodError` 캐치 후 collector를 `UNSUPPORTED`로 교체
- [ ] `null` MXBean → `UNCOLLECTED` sentinel 값
- [ ] `max == -1` (unbounded) → `committed`로 fallback
- [ ] unknown vendor type → `UnknownXxxMetric` fallback
- [ ] per-cycle try-catch (CollectJob 레벨)
- [ ] per-collector try-catch (Composite 레벨)
- [ ] dataSender 실패 → in-memory ring buffer drop policy + log throttle
- [ ] 에이전트 자체 OOM 방어 (snapshot list size 임계치)
- [ ] **에이전트 죽으면 비즈니스 앱이 안 죽는다는 것을 테스트로 증명**

### 4-6. 타이밍 / 클럭 스큐

- 인터벌은 **wall-clock measured**로 보내라 (스케줄 인터벌 신뢰 X)
- 컬렉터에서 **Now - 10분 이상 과거**는 drop (Pinot/시계열 DB 보호)
- 같은 agentId라도 `agentStartTime`이 다르면 다른 인스턴스로 취급
- agent 시계가 미래로 튀는 경우 따로 catch는 안 하는 듯 — NTP 동기화 가정

### 4-7. 스레드 명명 규칙

`PinpointThreadFactory("Pinpoint-stat-monitor", true)` 처럼 **운영자가 jstack 떠서 즉시 식별 가능한 이름**을 박기. 우리도:
- `apm-stat-monitor` (수집)
- `apm-stat-sender` (gRPC 송신)
- `apm-trace-sender`
- `apm-deadlock-detector`

---

## 6. Pinpoint 코드 reference index

자주 다시 볼 파일 한 곳에:

### 에이전트
- `agent-module/profiler/.../monitor/DefaultAgentStatMonitor.java:51` — 스케줄러 진입점
- `agent-module/profiler/.../monitor/CollectJob.java:62` — cycle 메인
- `agent-module/profiler/.../monitor/collector/AgentStatCollector.java:86` — 12개 sub-collector 묶음
- `agent-module/profiler/.../monitor/collector/jvmgc/BasicJvmGcMetricCollector.java:42` — JVM GC 수집 예
- `agent-module/profiler/.../monitor/metric/memory/DefaultMemoryMetric.java:34` — MXBean 호출 예
- `agent-module/profiler/.../monitor/metric/memory/DefaultDetailedMemoryMetric.java:78` — pool 사용률 변환
- `agent-module/profiler/.../monitor/metric/transaction/DefaultTransactionMetric.java:96` — delta gauge 패턴
- `agent-module/profiler/.../context/provider/stat/jvmgc/GarbageCollectorMetricProvider.java:46` — GC 자동 감지
- `agent-module/profiler-optional/profiler-optional-jdk8/.../cpu/oracle/DefaultCpuLoadMetric.java:38` — vendor 분기

### 컬렉터
- `collector/.../receiver/grpc/service/StatService.java:75` — gRPC 진입점
- `collector/.../handler/grpc/metric/AgentMetricBatchHandler.java:38` — 핸들러
- `collector/.../mapper/grpc/stat/GrpcAgentStatMapper.java:59` — visitor 패턴 매퍼
- `inspector-collector/.../service/PinotAgentStatService.java:71` — fan-out 진입
- `inspector-collector/.../service/PinotMappers.java:39` — reflection 자동 등록
- `inspector-collector/.../model/kafka/AgentStatModelConverter.java:78` — EAV 변환
- `inspector-collector/.../model/kafka/AgentStat.java:30` — EAV row 정의
- `inspector-collector/.../dao/pinot/DefaultAgentStatDao.java:51` — Kafka publish

### 웹
- `inspector-web/.../controller/AgentInspectorStatController.java:52` — 차트 API
- `commons-timeseries/.../TimeWindowSlotCentricSampler.java:53` — 200 포인트 다운샘플링

---

### 프롬프트 작성 원칙

1. 항상 **reference 파일을 절대 경로**로 명시 — 우리 프로젝트 + Pinpoint 둘 다
2. 변경 범위를 **번호 매겨서 열거** — "관련 파일 모두" X
3. **호환성 정책을 미리** — "기존 데이터 어떻게? default 값은?"
4. **읽기 vs 쓰기를 분리** — "분석만" 명시
5. **검증 명령어 포함** — `mvn test -pl xxx`
6. **graceful degradation 정책 매번** — 안 쓰면 LLM이 fail-fast로 짜는 경향
7. **스레드 이름 같은 운영 디테일도 명시** — jstack 식별성

---

## 8. 안티패턴 / 하지 말 것

| 안티패턴 | 이유 |
|---------|------|
| 측정 하나 실패하면 throw | 모니터링이 비즈니스 앱을 죽임 — 절대 금지 |
| 스케줄 인터벌을 collectInterval로 신뢰 | GC pause 시 TPS 계산 오류 — wall-clock measured 써야 함 |
| 모든 metric을 untyped double로 | 차트가 누적값/gauge 구분 못함 — 처음부터 enum |
| Plugin 의존성을 core에 넣기 | 한 plugin 빠지면 에이전트 jar 빌드 실패 — probe-by-class |
| MBean을 매 cycle 새로 lookup | 비용이 큼 — 부팅 시 1회 lookup 후 캐싱 |
| send 실패 시 retry storm | 백오프/드롭 정책 필요. 백오프 없는 retry는 컬렉터를 DDoS |
| agent thread 일반 thread name | 운영자가 jstack에서 식별 불가 — `apm-*` prefix 강제 |
| 컬렉터 시간 검증 없음 | 클럭 스큐로 시계열 segment 오염 — 10분 timeout 필수 |
| EAV 변환을 web 레이어에서 | 차트 응답 느려짐 — 수집 시점에 EAV로 변환 |

---

