package com.seeker.agent.core.context.propagation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 분산 추적 헤더에 실리는 컨텍스트의 도메인 표현 (W3C Trace Context).
 *
 * <p>이 클래스는 <strong>wire 표현</strong>이지 로컬 trace 상태가 아니다.
 * <ul>
 *   <li>inject 시: 호출자(local)가 자기 현재 spanId를 {@code parentSpanId}에 담아 보낸다.</li>
 *   <li>extract 시: 수신자가 받은 spanId를 자기 입장에서의 parentSpanId로 해석한다.</li>
 * </ul>
 *
 * <p>즉 wire의 "spanId 자리"는 항상 <em>호출자의 spanId = 수신자의 parentSpanId</em>를 의미한다.
 * 자기 spanId는 수신자가 로컬에서 새로 생성한다 (sender 측 pre-allocation 없음).
 *
 * <p>왜 local의 {@code TraceId}와 분리되어 있는가?
 * <ul>
 *   <li>{@code TraceId}는 (traceId, mySpanId, myParentSpanId)의 <strong>local 상태</strong>.</li>
 *   <li>{@code PropagationContext}는 (traceId, callerSpanId)의 <strong>wire 상태</strong>.</li>
 *   <li>둘을 한 자료구조에 섞으면 "이 spanId가 누구 거지?"가 헷갈린다 → 분리.</li>
 * </ul>
 *
 * <p>운반 필드:
 * <ul>
 *   <li>{@code traceId}      - traceparent로 운반되는 32-char hex 식별자 (16바이트)</li>
 *   <li>{@code parentSpanId} - traceparent의 parent-id 자리에 운반되는 long (호출자의 현재 spanId)</li>
 *   <li>{@code traceState}   - 벤더 전용 메타데이터(pAgentId 등)를 tracestate로 운반</li>
 *   <li>{@code baggage}      - 애플리케이션 컨텍스트(userId 등)를 baggage로 운반</li>
 * </ul>
 *
 * <p>불변(immutable) 객체 — 빌더로만 생성 가능. 인터셉터 사이에서 의도치 않은 mutation을 방지.
 */
public final class PropagationContext {

    /**
     * "비어있음(헤더 없음 또는 파싱 실패)" 상태를 나타내는 싱글톤.
     * extract 실패 시 null 대신 이 값을 반환하여 호출부의 null 체크 부담을 없앤다.
     */
    private static final PropagationContext EMPTY =
            new PropagationContext(null, -1, Collections.emptyMap(), Collections.emptyMap());

    /** 32-char lowercase hex (16바이트). 분산 추적 체인 전체에서 동일하게 유지된다. */
    private final String traceId;

    /**
     * wire의 spanId 자리에 실리는 값.
     * <ul>
     *   <li>inject 호출자 입장 → 자기 현재 spanId</li>
     *   <li>extract 결과 입장 → 자기 부모(호출자)의 spanId</li>
     * </ul>
     * 명명을 {@code parentSpanId}로 한 이유: extract 결과를 그대로 사용하기 직관적이라서.
     */
    private final long parentSpanId;

    /** tracestate에 실리는 벤더 전용 키-값 (예: pAgentId). 항상 unmodifiable. */
    private final Map<String, String> traceState;

    /** baggage에 실리는 애플리케이션 컨텍스트 (예: userId, tenantId). 항상 unmodifiable. */
    private final Map<String, String> baggage;

    /** private 생성자 — 외부에서는 {@link #builder()}로만 생성 가능. */
    private PropagationContext(String traceId,
                               long parentSpanId,
                               Map<String, String> traceState,
                               Map<String, String> baggage) {
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        this.traceState = traceState;
        this.baggage = baggage;
    }

    /**
     * 빈 컨텍스트(헤더 없음/실패)를 반환. 항상 같은 인스턴스 재사용 (메모리 절약).
     */
    public static PropagationContext empty() {
        return EMPTY;
    }

    /** 새 컨텍스트를 만들 때 사용하는 빌더. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 이 컨텍스트가 비어있는지 (extract 실패/헤더 없음을 의미).
     * 호출부에서 분기 처리에 사용한다.
     */
    public boolean isEmpty() {
        return traceId == null;
    }

    public String getTraceId() {
        return traceId;
    }

    public long getParentSpanId() {
        return parentSpanId;
    }

    public Map<String, String> getTraceState() {
        return traceState;
    }

    public Map<String, String> getBaggage() {
        return baggage;
    }

    /**
     * {@link PropagationContext} 빌더.
     *
     * <p>빌더를 둔 이유: 호출부가 traceId/parentSpanId만 채울 수도, tracestate/baggage까지 채울 수도 있다.
     * 향후 필드(예: trace-flags) 추가 시에도 호환성 유지가 쉽다.
     *
     * <p>{@link LinkedHashMap}을 쓰는 이유: 헤더 직렬화 시 키 순서를 안정적으로 유지하기 위함
     * (디버깅 시 헤더가 뒤섞여 보이는 걸 방지).
     */
    public static final class Builder {
        private String traceId;
        private long parentSpanId = -1;  // -1: 부모 없음(루트) 또는 미설정
        private final Map<String, String> traceState = new LinkedHashMap<>();
        private final Map<String, String> baggage = new LinkedHashMap<>();

        /** 32-char hex traceId 설정. */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * wire의 spanId 자리에 실릴 값 설정.
         * inject 호출자 기준 — 자기 현재 spanId를 넣는다.
         */
        public Builder parentSpanId(long parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        /**
         * tracestate 벤더 영역에 키-값을 추가한다.
         * key/value 중 하나라도 null이면 무시 (방어적 처리).
         */
        public Builder putTraceState(String key, String value) {
            if (key != null && value != null) {
                this.traceState.put(key, value);
            }
            return this;
        }

        /**
         * baggage에 키-값을 추가한다.
         * key/value 중 하나라도 null이면 무시.
         */
        public Builder putBaggage(String key, String value) {
            if (key != null && value != null) {
                this.baggage.put(key, value);
            }
            return this;
        }

        /**
         * 불변 인스턴스 생성. 빌더의 내부 맵은 복사본 + unmodifiable 래핑되어,
         * 빌더 재사용/수정이 결과 객체에 영향을 주지 않는다.
         */
        public PropagationContext build() {
            return new PropagationContext(
                    traceId,
                    parentSpanId,
                    Collections.unmodifiableMap(new LinkedHashMap<>(traceState)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(baggage)));
        }
    }
}
