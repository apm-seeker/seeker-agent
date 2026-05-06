package com.seeker.agent.metric.collector;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricCollector;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM 힙/논힙 메모리 사용량을 수집하는 컬렉터.
 *
 * <p>측정 항목:
 * <ul>
 *   <li>{@code jvm.memory.heap_used}      — 현재 사용 중 (GAUGE)</li>
 *   <li>{@code jvm.memory.heap_committed} — JVM이 OS로부터 확보한 힙 (GAUGE)</li>
 *   <li>{@code jvm.memory.heap_max}       — 힙 최대치. {@code -1}이면 committed로 fallback</li>
 *   <li>{@code jvm.memory.nonheap_used}   — Metaspace/CodeCache 등 (GAUGE)</li>
 *   <li>{@code jvm.memory.nonheap_committed}</li>
 *   <li>{@code jvm.memory.nonheap_max}    — 마찬가지로 -1 fallback</li>
 * </ul>
 *
 * <p>{@link MemoryMXBean}은 부팅 시 1회만 lookup해 캐싱 (Pinpoint 패턴).
 */
public class JvmMemoryCollector implements MetricCollector {

    private static final String METRIC_NAME = "jvm.memory";

    private final MemoryMXBean memoryBean;

    public JvmMemoryCollector() {
        // 매 cycle 새로 가져오지 않고 부팅 시 1회 lookup. ManagementFactory는 같은 인스턴스 반환을 보장.
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public String name() {
        return METRIC_NAME;
    }

    @Override
    public boolean isAvailable() {
        // MemoryMXBean은 표준 JDK 제공이므로 사실상 항상 true.
        // 안전망으로 null 체크만.
        try {
            return memoryBean != null && memoryBean.getHeapMemoryUsage() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public List<Metric> collect(long now) {
        List<Metric> out = new ArrayList<>(6);
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        out.add(Metric.gauge(METRIC_NAME, "heap_used", heap.getUsed(), now));
        out.add(Metric.gauge(METRIC_NAME, "heap_committed", heap.getCommitted(), now));
        // max == -1 (unbounded, 예: ZGC Metaspace)이면 committed로 fallback. Pinpoint:78 패턴.
        out.add(Metric.gauge(METRIC_NAME, "heap_max",
                heap.getMax() == -1 ? heap.getCommitted() : heap.getMax(), now));

        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        out.add(Metric.gauge(METRIC_NAME, "nonheap_used", nonHeap.getUsed(), now));
        out.add(Metric.gauge(METRIC_NAME, "nonheap_committed", nonHeap.getCommitted(), now));
        out.add(Metric.gauge(METRIC_NAME, "nonheap_max",
                nonHeap.getMax() == -1 ? nonHeap.getCommitted() : nonHeap.getMax(), now));
        return out;
    }
}
