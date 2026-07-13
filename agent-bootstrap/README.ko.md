# agent-bootstrap

[English](README.md)

`agent-bootstrap`은 Seeker Agent의 JVM 진입점입니다. `premain`을 제공하고, 런타임 모듈을 연결하며, built-in plugin을 설치하고, `-javaagent`로 사용할 최종 shadow jar를 생성합니다.

## 역할

- Java agent 진입점을 제공합니다.
- `seeker.config`를 로드합니다.
- agent identity와 sender 구현체를 초기화합니다.
- metric collection을 시작합니다.
- Byte Buddy instrumentation plugin을 설치합니다.
- 런타임 의존성을 하나의 attach 가능한 jar로 패키징합니다.

## 주요 컴포넌트

- `com.seeker.agent.bootstrap.AgentMain`  
  Java agent `Premain-Class`입니다. JVM이 대상 애플리케이션 시작 전에 `premain`을 호출합니다.

- `com.seeker.agent.bootstrap.lifecycle.AgentBootstrap`  
  config, sender, metric, instrumentation 초기화를 조율합니다.

- `com.seeker.agent.bootstrap.lifecycle.AgentRuntime`  
  초기화된 runtime component를 보관합니다.

- `com.seeker.agent.bootstrap.plugin.PluginPackInstaller`  
  built-in instrumentation plugin을 instrumentation engine에 등록합니다.

## 실행 흐름

```text
JVM starts
  -> AgentMain.premain(agentArgs, instrumentation)
  -> load seeker.config
  -> initialize agent identity
  -> initialize sender module
  -> initialize metric module
  -> install built-in plugins
  -> attach Byte Buddy transformers
  -> target application continues startup
```

agent startup은 fail-open이어야 합니다. agent 초기화가 실패해도 가능한 한 대상 애플리케이션은 계속 실행되어야 합니다.

## 빌드

최종 agent jar 빌드:

```bash
./gradlew :agent-bootstrap:shadowJar
```

산출물:

```text
agent-bootstrap/build/libs/agent-bootstrap-1.0-SNAPSHOT.jar
```

shadow jar manifest에는 아래 항목이 포함되어야 합니다.

```text
Premain-Class: com.seeker.agent.bootstrap.AgentMain
```

## 부착 예시

```bash
java \
  -javaagent:/path/to/agent-bootstrap-1.0-SNAPSHOT.jar \
  -Dseeker.config=/path/to/seeker.config \
  -jar your-application.jar
```

## 의존성

`agent-bootstrap`은 런타임 모듈에 의존합니다.

- `agent-config`
- `agent-core`
- `agent-instrument`
- `agent-metric`
- `agent-sender`
- `plugins/` 아래 built-in modules

## 확장 포인트

새 built-in plugin 추가 절차:

1. `plugins/` 아래에 plugin module을 만듭니다.
2. `com.seeker.agent.instrument.plugin.Plugin`을 구현합니다.
3. `settings.gradle`에 module을 추가합니다.
4. `agent-bootstrap`에 dependency를 추가합니다.
5. `PluginPackInstaller`에 등록합니다.
6. plugin README에 지원 범위를 문서화합니다.

## 테스트

```bash
./gradlew :agent-bootstrap:test
./gradlew :agent-bootstrap:shadowJar
```

## 주의사항

- bootstrap 코드는 작고 방어적으로 유지합니다.
- agent exception이 user application logic으로 전파되면 안 됩니다.
- agent는 다른 애플리케이션 JVM 안에서 실행되므로 dependency 추가에 주의해야 합니다.
- 운영 배포 전 dependency relocation과 classloader 동작을 검토해야 합니다.
