package com.seeker.agent.core.model;

/**
 * 현재 에이전트의 {@link AgentInfo}를 전역에서 조회할 수 있게 해주는 Holder.
 *
 * 부트스트랩 시 1회 등록되며, 인터셉터(HTTP 클라이언트 등)가 자기 자신의 agentId를
 * 분산 추적 헤더에 실어보낼 때 사용된다. (TraceContextHolder, PropagatorHolder와 동일 패턴)
 */
public final class AgentInfoHolder {

    private static volatile AgentInfo agentInfo;

    private AgentInfoHolder() {
    }

    public static void set(AgentInfo info) {
        agentInfo = info;
    }

    public static AgentInfo get() {
        return agentInfo;
    }
}
