package com.seeker.agent.config;

import java.util.Properties;

/**
 * 에이전트 식별 정보를 관리하는 클래스.
 * 에이전트 고유 ID와 대상 애플리케이션 이름을 보관합니다.
 */
public class AgentIdentityConfig {

    private final String agentId;
    private final String applicationName;

    public AgentIdentityConfig(Properties properties) {
        this.agentId = properties.getProperty("seeker.agentId", "unnamed-agent");
        this.applicationName = properties.getProperty("seeker.applicationName", "unnamed-application");
    }

    public String getAgentId() {
        return agentId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public String toString() {
        return "AgentIdentityConfig{" +
                "agentId='" + agentId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                '}';
    }
}
