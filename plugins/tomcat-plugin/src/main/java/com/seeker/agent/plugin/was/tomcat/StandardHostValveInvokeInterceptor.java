package com.seeker.agent.plugin.was.tomcat;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.context.TraceId;
import com.seeker.agent.core.model.ServiceType;
import com.seeker.agent.core.model.Span;
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

        // 분산 트레이싱: 요청 헤더에서 컨텍스트 추출
        TraceId tid = null;

        if (args != null && args.length > 0 && args[0] instanceof Request) {
            Request request = (Request) args[0];
            String encodedContext = request.getHeader("Seeker-Context");
            tid = TraceId.decode(encodedContext);
        }

        if (context.currentTraceObject() == null) {
            Trace trace;
            if (tid != null) {
                // 기존 트레이스 이어받기
                trace = context.newTraceObject(tid);
                System.out.println("[Seeker] 기존 Trace 이어받음: " + trace.getTraceId());
            } else {
                // 새로운 트레이스 시작
                trace = context.newTraceObject();
                System.out.println("[Seeker] 새로운 Trace 시작: " + trace.getTraceId());
            }

            // 루트 스팬에 메타데이터 주입
            Span span = trace.getSpan();
            // 서비스 타입을 TOMCAT으로 설정
            span.setServiceType(ServiceType.TOMCAT.getCode());
            if (args != null && args.length > 0 && args[0] instanceof Request) {
                Request request = (Request) args[0];
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
            // 전체 트레이스 종료 및 데이터 전송
            trace.finish();
            System.out.println("[Seeker] Tomcat 요청 처리 완료: " + trace.getTraceId() + " (elapsed: "
                    + trace.getSpan().getElapsedTime() + "ms)");
            // 스레드 로컬 컨텍스트 제거
            context.removeTraceObject();
        }
    }
}
