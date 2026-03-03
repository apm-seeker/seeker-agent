package com.seeker.agent.plugin.http;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * Apache HttpClient 4.x의 InternalHttpClient.doExecute 메서드를 가로채는 인터셉터입니다.
 * 외부 서비스 호출 시 트레이스 컨텍스트를 전파(Propagation)하는 역할을 합니다.
 */
public class HttpClientExecuteInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            System.out.println("[Seeker] HTTP 외부 요청 감지: " + className + "." + methodName + " 시작");
            trace.traceBlockBegin();
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            trace.traceBlockEnd();
            System.out.println("[Seeker] HTTP 외부 요청 완료: " + className + "." + methodName + " 종료");
        }
    }
}
