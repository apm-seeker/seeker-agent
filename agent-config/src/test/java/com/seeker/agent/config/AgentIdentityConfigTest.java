package com.seeker.agent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AgentIdentityConfigTest {

    @Nested
    @DisplayName("agentId")
    class AgentId {
        @Test
        @DisplayName("UUID 형식의 값을 자동으로 생성한다")
        void generatesUuid() {
            AgentIdentityConfig config = new AgentIdentityConfig(new Properties());

            assertDoesNotThrow(() -> UUID.fromString(config.getAgentId()));
        }

        @Test
        @DisplayName("인스턴스마다 서로 다른 ID를 생성한다")
        void uniquePerInstance() {
            AgentIdentityConfig a = new AgentIdentityConfig(new Properties());
            AgentIdentityConfig b = new AgentIdentityConfig(new Properties());

            assertNotEquals(a.getAgentId(), b.getAgentId());
        }
    }

    @Nested
    @DisplayName("agentName")
    class AgentName {
        @Test
        @DisplayName("프로퍼티 값을 그대로 보관한다")
        void usesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.agent-identity.name", "my-agent");

            AgentIdentityConfig config = new AgentIdentityConfig(props);

            assertEquals("my-agent", config.getAgentName());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 agentId의 앞 8자를 기본값으로 사용한다")
        void defaultsToAgentIdPrefix() {
            AgentIdentityConfig config = new AgentIdentityConfig(new Properties());

            assertEquals(config.getAgentId().substring(0, 8), config.getAgentName());
        }
    }

    @Nested
    @DisplayName("agentType")
    class AgentType {
        @Test
        @DisplayName("Tomcat으로 고정되어 있다")
        void isFixedToTomcat() {
            AgentIdentityConfig config = new AgentIdentityConfig(new Properties());

            assertEquals("Tomcat", config.getAgentType());
        }
    }

    @Nested
    @DisplayName("agentGroup")
    class AgentGroup {
        @Test
        @DisplayName("프로퍼티 값을 그대로 보관한다")
        void usesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.agent-identity.group", "my-group");

            AgentIdentityConfig config = new AgentIdentityConfig(props);

            assertEquals("my-group", config.getAgentGroup());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 agentId의 앞 8자를 기본값으로 사용한다")
        void defaultsToAgentIdPrefix() {
            AgentIdentityConfig config = new AgentIdentityConfig(new Properties());

            assertEquals(config.getAgentId().substring(0, 8), config.getAgentGroup());
        }
    }
}
