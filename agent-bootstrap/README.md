# agent-bootstrap

[한국어](README.ko.md)

`agent-bootstrap` is the JVM entry point for Seeker Agent. It owns `premain`, wires the runtime modules, installs built-in plugins, and produces the final shadow jar used with `-javaagent`.

## Role

- Exposes the Java agent entry point.
- Loads `seeker.config`.
- Initializes agent identity and sender implementations.
- Starts metric collection.
- Installs Byte Buddy instrumentation plugins.
- Packages all runtime dependencies into a single attachable jar.

## Main Components

- `com.seeker.agent.bootstrap.AgentMain`  
  Java agent `Premain-Class`. The JVM calls this through `premain` before the target application starts.

- `com.seeker.agent.bootstrap.lifecycle.AgentBootstrap`  
  Coordinates config, sender, metric, and instrumentation initialization.

- `com.seeker.agent.bootstrap.lifecycle.AgentRuntime`  
  Holds initialized runtime components.

- `com.seeker.agent.bootstrap.plugin.PluginPackInstaller`  
  Registers built-in instrumentation plugins with the instrumentation engine.

## Runtime Flow

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

Agent startup should be fail-open. If the agent cannot initialize, the target application should continue whenever possible.

## Build

Build the final agent jar:

```bash
./gradlew :agent-bootstrap:shadowJar
```

Output:

```text
agent-bootstrap/build/libs/agent-bootstrap-1.0-SNAPSHOT.jar
```

The shadow jar manifest must include:

```text
Premain-Class: com.seeker.agent.bootstrap.AgentMain
```

## Attach Example

```bash
java \
  -javaagent:/path/to/agent-bootstrap-1.0-SNAPSHOT.jar \
  -Dseeker.config=/path/to/seeker.config \
  -jar your-application.jar
```

## Dependencies

`agent-bootstrap` depends on the runtime modules:

- `agent-config`
- `agent-core`
- `agent-instrument`
- `agent-metric`
- `agent-sender`
- built-in modules under `plugins/`

## Extension Points

To add a new built-in plugin:

1. Create a plugin module under `plugins/`.
2. Implement `com.seeker.agent.instrument.plugin.Plugin`.
3. Add the module to `settings.gradle`.
4. Add the dependency to `agent-bootstrap`.
5. Register it in `PluginPackInstaller`.
6. Document the support scope in the plugin README.

## Tests

```bash
./gradlew :agent-bootstrap:test
./gradlew :agent-bootstrap:shadowJar
```

## Notes

- Keep bootstrap code small and defensive.
- Do not let agent exceptions propagate into user application logic.
- Be careful when adding dependencies because the agent runs inside another application's JVM.
- Review dependency relocation and classloader behavior before production distribution.
