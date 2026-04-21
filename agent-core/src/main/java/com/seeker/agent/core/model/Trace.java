package com.seeker.agent.core.model;

import com.seeker.agent.core.context.TraceId;
import java.util.ArrayDeque;
import java.util.Deque;
import com.seeker.agent.core.model.MethodType;

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
        // TODO : span에 해당하는 IP는 실제 호출하는 IP값으로 변경 예정
        this.span = new Span(traceId, "127.0.0.1", startTime);
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
     * 트레이스의 모든 작업이 완료되었을 때 호출합니다.
     */
    public void finish() {
        this.span.finish();
        com.seeker.agent.core.sender.DataSenderHolder.getSender().send(this.span);
    }

    /**
     * 트레이스 블록을 시작할 때 호출합니다.
     */
    public void traceBlockBegin() {
        traceBlockBegin(null, null);
    }

    public void traceBlockBegin(String className, String methodName) {
        SpanEvent event = new SpanEvent();
        event.markStartTime();
        event.addAttribute("className", className);
        event.addAttribute("methodName", methodName);
        event.setMethodType(MethodType.USER_METHOD.getCode());
        event.setDepth(spanEventStack.size() + 1);
        spanEventStack.push(event);
    }

    /**
     * 트레이스 블록을 종료할 때 호출합니다.
     */
    public void traceBlockEnd() {
        traceBlockEnd(null);
    }

    public void traceBlockEnd(Throwable throwable) {
        SpanEvent event = spanEventStack.poll();
        if (event != null) {
            event.finish();
            if (throwable != null) {
                event.setException(throwable.toString());
            }
            event.setSequence(span.getSpanEventList().size());
            span.addSpanEvent(event);
        }
    }

    /**
     * 현재 활성화된(가장 최근의) SpanEvent를 반환합니다.
     */
    public SpanEvent currentSpanEvent() {
        return spanEventStack.peek();
    }

    @Override
    public String toString() {
        return "Trace{" +
                "traceId=" + traceId +
                ", startTime=" + startTime +
                '}';
    }
}
