package com.seeker.agent.core.context;

/**
 * 전역 TraceContext에 접근하기 위한 Holder 클래스.
 */
public class TraceContextHolder {

    private static TraceContext traceContext;

    public static void setTraceContext(TraceContext context) {
        traceContext = context;
    }

    public static TraceContext getContext() {
        return traceContext;
    }
}
