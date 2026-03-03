package com.seeker.agent.plugin.was.tomcat;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * Tomcat StandardHostValve.invoke 메서드를 가로채서 웹 요청의 시작과 끝을 추적하는 인터셉터입니다.
 */
public class StandardHostValveInvokeInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        System.out.println("[Seeker] Tomcat 요청 수신: StandardHostValve.invoke 시작");

        TraceContext context = TraceContextHolder.getContext();
        if (context.currentTraceObject() == null) {
            Trace trace = context.newTraceObject();
            System.out.println("[Seeker] 새로운 Trace 시작: " + trace.getTraceId());
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            System.out.println("[Seeker] Tomcat 요청 처리 완료: " + trace.getTraceId());
            context.removeTraceObject();
        }
    }
}
