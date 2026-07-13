# agent-metric

[한국어](README.ko.md)

`agent-metric` collects JVM and system metrics on a fixed interval and sends them through the metric sender contract.

## Role

- Register built-in metric collectors.
- Run scheduled collection.
- Batch metric snapshots.
- Forward metric batches to `MetricSender`.

## Main Components

- `com.seeker.agent.metric.MetricModule`  
  Initializes metric collection based on configuration.

- `com.seeker.agent.metric.scheduler.MetricScheduler`  
  Runs collectors on a configured interval and sends batches.

- `com.seeker.agent.metric.collector.JvmGcCollector`
- `com.seeker.agent.metric.collector.JvmMemoryCollector`
- `com.seeker.agent.metric.collector.JvmThreadCollector`
- `com.seeker.agent.metric.collector.JvmClassCollector`
- `com.seeker.agent.metric.collector.SystemCpuCollector`
- `com.seeker.agent.metric.collector.GcTypeDetector`

## Runtime Flow

```text
AgentBootstrap
  -> MetricModule
  -> register collectors
  -> MetricScheduler starts
  -> collect snapshots every interval
  -> batch N collection cycles
  -> MetricSender sends batch
```

## Collected Metrics

| Collector | Examples |
| --- | --- |
| JVM GC | GC count, GC time, detected GC type |
| JVM memory | heap, non-heap, pool usage |
| JVM thread | live, daemon, peak thread counts |
| JVM class loading | loaded, unloaded, total loaded classes |
| CPU | process CPU and system CPU values |

## Configuration

| Key | Default | Description |
| --- | --- | --- |
| `seeker.metric.enabled` | `true` | Enable or disable all metric collection |
| `seeker.metric.interval.ms` | `5000` | Collection interval in milliseconds |
| `seeker.metric.batch.size` | `6` | Number of collection cycles per sender batch |

Example:

```properties
seeker.metric.enabled=true
seeker.metric.interval.ms=5000
seeker.metric.batch.size=6
```

## Adding A Collector

1. Implement the core metric collector contract.
2. Keep collection non-blocking and low-overhead.
3. Use stable metric names and value types.
4. Register the collector in `MetricModule`.
5. Add tests for metric names, values, and disabled behavior.
6. Document the collector in this README.

## Dependencies

`agent-metric` depends on:

- `agent-core` for metric models and sender contracts
- `agent-config` for metric settings

It should not depend on instrumentation plugins.

## Tests

```bash
./gradlew :agent-metric:test
```

## Notes

- Metric collection runs inside the target application JVM.
- Keep collectors bounded and predictable.
- Avoid expensive filesystem, network, or process calls.
- Metric schema changes can affect collector and UI compatibility.
