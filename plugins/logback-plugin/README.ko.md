# logback-plugin

[English](README.md)

`logback-plugin`은 Logback logging event를 수집하고 현재 trace context와 연결합니다.

## 지원 라이브러리

- Logback logging events

이 plugin에서 지원하지 않는 항목:

- Log4j2
- JUL
- tinylog
- 기타 logging framework

## 수집 항목

- log timestamp
- logger name
- severity
- message
- 가능한 경우 throwable information
- 현재 `traceId`와 `spanId`
- allowlist에 포함된 optional MDC values

## Instrumentation Targets

주요 클래스:

- `LogbackPlugin`
- `LogbackAppenderInterceptor`
- `LogbackEventConverter`

이 plugin은 Logback append flow를 intercept하고 logging event를 agent `LogRecord`로 변환합니다.

## 실행 흐름

```text
application writes Logback event
  -> interceptor checks log config
  -> guard prevents recursive capture
  -> current trace context is read
  -> LogRecord is created
  -> LogSender dispatches the record
```

## 설정

```properties
seeker.profiler.log.enabled=true
seeker.profiler.log.logback.enabled=true
seeker.profiler.log.min-level=ERROR
seeker.profiler.log.only-traced=true
seeker.profiler.log.mdc.enabled=false
seeker.profiler.log.mdc.keys=
```

## 제한사항

- MDC collection은 allowlist 기반이며 기본적으로 비활성화되어 있습니다.
- log masking은 완성되어 있지 않습니다.
- log message에 민감정보가 포함될 수 있습니다.
- recursive logging은 반드시 guard되어야 합니다.
- Logback이 아닌 framework는 별도 plugin이 필요합니다.

## 테스트 또는 샘플

`seeker.config`에서 log collection을 활성화하고 샘플 애플리케이션을 실행한 뒤 traced request 내부에서 log를 발생시킵니다. debug mode에서는 captured log record가 console sender를 통해 출력됩니다.
