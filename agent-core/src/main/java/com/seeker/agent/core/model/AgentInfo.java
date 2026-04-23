package com.seeker.agent.core.model;

/**
 * 에이전트 기동 시 Collector로 전송되는 에이전트 식별 정보입니다.
 */
public class AgentInfo {

    private final String agentId;
    private final String agentName;
    private final String agentType;
    private final String agentGroup;
    private final long startTime;

    public AgentInfo(String agentId, String agentName, String agentType, String agentGroup, long startTime) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentType = agentType;
        this.agentGroup = agentGroup;
        this.startTime = startTime;
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

    public long getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return "AgentInfo{" +
                "agentId='" + agentId + '\'' +
                ", agentName='" + agentName + '\'' +
                ", agentType='" + agentType + '\'' +
                ", agentGroup='" + agentGroup + '\'' +
                ", startTime=" + startTime +
                '}';
    }
}
