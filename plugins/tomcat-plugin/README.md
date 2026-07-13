# tomcat-plugin

[한국어](README.ko.md)

`tomcat-plugin` instruments Tomcat request handling and creates the root server span for inbound HTTP requests.

## Supported Library

- Apache Tomcat servlet request handling
- Spring Boot applications using embedded Tomcat

Jetty, Undertow, WebFlux, and Netty server instrumentation are not covered by this plugin.

## What It Captures

- inbound HTTP request entry
- root span start and finish
- request method
- request URI
- response status when available
- upstream W3C Trace Context through `traceparent`

## Instrumentation Targets

The plugin instruments Tomcat request pipeline classes and delegates runtime logic to:

- `TomcatPlugin`
- `StandardHostValveInvokeInterceptor`
- `HttpServletRequestGetter`

## Runtime Flow

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

## Configuration

Tomcat tracing is part of server-side instrumentation. It should be used together with service, JDBC, HTTP client, and log plugins depending on the target application.

Relevant settings:

```properties
seeker.profiler.spring.enabled=true
seeker.profiler.max-span-event-count=1500
seeker.profiler.sampling-rate=1.0
```

## Limitations

- Does not support Jetty or Undertow.
- Does not support WebFlux or reactive context propagation.
- Async servlet request context propagation is not production-grade yet.
- Attribute coverage is intentionally minimal and should be extended carefully.

## Tests Or Sample

Use the sample Spring Boot applications:

```bash
./gradlew :seeker-test:bootRun
./gradlew :seeker-test2:bootRun
```

Then attach the built agent jar with `-javaagent`.
