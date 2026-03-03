package com.seeker.agent.plugin.jdbc;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * PreparedStatement의 실행 메서드를 가로채서 SQL 쿼리 수행 시간을 추적하는 인터셉터입니다.
 */
public class PreparedStatementExecuteInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            System.out.println("[Seeker] JDBC 쿼리 실행 감지: " + className + "." + methodName + " 시작");
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
            System.out.println("[Seeker] JDBC 쿼리 실행 완료");
        }
    }
}
