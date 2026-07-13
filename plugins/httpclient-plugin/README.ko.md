# httpclient-plugin

[English](README.md)

`httpclient-plugin`은 Apache HttpClient outbound call을 instrument하고 W3C Trace Context header를 주입합니다.

## 지원 라이브러리

- Apache HttpClient 4.x

이 plugin에서 지원하지 않는 항목:

- Java 11 `java.net.http.HttpClient`
- OkHttp
- Spring WebClient
- Reactor Netty

## 수집 항목

- outbound HTTP client call
- external HTTP dependency span event
- 가능한 경우 request URL
- 가능한 경우 HTTP method
- 호출 실패 시 exception information
- W3C `traceparent` injection

## Instrumentation Targets

주요 클래스:

- `HttpClientPlugin`
- `HttpClientExecuteInterceptor`
- `ApacheHttpRequestSetter`

이 plugin은 Apache HttpClient execute path를 instrument하고 request setter를 사용해 trace header를 주입합니다.

## 실행 흐름

```text
application calls Apache HttpClient
  -> interceptor starts outbound SpanEvent
  -> current trace context is injected into request headers
  -> HTTP call proceeds
  -> interceptor records response or error
  -> SpanEvent is finished
```

## 설정

```properties
seeker.profiler.http.enabled=true
```

## 제한사항

- Apache HttpClient 4.x만 범위에 포함됩니다.
- redirect, retry, pooled connection 동작은 추가 검증이 필요할 수 있습니다.
- header와 URL masking은 완성되어 있지 않습니다.
- async/reactive HTTP client는 지원하지 않습니다.

## 테스트 또는 샘플

로컬 샘플 애플리케이션은 서비스 간 HTTP call을 포함합니다. debug mode로 trace header injection과 span event 생성을 먼저 확인하세요.
