package com.seeker.agent.core.model;

import com.seeker.agent.core.context.TraceId;
import java.util.ArrayList;
import java.util.List;

/**
 * 하나의 서버에서 요청을 처리하는 단위를 표현하는 클래스.
 */
public class Span {

    private final TraceId traceId;
    private final long startTime;
    private int elapsedTime;
    private final String remoteAddr;
    private final List<SpanEvent> spanEventList = new ArrayList<>();

    public Span(TraceId traceId, String remoteAddr) {
        this.traceId = traceId;
        this.startTime = System.currentTimeMillis();
        this.remoteAddr = remoteAddr;
    }

    public void finish() {
        this.elapsedTime = (int) (System.currentTimeMillis() - this.startTime);
    }

    public void addSpanEvent(SpanEvent event) {
        this.spanEventList.add(event);
    }

    public TraceId getTraceId() {
        return traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getElapsedTime() {
        return elapsedTime;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public List<SpanEvent> getSpanEventList() {
        return spanEventList;
    }

    @Override
    public String toString() {
        return "Span{" +
                "traceId=" + traceId +
                ", startTime=" + startTime +
                ", elapsedTime=" + elapsedTime +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", spanEventCount=" + spanEventList.size() +
                '}';
    }
}
