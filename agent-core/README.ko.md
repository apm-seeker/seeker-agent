# agent-core

[English](README.md)

`agent-core`는 Seeker Agent가 공유하는 domain model과 runtime contract를 담고 있습니다.

## 역할

- trace, span, span event, log, metric model을 정의합니다.
- trace context와 scope를 관리합니다.
- W3C Trace Context propagation contract를 제공합니다.
- runtime module이 사용하는 sender interface를 정의합니다.
- 공유 agent logic을 특정 instrumentation plugin과 분리합니다.

## 주요 컴포넌트

- `com.seeker.agent.core.model.Trace`
- `com.seeker.agent.core.model.Span`
- `com.seeker.agent.core.model.SpanEvent`
- `com.seeker.agent.core.model.MethodType`
- `com.seeker.agent.core.context.TraceContext`
- `com.seeker.agent.core.context.TraceContextHolder`
- `com.seeker.agent.core.context.ThreadLocalTraceContext`
- `com.seeker.agent.core.context.Scope`
- `com.seeker.agent.core.context.propagation.W3CTraceContextPropagator`
- `com.seeker.agent.core.log.LogRecord`
- `com.seeker.agent.core.metric.Metric`
- `com.seeker.agent.core.sender.DataSender`
- `com.seeker.agent.core.sender.LogSender`
- `com.seeker.agent.core.sender.MetricSender`

## 실행 흐름

Inbound request instrumentation은 trace를 새로 만들거나 이어받습니다.

```text
Tomcat plugin
  -> extract W3C trace context
  -> create Trace and root Span
  -> bind TraceContext to ThreadLocal
  -> downstream plugins create SpanEvent objects
  -> sender dispatches completed telemetry
  -> clear TraceContext
```

Nested instrumentation은 span event를 만듭니다.

```text
AroundInterceptor.before
  -> current trace
  -> traceBlockBegin
  -> attach method/database/http attributes

AroundInterceptor.after
  -> attach result or error attributes
  -> traceBlockEnd
```

## Context Propagation

`agent-core`는 `traceparent` 처리를 통해 W3C Trace Context를 지원합니다.

사용 위치:

- inbound plugin에서 upstream trace context 추출
- outbound plugin에서 downstream trace context 주입

## Log Correlation

Log collection은 현재 trace context를 사용해 아래 정보를 붙입니다.

- `traceId`
- `spanId`
- severity
- logger name
- message
- optional MDC values

Log collection은 recursive logging loop를 피하기 위해 guard가 필요합니다.

## 의존성

`agent-core`는 기반 모듈입니다. concrete plugin, transport implementation, application framework에 대한 의존을 피하고 가볍게 유지해야 합니다.

## 확장 포인트

- 새 method category는 `MethodType`에 추가합니다.
- 여러 sender 구현체가 같은 abstraction을 필요로 할 때만 sender contract를 추가합니다.
- model attribute 확장은 collector와 UI compatibility에 영향을 줄 수 있으므로 신중히 해야 합니다.

## 테스트

```bash
./gradlew :agent-core:test
```

## 주의사항

- agent state가 request나 thread 사이에 누수되면 안 됩니다.
- instrumentation 종료 시 scope를 반드시 닫아야 합니다.
- max span event count 같은 제한으로 trace data를 bounded하게 유지합니다.
- model과 protocol 변경은 compatibility-sensitive한 변경으로 봐야 합니다.
