package com.seeker.agent.plugin.jdbc;

import com.seeker.agent.core.context.SqlMetadataAccessor;
import com.seeker.agent.core.context.Scope;
import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.SpanEvent;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * PreparedStatement의 실행 메서드를 가로채서 SQL 쿼리 수행 시간을 추적하는 인터셉터입니다.
 */
public class PreparedStatementExecuteInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        TraceContext context = TraceContextHolder.getContext();
        Scope jdbcScope = context.getScope("JDBC");

        if (!jdbcScope.tryEnter()) {
            return;
        }

        Trace trace = context.currentTraceObject();
        if (trace != null) {
            System.out.println("[Seeker] JDBC 쿼리 실행 감지: " + className + "." + methodName + " 시작");
            trace.traceBlockBegin(className, methodName);

            // Accessor를 통해 깔끔한 SQL 추출
            if (target instanceof SqlMetadataAccessor) {
                String sql = ((SqlMetadataAccessor) target)._$seeker$getSql();
                if (sql != null) {
                    System.out.println(sql);
                    SpanEvent event = trace.currentSpanEvent();
                    if (event != null) {
                        event.addAttribute("sql", sql);
                    }
                }
            }
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Scope jdbcScope = context.getScope("JDBC");

        if (jdbcScope.isRoot()) {
            Trace trace = context.currentTraceObject();
            if (trace != null) {
                trace.traceBlockEnd(throwable);
                System.out.println("[Seeker] JDBC 쿼리 실행 완료");
            }
        }

        jdbcScope.leave();
    }
}
