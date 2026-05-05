# seeker-agent

분산 환경의 Java 애플리케이션을 비침투로 관측하는 APM 에이전트입니다.
JVM에 `-javaagent`로 부착되어 바이트코드를 조작(Byte Buddy)하고, 트레이스/메트릭을 gRPC로 Collector에 비동기 전송합니다.

핵심 설계는 아래 세 가지입니다.

- **W3C Trace Context 호환 분산 추적** — OpenTelemetry가 따르는 동일 표준 헤더로 다른 벤더 에이전트와 동일 trace를 공유합니다.
- **JVM/시스템 메트릭 주기 수집** — GC 횟수·시간, 힙/논힙 메모리, 스레드, 클래스 로딩, 프로세스/시스템 CPU를 일정 주기로 모아 보냅니다. GC는 Serial·Parallel·CMS·G1·ZGC·Shenandoah를 런타임에 자동 분류해 동일 스키마로 노출합니다.
- **gRPC 비동기 + 단일 채널 멀티 스트림 송신** — span/metric이 채널 하나를 공유하면서 각자 별도 stream을 사용해 한쪽 폭주가 다른 쪽을 막지 않도록 격리합니다.

---

## 빌드 & 에이전트 부착

### 1. 에이전트 jar 빌드

`agent-bootstrap` 모듈은 `com.gradleup.shadow`로 모든 의존 라이브러리를 단일 jar로 합치고, 매니페스트에 `Premain-Class: com.seeker.agent.bootstrap.AgentMain`을 등록해 JVM이 부팅 시 `AgentMain.premain()`을 호출하도록 만듭니다. 이렇게 만들어진 jar가 `-javaagent:`로 부착할 산출물입니다.

```bash
./gradlew :agent-bootstrap:shadowJar
# 산출물: agent-bootstrap/build/libs/agent-bootstrap-1.0-SNAPSHOT.jar
```

### 2. 대상 애플리케이션에 부착

#### Spring Boot fat-jar 실행 시

```bash
java \
  -javaagent:/path/to/agent-bootstrap-1.0-SNAPSHOT.jar \
  -Dseeker.config=/path/to/seeker.config \
  -jar your-application.jar
```

### 3. 동작 확인

부팅 시 표준 출력에 다음과 같은 로그가 찍힙니다.

```
[Seeker] Seeker Agent 가동 시작...
[Seeker] 로드된 설정: AgentIdentityConfig{...}, CollectorConfig{...}, ProfilerConfig{...}
[Seeker] 에이전트 등록 성공 (status=200, endpoint=http://.../agents)
[Seeker] GrpcSpanTransport 초기화 완료 (channel: collector.internal:9999)
[Seeker] metric collector registered: jvm.gc
[Seeker] metric collector registered: jvm.memory
[Seeker] metric scheduler started: interval=5000ms batch=6 collectors=5
[Seeker] Seeker Agent 설치 완료.
```

설정 점검만 하고 실제 송신은 하지 않으려면 `seeker.profiler.debug.enabled=true`로 켜세요. gRPC 채널을 만들지 않고 모든 데이터를 콘솔로 출력합니다.

---

## 설정

### 설정 파일 예시

`seeker.config` (애플리케이션 classpath 또는 외부 경로):

```properties
# ── 에이전트 식별 ─────────────────────────────────────────────
# 대시보드에서 이 인스턴스를 표시할 이름. 미지정 시 agentId 앞 8자 사용
seeker.agent-identity.name=order-service
# 같은 서비스의 여러 인스턴스를 묶는 그룹명 (예: 동일 서비스의 다중 노드)
seeker.agent-identity.group=payments

# ── Collector 연결 ───────────────────────────────────────────
# Collector(백엔드) 서버 호스트
seeker.collector.host=collector.internal
# 트레이스/메트릭을 보내는 gRPC 포트
seeker.collector.grpc-port=9999
# 부팅 시 1회 agent 등록(POST /agents)에 쓰는 HTTP 포트
seeker.collector.http-port=8081

# ── 프로파일러 ───────────────────────────────────────────────
# JDBC PreparedStatement 추적 on/off
seeker.profiler.jdbc.enabled=true
# Apache HttpClient 외부 호출 추적 on/off (분산 추적 헤더 inject 포함)
seeker.profiler.http.enabled=true
# Spring 관련 추적 on/off
seeker.profiler.spring.enabled=true
# 한 trace 안에 담을 수 있는 SpanEvent 최대 개수 — 무한 호출/재귀 방어용 상한
seeker.profiler.max-span-event-count=1500
# 트레이스 샘플링 비율 (1.0 = 100% 수집)
seeker.profiler.sampling-rate=1.0
# ServicePlugin 추적 대상 패키지 (콤마로 다중 지정). 이 하위의 public 메서드가 자동 추적됨
seeker.profiler.base-packages=com.example.order,com.example.common
# debug 모드 — Collector로 보내지 않고 수집 데이터를 콘솔에 출력 (로컬 점검용)
seeker.profiler.debug.enabled=false

# ── 메트릭 ───────────────────────────────────────────────────
# JVM/시스템 메트릭 수집 전체 on/off
seeker.metric.enabled=true
# 메트릭 수집 주기(ms). [1000, 10000] 범위로 자동 clamp
seeker.metric.interval.ms=5000
# 몇 cycle 모아 한 번에 송신할지. 예: 5000ms × 6 = 30초마다 batch 전송
seeker.metric.batch.size=6
```

