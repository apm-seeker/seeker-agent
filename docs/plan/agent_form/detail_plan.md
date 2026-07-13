# 전체 패키지 & 파일 역할 상세 정리

---

## agent-bootstrap

### `AgentMain.java`

Java Agent의 **진입점**. JVM이 애플리케이션 시작 전에 호출하는 `premain()`을 구현해야 해요.

구현해야 할 것:

- `premain(String agentArgs, Instrumentation inst)` - JVM 시작 시 자동 호출
- `agentmain(String agentArgs, Instrumentation inst)` - attach API로 동적 로드 시
- AgentClassLoader 생성 및 BootstrapCore에 Instrumentation 전달

```jsx
java

public class AgentMain {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        // 1. AgentClassLoader로 격리된 클래스로더 생성
        // 2. BootstrapCore 로드 & 초기화 위임
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        premain(agentArgs, instrumentation);
    }
}
```

### `AgentClassLoader.java`

Agent 클래스와 애플리케이션 클래스가 충돌하지 않도록 격리하는 ClassLoader.

구현해야 할 것:

- URLClassLoader 상속
- agent jar 내부 클래스를 우선 로드 (parent-first 역전)
- Bootstrap classpath에 필요한 클래스 등록 (Instrumentation.appendToBootstrapClassLoaderSearch)

```jsx
java

public class AgentClassLoader extends URLClassLoader {
    @Override
    protected Class<?> loadClass(String name, boolean resolve) {
        // agent 패키지는 직접 로드, 나머지는 parent에게 위임
    }
}
```

### `BootstrapCore.java`

Agent의 **실제 초기화 오케스트레이터**. AgentMain에서 위임받아 전체 초기화 순서를 관리해요.

구현해야 할 것:

- Config 로드
- TraceContext 초기화
- InstrumentEngine(ByteBuddy) 초기화
- PluginLoader 실행
- DataSender 초기화 및 연결

```jsx
java

public class BootstrapCore {
    public void initialize(Instrumentation instrumentation) {
        // 1. AgentConfig.load()
        // 2. TraceContext 싱글톤 초기화
        // 3. InstrumentEngine 생성
        // 4. PluginLoader.loadAll(engine, instrumentation)
        // 5. GrpcDataSender 초기화 & 연결 확인
    }
}
```

---

## agent-core

### `context/TraceContext.java`

**Trace의 생명주기 전체를 관리**하는 핵심 클래스. 싱글톤으로 운영돼요.

구현해야 할 것:

- ThreadLocal로 현재 스레드의 Trace 보관
- 새 Trace 생성 (`newTraceObject`)
- 기존 TraceId로 Trace 이어받기 (`continueTraceObject`) - 분산 추적용
- 현재 Trace 조회/제거
- Sampler 연동 (샘플링 대상인지 판단)

```jsx
java

public class TraceContext {
    private final ThreadLocal<Trace> traceHolder = new ThreadLocal<>();
    private final Sampler sampler;

    public Trace newTraceObject() { ... }
    public Trace continueTraceObject(TraceId traceId) { ... }
    public Trace currentTraceObject() { return traceHolder.get(); }
    public void removeTraceObject() { traceHolder.remove(); }
}
```

### `context/TraceId.java`

**분산 추적의 핵심 식별자** 묶음.

구현해야 할 것:

- traceId (전체 요청 체인 식별, UUID or 128bit)
- spanId (현재 서버의 Span 식별)
- parentSpanId (호출한 서버의 spanId)
- flags (샘플링 여부 등)
- HTTP Header 파싱/직렬화

```jsx
java

public class TraceId {
    private final String traceId;
    private final long spanId;
    private final long parentSpanId;
    private final short flags;

    public static TraceId newTraceId() { ... }          // 최초 생성
    public TraceId getNextTraceId() { ... }             // 다음 서버로 전파할 TraceId
    public static TraceId parse(String header) { ... }  // Header에서 파싱
    public String toHeader() { ... }                    // Header로 직렬화
}
```

