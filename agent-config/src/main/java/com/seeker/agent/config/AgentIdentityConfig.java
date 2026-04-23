package com.seeker.agent.config;

import java.util.Properties;
import java.util.UUID;

/**
 * 에이전트 식별 정보를 관리하는 클래스.
 * 에이전트 ID(자동 생성), 이름, 타입, 그룹을 보관합니다.
 */
public class AgentIdentityConfig {

    private final String agentId;
    private final String agentName;
    private final String agentType;
    private final String agentGroup;

    public AgentIdentityConfig(Properties properties) {
        this.agentId = UUID.randomUUID().toString();
        String idPrefix = agentId.substring(0, 8);
        this.agentName = properties.getProperty("seeker.agent-identity.name", idPrefix);
        // TODO 추후 런타임 감지로 자동 결정하도록 변경
        this.agentType = "Tomcat";
        this.agentGroup = properties.getProperty("seeker.agent-identity.group", idPrefix);
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getAgentGroup() {
        return agentGroup;
    }

    @Override
    public String toString() {
        return "AgentIdentityConfig{" +
                "agentId='" + agentId + '\'' +
                ", agentName='" + agentName + '\'' +
                ", agentType='" + agentType + '\'' +
                ", agentGroup='" + agentGroup + '\'' +
                '}';
    }
}