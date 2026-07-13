# agent-metric

[English](README.md)

`agent-metric`은 고정 interval로 JVM/system metric을 수집하고 metric sender contract를 통해 전송합니다.

## 역할

- built-in metric collector를 등록합니다.
- scheduled collection을 실행합니다.
- metric snapshot을 batch로 묶습니다.
- metric batch를 `MetricSender`로 전달합니다.

## 주요 컴포넌트

- `com.seeker.agent.metric.MetricModule`  
  설정에 따라 metric collection을 초기화합니다.

- `com.seeker.agent.metric.scheduler.MetricScheduler`  
  configured interval로 collector를 실행하고 batch를 전송합니다.

- `com.seeker.agent.metric.collector.JvmGcCollector`
- `com.seeker.agent.metric.collector.JvmMemoryCollector`
- `com.seeker.agent.metric.collector.JvmThreadCollector`
- `com.seeker.agent.metric.collector.JvmClassCollector`
- `com.seeker.agent.metric.collector.SystemCpuCollector`
- `com.seeker.agent.metric.collector.GcTypeDetector`

## 실행 흐름

```text
AgentBootstrap
  -> MetricModule
  -> register collectors
  -> MetricScheduler starts
  -> collect snapshots every interval
  -> batch N collection cycles
  -> MetricSender sends batch
```

## 수집 Metric

| Collector | Examples |
| --- | --- |
| JVM GC | GC count, GC time, detected GC type |
| JVM memory | heap, non-heap, pool usage |
| JVM thread | live, daemon, peak thread counts |
| JVM class loading | loaded, unloaded, total loaded classes |
| CPU | process CPU and system CPU values |

## 설정

| Key | Default | Description |
| --- | --- | --- |
| `seeker.metric.enabled` | `true` | metric collection 전체 활성화 |
| `seeker.metric.interval.ms` | `5000` | collection interval in milliseconds |
| `seeker.metric.batch.size` | `6` | sender batch당 collection cycle 수 |

예시:

```properties
seeker.metric.enabled=true
seeker.metric.interval.ms=5000
seeker.metric.batch.size=6
```

## Collector 추가

1. core metric collector contract를 구현합니다.
2. collection은 non-blocking, low-overhead로 유지합니다.
3. stable metric name과 value type을 사용합니다.
4. `MetricModule`에 collector를 등록합니다.
5. metric name, value, disabled behavior 테스트를 추가합니다.
6. 이 README에 collector를 문서화합니다.

## 의존성

`agent-metric`은 아래 모듈에 의존합니다.

- `agent-core`: metric model과 sender contract
- `agent-config`: metric settings

instrumentation plugin에는 의존하지 않아야 합니다.

## 테스트

```bash
./gradlew :agent-metric:test
```

## 주의사항

- metric collection은 대상 application JVM 안에서 실행됩니다.
- collector는 bounded하고 predictable해야 합니다.
- expensive filesystem, network, process call을 피합니다.
- metric schema 변경은 collector와 UI compatibility에 영향을 줄 수 있습니다.
