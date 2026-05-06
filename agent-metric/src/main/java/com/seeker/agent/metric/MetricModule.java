package com.seeker.agent.metric;

import com.seeker.agent.config.properties.ProfilerConfig;
import com.seeker.agent.core.metric.MetricRegistry;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.sender.MetricSender;
import com.seeker.agent.metric.collector.JvmClassCollector;
import com.seeker.agent.metric.collector.JvmGcCollector;
import com.seeker.agent.metric.collector.JvmMemoryCollector;
import com.seeker.agent.metric.collector.JvmThreadCollector;
import com.seeker.agent.metric.collector.SystemCpuCollector;
import com.seeker.agent.metric.scheduler.MetricScheduler;

import java.io.Closeable;
import java.util.function.Supplier;

/**
 * Metric 모듈의 public lifecycle entry point.
 *
 * <p>JVM metric collector 등록, {@link MetricScheduler} 생성, scheduler 시작/종료를
 * metric 모듈 내부 책임으로 묶는다. bootstrap은 metric collector 목록이나 scheduler
 * 설정 방식을 알지 않고, 설정과 sender 연결만 넘긴다.
 */
public final class MetricModule implements Closeable {

    private final MetricScheduler scheduler;
    private boolean closed;

    private MetricModule(MetricScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static MetricModule startIfEnabled(ProfilerConfig profilerConfig,
                                              Supplier<MetricSender> metricSenderSupplier,
                                              AgentInfo agentInfo) {
        if (!profilerConfig.isMetricEnabled()) {
            return null;
        }

        MetricRegistry registry = new MetricRegistry();
        registry.register(new JvmGcCollector());
        registry.register(new JvmMemoryCollector());
        registry.register(new JvmThreadCollector());
        registry.register(new JvmClassCollector());
        registry.register(new SystemCpuCollector());

        MetricScheduler scheduler = new MetricScheduler(
                registry,
                metricSenderSupplier.get(),
                agentInfo,
                profilerConfig.getMetricIntervalMs(),
                profilerConfig.getMetricBatchSize());
        scheduler.start();

        return new MetricModule(scheduler);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        scheduler.stop();
    }
}
