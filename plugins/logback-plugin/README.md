# logback-plugin

[한국어](README.ko.md)

`logback-plugin` captures Logback logging events and correlates them with the current trace context.

## Supported Library

- Logback logging events

Not supported by this plugin:

- Log4j2
- JUL
- tinylog
- other logging frameworks

## What It Captures

- log timestamp
- logger name
- severity
- message
- throwable information when available
- current `traceId` and `spanId`
- optional MDC values from an allowlist

## Instrumentation Targets

Main classes:

- `LogbackPlugin`
- `LogbackAppenderInterceptor`
- `LogbackEventConverter`

The plugin intercepts Logback append flow and converts logging events into agent `LogRecord` objects.

## Runtime Flow

```text
application writes Logback event
  -> interceptor checks log config
  -> guard prevents recursive capture
  -> current trace context is read
  -> LogRecord is created
  -> LogSender dispatches the record
```

## Configuration

```properties
seeker.profiler.log.enabled=true
seeker.profiler.log.logback.enabled=true
seeker.profiler.log.min-level=ERROR
seeker.profiler.log.only-traced=true
seeker.profiler.log.mdc.enabled=false
seeker.profiler.log.mdc.keys=
```

## Limitations

- MDC collection is allowlist-based and disabled by default.
- Log masking is not complete.
- Log messages may contain sensitive data.
- Recursive logging must remain guarded.
- Non-Logback frameworks require separate plugins.

## Tests Or Sample

Enable log collection in `seeker.config`, run a sample application, and trigger logs inside a traced request. In debug mode, captured log records are printed through the console sender.