`name`/`group`을 지정하지 않으면 agentId 앞 8자가 사용됩니다.

---

## 모듈 구성

```
agent-bootstrap   에이전트 시작점 — 다른 모듈을 초기화하고 단일 agent.jar로 패키징
agent-config      설정 파일 로드 및 파싱
agent-core        트레이스 컨텍스트와 분산 추적 헤더 처리, Trace, Span, Span Event 모델 정의
agent-instrument  바이트코드 조작 — 어떤 메서드를 어떤 인터셉터로 가로챌지
agent-metric      JVM/시스템 메트릭 주기 수집
agent-sender      트레이스/메트릭을 Collector로 전송
plugins/
  tomcat-plugin     요청 진입점에서 root span 생성 및 분산 추적 헤더 수신
  httpclient-plugin 외부 호출에 W3C 헤더 inject
  jdbc-plugin       SQL 정보 수집
  service-plugin    base-packages 하위 클래스의 public 메서드 추적
```

---

## 추적 항목 추가

기본 제공되는 plugin(Tomcat, HttpClient, JDBC, Service) 외의 라이브러리를 추적하고 싶다면 다음 세 단계를 거칩니다.

### 1. `Plugin` 구현 — 어떤 클래스/메서드를 가로챌지 정의

`plugins/` 아래에 새 모듈을 만들고 `com.seeker.agent.instrument.plugin.Plugin`을 구현합니다.
구현 안에서:

1. 가로챌 클래스 이름을 `AgentBuilder.type(named("..."))`로 지정
2. 작성한 인터셉터를 `InterceptorRegistry.register("이름", new MyInterceptor())`로 등록
3. `BaseTransformer("이름", 메서드 매처)`를 반환해 Byte Buddy가 해당 메서드에 Advice를 붙이게 함

마지막으로 `agent-bootstrap`의 `AgentMain`에 `engine.addPlugin(new MyPlugin())` 한 줄을 추가하면 부착 시 자동 등록됩니다.

### 2. `AroundInterceptor` 구현 — SpanEvent 생성/마감

`com.seeker.agent.instrument.interceptor.AroundInterceptor`를 구현해 before/after 로직을 작성합니다.

- **before**: `TraceContextHolder.getContext().currentTraceObject()`로 현재 trace를 꺼내, `trace.traceBlockBegin(...)`으로 SpanEvent를 시작. 메서드 인자 등에서 필요한 데이터를 `event.addAttribute(...)`로 기록하고, 1번에서 등록한 `MethodType` 코드를 `event.setMethodType(...)`로 지정.
- **after**: 반환값/예외에서 추가로 기록할 게 있으면 attribute에 담고, `trace.traceBlockEnd(throwable)`로 SpanEvent를 마감.

기존 `HttpClientExecuteInterceptor`, `PreparedStatementExecuteInterceptor`가 좋은 참고 예시입니다.

### 3. `MethodType` enum 값 추가 — 호출 종류 코드 등록

`agent-core`의 `com.seeker.agent.core.model.MethodType` enum에 새 항목을 추가합니다 (예: `REDIS(3000)`, `KAFKA_PRODUCER(4000)`).

차트/UI가 이 코드 값으로 호출 종류(JDBC vs HTTP vs Redis 등)를 분류하므로, 같은 카테고리는 가까운 숫자대로 묶어 등록하는 것을 권장합니다.
