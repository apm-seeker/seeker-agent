# tomcat-plugin

[English](README.md)

`tomcat-plugin`은 Tomcat request handling을 instrument하고 inbound HTTP request의 root server span을 생성합니다.

## 지원 라이브러리

- Apache Tomcat servlet request handling
- embedded Tomcat을 사용하는 Spring Boot 애플리케이션

Jetty, Undertow, WebFlux, Netty server instrumentation은 이 plugin의 범위가 아닙니다.

## 수집 항목

- inbound HTTP request entry
- root span start and finish
- request method
- request URI
- 가능한 경우 response status
- `traceparent`를 통한 upstream W3C Trace Context

## Instrumentation Targets

이 plugin은 Tomcat request pipeline class를 instrument하고 runtime logic을 아래 클래스에 위임합니다.

- `TomcatPlugin`
- `StandardHostValveInvokeInterceptor`
- `HttpServletRequestGetter`

## 실행 흐름

```text
Tomcat receives request
  -> plugin extracts traceparent if present
  -> creates Trace and root Span
  -> binds TraceContext
  -> application handles request
  -> plugin finishes root Span
  -> sender receives completed trace
  -> clears TraceContext
```

## 설정

Tomcat tracing은 server-side instrumentation의 일부입니다. 대상 애플리케이션에 따라 service, JDBC, HTTP client, log plugin과 함께 사용합니다.

관련 설정:

```properties
seeker.profiler.spring.enabled=true
seeker.profiler.max-span-event-count=1500
seeker.profiler.sampling-rate=1.0
```

## 제한사항

- Jetty 또는 Undertow를 지원하지 않습니다.
- WebFlux 또는 reactive context propagation을 지원하지 않습니다.
- Async servlet request context propagation은 아직 production-grade가 아닙니다.
- attribute coverage는 의도적으로 최소화되어 있으며 신중하게 확장해야 합니다.

## 테스트 또는 샘플

샘플 Spring Boot 애플리케이션을 사용합니다.

```bash
./gradlew :seeker-test:bootRun
./gradlew :seeker-test2:bootRun
```

빌드한 agent jar를 `-javaagent`로 부착해 확인합니다.
