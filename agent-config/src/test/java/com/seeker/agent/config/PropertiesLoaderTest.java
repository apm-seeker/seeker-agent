package com.seeker.agent.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesLoaderTest {

    @BeforeEach
    void setUp() {
        System.clearProperty("seeker.agent-identity.id");
        System.clearProperty("seeker.agent-identity.application-name");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("seeker.agent-identity.id");
        System.clearProperty("seeker.agent-identity.application-name");
    }

    @Test
    @DisplayName("시스템 프로퍼티 값이 로드된 Properties에 포함된다")
    void systemPropertyIncluded() {
        System.setProperty("seeker.agent-identity.id", "loader-agent");
        System.setProperty("seeker.agent-identity.application-name", "loader-app");

        Properties properties = PropertiesLoader.load();

        assertEquals("loader-agent", properties.getProperty("seeker.agent-identity.id"));
        assertEquals("loader-app", properties.getProperty("seeker.agent-identity.application-name"));
    }

    @Test
    @DisplayName("시스템 프로퍼티로 주입한 값이 Config 객체에 반영된다")
    void systemPropertyOverridesConfig() {
        System.setProperty("seeker.agent-identity.id", "override-agent");
        System.setProperty("seeker.agent-identity.application-name", "override-app");

        Properties properties = PropertiesLoader.load();
        AgentIdentityConfig identity = new AgentIdentityConfig(properties);

        assertEquals("override-agent", identity.getAgentId());
        assertEquals("override-app", identity.getApplicationName());
    }
}
