package com.seeker.agent.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AgentInfoInitializerTest {

    @Test
    @DisplayName("AgentInfo를 생성하고 AgentInfoHolder에 등록한다")
    void initializeAgentInfo() {
        AgentInfo agentInfo = AgentInfoInitializer.initialize(
                "agent-id",
                "agent-name",
                "agent-type",
                "agent-group",
                123L);

        assertEquals("agent-id", agentInfo.getAgentId());
        assertEquals("agent-name", agentInfo.getAgentName());
        assertEquals("agent-type", agentInfo.getAgentType());
        assertEquals("agent-group", agentInfo.getAgentGroup());
        assertEquals(123L, agentInfo.getStartTime());
        assertSame(agentInfo, AgentInfoHolder.get());
    }
}
