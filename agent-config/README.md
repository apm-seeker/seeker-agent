# agent-config

[한국어](README.ko.md)

`agent-config` loads `seeker.config` and exposes typed configuration objects used by the rest of the agent.

## Role

- Load configuration from a classpath resource or external file.
- Apply default values.
- Convert string properties into typed config classes.
- Keep configuration parsing separate from runtime modules.

## Main Components

- `com.seeker.agent.config.SeekerConfig`  
  Root configuration object.

- `com.seeker.agent.config.loader.PropertiesLoader`  
  Loads Java properties from the configured location.

- `com.seeker.agent.config.properties.AgentIdentityConfig`  
  Agent identity and grouping.

- `com.seeker.agent.config.properties.CollectorConfig`  
  Collector host and ports.

- `com.seeker.agent.config.properties.ProfilerConfig`  
  Tracing, instrumentation, sampling, and debug settings.

- `com.seeker.agent.config.properties.LogConfig`  
  Log collection and MDC settings.

## Runtime Flow

```text
AgentMain
  -> PropertiesLoader
  -> SeekerConfig
  -> typed config objects
  -> bootstrap, sender, metric, and plugin initialization
```

## Configuration Loading

An external config can be supplied with:

```bash
-Dseeker.config=/path/to/seeker.config
```

If no external path is supplied, the agent can load `seeker.config` from the application classpath.

## Configuration Keys

| Key | Default | Description |
| --- | --- | --- |
| `seeker.agent-identity.name` | agent ID prefix | Display name for this agent instance |
| `seeker.agent-identity.group` | empty | Logical service or group name |
| `seeker.collector.host` | `127.0.0.1` | Collector host |
| `seeker.collector.grpc-port` | `9999` | gRPC telemetry port |
| `seeker.collector.http-port` | `8081` | HTTP agent registration port |
| `seeker.profiler.jdbc.enabled` | `true` | Enable JDBC instrumentation |
| `seeker.profiler.http.enabled` | `true` | Enable HTTP client instrumentation |
| `seeker.profiler.spring.enabled` | `true` | Enable service/Spring-oriented instrumentation |
| `seeker.profiler.base-packages` | empty | Comma-separated package prefixes for service method tracing |
| `seeker.profiler.max-span-event-count` | `1500` | Maximum span events in one trace |
| `seeker.profiler.sampling-rate` | `1.0` | Trace sampling ratio |
| `seeker.profiler.debug.enabled` | `false` | Use console sender instead of collector transport |
| `seeker.profiler.log.enabled` | `false` | Enable log collection |
| `seeker.profiler.log.logback.enabled` | `true` | Enable Logback plugin |
| `seeker.profiler.log.min-level` | `ERROR` | Minimum log level to collect |
| `seeker.profiler.log.only-traced` | `true` | Collect only logs with active trace context |
| `seeker.profiler.log.mdc.enabled` | `false` | Enable MDC capture |
| `seeker.profiler.log.mdc.keys` | empty | Allowlist of MDC keys |
| `seeker.metric.enabled` | `true` | Enable metric collection |
| `seeker.metric.interval.ms` | `5000` | Metric collection interval |
| `seeker.metric.batch.size` | `6` | Number of metric cycles per batch |

## Sample

```properties
seeker.agent-identity.name=order-service
seeker.agent-identity.group=payments

seeker.collector.host=127.0.0.1
seeker.collector.grpc-port=9999
seeker.collector.http-port=8081

seeker.profiler.debug.enabled=true
seeker.profiler.base-packages=com.example.order

seeker.metric.enabled=true
seeker.metric.interval.ms=5000
seeker.metric.batch.size=6
```

## Dependencies

`agent-config` should stay independent from instrumentation and sender implementations. It is safe for other modules to depend on config, but config should not depend on runtime modules.

## Tests

```bash
./gradlew :agent-config:test
```

## Notes

- Keep defaults conservative for local development.
- Document every new public config key in this README and the root README when it affects user behavior.
- Avoid parsing logic in bootstrap or plugin modules; add it here instead.
