package com.seeker.agent.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 스팬 내에서 호출된 개별 메서드의 실행 정보를 표현하는 스팬 이벤트.
 *
 * <p>
 * {@link Span} 하나에 여러 {@code SpanEvent}가 쌓이며, 함수 콜스택을 표현한다.
 *
 * <pre>
 * SpanEvent (UserController.getUser,  elapsed: 100ms)
 * SpanEvent (UserService.findById,    elapsed: 80ms)
 * SpanEvent (UserRepository.select,   elapsed: 60ms, serviceType: MYSQL)
 * </pre>
 */
public class SpanEvent {

    /**
     * 스팬 내에서 이 이벤트의 실행 순서.
     *
     * <p>
     * {@code depth}와 함께 콜스택 트리를 100% 복구할 수 있다.
     * depth=N인 이벤트의 부모는 sequence 상 직전에 등장한 depth=N-1인 이벤트다.
     *
     * <pre>
     * seq=0, depth=1  A()
     * seq=1, depth=2    B()   → 부모: A
     * seq=2, depth=3      C() → 부모: B
     * seq=3, depth=2    D()   → 부모: A
     * </pre>
     */
    private int sequence;

    /** 콜스택에서 이 이벤트의 중첩 깊이. 루트는 1. */
    private int depth;

    /** 이벤트 시작 시각 (Unix timestamp, ms). */
    private long startTime;

    /** 이벤트 처리 소요 시간 (ms). */
    private int elapsedTime;

    /**
     * 호출된 API의 ID.
     *
     * <p>
     * 에이전트 시작 시 바이트코드 조작으로 감지된 메서드 시그니처를 수집 서버에 등록하고
     * 발급받은 정수 ID. 매 이벤트마다 전체 메서드명 문자열 대신 이 ID만 전송해 네트워크 비용을 줄인다.
     *
     * <pre>
     * apiId: 101  →  "com.example.UserController.getUser(HttpServletRequest)"
     * </pre>
     */
    private int apiId;

    /**
     * 이벤트에 대한 부가 정보.
     *
     * <p>
     * 키-값 형태로 다양한 데이터를 기록한다.
     *
     * <pre>
     * "sql"    → "SELECT * FROM users WHERE id = ?" -> 추후 해싱을 통해 최적화 예정
     * </pre>
     */
    private Map<String, String> attributes;

    /**
     * 이벤트 시작 시각을 현재 시각으로 기록한다.
     */
    public void markStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 이벤트 종료 시각을 현재 시각으로 기록하고 소요 시간을 계산한다.
     */
    public void finish() {
        this.elapsedTime = (int) now();
    }

    private long now() {
        return System.currentTimeMillis() - this.startTime;
    }

    /**
     * 부가 정보를 추가한다.
     *
     * @param key   속성 키 (ex. "sql", "params")
     * @param value 속성 값
     */
    public void addAttribute(String key, String value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(key, value);
    }

    /**
     * 이벤트 시작 시각을 반환한다.
     *
     * @return 시작 시각 (Unix timestamp, ms)
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 이벤트 처리 소요 시간을 반환한다.
     *
     * @return 소요 시간 (ms)
     */
    public int getElapsedTime() {
        return elapsedTime;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getApiId() {
        return apiId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "SpanEvent{" +
                "sequence=" + sequence +
                ", depth=" + depth +
                ", startTime=" + startTime +
                ", elapsedTime=" + elapsedTime +
                ", apiId=" + apiId +
                ", attributes=" + attributes +
                '}';
    }
}
