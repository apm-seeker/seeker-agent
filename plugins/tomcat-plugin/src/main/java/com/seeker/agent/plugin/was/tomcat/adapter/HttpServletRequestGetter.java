package com.seeker.agent.plugin.was.tomcat.adapter;

import com.seeker.agent.core.context.propagation.HeaderGetter;
import org.apache.catalina.connector.Request;

/**
 * Tomcat의 {@link Request}로부터 헤더를 읽어오는 어댑터.
 * propagator의 라이브러리 의존성을 plugin 모듈로 분리하는 역할.
 */
public final class HttpServletRequestGetter implements HeaderGetter<Request> {

    public static final HttpServletRequestGetter INSTANCE = new HttpServletRequestGetter();

    private HttpServletRequestGetter() {
    }

    @Override
    public String getHeader(Request carrier, String name) {
        if (carrier == null || name == null) {
            return null;
        }
        return carrier.getHeader(name);
    }
}
