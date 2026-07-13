# jdbc-plugin

[English](README.md)

`jdbc-plugin`은 JDBC `PreparedStatement` execution을 instrument하고 SQL 관련 span event를 기록합니다.

## 지원 라이브러리

- 표준 JDBC `PreparedStatement` execution path
- 표준 API를 통해 사용되는 JDBC driver

Database vendor-specific behavior는 추가 검증이 필요할 수 있습니다.

## 수집 항목

- SQL execution span event
- 가능한 경우 SQL statement text
- thrown execution error
- trace model에서 사용하는 method type

## Instrumentation Targets

주요 클래스:

- `JdbcPlugin`
- `ConnectionPrepareStatementInterceptor`
- `PreparedStatementExecuteInterceptor`

이 plugin은 statement가 준비될 때 SQL metadata를 저장하고, prepared statement가 실행될 때 execution을 기록합니다.

## 실행 흐름

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

## 설정

```properties
seeker.profiler.jdbc.enabled=true
```

## 제한사항

- SQL parameter masking은 완성되어 있지 않습니다.
- raw SQL text에 민감정보가 포함될 수 있습니다.
- Statement, CallableStatement, batch execution, vendor-specific wrapper는 별도 coverage가 필요할 수 있습니다.
- connection pool instrumentation은 이 plugin에서 다루지 않습니다.

## 테스트 또는 샘플

로컬 MySQL Docker 설정과 함께 `seeker-test` 또는 `seeker-test2`를 사용합니다. 샘플 credential은 local-only이며 운영 환경에서 재사용하면 안 됩니다.