### `context/AsyncContext.java`

**비동기 처리 시 ThreadLocal Trace를 다른 스레드로 전달**하는 용도.

구현해야 할 것:

- 현재 Trace 캡처 (부모 스레드에서)
- 다른 스레드에서 Trace 복원
- 비동기 작업 완료 후 정리

```jsx
java

public class AsyncContext {
    private final Trace trace;

    public static AsyncContext capture() {
        // 현재 ThreadLocal에서 Trace 캡처
    }

    public Trace continueAsyncTraceObject() {
        // 새 스레드의 ThreadLocal에 Trace 복원
    }
}
```

### `model/Trace.java`

**하나의 요청(트랜잭션) 전체**를 표현. Span + SpanEvent 리스트를 가져요.

구현해야 할 것:

- SpanRecorder 제공
- SpanEvent 스택 관리 (traceBlockBegin/End)
- 샘플링 여부에 따른 No-op 처리
- close() 시 Storage로 플러시

```jsx
java

public class Trace {
    private final Span span;
    private final Deque<SpanEvent> spanEventStack = new ArrayDeque<>();
    private final boolean sampling;

    public SpanRecorder getSpanRecorder() { ... }
    public SpanEventRecorder traceBlockBegin() { ... }  // SpanEvent 시작
    public void traceBlockEnd() { ... }                  // SpanEvent 종료
    public void close() { ... }                          // Storage로 전송
}
```

### `model/Span.java`

**하나의 서버 요청 단위** 데이터 클래스. Pinpoint Collector로 전송되는 단위예요.

구현해야 할 것:

- TraceId, agentId, applicationName
- startTime, elapsedTime
- rpc (URL), remoteAddr, endPoint
- serviceType (ServiceTypeCode)
- exceptionInfo
- SpanEvent 리스트 보관

```jsx
public class Span {
    private TraceId traceId;
    private String agentId;
    private long startTime;
    private int elapsedTime;
    private String rpc;
    private String remoteAddr;
    private int serviceType;
    private Throwable exceptionInfo;
    private List<SpanEvent> spanEventList = new ArrayList<>();
}
```

### `model/SpanEvent.java`

**Span 내부의 세부 작업 단위** (DB 쿼리, 외부 HTTP 호출 등).

구현해야 할 것:

- sequence (Span 내 순서)
- startElapsed / endElapsed (Span 시작 기준 상대 시간)
- depth (콜스택 깊이)
- serviceType
- destinationId (DB명, 호스트 등)
- nextSpanId (다음 서버로 전파 시)
- apiId, exceptionInfo

```jsx
java

public class SpanEvent {
    private int sequence;
    private int startElapsed;
    private int endElapsed;
    private int depth;
    private int serviceType;
    private String destinationId;
    private long nextSpanId = -1;
    private Throwable exceptionInfo;
}
```

### `model/SpanChunk.java`

**SpanEvent를 배치로 묶어서 전송**하기 위한 컨테이너. 장기 실행 트랜잭션에서 SpanEvent를 중간중간 flush할 때 사용해요.

구현해야 할 것:

- TraceId 보관 (어느 Trace의 것인지)
- SpanEvent 리스트
- 전송 시각

### `sampler/Sampler.java` (인터페이스)

```jsx
java

public interface Sampler {
    boolean isSampling();
}
```

### `sampler/RateSampler.java`

**비율 기반 샘플링** 구현체. 예: 100개 중 1개만 추적.

구현해야 할 것:

- 샘플링 비율 설정 (1/n)
- AtomicLong 카운터로 스레드 안전하게 순서 관리
- isSampling()에서 카운터 % rate == 0 판단

### `recorder/SpanRecorder.java`

**Span에 데이터를 기록**하는 인터페이스/구현체. Interceptor가 직접 Span을 건드리지 않고 Recorder를 통해서만 기록

구현해야 할 것:

