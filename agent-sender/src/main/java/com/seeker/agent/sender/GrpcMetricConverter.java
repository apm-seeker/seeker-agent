package com.seeker.agent.sender;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricSnapshot;
import com.seeker.agent.core.metric.MetricValueType;
import com.seeker.collector.global.grpc.DataMessage;

/**
 * 도메인 {@link MetricSnapshot}을 gRPC proto {@link DataMessage}로 변환하는 컨버터.
 *
 * <p>{@code GrpcDataMessageConverter}(Span 변환)와 같은 책임 분리 패턴 — sender는 컨버터에 변환을 위임하고
 * 자기는 채널/스트림 라이프사이클만 담당.
 */
public class GrpcMetricConverter {

    /**
     * 도메인 스냅샷을 wire 메시지로 변환한다.
     *
     * <p>변환 규칙:
     * <ul>
     *   <li>4-tuple ID(applicationName, agentId, agentStartTime, timestamp)는 1:1 매핑</li>
     *   <li>{@code collectIntervalMs}는 그대로 — wall-clock measured 의미 보존</li>
     *   <li>각 {@link Metric}은 {@code MetricPoint}로 변환되고 tags는 그대로 복사</li>
     *   <li>null 문자열은 빈 문자열로 보정 (proto3는 null을 표현 못함)</li>
     * </ul>
     */
    public DataMessage toDataMessage(MetricSnapshot snapshot) {
        com.seeker.collector.global.grpc.MetricSnapshot.Builder builder =
                com.seeker.collector.global.grpc.MetricSnapshot.newBuilder()
                        .setApplicationName(nullSafe(snapshot.getApplicationName()))
                        .setAgentId(nullSafe(snapshot.getAgentId()))
                        .setTimestamp(snapshot.getTimestamp())
                        .setCollectIntervalMs(snapshot.getCollectIntervalMs());

        for (Metric m : snapshot.getMetrics()) {
            builder.addPoints(toProtoPoint(m));
        }

        return DataMessage.newBuilder()
                .setMetricSnapshot(builder.build())
                .build();
    }

    /**
     * 도메인 {@link Metric} → proto {@code MetricPoint}.
     */
    private com.seeker.collector.global.grpc.MetricPoint toProtoPoint(Metric m) {
        com.seeker.collector.global.grpc.MetricPoint.Builder pb =
                com.seeker.collector.global.grpc.MetricPoint.newBuilder()
                        .setMetricName(nullSafe(m.getMetricName()))
                        .setFieldName(nullSafe(m.getFieldName()))
                        .setValue(m.getValue())
                        .setType(toProtoType(m.getType()));

        // tags는 immutable map. 비어있어도 putAllTags(emptyMap)는 안전.
        if (m.getTags() != null && !m.getTags().isEmpty()) {
            pb.putAllTags(m.getTags());
        }
        return pb.build();
    }

    /**
     * 도메인 enum → proto enum 매핑.
     * 새 값이 도메인에 추가되면 컴파일러가 default 분기로 떨어지는 걸 잡아 알려줌(switch exhaustive 보장 안 됨에 유의).
     */
    private com.seeker.collector.global.grpc.MetricValueType toProtoType(MetricValueType t) {
        if (t == null) return com.seeker.collector.global.grpc.MetricValueType.GAUGE;
        switch (t) {
            case GAUGE:      return com.seeker.collector.global.grpc.MetricValueType.GAUGE;
            case CUMULATIVE: return com.seeker.collector.global.grpc.MetricValueType.CUMULATIVE;
            case DELTA:      return com.seeker.collector.global.grpc.MetricValueType.DELTA;
            case WINDOW:     return com.seeker.collector.global.grpc.MetricValueType.WINDOW;
            default:         return com.seeker.collector.global.grpc.MetricValueType.GAUGE;
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
