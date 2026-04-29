package com.seeker.agent.plugin.http;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.context.TraceId;
import com.seeker.agent.core.context.propagation.PropagationContext;
import com.seeker.agent.core.context.propagation.PropagatorHolder;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.model.AgentInfoHolder;
import com.seeker.agent.core.model.MethodType;
import com.seeker.agent.core.model.SpanEvent;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;
import com.seeker.agent.plugin.http.adapter.ApacheHttpRequestSetter;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;

/**
 * Apache HttpClient 4.x의 InternalHttpClient.doExecute 메서드를 가로채는 인터셉터입니다.
 * 외부 서비스 호출 시 W3C Trace Context를 헤더로 전파(Propagation)하는 역할을 합니다.
 *
 * <p>현재 trace의 spanId를 그대로 wire에 실어 보낸다. 다음 hop은 이 값을 자기 parentSpanId로 받고
 * 자기 spanId는 자체 생성한다.
 */
public class HttpClientExecuteInterceptor implements AroundInterceptor {

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            System.out.println("[Seeker] HTTP 외부 요청 감지: " + className + "." + methodName + " 시작");
            // 외부 HTTP 호출 블록 시작
            trace.traceBlockBegin(className, methodName);

            SpanEvent event = trace.currentSpanEvent();
            if (event != null) {
                // MethodType을 HTTP_CLIENT로 설정
                event.setMethodType(MethodType.HTTP_CLIENT.getCode());
            }

            // Distributed Tracing: W3C 헤더 inject — 현재 spanId를 그대로 송출
            if (args != null && args.length > 1 && args[1] instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) args[1];
                TraceId current = trace.getTraceId();

                PropagationContext.Builder ctxBuilder = PropagationContext.builder()
                        .traceId(current.getTraceId())
                        .parentSpanId(current.getSpanId());

                AgentInfo myInfo = AgentInfoHolder.get();
                if (myInfo != null && myInfo.getAgentId() != null) {
                    ctxBuilder.putTraceState("pAgentId", myInfo.getAgentId());
                }
                PropagatorHolder.get().inject(ctxBuilder.build(), request, ApacheHttpRequestSetter.INSTANCE);

                // URL과 Method를 캡쳐하여 Attribute 추가
                RequestLine requestLine = request.getRequestLine();
                if (requestLine != null && event != null) {
                    event.addAttribute("http.url", requestLine.getUri());
                    event.addAttribute("http.method", requestLine.getMethod());
                }
            }

            // HttpHost(args[0]) 정보를 통해 목적지 식별
            if (args != null && args.length > 0 && args[0] != null && event != null) {
                Object hostObj = args[0];
                if (hostObj.getClass().getName().contains("HttpHost")) {
                    event.addAttribute("destinationId", hostObj.toString());
                } else {
                    RequestLine requestLine = (args.length > 1 && args[1] instanceof HttpRequest)
                            ? ((HttpRequest) args[1]).getRequestLine() : null;
                    if (requestLine != null) {
                        event.addAttribute("destinationId", requestLine.getUri());
                    }
                }
            }
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            // 응답 상태 코드를 attribute로 기록
            SpanEvent event = trace.currentSpanEvent();
            if (event != null && result instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) result;
                if (response.getStatusLine() != null) {
                    event.addAttribute("http.code",
                            String.valueOf(response.getStatusLine().getStatusCode()));
                }
            }
            // 외부 호출 블록 종료 및 예외 정보 기록
            trace.traceBlockEnd(throwable);
            System.out.println("[Seeker] HTTP 외부 요청 완료: " + className + "." + methodName + " 종료");
        }
    }
}
