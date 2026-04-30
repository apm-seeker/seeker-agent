package com.seeker.agent.metric.collector;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricCollector;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GC count / time을 수집하는 컬렉터.
 *
 * <p>측정 항목:
 * <ul>
 *   <li>{@code jvm.gc.count}   — 누적 GC 횟수 (CUMULATIVE — 서버에서 차분)</li>
 *   <li>{@code jvm.gc.time_ms} — 누적 GC 소요 시간 (CUMULATIVE)</li>
 * </ul>
 *
 * <p>tags:
 * <ul>
 *   <li>{@code gc_type} — {@link GcTypeDetector.GcType}으로 분류된 알고리즘 (예: "g1", "zgc")</li>
 *   <li>{@code gc_name} — MXBean 원본 이름 (예: "G1 Young Generation")</li>
 * </ul>
 *
 * <p>차트는 {@code gc_type} tag로 GC 알고리즘별 시리즈 분리, {@code gc_name}으로 young/old 구분 가능.
 */
public class JvmGcCollector implements MetricCollector {

    private static final String METRIC_NAME = "jvm.gc";

    private final List<GarbageCollectorMXBean> gcBeans;

    public JvmGcCollector() {
        // 부팅 시 1회 lookup. JVM 가동 중 GC bean 목록은 변하지 않는다.
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    @Override
    public String name() {
        return METRIC_NAME;
    }

    @Override
    public boolean isAvailable() {
        return gcBeans != null && !gcBeans.isEmpty();
    }

    @Override
    public List<Metric> collect(long now) {
        List<Metric> out = new ArrayList<>(gcBeans.size() * 2);
        for (GarbageCollectorMXBean bean : gcBeans) {
            // 매 GC bean마다 tags를 따로 만들어 어떤 GC인지 식별 가능하게.
            Map<String, String> tags = new LinkedHashMap<>(2);
            tags.put("gc_type", GcTypeDetector.classify(bean.getName()).label());
            tags.put("gc_name", bean.getName());

            // CUMULATIVE: 누적값을 그대로 보내고, 서버가 차분을 계산.
            out.add(Metric.cumulative(METRIC_NAME, "count", bean.getCollectionCount(), now, tags));
            out.add(Metric.cumulative(METRIC_NAME, "time_ms", bean.getCollectionTime(), now, tags));
        }
        return out;
    }
}
