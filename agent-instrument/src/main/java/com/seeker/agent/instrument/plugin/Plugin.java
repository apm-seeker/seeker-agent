package com.seeker.agent.instrument.plugin;

import net.bytebuddy.agent.builder.AgentBuilder;

/**
 * 에이전트 플러그인을 위한 인터페이스
 */
public interface Plugin {
    /**
     * 해당 플러그인의 인스트루멘테이션 설정을 수행합니다.
     *
     * @param agentBuilder 구성할 AgentBuilder 인스턴스
     * @return 설정이 완료된 AgentBuilder 인스턴스
     */
    AgentBuilder transform(AgentBuilder agentBuilder);
}
