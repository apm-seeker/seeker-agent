package com.seeker.agent.core.model;

public final class AgentInfoInitializer {

    private AgentInfoInitializer() {
    }

    public static AgentInfo initialize(String agentId,
                                       String agentName,
                                       String agentType,
                                       String agentGroup,
                                       long startTimeMs) {
        AgentInfo agentInfo = new AgentInfo(agentId, agentName, agentType, agentGroup, startTimeMs);
        AgentInfoHolder.set(agentInfo);
        return agentInfo;
    }
}
