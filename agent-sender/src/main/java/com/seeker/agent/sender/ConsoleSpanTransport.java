package com.seeker.agent.sender;

import com.seeker.agent.core.model.MethodType;
import com.seeker.agent.core.model.Span;
import com.seeker.agent.core.model.SpanEvent;

/**
 * Span을 콘솔에 트리 형태로 출력하는 SpanTransport 구현체입니다.
 * 디버그 모드에서 Collector 통신 없이 수집 데이터를 확인하는 용도로 사용됩니다.
 */
public class ConsoleSpanTransport implements SpanTransport {

    public ConsoleSpanTransport() {
        System.out.println("[Seeker] ConsoleSpanTransport 초기화 (collector 통신 없음)");
    }

    @Override
    public void send(Span span) {
        System.out.println("[Seeker-DEBUG][SPAN]\n" + format(span));
    }

    @Override
    public void close() {
        // no-op
    }

    private String format(Span span) {
        StringBuilder sb = new StringBuilder();
        sb.append("Span{")
                .append("traceId=").append(span.getTraceId())
                .append(", agentId=").append(span.getAgentId())
                .append(", uri=").append(span.getUri())
                .append(", endPoint=").append(span.getEndPoint())
                .append(", serviceType=").append(span.getServiceType())
                .append(", remoteAddr='").append(span.getRemoteAddr()).append('\'')
                .append(", startTime=").append(span.getStartTime())
                .append(", elapsedTime=").append(span.getElapsedTime()).append("ms")
                .append(", spanEventCount=").append(span.getSpanEventList().size());
        if (span.getExceptionInfo() != null) {
            sb.append(", exceptionInfo=").append(span.getExceptionInfo());
        }
        sb.append('}');
        for (SpanEvent event : span.getSpanEventList()) {
            sb.append('\n').append(formatEvent(event));
        }
        return sb.toString();
    }

    private String formatEvent(SpanEvent event) {
        StringBuilder indent = new StringBuilder();
        for (int i = 1; i < event.getDepth(); i++) {
            indent.append("  ");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("└ SpanEvent#").append(event.getSequence())
                .append(" depth=").append(event.getDepth())
                .append(" type=").append(MethodType.fromCode(event.getMethodType()).name())
                .append(" elapsed=").append(event.getElapsedTime()).append("ms");
        if (event.getAttributes() != null && !event.getAttributes().isEmpty()) {
            sb.append(" attributes=").append(event.getAttributes());
        }
        if (event.getException() != null) {
            sb.append(" exception=").append(event.getException());
        }
        return sb.toString();
    }
}
