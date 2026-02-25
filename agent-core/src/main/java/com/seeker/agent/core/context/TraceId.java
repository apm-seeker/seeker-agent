package com.seeker.agent.core.context;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 분산 추적의 핵심 식별자 묶음.
 * traceId(전체 요청 체인), spanId(현재 서버 호출), parentSpanId(나를 호출한 서버의 spanId)를 포함합니다.
 */
public class TraceId {

    private final String traceId;
    private final long spanId;
    private final long parentSpanId;

    /**
     * root Span일때의 Trace를 생성할때 해당 생성자를 사용한다.
     */
    // TODO traceId 를 생성을 할때는 UUID를 사용하지 않고 각 ID의 조합으로 수정을 한다.
    public TraceId() {
        this(UUID.randomUUID().toString(), generateId(), -1);
    }

    /**
     * trace가 전파되어선 왔을때의 기존 traceId에 맞게 생성을 해준다.
     */
    public TraceId(String traceId, long spanId, long parentSpanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    /**
     * 다음 서버로 전파할 때 사용할 새로운 TraceId를 생성합니다.
     * traceId는 유지되고, 현재 spanId가 부모 spanId가 되며, 새로운 spanId를 생성합니다.
     */
    public TraceId getNextTraceId() {
        return new TraceId(traceId, generateId(), spanId);
    }

    public String getTraceId() {
        return traceId;
    }

    public long getSpanId() {
        return spanId;
    }

    public long getParentSpanId() {
        return parentSpanId;
    }



    private static long generateId() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public String toString() {
        return "TraceId{" +
                "traceId='" + traceId + '\'' +
                ", spanId=" + spanId +
                ", parentSpanId=" + parentSpanId +
                '}';
    }
}
