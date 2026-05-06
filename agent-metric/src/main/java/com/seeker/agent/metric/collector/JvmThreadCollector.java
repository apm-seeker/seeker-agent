package com.seeker.agent.metric.collector;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricCollector;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * 스레드 개수를 수집하는 컬렉터.
 *
 * <p>측정 항목:
 * <ul>
 *   <li>{@code jvm.thread.count}        — 현재 스레드 수 (GAUGE)</li>
 *   <li>{@code jvm.thread.daemon_count} — 데몬 스레드 수 (GAUGE)</li>
 *   <li>{@code jvm.thread.peak_count}   — 시작 이후 피크 스레드 수 (CUMULATIVE 성격이지만 표시는 GAUGE)</li>
 *   <li>{@code jvm.thread.total_started_count} — 시작 이후 누적 생성 스레드 수 (CUMULATIVE)</li>
 * </ul>
 *
 * <p>스레드 leak 의심 상황 분석에 유용 — peak가 계속 우상향한다면 스레드를 정리하지 않는 코드 의심.
 */
public class JvmThreadCollector implements MetricCollector {

    private static final String METRIC_NAME = "jvm.thread";

    private final ThreadMXBean threadBean;

    public JvmThreadCollector() {
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    public String name() {
        return METRIC_NAME;
    }

    @Override
    public boolean isAvailable() {
        return threadBean != null;
    }

    @Override
    public List<Metric> collect(long now) {
        List<Metric> out = new ArrayList<>(4);
        out.add(Metric.gauge(METRIC_NAME, "count", threadBean.getThreadCount(), now));
        out.add(Metric.gauge(METRIC_NAME, "daemon_count", threadBean.getDaemonThreadCount(), now));
        out.add(Metric.gauge(METRIC_NAME, "peak_count", threadBean.getPeakThreadCount(), now));
        out.add(Metric.cumulative(METRIC_NAME, "total_started_count",
                threadBean.getTotalStartedThreadCount(), now));
        return out;
    }
}