- recordRpc(String rpc)
- recordRemoteAddress(String remoteAddr)
- recordServiceType(int serviceType)
- recordException(Throwable t)
- recordApiId(int apiId)

### `recorder/SpanEventRecorder.java`

**SpanEvent에 데이터를 기록**하는 인터페이스/구현체.

구현해야 할 것:

- recordServiceType(int serviceType)
- recordDestinationId(String destinationId)
- recordNextSpanId(long nextSpanId) - 외부 호출 시
- recordException(Throwable t)
- markBeforeTime() / markAfterTime()

### `storage/BufferedStorage.java`

**완성된 Span을 메모리 버퍼에 보관**했다가 DataSender로 넘긴다.

구현해야 할 것:

- LinkedBlockingQueue로 Span 보관
- store(Span) - Trace.close() 시 호출됨
- flush() - 주기적으로 또는 큐가 차면 Sender로 전달

### `storage/StorageFactory.java`

BufferedStorage 인스턴스를 생성/관리하는 팩토리.

---

## agent-instrument

### `InstrumentEngine.java`

**ByteBuddy 인스턴스를 중앙에서 관리**. 모든 플러그인이 이 엔진을 통해 인스트루먼테이션을 등록해요.

구현해야 할 것:

- ByteBuddy 인스턴스 생성 및 보관
- `instrument()` 메서드로 AgentBuilder 제공
- Instrumentation 참조 보관

```jsx
java

public class InstrumentEngine {
    private final ByteBuddy byteBuddy;
    private final Instrumentation instrumentation;

    public AgentBuilder instrument() {
        return new AgentBuilder.Default(byteBuddy)
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE);
    }
}
```

### `transformer/BaseTransformer.java`

**ByteBuddy의 Transformer 공통 기반 클래스**. 플러그인마다 반복되는 설정을 추상화해요.

구현해야 할 것:

- AgentBuilder.Transformer 구현
- 예외 발생 시 원본 클래스 유지 (transform 실패해도 앱이 죽지 않게)
- 로깅

### `interceptor/Interceptor.java`

최상위 마커 인터페이스.

```jsx
java

public interface Interceptor {}
```

### `interceptor/AroundInterceptor.java`

**메서드 전후를 가로채는 공통 인터페이스**.

```jsx
java

public interface AroundInterceptor extends Interceptor {
    void before(Object target, Object[] args);
    void after(Object target, Object[] args, Object result, Throwable throwable);
}
```

### `interceptor/ExceptionHandler.java`

**Interceptor 내부에서 예외가 발생했을 때 처리**. Interceptor 오류가 애플리케이션에 영향 주면 안 되므로 필수예요.

구현해야 할 것:

- Interceptor before/after에서 발생한 예외를 catch해서 로깅만 하고 삼킴
- 원본 메서드 실행은 무조건 보장

### `plugin/PluginLoader.java`

**모든 플러그인을 로드하고 InstrumentEngine에 등록**하는 조율자.

구현해야 할 것:

- WasPluginRegistry.loadApplicable() 호출
- HttpClientPlugin, JdbcPlugin, SpringMvcPlugin 등록
- 플러그인 로드 실패 시 해당 플러그인만 스킵 (앱 전체에 영향 X)

```jsx
java

public class PluginLoader {
    public void loadAll(InstrumentEngine engine, Instrumentation instrumentation) {
        // WAS 플러그인 (자동 감지)
        new WasPluginRegistry().loadApplicable(engine, instrumentation);
        // 나머지 플러그인
        safeLoad(new HttpClientPlugin(), engine, instrumentation);
        safeLoad(new JdbcPlugin(), engine, instrumentation);
        safeLoad(new SpringMvcPlugin(), engine, instrumentation);
    }
}
```

### `plugin/was/spi/WasPlugin.java`

java

`public interface WasPlugin {
    String getName();
    boolean isApplicable();   // 클래스패스 체크
    void setup(InstrumentEngine engine, Instrumentation instrumentation);
}`

