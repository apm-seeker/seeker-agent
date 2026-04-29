package com.seeker.agent.plugin.http.adapter;

import com.seeker.agent.core.context.propagation.HeaderSetter;
import org.apache.http.HttpRequest;

/**
 * Apache HttpComponents의 {@link HttpRequest}에 헤더를 기록하는 어댑터.
 */
public final class ApacheHttpRequestSetter implements HeaderSetter<HttpRequest> {

    public static final ApacheHttpRequestSetter INSTANCE = new ApacheHttpRequestSetter();

    private ApacheHttpRequestSetter() {
    }

    @Override
    public void setHeader(HttpRequest carrier, String name, String value) {
        if (carrier == null || name == null || value == null) {
            return;
        }
        // 동일 헤더 중복 방지: 기존 헤더가 있으면 교체.
        carrier.removeHeaders(name);
        carrier.addHeader(name, value);
    }
}
