package com.seeker.agent.core.model;

/**
 * 사용자의 단일 요청을 표현하는 트레이스.
 *
 * <p>
 * 분산 환경에서 하나의 요청이 여러 서버를 거치는 전체 흐름을 식별합니다.
 * 각 서버에서의 처리 정보는 {@link Span}으로 기록된다.
 *
 * <pre>
 * Trace (사용자 요청 1건)
 *   └─ Span (서버 처리 단위)
 *        ├─ SpanEvent (메서드 호출)
 *        └─ SpanEvent (메서드 호출)
 * </pre>
 */
public class Trace {

    /** 분산 환경에서 요청을 식별하는 고유 ID. */
    private final String traceId;

    /** 요청이 시작된 시각 (Unix timestamp, ms). */
    private final long startTime;

    /**
     * 트레이스를 생성한다.
     *
     * @param traceId   분산 환경에서 요청을 식별하는 고유 ID
     * @param startTime 요청 시작 시각 (Unix timestamp, ms)
     */
    public Trace(String traceId, long startTime) {
        this.traceId = traceId;
        this.startTime = startTime;
    }

    /**
     * 분산 환경에서 요청을 식별하는 고유 ID를 반환한다.
     *
     * @return 트레이스 ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 요청이 시작된 시각을 반환한다.
     *
     * @return 요청 시작 시각 (Unix timestamp, ms)
     */
    public long getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return "Trace{traceId='" + traceId + "', startTime=" + startTime + '}';
    }
}
