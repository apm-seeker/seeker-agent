# agent-sender

[English](README.md)

`agent-sender`는 agent telemetry를 transport message로 변환해 collector 또는 console output으로 전송합니다.

## 역할

- trace, metric, log, agent registration data를 전송합니다.
- gRPC transport 구현체를 제공합니다.
- debug mode용 console sender 구현체를 제공합니다.
- internal model을 protocol message로 변환합니다.
- async dispatcher를 통해 network I/O를 instrumentation path에서 분리합니다.

## 주요 컴포넌트

- `com.seeker.agent.sender.SenderModule`  
  설정에 따라 sender 구현체를 초기화합니다.

- `com.seeker.agent.sender.GrpcChannelHolder`  
  collector gRPC channel을 소유합니다.

- `com.seeker.agent.sender.GrpcSpanTransport`
- `com.seeker.agent.sender.GrpcMetricSender`
- `com.seeker.agent.sender.log.GrpcLogTransport`
- `com.seeker.agent.sender.HttpAgentInfoSender`

- `com.seeker.agent.sender.AsyncSpanDispatcher`
- `com.seeker.agent.sender.log.AsyncLogDispatcher`

- `com.seeker.agent.sender.console.ConsoleSpanTransport`
- `com.seeker.agent.sender.console.ConsoleMetricSender`
- `com.seeker.agent.sender.console.ConsoleLogTransport`
- `com.seeker.agent.sender.console.ConsoleAgentInfoSender`

- `agent-sender/src/main/proto/seeker.proto`

## 실행 흐름

일반 collector mode:

```text
AgentBootstrap
  -> SenderModule
  -> register agent through HTTP
  -> create gRPC channel
  -> create span, metric, log senders
  -> async dispatchers send telemetry to collector
```

Debug mode:

```text
seeker.profiler.debug.enabled=true
  -> SenderModule selects console implementations
  -> no collector channel is created
  -> telemetry is printed locally
```

## 설정

| Key | Default | Description |
| --- | --- | --- |
| `seeker.collector.host` | `127.0.0.1` | collector host |
| `seeker.collector.grpc-port` | `9999` | gRPC telemetry port |
| `seeker.collector.http-port` | `8081` | HTTP registration port |
| `seeker.profiler.debug.enabled` | `false` | collector transport 대신 console sender 사용 |

## Protocol Changes

`seeker.proto`를 변경한다면:

1. Gradle을 통해 generated class를 다시 빌드합니다.
2. collector-side proto와 handler를 업데이트합니다.
3. compatibility impact를 문서화합니다.
4. 새 field 또는 message에 대한 test/sample coverage를 추가합니다.

## 의존성

`agent-sender`는 아래 모듈에 의존합니다.

- `agent-core`: telemetry model과 sender contract
- `agent-config`: collector/debug settings
- collector transport용 gRPC/protobuf libraries

instrumentation plugin에는 의존하지 않아야 합니다.

## 테스트

```bash
./gradlew :agent-sender:test
```

## 주의사항

- application request advice에서 telemetry를 synchronously 전송하지 않습니다.
- 새 dispatch path를 추가할 때 queue와 buffer를 bounded하게 유지합니다.
- 현재 gRPC transport는 plaintext channel creation을 사용합니다.
- debug mode는 sensitive telemetry를 stdout에 출력할 수 있습니다.
- sender failure가 대상 application을 깨뜨리면 안 됩니다.
