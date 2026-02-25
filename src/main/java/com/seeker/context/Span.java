package com.seeker.context;

import java.util.ArrayList;
import java.util.List;

/**
 * 하나의 서버에서 요청을 처리하는 단위를 표현하는 클래스.
 *
 * <p>분산 환경에서 요청이 여러 서버를 거칠 경우, 각 서버마다 하나의 {@code Span}이 1개씩 생성된다.
 * 서버의 메타데이터와 처리 시간을 기록하며,
 * 내부에서 호출된 메서드들은 {@link SpanEvent} 목록으로 관리된다.
 *
 * <pre>
 * Span (api-server, elapsed: 120ms)
 *   ├─ SpanEvent (UserController.getUser)
 *   └─ SpanEvent (UserRepository.findById)
 * </pre>
 */
public class Span {

    /** 이 스팬이 속한 트레이스 ID. */
    private final String traceId;

    /** 이 스팬의 고유 ID. String 보다는 랜덤 값 같은 것을 사용할 예정*/
    private long spanId;

    /** 부모 스팬의 ID. 루트 스팬인 경우 -1. */
    private long parentSpanId = -1;

    /** 스팬 시작 시각 (Unix timestamp, ms). */
    private long startTime;

    /** 스팬 처리 소요 시간 (ms). */
    private int elapsedTime;

    /** 해당 서버를 호출한 ip 주소. */
    private String remoteAddr;

    /**
     * 스팬을 생성한다.
     *
     * @param traceId      이 스팬이 속한 트레이스 ID
     * @param spanId       이 스팬의 고유 ID
     * @param parentSpanId 부모 스팬의 ID. 루트 스팬인 경우 -1
     * @param remoteAddr   해당 서버를 호출한 ip 주소
     */
    public Span(String traceId, long spanId, long parentSpanId, String remoteAddr) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.startTime = now();
        this.remoteAddr = remoteAddr;
    }

    /**
     * 스팬 종료 시각을 현재 시각으로 기록하고 소요 시간을 계산한다.
     */
    public void finish() {
        this.elapsedTime = (int) now();
    }

    private long now() {
        return (System.currentTimeMillis() - this.startTime);
    }

    public String getTraceId() {
        return traceId;
    }

    public long getSpanId() {
        return spanId;
    }

    public long getParentSpanId() {
        return parentSpanId;
    }

    public boolean isRoot() {
        return parentSpanId == -1;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getElapsedTime() {
        return elapsedTime;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public String toString() {
        return "Span{" +
                "traceId='" + traceId + '\'' +
                ", spanId=" + spanId +
                ", parentSpanId=" + parentSpanId +
                ", startTime=" + startTime +
                ", elapsedTime=" + elapsedTime +
                ", remoteAddr='" + remoteAddr + '\'' +
                '}';
    }
}
