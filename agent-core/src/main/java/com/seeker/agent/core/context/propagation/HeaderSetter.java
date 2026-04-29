package com.seeker.agent.core.context.propagation;

/**
 * 캐리어(C)에 헤더 값을 기록하는 추상화.
 *
 * <p>{@link HeaderGetter}와 쌍을 이루며, 둘 다 propagator를 특정 라이브러리 타입에서 분리시키는 역할을 한다.
 * inject(보내는 쪽)에서는 {@code HeaderSetter}를, extract(받는 쪽)에서는 {@code HeaderGetter}를 사용한다.
 *
 * <p>구현체는 각 plugin의 adapter 패키지에 둔다 (예: {@code ApacheHttpRequestSetter}). 이렇게 해야
 * agent-core가 라이브러리에 의존하지 않고, 새 라이브러리(OkHttp, gRPC 등) 추가가 plugin 단위로 독립된다.
 *
 * @param <C> 캐리어의 구체 타입 (Apache의 HttpRequest, OkHttp의 Request 등)
 */
public interface HeaderSetter<C> {

    /**
     * 캐리어에 주어진 이름/값으로 헤더를 설정한다.
     *
     * <p>구현체는 동일 헤더가 이미 존재하는 경우의 처리(덮어쓰기 vs 추가)를 캐리어 라이브러리의 관례에 맞게 결정해야 한다.
     * 일반적으로 분산 추적 헤더는 중복되면 안 되므로 덮어쓰기가 안전하다.
     *
     * @param carrier 헤더를 기록할 운반체
     * @param name    헤더 이름 (예: {@code "traceparent"})
     * @param value   헤더 값
     */
    void setHeader(C carrier, String name, String value);
}
