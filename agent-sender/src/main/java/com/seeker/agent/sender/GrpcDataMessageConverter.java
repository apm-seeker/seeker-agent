package com.seeker.agent.sender;

import com.seeker.agent.core.model.Span;
import com.seeker.agent.core.model.SpanEvent;
import com.seeker.collector.global.grpc.DataMessage;
import com.seeker.collector.global.grpc.TraceId;

/**
 * 프로젝트 내의 Span 모델을 gRPC 통신을 위한 Protobuf 메시지로 변환하는 클래스입니다.
 */
public class GrpcDataMessageConverter {

    private final String applicationName;
    private final String agentId;

    public GrpcDataMessageConverter(String applicationName, String agentId) {
        this.applicationName = applicationName != null ? applicationName : "Unknown-App";
        this.agentId = agentId != null ? agentId : "Unknown-Agent";
    }

    public DataMessage toDataMessage(Span span) {
        com.seeker.collector.global.grpc.Span.Builder spanBuilder = com.seeker.collector.global.grpc.Span.newBuilder()
                .setTraceId(TraceId.newBuilder()
                        .setTraceId(span.getTraceId().getTraceId())
                        .setSpanId(span.getTraceId().getSpanId())
                        .setParentSpanId(span.getTraceId().getParentSpanId())
                        .build())
                .setStartTime(span.getStartTime())
                .setElapsedTime(span.getElapsedTime())
                .setRemoteAddr(span.getRemoteAddr() != null ? span.getRemoteAddr() : "")
                .setUri(span.getUri() != null ? span.getUri() : "")
                .setEndPoint(span.getEndPoint() != null ? span.getEndPoint() : "")
                .setServiceType(span.getServiceType())
                .setApplicationName(applicationName)
                .setAgentId(agentId);

        if (span.getExceptionInfo() != null) {
            spanBuilder.setExceptionInfo(span.getExceptionInfo());
        }

        for (SpanEvent event : span.getSpanEventList()) {
            spanBuilder.addSpanEventList(toSpanEventMessage(event));
        }

        return DataMessage.newBuilder()
                .setSpan(spanBuilder.build())
                .build();
    }

    private com.seeker.collector.global.grpc.SpanEvent toSpanEventMessage(SpanEvent event) {
        com.seeker.collector.global.grpc.SpanEvent.Builder eventBuilder = com.seeker.collector.global.grpc.SpanEvent.newBuilder()
                .setSequence(event.getSequence())
                .setDepth(event.getDepth())
                .setStartTime(event.getStartTime())
                .setElapsedTime(event.getElapsedTime())
                .setMethodType(event.getMethodType())
                .setExceptionInfo(event.getException() != null ? event.getException() : "");

        if (event.getAttributes() != null) {
            eventBuilder.putAllAttributes(event.getAttributes());
        }

        return eventBuilder.build();
    }
}
