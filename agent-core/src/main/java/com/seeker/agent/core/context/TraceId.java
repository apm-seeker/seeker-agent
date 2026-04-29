package com.seeker.agent.core.context;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 분산 추적의 핵심 식별자 묶음. 순수 값 객체.
 *
 * <p>W3C Trace Context 호환을 위해 다음 표현을 따른다.
 * <ul>
 *   <li>{@code traceId} : 16바이트 → 32-char lowercase hex 문자열</li>
 *   <li>{@code spanId}, {@code parentSpanId} : 64-bit long (gRPC proto 호환).
 *       헤더로 직렬화될 때만 16-char hex로 변환된다.</li>
 * </ul>
 *
 * <p>spanId는 항상 <strong>로컬에서 생성</strong>된다. 호출자는 자기 spanId를 wire로 보내고,
 * 수신자는 받은 spanId를 자기 입장의 parentSpanId로 삼아 새 spanId를 부여받는다.
 * (sender 측 pre-allocation 구조가 아님.)
 *
 * <p>헤더 직렬화/역직렬화는 {@link com.seeker.agent.core.context.propagation.TraceContextPropagator}에 위임한다.
 */
public class TraceId {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final String traceId;
    private final long spanId;
    private final long parentSpanId;

    /**
     * root Span일 때(루트 trace의 시작점)의 TraceId를 생성한다.
     */
    public TraceId() {
        this(generateTraceId(), generateSpanId(), -1);
    }

    public TraceId(String traceId, long spanId, long parentSpanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    /**
     * 외부에서 받은 trace에 합류할 때 사용하는 팩토리.
     * 받은 {@code traceId}/{@code parentSpanId}는 그대로 두고, <strong>자기 spanId는 로컬에서 새로 생성</strong>한다.
     */
    public static TraceId continueWith(String traceId, long parentSpanId) {
        return new TraceId(traceId, generateSpanId(), parentSpanId);
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

    /**
     * W3C 표준에 맞는 16바이트(32 hex char) traceId 생성.
     */
    private static String generateTraceId() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        char[] out = new char[32];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private static long generateSpanId() {
        return ThreadLocalRandom.current().nextLong();
    }

    @Override
    public String toString() {
        return "TraceId{" +
                "traceId='" + traceId + '\'' +
                ", spanId=" + spanId +
                ", parentSpanId=" + parentSpanId +
                '}';
    }
}
