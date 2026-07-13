# Contributing to Seeker Agent

Seeker Agent is a Java `-javaagent` APM project. Contributions should be careful about application safety, dependency isolation, and runtime overhead.

## Development Setup

Requirements:

- JDK 17 or newer
- Gradle wrapper from this repository
- Optional: Docker for local sample applications
- Optional: a local Seeker collector for end-to-end transport testing

Run tests:

```bash
./gradlew test
```

Build the final agent jar:

```bash
./gradlew :agent-bootstrap:shadowJar
```

Compile a focused module:

```bash
./gradlew :agent-bootstrap:compileJava
./gradlew :agent-instrument:compileJava
```

## Project Layout

```text
agent-bootstrap   JVM premain, lifecycle wiring, final shadow jar
agent-config      seeker.config loading and typed config
agent-core        trace/span/metric/log domain models and holders
agent-instrument  Byte Buddy engine and interceptor registry
agent-metric      JVM/system metric collection
agent-sender      gRPC/console sender implementations
plugins/*         built-in instrumentation plugins
seeker-test*      local sample applications
```

Read the relevant module README before changing a module.

## Pull Request Checklist

Before opening a PR:

- Run the relevant Gradle task locally.
- Add or update tests for behavior changes.
- Update docs when changing public behavior, config, protocol, supported integrations, or limitations.
- Keep changes scoped to one feature or fix.
- Do not introduce blocking network I/O on application request threads.
- Do not let agent exceptions propagate into user application logic.
- Mention compatibility impact for protocol, model, or plugin target changes.

## Agent Safety Guidelines

Agent code runs inside another JVM. Keep these rules:

- Prefer fail-open behavior. Monitoring failure must not break business logic.
- Avoid heavy work in Byte Buddy advice paths.
- Use async queues for network transmission.
- Bound queues and buffers.
- Avoid logging loops. Use guard patterns for log collection.
- Be explicit about supported framework versions.
- Keep dependency conflicts in mind when adding libraries.

## Adding A Plugin

1. Create a module under `plugins/`.
2. Implement `com.seeker.agent.instrument.plugin.Plugin`.
3. Register an `AroundInterceptor` with `InterceptorRegistry`.
4. Use `BaseTransformer` for method advice.
5. Add the module to `settings.gradle`.
6. Add the plugin dependency to `agent-bootstrap`.
7. Register the plugin in `PluginPackInstaller`.
8. Add or update tests.
9. Document support scope, target classes, config keys, and limitations in the plugin README.

## Configuration Changes

If you add, rename, or change a config key:

- update `agent-config/README.md`
- update the root `README.md` when the key affects user behavior
- add tests for default values and parsing
- describe migration impact in `CHANGELOG.md`

## Protocol Changes

If you change `agent-sender/src/main/proto/seeker.proto`:

- regenerate or compile proto classes through Gradle
- update collector-side proto and handlers
- document backward compatibility implications
- add tests or sample coverage

## Documentation Changes

Update documentation when a change affects:

- startup or build commands
- supported frameworks or versions
- config keys
- telemetry fields
- plugin behavior
- known limitations
- security or privacy behavior

## Issue Reporting

Please include:

- Java version
- framework and version, for example Spring Boot, Tomcat, Logback
- agent config with secrets removed
- startup logs
- reproduction steps
- expected behavior
- actual behavior
