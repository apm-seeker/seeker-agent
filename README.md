# seeker-agent

[한국어](README.ko.md)

`seeker-agent` is a Java `-javaagent` APM agent for observing JVM applications without changing application code.

It attaches to a target JVM, instruments supported libraries with Byte Buddy, keeps trace context in-process, collects JVM metrics, correlates Logback events with trace/span IDs, and sends telemetry to a Seeker collector through gRPC.

> Project status: early-stage. The agent is suitable for local experiments and controlled test environments. Review the limitations and security notes before production use.

## Features

- Java `-javaagent` attachment
- Tomcat inbound request tracing
- Apache HttpClient 4.x outbound call tracing with W3C Trace Context injection
- JDBC `PreparedStatement` tracing
- package-prefix based service method tracing
- JVM and system metrics for GC, memory, thread, class loading, and CPU
- Logback log correlation with `traceId` and `spanId`
- gRPC span, metric, and log transport
- console sender debug mode for local inspection

## Requirements

- JDK 17 or newer for building this repository
- Gradle wrapper included in this repository
- A target JVM application that can be started with `-javaagent`
- Optional: Docker for sample applications
- Optional: a Seeker collector endpoint for gRPC and agent registration

## Quick Start

### 1. Build the agent jar

```bash
./gradlew :agent-bootstrap:shadowJar
```

The agent jar is created at:

```text
agent-bootstrap/build/libs/agent-bootstrap-1.0-SNAPSHOT.jar
```

### 2. Attach it to an application

```bash
java \
  -javaagent:/path/to/agent-bootstrap-1.0-SNAPSHOT.jar \
  -Dseeker.config=/path/to/seeker.config \
  -jar your-application.jar
```

### 3. Use debug mode without a collector

When `seeker.profiler.debug.enabled=true`, the agent does not create a gRPC channel and prints collected telemetry to the console sender.

```properties
seeker.profiler.debug.enabled=true
```

This is the recommended first check for local development.

## Configuration

`seeker.config` can be loaded from the application classpath or from an external path through `-Dseeker.config=/path/to/seeker.config`.

```properties
# Agent identity
seeker.agent-identity.name=order-service
seeker.agent-identity.group=payments

# Collector
seeker.collector.host=127.0.0.1
seeker.collector.grpc-port=9999
seeker.collector.http-port=8081

# Instrumentation
seeker.profiler.jdbc.enabled=true
seeker.profiler.http.enabled=true
seeker.profiler.spring.enabled=true
seeker.profiler.base-packages=com.example.order,com.example.common
seeker.profiler.max-span-event-count=1500
seeker.profiler.sampling-rate=1.0
seeker.profiler.debug.enabled=false

# Logs
seeker.profiler.log.enabled=false
seeker.profiler.log.logback.enabled=true
seeker.profiler.log.min-level=ERROR
seeker.profiler.log.only-traced=true
seeker.profiler.log.mdc.enabled=false
seeker.profiler.log.mdc.keys=

# Metrics
seeker.metric.enabled=true
seeker.metric.interval.ms=5000
seeker.metric.batch.size=6
```

For the full configuration contract, see [agent-config/README.md](agent-config/README.md).

## Project Layout

```text
agent-bootstrap   JVM premain entry point, lifecycle wiring, final shadow jar
agent-config      seeker.config loading and typed configuration
agent-core        trace, span, metric, log models, context, propagation, sender interfaces
agent-instrument  Byte Buddy instrumentation engine and interceptor contracts
agent-metric      JVM and system metric collectors
agent-sender      gRPC and console transport implementations
plugins/          built-in instrumentation plugins
seeker-test       local sample application
seeker-test2      local multi-service sample application
```

## Module Documentation

- [agent-bootstrap](agent-bootstrap/README.md)
- [agent-config](agent-config/README.md)
- [agent-core](agent-core/README.md)
- [agent-instrument](agent-instrument/README.md)
- [agent-metric](agent-metric/README.md)
- [agent-sender](agent-sender/README.md)
- [tomcat-plugin](plugins/tomcat-plugin/README.md)
- [httpclient-plugin](plugins/httpclient-plugin/README.md)
- [jdbc-plugin](plugins/jdbc-plugin/README.md)
- [service-plugin](plugins/service-plugin/README.md)
- [logback-plugin](plugins/logback-plugin/README.md)

## Supported Scope

Supported:

- Java `-javaagent` startup
- Tomcat request tracing
- Apache HttpClient 4.x tracing
- JDBC `PreparedStatement` tracing
- package-prefix based public service method tracing
- W3C Trace Context propagation
- Logback log correlation
- JVM and system metric collection
- gRPC telemetry transport

Not yet supported:

- Jetty, Undertow, WebFlux, and reactive server instrumentation
- Java 11 HttpClient, OkHttp, and WebClient plugins
- Log4j2 and JUL plugins
- async executor and reactive context propagation
- complete OpenTelemetry compatibility
- production-grade reconnect, retry, and sender self-metrics
- complete SQL/log masking policy

See [docs/supported-matrix.md](docs/supported-matrix.md) and [docs/limitations.md](docs/limitations.md).

## Local Samples

The sample applications are for local development only. Their Docker and Spring configuration may contain simple local credentials such as `root` or `password`; do not reuse them in production.

See [docs/sample-scenario.md](docs/sample-scenario.md) for scenario-oriented testing.

## Development

Run all tests:

```bash
./gradlew test
```

Build the final agent jar:

```bash
./gradlew :agent-bootstrap:shadowJar
```

Compile a focused module:

```bash
./gradlew :agent-instrument:compileJava
```

Before changing instrumentation code, read [agent-instrument/README.md](agent-instrument/README.md) and the plugin README for the library you are touching.

## Security And Privacy

Agent telemetry can contain URLs, SQL statements, log messages, MDC values, headers, or application identifiers depending on configuration and instrumentation scope.

Current security limitations include:

- gRPC transport currently uses plaintext channel creation.
- SQL and log masking policies are incomplete.
- MDC collection is allowlist-based but must still be configured carefully.
- debug mode may print telemetry to stdout.

Report sensitive security issues through the process in [SECURITY.md](SECURITY.md).

## Contributing

Contributions are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md), and keep agent safety in mind: instrumentation failure must not break the target application.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
