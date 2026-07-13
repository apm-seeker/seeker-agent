# agent-instrument

[한국어](README.ko.md)

`agent-instrument` contains the Byte Buddy based instrumentation engine and the contracts used by built-in plugins.

## Role

- Configure Byte Buddy transformers.
- Let plugins declare target classes and methods.
- Register interceptors used by advice code.
- Keep instrumentation mechanics separate from plugin-specific behavior.

## Main Components

- `com.seeker.agent.instrument.InstrumentEngine`  
  Installs plugin transformers into the JVM instrumentation instance.

- `com.seeker.agent.instrument.plugin.Plugin`  
  Contract implemented by each instrumentation plugin.

- `com.seeker.agent.instrument.transformer.BaseTransformer`  
  Common transformer for applying around advice to matched methods.

- `com.seeker.agent.instrument.interceptor.AroundInterceptor`  
  Runtime hook for `before` and `after` method interception.

- `com.seeker.agent.instrument.interceptor.InterceptorRegistry`  
  Registry that maps advice names to interceptor instances.

- `com.seeker.agent.instrument.matcher.StandardMatchers`  
  Shared matcher helpers.

## Runtime Flow

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

A plugin should describe:

- which type is instrumented
- which methods are intercepted
- which interceptor handles the runtime logic
- which configuration flag enables or disables it

## Adding A Plugin

1. Create a module under `plugins/`.
2. Implement `Plugin`.
3. Create one or more `AroundInterceptor` implementations.
4. Register interceptors in `InterceptorRegistry`.
5. Return a `BaseTransformer` with a narrow method matcher.
6. Add the module to `settings.gradle`.
7. Add the dependency to `agent-bootstrap`.
8. Register the plugin in `PluginPackInstaller`.
9. Add a plugin README with supported version and limitations.

## Matcher Guidelines

- Match the narrowest class and method set possible.
- Avoid broad package scanning unless the user explicitly configured it.
- Do not instrument agent classes.
- Do not instrument logging paths without recursion guards.
- Be careful with overloaded methods and framework proxy classes.

## Safety Guidelines

Instrumentation runs on application request paths, so it must be defensive.

- Do not perform blocking network I/O inside advice.
- Keep allocations small.
- Catch agent-side exceptions.
- Do not change application return values.
- Do not swallow application exceptions.
- Always close trace blocks in `after` logic.

## Dependencies

`agent-instrument` depends on `agent-core` for trace context and interceptor behavior. It should not depend on specific application frameworks; framework-specific logic belongs in plugins.

## Tests

```bash
./gradlew :agent-instrument:test
```

## Notes

- Bytecode instrumentation failures should degrade observability, not application behavior.
- Any new advice path should be reviewed for overhead.
- Public extension contracts should remain stable for plugin authors.
