# jdbc-plugin

[한국어](README.ko.md)

`jdbc-plugin` instruments JDBC `PreparedStatement` execution and records SQL-related span events.

## Supported Library

- Standard JDBC `PreparedStatement` execution paths
- JDBC drivers used through the standard API

Database vendor-specific behavior may need additional validation.

## What It Captures

- SQL execution span event
- SQL statement text when available
- execution error when thrown
- method type used by the trace model

## Instrumentation Targets

Main classes:

- `JdbcPlugin`
- `ConnectionPrepareStatementInterceptor`
- `PreparedStatementExecuteInterceptor`

The plugin captures SQL metadata when a statement is prepared and records execution when the prepared statement runs.

## Runtime Flow

```text
application prepares SQL
  -> SQL metadata is associated with PreparedStatement
application executes PreparedStatement
  -> interceptor starts JDBC SpanEvent
  -> SQL attributes are added
  -> JDBC call proceeds
  -> interceptor records error if thrown
  -> SpanEvent is finished
```

## Configuration

```properties
seeker.profiler.jdbc.enabled=true
```

## Limitations

- SQL parameter masking is not complete.
- Raw SQL text may contain sensitive information.
- Statement, CallableStatement, batch execution, and vendor-specific wrappers may need separate coverage.
- Connection pool instrumentation is not handled here.

## Tests Or Sample

Use `seeker-test` or `seeker-test2` with their local MySQL Docker setup. The sample credentials are local-only and must not be reused in production.
