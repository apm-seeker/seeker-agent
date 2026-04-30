package com.seeker.agent.core.metric;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 한 메트릭 값을 표현하는 불변 도메인 객체.
 *
 * <p>예시:
 * <pre>
 *   metricName="jvm.gc"     fieldName="heap_used"        value=12345678   type=GAUGE
 *   metricName="jvm.gc"     fieldName="count"            value=42         type=CUMULATIVE
 *                                                                          tags={gc_type:"G1_OLD"}
 *   metricName="system.cpu" fieldName="jvm_load"         value=0.32       type=GAUGE
 * </pre>
 *
 * <p>{@code metricName} + {@code fieldName} 조합이 식별자 — 차트는 이 둘을 합쳐 "jvm.gc.heap_used"로 표시.
 * {@code tags}는 같은 fieldName 내에서 차원 분해(예: GC 종류별)에 사용.
 */
public final class Metric {

    private final String metricName;
    private final String fieldName;
    private final double value;
    private final MetricValueType type;
    private final long timestamp;
    private final Map<String, String> tags;

    public Metric(String metricName, String fieldName, double value,
                  MetricValueType type, long timestamp, Map<String, String> tags) {
        this.metricName = metricName;
        this.fieldName = fieldName;
        this.value = value;
        this.type = type;
        this.timestamp = timestamp;
        this.tags = (tags == null || tags.isEmpty())
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(tags));
    }

    // -------- factory --------

    /** GAUGE 메트릭 생성 (tags 없음). */
    public static Metric gauge(String metricName, String fieldName, double value, long timestamp) {
        return new Metric(metricName, fieldName, value, MetricValueType.GAUGE, timestamp, null);
    }

    /** GAUGE 메트릭 생성 (tags 포함). */
    public static Metric gauge(String metricName, String fieldName, double value, long timestamp,
                               Map<String, String> tags) {
        return new Metric(metricName, fieldName, value, MetricValueType.GAUGE, timestamp, tags);
    }

    /** CUMULATIVE 메트릭 생성 (서버에서 차분). */
    public static Metric cumulative(String metricName, String fieldName, double value, long timestamp) {
        return new Metric(metricName, fieldName, value, MetricValueType.CUMULATIVE, timestamp, null);
    }

    /** CUMULATIVE 메트릭 생성 (tags 포함). */
    public static Metric cumulative(String metricName, String fieldName, double value, long timestamp,
                                    Map<String, String> tags) {
        return new Metric(metricName, fieldName, value, MetricValueType.CUMULATIVE, timestamp, tags);
    }

    /** DELTA 메트릭 생성 (에이전트에서 이미 차분 처리됨). */
    public static Metric delta(String metricName, String fieldName, double value, long timestamp) {
        return new Metric(metricName, fieldName, value, MetricValueType.DELTA, timestamp, null);
    }

    /** WINDOW 메트릭 생성 (한 cycle 윈도우값). */
    public static Metric window(String metricName, String fieldName, double value, long timestamp) {
        return new Metric(metricName, fieldName, value, MetricValueType.WINDOW, timestamp, null);
    }

    // -------- getters --------

    public String getMetricName() { return metricName; }
    public String getFieldName() { return fieldName; }
    public double getValue() { return value; }
    public MetricValueType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public Map<String, String> getTags() { return tags; }

    @Override
    public String toString() {
        return "Metric{" + metricName + "." + fieldName + "=" + value
                + " (" + type + ")"
                + (tags.isEmpty() ? "" : " tags=" + tags)
                + " ts=" + timestamp + '}';
    }
}
