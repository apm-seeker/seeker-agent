package com.seeker.agent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentIdentityConfigTest {

    @Nested
    @DisplayName("agentId")
    class AgentId {
        @Test
        @DisplayName("프로퍼티 값을 그대로 보관한다")
        void usesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.agent-identity.id", "my-agent");

            AgentIdentityConfig config = new AgentIdentityConfig(props);

            assertEquals("my-agent", config.getAgentId());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 unnamed-agent를 사용한다")
        void defaultsToUnnamedAgent() {
            AgentIdentityConfig config = new AgentIdentityConfig(new Properties());

            assertEquals("unnamed-agent", config.getAgentId());
        }
    }

    @Nested
    @DisplayName("applicationName")
    class ApplicationName {
        @Test
        @DisplayName("프로퍼티 값을 그대로 보관한다")
        void usesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.agent-identity.application-name", "my-app");

            AgentIdentityConfig config = new AgentIdentityConfig(props);

            assertEquals("my-app", config.getApplicationName());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 unnamed-application을 사용한다")
        void defaultsToUnnamedApplication() {
            AgentIdentityConfig config = new AgentIdentityConfig(new Properties());

            assertEquals("unnamed-application", config.getApplicationName());
        }
    }
}
