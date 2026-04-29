package com.seeker.agent.core.context.propagation;

/**
 * 추적 컨텍스트를 헤더로 직렬화(inject)/역직렬화(extract)하는 정책 인터페이스.
 *
 * <p>이 인터페이스의 의의는 <strong>전파 포맷의 추상화</strong>이다.
 * 새로운 포맷(W3C Trace Context, B3, Jaeger uber-trace-id 등)을 지원해야 할 때, 구현체만 추가하면 된다.
 * 인터셉터·plugin 등 호출부 코드는 시그니처가 같으므로 영향받지 않는다.
 *
 * <p>호출은 {@link PropagatorHolder#get()}을 통해 전역 인스턴스를 꺼내 쓰는 것이 일반적이다.
 *
 * <p>구현 시 주의:
 * <ul>
 *   <li>인터셉터의 hot path에서 호출되므로 가능한 한 가볍게 동작해야 한다.</li>
 *   <li>입력 헤더가 깨져 있어도 절대 throw 하지 않는다 — 인터셉터의 예외는 비즈니스 로직을 깨뜨릴 수 있다.
 *       복원 실패 시 {@link PropagationContext#empty()}를 반환한다.</li>
 *   <li>스레드-세이프해야 한다 (멀티 스레드 요청 처리 환경).</li>
 * </ul>
 */
public interface TraceContextPropagator {

    /**
     * 현재 추적 컨텍스트를 캐리어의 헤더로 기록한다.
     *
     * <p>호출자(sender)는 자기 trace의 현재 상태를 {@code context}에 담아 전달한다.
     * W3C 모델에서는 wire의 spanId 자리에 호출자의 현재 spanId가 실린다 — 다음 hop은 그 값을 자기 parentSpanId로 사용한다.
     *
     * @param context 보낼 추적 컨텍스트 (호출자의 현재 상태)
     * @param carrier 헤더 운반체 (HttpRequest 등)
     * @param setter  캐리어에 헤더를 기록할 수단 (라이브러리별 어댑터)
     * @param <C>     캐리어 타입
     */
    <C> void inject(PropagationContext context, C carrier, HeaderSetter<C> setter);

    /**
     * 캐리어의 헤더에서 추적 컨텍스트를 복원한다.
     *
     * <p>수신자는 반환된 {@code PropagationContext.parentSpanId}를 자기 입장에서의 parentSpanId로 사용하고,
     * 자기 spanId는 로컬에서 새로 생성한다(W3C self-generation 모델).
     *
     * @param carrier 헤더 운반체 (HttpServletRequest 등)
     * @param getter  캐리어에서 헤더를 읽을 수단 (라이브러리별 어댑터)
     * @param <C>     캐리어 타입
     * @return 복원된 컨텍스트. 헤더가 없거나 파싱 실패 시 {@link PropagationContext#empty()} (null 반환하지 않음)
     */
    <C> PropagationContext extract(C carrier, HeaderGetter<C> getter);
}
