# httpclient-plugin

[한국어](README.ko.md)

`httpclient-plugin` instruments Apache HttpClient outbound calls and injects W3C Trace Context headers.

## Supported Library

- Apache HttpClient 4.x

Not supported by this plugin:

- Java 11 `java.net.http.HttpClient`
- OkHttp
- Spring WebClient
- Reactor Netty

## What It Captures

- outbound HTTP client calls
- span event for external HTTP dependency
- request URL when available
- HTTP method when available
- exception information when the call fails
- W3C `traceparent` injection

## Instrumentation Targets

Main classes:

- `HttpClientPlugin`
- `HttpClientExecuteInterceptor`
- `ApacheHttpRequestSetter`

The plugin instruments Apache HttpClient execute paths and uses a request setter to inject trace headers.

## Runtime Flow

```text
application calls Apache HttpClient
  -> interceptor starts outbound SpanEvent
  -> current trace context is injected into request headers
  -> HTTP call proceeds
  -> interceptor records response or error
  -> SpanEvent is finished
```

## Configuration

```properties
seeker.profiler.http.enabled=true
```

## Limitations

- Only Apache HttpClient 4.x is in scope.
- Redirects, retries, and pooled connections may create behavior that needs additional validation.
- Header and URL masking is not complete.
- Async/reactive HTTP clients are not covered.

## Tests Or Sample

The local sample applications include HTTP calls between services. Use debug mode first to verify trace header injection and span event creation.
