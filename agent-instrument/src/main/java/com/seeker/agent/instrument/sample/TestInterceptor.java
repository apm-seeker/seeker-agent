package com.seeker.agent.instrument.sample;

import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * 메서드 실행 전후로 로그를 출력하는 간단한 테스트용 인터셉터입니다.
 */
public class TestInterceptor implements AroundInterceptor {
    @Override
    public void before(Object target, Object[] args) {
        System.out.println("[Seeker] 메서드 실행 시작: " + (target != null ? target.getClass().getName() : "static"));
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        System.out.println("[Seeker] 메서드 실행 종료");
    }
}
