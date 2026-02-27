package com.seeker.agent.instrument.interceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 인터셉터 인스턴스를 관리하고 조회하기 위한 레지스트리입니다.
 */
public class InterceptorRegistry {
    private static final ConcurrentMap<String, AroundInterceptor> interceptors = new ConcurrentHashMap<>();

    /**
     * 인터셉터를 등록합니다.
     * 
     * @param name        인터셉터 식별 이름
     * @param interceptor 등록할 인터셉터 인스턴스
     */
    public static void register(String name, AroundInterceptor interceptor) {
        interceptors.put(name, interceptor);
    }

    /**
     * 등록된 인터셉터를 조회합니다.
     * 
     * @param name 인터셉터 식별 이름
     * @return 조회된 인터셉터 인스턴스 (없으면 null)
     */
    public static AroundInterceptor getInterceptor(String name) {
        return interceptors.get(name);
    }
}
