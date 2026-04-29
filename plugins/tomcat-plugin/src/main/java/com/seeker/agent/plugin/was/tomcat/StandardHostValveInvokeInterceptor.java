package com.seeker.agent.plugin.was.tomcat;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.context.propagation.PropagationContext;
import com.seeker.agent.core.context.propagation.PropagatorHolder;
import com.seeker.agent.core.model.ServiceType;
import com.seeker.agent.core.model.Span;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;
import com.seeker.agent.plugin.was.tomcat.adapter.HttpServletRequestGetter;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * Tomcat StandardHostValve.invoke 메서드를 가로채서 웹 요청의 시작과 끝을 추적하는 인터셉터입니다.
 *
 * <p>W3C Trace Context를 받아 trace에 합류한다. wire의 spanId를 자기 parentSpanId로 사용하고,
 * 자기 spanId는 {@link TraceContext#continueTraceObject(String, long)} 안에서 새로 생성된다.
 */
public class StandardHostValveInvokeInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        System.out.println("[Seeker] Tomcat 요청 수신: StandardHostValve.invoke 시작");

        TraceContext context = TraceContextHolder.getContext();

        // 분산 트레이싱: 요청 헤더에서 wire 컨텍스트 추출 (W3C Trace Context)
        PropagationContext propagated = PropagationContext.empty();
        String parentAgentId = null;
        Request request = null;
        if (args != null && args.length > 0 && args[0] instanceof Request) {
            request = (Request) args[0];
            propagated = PropagatorHolder.get()
                    .extract(request, HttpServletRequestGetter.INSTANCE);
            if (!propagated.isEmpty()) {
                parentAgentId = propagated.getTraceState().get("pAgentId");
            }
        }

        if (context.currentTraceObject() == null) {
            Trace trace;
            if (!propagated.isEmpty()) {
                // 받은 traceId/parentSpanId 위에 자기 spanId를 새로 생성하여 합류
                trace = context.continueTraceObject(propagated.getTraceId(), propagated.getParentSpanId());
                System.out.println("[Seeker] 기존 Trace 이어받음: " + trace.getTraceId());
            } else {
                // 새로운 루트 트레이스 시작
                trace = context.newTraceObject();
                System.out.println("[Seeker] 새로운 Trace 시작: " + trace.getTraceId());
            }

            // 루트 스팬에 메타데이터 주입
            Span span = trace.getSpan();
            // 서비스 타입을 TOMCAT으로 설정
            span.setServiceType(ServiceType.TOMCAT.getCode());
            if (parentAgentId != null && !parentAgentId.isEmpty()) {
                span.setParentAgentId(parentAgentId);
                System.out.println("[Seeker] Parent Agent: " + parentAgentId);
            }
            if (request != null) {
                // 요청 URI 및 엔드포인트 정보 설정
                span.setUri(request.getRequestURI());
                span.setEndPoint(request.getLocalAddr() + ":" + request.getLocalPort());
            }
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            // HTTP 응답 상태 코드 기록
            if (args != null && args.length > 1 && args[1] instanceof Response) {
                Response response = (Response) args[1];
                trace.getSpan().setStatusCode(response.getStatus());
            }
            // 전체 트레이스 종료 및 데이터 전송
            trace.finish();
            System.out.println("[Seeker] Tomcat 요청 처리 완료: " + trace.getTraceId() + " (elapsed: "
                    + trace.getSpan().getElapsedTime() + "ms)");
            // 스레드 로컬 컨텍스트 제거
            context.removeTraceObject();
        }
    }
}
