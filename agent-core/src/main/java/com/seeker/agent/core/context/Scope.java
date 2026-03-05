package com.seeker.agent.core.context;

/**
 * 플러그인 내에서 중복 트래킹(Re-entrancy)을 방지하기 위한 스코프 관리 인터페이스.
 * 
 * <pre>
 * if (scope.tryEnter()) {
 *     try {
 *         // 트래킹 로직
 *     } finally {
 *         scope.leave();
 *     }
 * }
 * </pre>
 */
public interface Scope {
    /**
     * 현재 스코프에 진입을 시도합니다.
     * 
     * @return 이미 스코프 내부에 있다면 false, 처음 진입하는 것이면 true
     */
    boolean tryEnter();

    /**
     * 스코프에서 나갑니다. 진입 횟수가 0이 되면 스코프가 완전히 종료됩니다.
     */
    void leave();

    /**
     * 현재 스택의 가장 바깥쪽(Root)인지 확인합니다.
     */
    boolean isRoot();

    /**
     * 현재 스코프의 이름을 반환합니다.
     */
    String getName();
}
