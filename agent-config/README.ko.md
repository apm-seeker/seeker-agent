# agent-config

[English](README.md)

`agent-config`는 `seeker.config`를 로드하고 agent의 다른 모듈이 사용하는 typed configuration object를 제공합니다.

## 역할

- classpath resource 또는 외부 파일에서 설정을 로드합니다.
- 기본값을 적용합니다.
- 문자열 property를 typed config class로 변환합니다.
- configuration parsing을 runtime module과 분리합니다.

## 주요 컴포넌트

- `com.seeker.agent.config.SeekerConfig`  
  root configuration object입니다.

- `com.seeker.agent.config.loader.PropertiesLoader`  
  지정된 위치에서 Java properties를 로드합니다.

- `com.seeker.agent.config.properties.AgentIdentityConfig`  
  agent identity와 grouping 설정입니다.

- `com.seeker.agent.config.properties.CollectorConfig`  
  collector host와 port 설정입니다.

- `com.seeker.agent.config.properties.ProfilerConfig`  
  tracing, instrumentation, sampling, debug 설정입니다.

- `com.seeker.agent.config.properties.LogConfig`  
  log collection과 MDC 설정입니다.

## 실행 흐름

```text
AgentMain
  -> PropertiesLoader
  -> SeekerConfig
  -> typed config objects
  -> bootstrap, sender, metric, and plugin initialization
```

## 설정 로딩

외부 설정 파일은 아래 방식으로 지정합니다.

```bash
-Dseeker.config=/path/to/seeker.config
```

외부 경로가 없으면 agent는 application classpath에서 `seeker.config`를 로드할 수 있습니다.

## 설정 키

| Key | Default | Description |
| --- | --- | --- |
| `seeker.agent-identity.name` | agent ID prefix | agent instance 표시 이름 |
| `seeker.agent-identity.group` | empty | 논리 service 또는 group 이름 |
| `seeker.collector.host` | `127.0.0.1` | collector host |
| `seeker.collector.grpc-port` | `9999` | gRPC telemetry port |
| `seeker.collector.http-port` | `8081` | HTTP agent registration port |
| `seeker.profiler.jdbc.enabled` | `true` | JDBC instrumentation 활성화 |
| `seeker.profiler.http.enabled` | `true` | HTTP client instrumentation 활성화 |
| `seeker.profiler.spring.enabled` | `true` | service/Spring-oriented instrumentation 활성화 |
| `seeker.profiler.base-packages` | empty | service method tracing 대상 package prefix 목록 |
| `seeker.profiler.max-span-event-count` | `1500` | 하나의 trace에 담을 수 있는 최대 span event 수 |
| `seeker.profiler.sampling-rate` | `1.0` | trace sampling ratio |
| `seeker.profiler.debug.enabled` | `false` | collector transport 대신 console sender 사용 |
| `seeker.profiler.log.enabled` | `false` | log collection 활성화 |
| `seeker.profiler.log.logback.enabled` | `true` | Logback plugin 활성화 |
| `seeker.profiler.log.min-level` | `ERROR` | 수집할 최소 log level |
| `seeker.profiler.log.only-traced` | `true` | active trace context가 있는 log만 수집 |
| `seeker.profiler.log.mdc.enabled` | `false` | MDC capture 활성화 |
| `seeker.profiler.log.mdc.keys` | empty | 수집할 MDC key allowlist |
| `seeker.metric.enabled` | `true` | metric collection 활성화 |
| `seeker.metric.interval.ms` | `5000` | metric collection interval |
| `seeker.metric.batch.size` | `6` | sender batch당 metric cycle 수 |

## 예시

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

## 의존성

`agent-config`는 instrumentation이나 sender 구현체와 독립적으로 유지해야 합니다. 다른 모듈이 config에 의존하는 것은 괜찮지만, config가 runtime module에 의존하면 안 됩니다.

## 테스트

```bash
./gradlew :agent-config:test
```

## 주의사항

- 기본값은 로컬 개발에 보수적으로 맞춥니다.
- 새로운 public config key는 이 README와, 사용자 동작에 영향을 주는 경우 root README에도 문서화합니다.
- bootstrap이나 plugin module에 parsing logic을 두지 말고 이 모듈에 추가합니다.
