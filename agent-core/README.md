# agent-core

[한국어](README.ko.md)

`agent-core` contains the shared domain model and runtime contracts used by Seeker Agent.

## Role

- Define trace, span, span event, log, and metric models.
- Manage trace context and scope.
- Provide W3C Trace Context propagation contracts.
- Define sender interfaces used by runtime modules.
- Keep shared agent logic independent from specific instrumentation plugins.

## Main Components

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

## Runtime Flow

Inbound request instrumentation creates or continues a trace:

```text
Tomcat plugin
  -> extract W3C trace context
  -> create Trace and root Span
  -> bind TraceContext to ThreadLocal
  -> downstream plugins create SpanEvent objects
  -> sender dispatches completed telemetry
  -> clear TraceContext
```

Nested instrumentation creates span events:

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

`agent-core` supports W3C Trace Context through `traceparent` handling.

Used by:

- inbound plugins to extract upstream trace context
- outbound plugins to inject downstream trace context

## Log Correlation

Log collection uses the current trace context to attach:

- `traceId`
- `spanId`
- severity
- logger name
- message
- optional MDC values

Log collection should be guarded to avoid recursive logging loops.

## Dependencies

`agent-core` is a foundational module. Keep it lightweight and avoid dependencies on concrete plugins, transport implementations, or application frameworks.

## Extension Points

- Add new method categories in `MethodType`.
- Add new sender contracts only when multiple sender implementations need the same abstraction.
- Extend model attributes carefully because collector and UI compatibility may be affected.

## Tests

```bash
./gradlew :agent-core:test
```

## Notes

- Agent state must not leak across requests or threads.
- Always close scopes when instrumentation exits.
- Bound trace data with limits such as max span event count.
- Treat model and protocol changes as compatibility-sensitive.
