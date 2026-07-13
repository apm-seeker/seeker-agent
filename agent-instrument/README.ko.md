# agent-instrument

[English](README.md)

`agent-instrument`는 Byte Buddy 기반 instrumentation engine과 built-in plugin이 사용하는 contract를 담고 있습니다.

## 역할

- Byte Buddy transformer를 설정합니다.
- plugin이 target class와 method를 선언할 수 있게 합니다.
- advice code가 사용하는 interceptor를 등록합니다.
- instrumentation mechanism을 plugin-specific behavior와 분리합니다.

## 주요 컴포넌트

- `com.seeker.agent.instrument.InstrumentEngine`  
  plugin transformer를 JVM instrumentation instance에 설치합니다.

- `com.seeker.agent.instrument.plugin.Plugin`  
  각 instrumentation plugin이 구현하는 contract입니다.

- `com.seeker.agent.instrument.transformer.BaseTransformer`  
  matched method에 around advice를 적용하는 공통 transformer입니다.

- `com.seeker.agent.instrument.interceptor.AroundInterceptor`  
  method interception의 `before`, `after` runtime hook입니다.

- `com.seeker.agent.instrument.interceptor.InterceptorRegistry`  
  advice name을 interceptor instance에 매핑하는 registry입니다.

- `com.seeker.agent.instrument.matcher.StandardMatchers`  
  공통 matcher helper입니다.

## 실행 흐름

```text
AgentBootstrap
  -> PluginPackInstaller
  -> plugin creates transformer definitions
  -> InstrumentEngine installs Byte Buddy agent builder
  -> target class loads
  -> Byte Buddy applies advice
  -> advice calls AroundInterceptor through InterceptorRegistry
```

## Plugin Contract

plugin은 아래 내용을 설명해야 합니다.

- 어떤 type을 instrument하는지
- 어떤 method를 intercept하는지
- 어떤 interceptor가 runtime logic을 처리하는지
- 어떤 configuration flag로 켜고 끄는지

## Plugin 추가

1. `plugins/` 아래에 module을 만듭니다.
2. `Plugin`을 구현합니다.
3. 하나 이상의 `AroundInterceptor`를 만듭니다.
4. interceptor를 `InterceptorRegistry`에 등록합니다.
5. 좁은 method matcher를 가진 `BaseTransformer`를 반환합니다.
6. `settings.gradle`에 module을 추가합니다.
7. `agent-bootstrap`에 dependency를 추가합니다.
8. `PluginPackInstaller`에 plugin을 등록합니다.
9. plugin README에 지원 버전과 제한사항을 작성합니다.

## Matcher Guidelines

- 가능한 가장 좁은 class와 method set을 match합니다.
- 사용자가 명시적으로 설정한 경우가 아니면 broad package scanning을 피합니다.
- agent class를 instrument하지 않습니다.
- recursion guard 없이 logging path를 instrument하지 않습니다.
- overloaded method와 framework proxy class에 주의합니다.

## Safety Guidelines

Instrumentation은 application request path에서 실행되므로 방어적으로 작성해야 합니다.

- advice 안에서 blocking network I/O를 수행하지 않습니다.
- allocation을 작게 유지합니다.
- agent-side exception을 잡습니다.
- application return value를 변경하지 않습니다.
- application exception을 삼키지 않습니다.
- `after` logic에서 trace block을 반드시 닫습니다.

## 의존성

`agent-instrument`는 trace context와 interceptor behavior를 위해 `agent-core`에 의존합니다. 특정 application framework에 의존하지 않아야 하며, framework-specific logic은 plugin에 둡니다.

## 테스트

```bash
./gradlew :agent-instrument:test
```

## 주의사항

- bytecode instrumentation 실패는 observability 저하로 끝나야 하며 application behavior를 바꾸면 안 됩니다.
- 새 advice path는 overhead 관점에서 검토해야 합니다.
- public extension contract는 plugin author를 위해 안정적으로 유지해야 합니다.
