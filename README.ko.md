# seeker-agent

[English](README.md)

`seeker-agent`는 애플리케이션 코드를 수정하지 않고 JVM 애플리케이션을 관측하기 위한 Java `-javaagent` APM 에이전트입니다.

대상 JVM에 부착되어 Byte Buddy로 지원 라이브러리를 계측하고, 프로세스 안에서 trace context를 관리하며, JVM 메트릭을 수집하고, Logback 이벤트를 trace/span ID와 연결한 뒤 gRPC로 Seeker collector에 전송합니다.

> 프로젝트 상태: early-stage입니다. 로컬 실험과 통제된 테스트 환경에 적합합니다. 운영 환경에 적용하기 전 제한사항과 보안 주의사항을 반드시 검토하세요.

## 주요 기능

- Java `-javaagent` 부착
- Tomcat inbound request tracing
- Apache HttpClient 4.x outbound call tracing 및 W3C Trace Context 주입
- JDBC `PreparedStatement` tracing
- package-prefix 기반 service method tracing
- GC, memory, thread, class loading, CPU JVM/system metric 수집
- Logback 로그와 `traceId`, `spanId` 상관관계
- gRPC span, metric, log 전송
- 로컬 확인용 console sender debug mode

## 요구사항

- 이 저장소 빌드용 JDK 17 이상
- 저장소에 포함된 Gradle wrapper
- `-javaagent`로 시작할 수 있는 대상 JVM 애플리케이션
- 선택사항: 샘플 애플리케이션 실행용 Docker
- 선택사항: gRPC 및 agent registration을 받을 Seeker collector endpoint

## Quick Start

### 1. agent jar 빌드

```bash
./gradlew :agent-bootstrap:shadowJar
```

agent jar는 아래 경로에 생성됩니다.

```text
agent-bootstrap/build/libs/agent-bootstrap-1.0-SNAPSHOT.jar
```

### 2. 애플리케이션에 agent 부착

```bash
java \
  -javaagent:/path/to/agent-bootstrap-1.0-SNAPSHOT.jar \
  -Dseeker.config=/path/to/seeker.config \
  -jar your-application.jar
```

### 3. collector 없이 debug mode 사용

`seeker.profiler.debug.enabled=true`이면 agent는 gRPC channel을 만들지 않고 수집한 telemetry를 console sender로 출력합니다.

```properties
seeker.profiler.debug.enabled=true
```

로컬 개발에서 가장 먼저 확인하기 좋은 모드입니다.

## 설정

`seeker.config`는 애플리케이션 classpath에서 로드하거나, `-Dseeker.config=/path/to/seeker.config`로 외부 경로를 지정할 수 있습니다.

```properties
# Agent identity
seeker.agent-identity.name=order-service
seeker.agent-identity.group=payments

# Collector
seeker.collector.host=127.0.0.1
seeker.collector.grpc-port=9999
seeker.collector.http-port=8081

# Instrumentation
seeker.profiler.jdbc.enabled=true
seeker.profiler.http.enabled=true
seeker.profiler.spring.enabled=true
seeker.profiler.base-packages=com.example.order,com.example.common
seeker.profiler.max-span-event-count=1500
seeker.profiler.sampling-rate=1.0
seeker.profiler.debug.enabled=false

# Logs
seeker.profiler.log.enabled=false
seeker.profiler.log.logback.enabled=true
seeker.profiler.log.min-level=ERROR
seeker.profiler.log.only-traced=true
seeker.profiler.log.mdc.enabled=false
seeker.profiler.log.mdc.keys=

# Metrics
seeker.metric.enabled=true
seeker.metric.interval.ms=5000
seeker.metric.batch.size=6
```

전체 설정 계약은 [agent-config/README.md](agent-config/README.md)를 참고하세요.

## 프로젝트 구조

```text
agent-bootstrap   JVM premain entry point, lifecycle wiring, final shadow jar
agent-config      seeker.config loading and typed configuration
agent-core        trace, span, metric, log models, context, propagation, sender interfaces
agent-instrument  Byte Buddy instrumentation engine and interceptor contracts
agent-metric      JVM and system metric collectors
agent-sender      gRPC and console transport implementations
plugins/          built-in instrumentation plugins
seeker-test       local sample application
seeker-test2      local multi-service sample application
```

## 모듈 문서

- [agent-bootstrap](agent-bootstrap/README.md)
- [agent-config](agent-config/README.md)
- [agent-core](agent-core/README.md)
- [agent-instrument](agent-instrument/README.md)
- [agent-metric](agent-metric/README.md)
- [agent-sender](agent-sender/README.md)
- [tomcat-plugin](plugins/tomcat-plugin/README.md)
- [httpclient-plugin](plugins/httpclient-plugin/README.md)
- [jdbc-plugin](plugins/jdbc-plugin/README.md)
- [service-plugin](plugins/service-plugin/README.md)
- [logback-plugin](plugins/logback-plugin/README.md)

## 지원 범위

지원:

- Java `-javaagent` startup
- Tomcat request tracing
- Apache HttpClient 4.x tracing
- JDBC `PreparedStatement` tracing
- package-prefix 기반 public service method tracing
- W3C Trace Context propagation
- Logback log correlation
- JVM/system metric collection
- gRPC telemetry transport

아직 지원하지 않음:

- Jetty, Undertow, WebFlux, reactive server instrumentation
- Java 11 HttpClient, OkHttp, WebClient plugins
- Log4j2, JUL plugins
- async executor 및 reactive context propagation
- complete OpenTelemetry compatibility
- production-grade reconnect, retry, sender self-metrics
- complete SQL/log masking policy

[docs/supported-matrix.md](docs/supported-matrix.md)와 [docs/limitations.md](docs/limitations.md)를 참고하세요.

## 로컬 샘플

샘플 애플리케이션은 로컬 개발 전용입니다. Docker와 Spring 설정에 `root`, `password` 같은 단순 로컬 credential이 포함될 수 있으며, 운영 환경에서 재사용하면 안 됩니다.

시나리오 기반 테스트는 [docs/sample-scenario.md](docs/sample-scenario.md)를 참고하세요.

## 개발

전체 테스트 실행:

```bash
./gradlew test
```

최종 agent jar 빌드:

```bash
./gradlew :agent-bootstrap:shadowJar
```

특정 모듈 컴파일:

```bash
./gradlew :agent-instrument:compileJava
```

instrumentation 코드를 변경하기 전에는 [agent-instrument/README.md](agent-instrument/README.md)와 수정하려는 plugin README를 먼저 읽는 것을 권장합니다.

## 보안 및 개인정보

설정과 계측 범위에 따라 agent telemetry에는 URL, SQL statement, log message, MDC value, header, application identifier가 포함될 수 있습니다.

현재 보안 제한사항:

- gRPC transport는 현재 plaintext channel creation을 사용합니다.
- SQL/log masking policy는 완성되어 있지 않습니다.
- MDC collection은 allowlist 기반이지만 신중히 설정해야 합니다.
- debug mode는 telemetry를 stdout에 출력할 수 있습니다.

민감한 보안 이슈는 [SECURITY.md](SECURITY.md)의 절차에 따라 제보하세요.

## 기여

기여를 환영합니다. 먼저 [CONTRIBUTING.md](CONTRIBUTING.md)를 읽어주세요. agent 안전성이 중요합니다. 계측 실패가 대상 애플리케이션 장애로 이어지면 안 됩니다.

## 라이선스

Apache License 2.0입니다. [LICENSE](LICENSE)와 [NOTICE](NOTICE)를 참고하세요.
