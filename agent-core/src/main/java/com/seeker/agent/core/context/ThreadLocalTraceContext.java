package com.seeker.agent.core.context;

import com.seeker.agent.core.model.Trace;

/**
 * Trace의 생명주기 전체를 관리하는 핵심 클래스.
 * ThreadLocal을 사용하여 현재 스레드의 트레이스 정보를 보관합니다.
 */
public class ThreadLocalTraceContext implements TraceContext {

    private final ThreadLocal<Trace> traceHolder = new ThreadLocal<>();

    public ThreadLocalTraceContext() {
    }

    /**
     * 새로운 트레이스를 생성합니다.
     */
    @Override
    public Trace newTraceObject() {
        TraceId traceId = new TraceId();
        Trace trace = new Trace(traceId, System.currentTimeMillis());
        traceHolder.set(trace);
        return trace;
    }

    /**
     * 외부(부모)로부터 전달받은 TraceId를 이어받아 트레이스를 생성합니다.
     */
    public Trace continueTraceObject(TraceId traceId) {
        Trace trace = new Trace(traceId, System.currentTimeMillis());
        traceHolder.set(trace);
        return trace;
    }

    /**
     * 현재 스레드에 보관된 트레이스를 조회합니다.
     */
    @Override
    public Trace currentTraceObject() {
        return traceHolder.get();
    }

    @Override
    public void setTraceObject(Trace trace) {
        if (trace == null) {
            removeTraceObject();
            return;
        }
        traceHolder.set(trace);
    }

    /**
     * 현재 스레드의 트레이스 정보를 삭제합니다.
     * 요청이 끝나는 시점에 반드시 호출되어야 메모리 누수를 방지할 수 있습니다.
     */
    @Override
    public void removeTraceObject() {
        traceHolder.remove();
    }
}
