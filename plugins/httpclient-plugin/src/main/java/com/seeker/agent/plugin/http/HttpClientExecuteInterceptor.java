package com.seeker.agent.plugin.http;

import com.seeker.agent.core.context.TraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.context.TraceId;
import com.seeker.agent.core.model.MethodType;
import com.seeker.agent.core.model.SpanEvent;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;

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
            // 외부 HTTP 호출 블록 시작
            trace.traceBlockBegin(className, methodName);

            SpanEvent event = trace.currentSpanEvent();
            if (event != null) {
                // MethodType을 HTTP_CLIENT로 설정
                event.setMethodType(MethodType.HTTP_CLIENT.getCode());
            }

            // Distributed Tracing: Inject headers into HttpRequest (args[1])
            if (args != null && args.length > 1 && args[1] instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) args[1];
                TraceId nextId = trace.getTraceId().getNextTraceId();

                request.addHeader("Seeker-Context", nextId.encode());

                // URL과 Method를 캡쳐하여 Attribute 추가
                RequestLine requestLine = request.getRequestLine();
                if (requestLine != null) {
                    String uri = requestLine.getUri();
                    String httpMethod = requestLine.getMethod();

                    if (event != null) {
                        event.addAttribute("http.url", uri);
                        event.addAttribute("http.method", httpMethod);
                        // 다음 서버로 전파되는 Span ID 기록
                        event.addAttribute("nextSpanId", String.valueOf(nextId.getSpanId()));
                    }
                }
            }

            // HttpHost(args[0]) 정보를 통해 목적지 식별
            if (args != null && args.length > 0 && args[0] != null && event != null) {
                event.addAttribute("destinationId", args[0].toString());
            }
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        TraceContext context = TraceContextHolder.getContext();
        Trace trace = context.currentTraceObject();

        if (trace != null) {
            // 외부 호출 블록 종료 및 예외 정보 기록
            trace.traceBlockEnd(throwable);
            System.out.println("[Seeker] HTTP 외부 요청 완료: " + className + "." + methodName + " 종료");
        }
    }
}