### `plugin/was/spi/WasInterceptor.java`

**WAS마다 Request 객체가 다르기 때문에** 추출 방법을 추상화.

java

`public interface WasInterceptor extends AroundInterceptor {
    String getRequestUri(Object[] args);
    String getRemoteAddress(Object[] args);
    String getHttpMethod(Object[] args);
    String getHeader(Object[] args, String headerName);  // TraceId 추출용
}`

### `plugin/was/spi/AbstractWasInterceptor.java`

**공통 before/after 트레이싱 로직**을 여기에 구현. WAS별 구현체는 Request 파싱만 담당.

구현해야 할 것:

- before: TraceId 헤더 추출 → continueTrace or newTrace → SpanRecorder 기록
- after: 예외 기록 → trace.close() → TraceContext.removeTraceObject()

java

`public abstract class AbstractWasInterceptor implements WasInterceptor {

    @Override
    public void before(Object target, Object[] args) {
        String traceIdHeader = getHeader(args, "X-Pinpoint-TraceId");

        Trace trace = (traceIdHeader != null)
            ? traceContext.continueTraceObject(TraceId.parse(traceIdHeader))
            : traceContext.newTraceObject();

        SpanRecorder recorder = trace.getSpanRecorder();
        recorder.recordRpc(getRequestUri(args));
        recorder.recordRemoteAddress(getRemoteAddress(args));
        recorder.recordServiceType(getServiceType());
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable t) {
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) return;
        try {
            if (t != null) trace.getSpanRecorder().recordException(t);
        } finally {
            trace.close();
            traceContext.removeTraceObject();
        }
    }

    protected abstract int getServiceType();
}`

### `plugin/was/WasPluginRegistry.java`

**클래스패스를 보고 적합한 WAS 플러그인을 자동 선택**해서 로드.

java

`public class WasPluginRegistry {
    private static final List<WasPlugin> PLUGINS = List.of(
        new TomcatPlugin()
        // new JettyPlugin(),    // 추후 추가
        // new UndertowPlugin()
    );

    public void loadApplicable(InstrumentEngine engine, Instrumentation inst) {
        PLUGINS.stream()
               .filter(WasPlugin::isApplicable)
               .forEach(p -> p.setup(engine, inst));
    }
}`

### `plugin/was/tomcat/TomcatPlugin.java`

**StandardHostValve.invoke()를 ByteBuddy로 인터셉트** 등록.

구현해야 할 것:

- isApplicable(): `org.apache.catalina.core.StandardHostValve` 클래스 존재 여부 체크
- setup(): ByteBuddy로 invoke() 메서드에 StandardHostValveInterceptor 연결

### `plugin/was/tomcat/interceptor/StandardHostValveInterceptor.java`

**AbstractWasInterceptor를 상속**해서 Tomcat의 Request 파싱만 구현.

구현해야 할 것:

- args[0]을 `org.apache.catalina.connector.Request`로 캐스팅
- getRequestUri, getRemoteAddress, getHttpMethod, getHeader 구현
- getServiceType()은 ServiceType.TOMCAT 반환

java

`public class StandardHostValveInterceptor extends AbstractWasInterceptor {

    @Override
    public String getRequestUri(Object[] args) {
        return ((org.apache.catalina.connector.Request) args[0]).getRequestURI();
    }

    @Override
    public String getHeader(Object[] args, String headerName) {
        return ((org.apache.catalina.connector.Request) args[0]).getHeader(headerName);
    }

    @Override
    protected int getServiceType() { return ServiceType.TOMCAT; }
}`

### `plugin/servlet/interceptor/FilterInterceptor.java`

**Filter.doFilter()를 인터셉트해서 SpanEvent로 기록**. Span 시작/종료가 아니라 SpanEvent 추가만 해요.

구현해야 할 것:

- 현재 Trace가 없으면 아무것도 안 함 (WAS Interceptor가 먼저 실행됐어야 함)
- traceBlockBegin()으로 SpanEvent 시작
- doFilter 완료 후 traceBlockEnd()

