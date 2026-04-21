package com.seeker.agent.plugin.service;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.SpanEvent;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

import java.lang.reflect.Method;

/**
 * 일반 비즈니스 메서드 실행을 SpanEvent로 기록하는 인터셉터.
 */
public class ServiceInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            System.out.println("[Seeker] Service 실행 감지: " + className + "." + methodName + " 시작");
            // 일반 서비스 메서드 추적 시작 (기본 USER_METHOD 타입)
            trace.traceBlockBegin(className, methodName);
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            // 서비스 메서드 추적 종료 및 예외 정보 기록
            trace.traceBlockEnd(throwable);
            System.out.println("[Seeker] Service 실행 완료: " + className + "." + methodName + " 종료");
        }
    }
}
