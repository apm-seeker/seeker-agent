package com.seeker.agent.plugin.was.tomcat;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.context.TraceId;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;
import org.apache.catalina.connector.Request;

/**
 * Tomcat StandardHostValve.invoke 메서드를 가로채서 웹 요청의 시작과 끝을 추적하는 인터셉터입니다.
 */
public class StandardHostValveInvokeInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        System.out.println("[Seeker] Tomcat 요청 수신: StandardHostValve.invoke 시작");

        TraceContext context = TraceContextHolder.getContext();

        // Distributed Tracing: Extract single header context from Request object
        // (args[0])
        TraceId tid = null;

        if (args != null && args.length > 0 && args[0] instanceof Request) {
            Request request = (Request) args[0];
            String encodedContext = request.getHeader("Seeker-Context");
            tid = TraceId.decode(encodedContext);
        }

        if (context.currentTraceObject() == null) {
            Trace trace;
            if (tid != null) {
                // Continue existing trace
                trace = context.newTraceObject(tid);
                System.out.println("[Seeker] 기존 Trace 이어받음: " + trace.getTraceId());
            } else {
                // Start new trace
                trace = context.newTraceObject();
                System.out.println("[Seeker] 새로운 Trace 시작: " + trace.getTraceId());
            }
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            trace.finish(); // 전체 소요 시간 기록
            System.out.println("[Seeker] Tomcat 요청 처리 완료: " + trace.getTraceId() + " (elapsed: "
                    + trace.getSpan().getElapsedTime() + "ms)");
            context.removeTraceObject();
        }
    }
}
