# service-plugin

[한국어](README.ko.md)

`service-plugin` instruments application service methods under configured package prefixes.

## Supported Library

This plugin is package-prefix based and is not tied to a single framework. It is commonly used with Spring applications, but the matching rule is based on configured Java packages.

## What It Captures

- public method entry and exit under configured base packages
- nested span events inside the current trace
- method class and method name
- exceptions thrown by instrumented methods

## Instrumentation Targets

Main classes:

- `ServicePlugin`
- `ServiceInterceptor`

The plugin matches application classes under `seeker.profiler.base-packages`.

## Runtime Flow

```text
Tomcat root span is active
  -> application calls configured service method
  -> ServiceInterceptor starts SpanEvent
  -> method proceeds
  -> interceptor records thrown exception if any
  -> SpanEvent is finished
```

## Configuration

```properties
seeker.profiler.spring.enabled=true
seeker.profiler.base-packages=com.example.order,com.example.common
```

Choose package prefixes carefully. A very broad package can add unnecessary overhead.

## Limitations

- Only configured package prefixes are instrumented.
- Private methods are not a primary target.
- Framework proxy behavior may affect observed class and method names.
- Async executor and reactive context propagation are not covered.
- Broad matching can increase startup and runtime overhead.

## Tests Or Sample

Use the sample applications and configure `seeker.profiler.base-packages` to point at their service packages.
