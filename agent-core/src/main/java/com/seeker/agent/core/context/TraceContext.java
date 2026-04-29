package com.seeker.agent.core.context;

import com.seeker.agent.core.model.Trace;

public interface TraceContext {

    /**
     * 새로운 루트 trace를 시작한다.
     */
    Trace newTraceObject();

    /**
     * 외부에서 전달된 trace에 합류한다. 자기 spanId는 <strong>로컬에서 새로 생성</strong>되고,
     * 받은 {@code parentSpanId}가 자기 부모로 기록된다.
     *
     * @param traceId      32-char hex 공유 trace 식별자
     * @param parentSpanId 호출자의 spanId (자기 입장에서의 parentSpanId)
     */
    Trace continueTraceObject(String traceId, long parentSpanId);

    Trace currentTraceObject();

    void setTraceObject(Trace trace);

    void removeTraceObject();

    Scope getScope(String name);
}
