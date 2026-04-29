package com.seeker.agent.core.context.propagation;

/**
 * 전역에서 {@link TraceContextPropagator}에 접근하기 위한 Holder.
 *
 * <p>왜 정적 holder인가?
 * <ul>
 *   <li>인터셉터는 ByteBuddy로 주입되어 일반 DI 컨테이너 바깥에서 동작한다 — 생성자 주입이 어렵다.</li>
 *   <li>부트스트랩 시 1회만 등록되고 런타임 내내 재사용된다 — 정적 필드가 가장 단순하고 빠르다.</li>
 * </ul>
 *
 * <p>사용 흐름:
 * <ol>
 *   <li>{@code AgentMain.premain()}에서 {@link #setPropagator(TraceContextPropagator)} 1회 호출</li>
 *   <li>인터셉터에서 {@link #get()}으로 꺼내 inject/extract 호출</li>
 * </ol>
 *
 * <p>{@code TraceContextHolder}, {@code AgentInfoHolder}와 같은 패턴.
 *
 * <p>기본값으로 {@link W3CTraceContextPropagator} 인스턴스를 갖고 있어서, 부트스트랩이 누락되어도
 * 인터셉터가 NPE를 일으키지 않고 W3C 동작을 한다 — 안전한 자기충족 기본값.
 */
public final class PropagatorHolder {

    /**
     * volatile로 멀티 스레드 가시성 보장.
     * 부트스트랩 시점 이후로는 거의 read-only이지만, 테스트에서 stub 주입 가능성을 위해 mutable로 둔다.
     */
    private static volatile TraceContextPropagator propagator = new W3CTraceContextPropagator();

    /** 인스턴스화 차단 — 정적 유틸리티. */
    private PropagatorHolder() {
    }

    /**
     * 전역 propagator를 등록한다.
     * 일반적으로 {@code AgentMain.premain()}에서 1회만 호출된다.
     *
     * @param p 새 propagator. {@code null}이면 무시(기존 값 유지) — 안전한 기본값을 보존하기 위함.
     */
    public static void setPropagator(TraceContextPropagator p) {
        if (p != null) {
            propagator = p;
        }
    }

    /**
     * 전역 propagator를 반환한다. 부트스트랩이 누락된 경우에도 기본 W3C 인스턴스가 반환되므로 null이 아니다.
     */
    public static TraceContextPropagator get() {
        return propagator;
    }
}
