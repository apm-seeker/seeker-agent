package com.seeker.agent.metric.collector;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricCollector;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 클래스 로딩 통계를 수집하는 컬렉터.
 *
 * <p>측정 항목:
 * <ul>
 *   <li>{@code jvm.class.loaded_count}        — 현재 로드된 클래스 수 (GAUGE)</li>
 *   <li>{@code jvm.class.total_loaded_count}  — 시작 이후 누적 로드 (CUMULATIVE)</li>
 *   <li>{@code jvm.class.unloaded_count}      — 시작 이후 누적 언로드 (CUMULATIVE)</li>
 * </ul>
 *
 * <p>{@code total_loaded_count}가 운영 중에도 계속 증가하면 동적 클래스 생성(예: dynamic proxy 폭증, 람다 메가모프 사이트)
 * 등을 의심할 수 있는 신호.
 */
public class JvmClassCollector implements MetricCollector {

    private static final String METRIC_NAME = "jvm.class";

    private final ClassLoadingMXBean classBean;

    public JvmClassCollector() {
        this.classBean = ManagementFactory.getClassLoadingMXBean();
    }

    @Override
    public String name() {
        return METRIC_NAME;
    }

    @Override
    public boolean isAvailable() {
        return classBean != null;
    }

    @Override
    public List<Metric> collect(long now) {
        List<Metric> out = new ArrayList<>(3);
        out.add(Metric.gauge(METRIC_NAME, "loaded_count", classBean.getLoadedClassCount(), now));
        out.add(Metric.cumulative(METRIC_NAME, "total_loaded_count",
                classBean.getTotalLoadedClassCount(), now));
        out.add(Metric.cumulative(METRIC_NAME, "unloaded_count",
                classBean.getUnloadedClassCount(), now));
        return out;
    }
}
