apm-agent/
├── agent-bootstrap/
│   ├── src/main/java/
│   │   └── com/seeker/apm/
│   │       ├── AgentMain.java
│   │       ├── AgentClassLoader.java
│   │       └── BootstrapCore.java
│   └── build.gradle
│
├── agent-core/
│   ├── src/main/java/
│   │   └── com/seeker/apm/core/
│   │       ├── context/
│   │       │   ├── TraceContext.java
│   │       │   ├── TraceId.java
│   │       │   └── AsyncContext.java
│   │       ├── model/
│   │       │   ├── Trace.java
│   │       │   ├── Span.java
│   │       │   ├── SpanEvent.java
│   │       │   └── SpanChunk.java
│   │       ├── sampler/
│   │       │   ├── Sampler.java
│   │       │   └── RateSampler.java
│   │       ├── recorder/
│   │       │   ├── SpanRecorder.java
│   │       │   └── SpanEventRecorder.java
│   │       └── storage/
│   │           ├── BufferedStorage.java
│   │           └── StorageFactory.java
│   └── build.gradle
│
├── agent-instrument/
│   ├── src/main/java/
│   │   └── com/seeker/apm/instrument/
│   │       ├── InstrumentEngine.java
│   │       ├── transformer/
│   │       │   └── BaseTransformer.java
│   │       ├── interceptor/
│   │       │   ├── Interceptor.java
│   │       │   ├── AroundInterceptor.java
│   │       │   └── ExceptionHandler.java
│   │       └── plugin/
│   │           ├── PluginLoader.java             # WasPluginRegistry 호출 추가
│   │           │
│   │           ├── was/                          # ← 신규 (servlet 대체)
│   │           │   ├── spi/
│   │           │   │   ├── WasPlugin.java              # WAS 플러그인 인터페이스
│   │           │   │   ├── WasInterceptor.java         # URI/IP/Header 추출 인터페이스
│   │           │   │   └── AbstractWasInterceptor.java # 공통 before/after 트레이싱 로직
│   │           │   ├── WasPluginRegistry.java          # isApplicable()로 자동 감지
│   │           │   ├── tomcat/                         # 현재 구현체
│   │           │   │   ├── TomcatPlugin.java
│   │           │   │   └── interceptor/
│   │           │   │       └── StandardHostValveInterceptor.java
│   │           │   ├── jetty/                          # 추후 구현
│   │           │   │   └── .gitkeep
│   │           │   └── undertow/                       # 추후 구현
│   │           │       └── .gitkeep
│   │           │
│   │           ├── servlet/                      # ← 역할 축소 (Filter 보조 추적용)
│   │           │   └── interceptor/
│   │           │       └── FilterInterceptor.java     # Filter SpanEvent 기록용
│   │           │
│   │           ├── http/
│   │           │   ├── HttpClientPlugin.java
│   │           │   └── interceptor/
│   │           │       └── HttpClientInterceptor.java
│   │           ├── jdbc/
│   │           │   └── JdbcPlugin.java
│   │           └── spring/
│   │               └── SpringMvcPlugin.java
│   └── build.gradle
│
├── agent-sender/
│   ├── src/main/java/
│   │   └── com/seeker/apm/sender/
│   │       ├── DataSender.java
│   │       ├── GrpcDataSender.java
│   │       ├── queue/
│   │       │   └── AsyncQueueingExecutor.java
│   │       └── converter/
│   │           ├── SpanConverter.java
│   │           └── SpanEventConverter.java
│   └── build.gradle
│
├── agent-config/
│   └── src/main/java/
│       └── com/seeker/apm/config/
│           ├── AgentConfig.java
│           └── ProfilerConfig.java
│
└── agent-distribution/
├── src/
│   └── resources/
│       ├── META-INF/
│       │   └── MANIFEST.MF
│       └── profiles/
│           └── release/
│               └── pinpoint.config
└── build.gradle