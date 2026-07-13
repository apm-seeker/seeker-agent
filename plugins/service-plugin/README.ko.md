# service-plugin

[English](README.md)

`service-plugin`은 설정된 package prefix 아래의 application service method를 instrument합니다.

## 지원 라이브러리

이 plugin은 package-prefix 기반이며 특정 framework에 묶이지 않습니다. Spring 애플리케이션에서 주로 사용하지만, matching rule은 설정된 Java package를 기준으로 합니다.

## 수집 항목

- configured base package 아래 public method entry/exit
- current trace 내부의 nested span event
- method class와 method name
- instrumented method가 던진 exception

## Instrumentation Targets

주요 클래스:

- `ServicePlugin`
- `ServiceInterceptor`

이 plugin은 `seeker.profiler.base-packages` 아래 application class를 match합니다.

## 실행 흐름

```text
Tomcat root span is active
  -> application calls configured service method
  -> ServiceInterceptor starts SpanEvent
  -> method proceeds
  -> interceptor records thrown exception if any
  -> SpanEvent is finished
```

## 설정

```properties
seeker.profiler.spring.enabled=true
seeker.profiler.base-packages=com.example.order,com.example.common
```

package prefix는 신중히 선택해야 합니다. 너무 넓은 package를 설정하면 불필요한 overhead가 생길 수 있습니다.

## 제한사항

- 설정된 package prefix만 instrument합니다.
- private method는 주요 target이 아닙니다.
- framework proxy behavior가 관측되는 class/method name에 영향을 줄 수 있습니다.
- async executor와 reactive context propagation은 지원하지 않습니다.
- broad matching은 startup/runtime overhead를 증가시킬 수 있습니다.

## 테스트 또는 샘플

샘플 애플리케이션을 사용하고 `seeker.profiler.base-packages`를 샘플 service package로 설정합니다.
