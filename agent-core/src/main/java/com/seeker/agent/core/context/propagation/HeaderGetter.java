package com.seeker.agent.core.context.propagation;

/**
 * 캐리어(C)로부터 헤더 값을 읽어오는 추상화.
 *
 * <p>캐리어란 헤더를 가진 모든 매개체를 가리킨다. 예시:
 * <ul>
 *   <li>{@code org.apache.catalina.connector.Request}      — Tomcat 서블릿 요청</li>
 *   <li>{@code org.apache.http.HttpRequest}                 — Apache HttpClient 요청</li>
 *   <li>{@code okhttp3.Request}                             — OkHttp 요청</li>
 *   <li>{@code io.grpc.Metadata}                            — gRPC 메타데이터</li>
 * </ul>
 *
 * <p>이 인터페이스를 통해 propagator는 특정 라이브러리의 타입을 알 필요 없이 동작한다.
 * 라이브러리 의존성은 각 plugin 안의 어댑터(예: {@code HttpServletRequestGetter})에 격리된다.
 *
 * @param <C> 캐리어의 구체 타입 (Tomcat의 Request, OkHttp의 Request 등)
 */
public interface HeaderGetter<C> {

    /**
     * 캐리어에서 주어진 이름의 헤더 값을 반환한다.
     *
     * @param carrier 헤더를 보유한 운반체
     * @param name    조회할 헤더 이름 (예: {@code "traceparent"})
     * @return 헤더 값, 존재하지 않으면 {@code null}
     */
    String getHeader(C carrier, String name);
}
