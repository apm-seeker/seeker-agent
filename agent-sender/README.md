# agent-sender

[한국어](README.ko.md)

`agent-sender` converts agent telemetry into transport messages and sends it to a collector or console output.

## Role

- Send traces, metrics, logs, and agent registration data.
- Provide gRPC transport implementations.
- Provide console sender implementations for debug mode.
- Convert internal models into protocol messages.
- Isolate network I/O from instrumentation paths through async dispatchers.

## Main Components

- `com.seeker.agent.sender.SenderModule`  
  Initializes sender implementations based on configuration.

- `com.seeker.agent.sender.GrpcChannelHolder`  
  Owns the collector gRPC channel.

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

## Runtime Flow

Normal collector mode:

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

## Configuration

| Key | Default | Description |
| --- | --- | --- |
| `seeker.collector.host` | `127.0.0.1` | Collector host |
| `seeker.collector.grpc-port` | `9999` | gRPC telemetry port |
| `seeker.collector.http-port` | `8081` | HTTP registration port |
| `seeker.profiler.debug.enabled` | `false` | Use console sender instead of collector transport |

## Protocol Changes

If `seeker.proto` changes:

1. Rebuild generated classes through Gradle.
2. Update collector-side proto and handlers.
3. Document compatibility impact.
4. Add tests or sample coverage for the new field or message.

## Dependencies

`agent-sender` depends on:

- `agent-core` for telemetry models and sender contracts
- `agent-config` for collector and debug settings
- gRPC/protobuf libraries for collector transport

It should not depend on instrumentation plugins.

## Tests

```bash
./gradlew :agent-sender:test
```

## Notes

- Do not send telemetry synchronously from application request advice.
- Bound queues and buffers when adding new dispatch paths.
- Current gRPC transport uses plaintext channel creation.
- Debug mode can print sensitive telemetry to stdout.
- Sender failure should not break the target application.