### `plugin/http/HttpClientPlugin.java`

**외부 HTTP 호출(RestTemplate, OkHttp 등) 인터셉트** 등록.

구현해야 할 것:

- 대상 클래스 지정 (예: `org.springframework.web.client.RestTemplate`)
- execute() 메서드에 HttpClientInterceptor 연결

### `plugin/http/interceptor/HttpClientInterceptor.java`

외부 HTTP 호출 시 **분산 추적 헤더를 Request에 주입**하고 SpanEvent 기록.

구현해야 할 것:

- before: traceBlockBegin() → nextSpanId 생성 → Request 헤더에 TraceId/SpanId 주입
- after: 응답 코드 기록 → traceBlockEnd()

### `plugin/jdbc/JdbcPlugin.java`

**JDBC Connection.prepareStatement(), execute() 등 인터셉트** 등록.

### `plugin/spring/SpringMvcPlugin.java`

**@RequestMapping 핸들러 메서드 인터셉트**. Controller 메서드명을 SpanEvent에 기록하는 용도예요.

---

## agent-sender

### `DataSender.java` (인터페이스)

```jsx
java

`public interface DataSender {
    void send(Span span);
    void send(SpanChunk spanChunk);
    void stop();
}

```

### `GrpcDataSender.java`

**gRPC로 Collector에 Span 전송**

구현해야 할 것:
- gRPC Channel 생성 및 관리
- SpanConverter로 Span → gRPC proto 변환
- 재연결 로직 (Collector 장애 시)
- send()는 비동기 (AsyncQueueingExecutor 경유)

### `queue/AsyncQueueingExecutor.java`

**Span을 큐에 넣고 별도 스레드에서 Sender로 전달**

애플리케이션 스레드가 Sender 지연에 영향받지 않도록 해요.

구현해야 할 것:
- LinkedBlockingQueue로 Span 보관
- 단일 consumer 스레드로 큐에서 꺼내 GrpcDataSender.send() 호출
- 큐가 꽉 찼을 때 드롭 처리 + 경고 로그

### `converter/SpanConverter.java`

**Span 모델 → gRPC proto 메시지 변환**

### `converter/SpanEventConverter.java`

**SpanEvent 모델 → gRPC proto 메시지 변환**

---

## agent-config

### `AgentConfig.java`

**seeker.config 파일 로드 및 파싱**

구현해야 할 것:
- agentId, applicationName
- collector.host, collector.port
- sampling.rate
- 설정 파일 위치 탐색 (시스템 프로퍼티 → 기본 경로 순)

### `ProfilerConfig.java`

**세부 프로파일링 설정 관리.**

구현해야 할 것:
- 플러그인별 활성화 여부 (jdbc.enable, http.enable 등)
- 최대 SpanEvent 개수
- 큐 사이즈

---

### 초기화 순서 정리

```jsx
AgentMain.premain()
    → AgentClassLoader 생성
    → BootstrapCore.initialize()
        → AgentConfig.load()
        → TraceContext 초기화 (싱글톤)
        → StorageFactory → BufferedStorage 생성
        → AsyncQueueingExecutor 시작
        → GrpcDataSender 초기화
        → InstrumentEngine 생성
        → PluginLoader.loadAll()
            → WasPluginRegistry → TomcatPlugin.setup()
            → HttpClientPlugin.setup()
            → JdbcPlugin.setup()
            → SpringMvcPlugin.setup()
```

---

## 의존성 방향

```jsx
agent-bootstrap
    → agent-core
    → agent-instrument
    → agent-sender
    → agent-config

agent-instrument → agent-core (TraceContext, Trace 사용)
agent-sender     → agent-core (Span, SpanEvent 모델 사용)
agent-core       → agent-config (설정값 참조)
```

순환 의존성이 없도록 **agent-core는 다른 모듈에 의존하지 않는 것**이 핵심