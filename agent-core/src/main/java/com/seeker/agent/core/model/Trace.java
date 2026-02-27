package com.seeker.agent.core.model;

import com.seeker.agent.core.context.TraceId;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 사용자의 단일 요청을 표현하는 트레이스.
 */
public class Trace {

    private final TraceId traceId;
    private final long startTime;
    private final Span span;
    private final Deque<SpanEvent> spanEventStack = new ArrayDeque<>();

    public Trace(TraceId traceId, long startTime) {
        this.traceId = traceId;
        this.startTime = startTime;

        // 트레이스 생성 시 루트 스팬도 함께 생성
        // TODO : span에 해당하는 IP는 실제 호츌하는 IP값으로 변경 예정
        this.span = new Span(traceId, "127.0.0.1");
    }

    public TraceId getTraceId() {
        return traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public Span getSpan() {
        return span;
    }

    /**
     * 트레이스를 시작할 때 호출합니다.
     * 새로운 SpanEvent를 생성하여 스택에 쌓습니다.
     */
    public void traceBlockBegin() {
        SpanEvent event = new SpanEvent();
        event.markStartTime();
        event.setDepth(spanEventStack.size() + 1);
        spanEventStack.push(event);
    }

    /**
     * 트레이스 블록을 종료할 때 호출합니다.
     * 스택에서 이벤트를 꺼내 처리를 완료하고 스팬에 추가합니다.
     */
    public void traceBlockEnd() {
        SpanEvent event = spanEventStack.poll();
        if (event != null) {
            event.finish();
            event.setSequence(span.getSpanEventList().size());
            span.addSpanEvent(event);
        }
    }

    @Override
    public String toString() {
        return "Trace{" +
                "traceId=" + traceId +
                ", startTime=" + startTime +
                '}';
    }
}
