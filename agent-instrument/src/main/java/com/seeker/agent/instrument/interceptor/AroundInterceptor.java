package com.seeker.agent.instrument.interceptor;

/**
 * 대상 메서드의 호출 전후로 로직을 실행할 수 있는 인터셉터 인터페이스입니다.
 */
public interface AroundInterceptor extends Interceptor {
    /**
     * 대상 메서드 실행 전에 실행될 로직입니다.
     *
     * @param target 대상 인스턴스 (정적 메서드인 경우 null)
     * @param args   메서드 인자 배열
     */
    void before(Object target, Object[] args);

    /**
     * 대상 메서드 실행 후에 실행될 로직입니다.
     *
     * @param target    대상 인스턴스 (정적 메서드인 경우 null)
     * @param args      메서드 인자 배열
     * @param result    메서드 실행 결과값
     * @param throwable 메서드 실행 중 발생한 예외 (정상 종료 시 null)
     */
    void after(Object target, Object[] args, Object result, Throwable throwable);
}
