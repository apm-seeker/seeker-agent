package com.seeker.agent.core.metric;

import com.seeker.agent.core.model.AgentInfo;

import java.util.Collections;
import java.util.List;

/**
 * 한 cycle에 수집된 모든 메트릭의 묶음. 송신 단위.
 *
 * <p>4-tuple ID(applicationName, agentId, agentStartTime, timestamp)를 갖는 이유는 Pinpoint와 동일.
 * 같은 agentId라도 {@code agentStartTime}이 다르면 재시작된 다른 인스턴스로 취급되어 시계열이 분리된다.
 *
 * <p>{@code collectIntervalMs}는 <strong>wall-clock measured</strong> 값.
 * 스케줄러 인터벌(예: 5000ms)이 아니라 실제 측정된 cycle 간 경과 시간을 담는다.
 * GC pause로 8초가 됐으면 8000을 보낸다 — 서버의 TPS 계산이 정확해지는 핵심 인사이트.
 */
public final class MetricSnapshot {

    private final String applicationName;
    private final String agentId;
    private final long agentStartTime;
    private final long timestamp;
    private final long collectIntervalMs;
    private final List<Metric> metrics;

    public MetricSnapshot(String applicationName, String agentId, long agentStartTime,
                          long timestamp, long collectIntervalMs, List<Metric> metrics) {
        this.applicationName = applicationName;
        this.agentId = agentId;
        this.agentStartTime = agentStartTime;
        this.timestamp = timestamp;
        this.collectIntervalMs = collectIntervalMs;
        this.metrics = (metrics == null) ? Collections.emptyList()
                : Collections.unmodifiableList(metrics);
    }

    /**
     * AgentInfo + 시점 정보로부터 편리하게 생성하는 팩토리.
     */
    public static MetricSnapshot of(AgentInfo agentInfo, long timestamp,
                                    long collectIntervalMs, List<Metric> metrics) {
        return new MetricSnapshot(
                agentInfo.getAgentName(),
                agentInfo.getAgentId(),
                agentInfo.getStartTime(),
                timestamp,
                collectIntervalMs,
                metrics);
    }

    public String getApplicationName() { return applicationName; }
    public String getAgentId() { return agentId; }
    public long getAgentStartTime() { return agentStartTime; }
    public long getTimestamp() { return timestamp; }
    public long getCollectIntervalMs() { return collectIntervalMs; }
    public List<Metric> getMetrics() { return metrics; }

    @Override
    public String toString() {
        return "MetricSnapshot{"
                + "agent=" + agentId
                + " ts=" + timestamp
                + " interval=" + collectIntervalMs + "ms"
                + " count=" + metrics.size() + '}';
    }
}
