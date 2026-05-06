package com.seeker.agent.config.loader;

import com.seeker.agent.config.properties.AgentIdentityConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesLoaderTest {

    @BeforeEach
    void setUp() {
        System.clearProperty("seeker.agent-identity.name");
        System.clearProperty("seeker.agent-identity.group");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("seeker.agent-identity.name");
        System.clearProperty("seeker.agent-identity.group");
    }

    @Test
    @DisplayName("시스템 프로퍼티 값이 로드된 Properties에 포함된다")
    void systemPropertyIncluded() {
        System.setProperty("seeker.agent-identity.name", "loader-agent");
        System.setProperty("seeker.agent-identity.group", "loader-group");

        Properties properties = PropertiesLoader.load();

        assertEquals("loader-agent", properties.getProperty("seeker.agent-identity.name"));
        assertEquals("loader-group", properties.getProperty("seeker.agent-identity.group"));
    }

    @Test
    @DisplayName("시스템 프로퍼티로 주입한 값이 Config 객체에 반영된다")
    void systemPropertyOverridesConfig() {
        System.setProperty("seeker.agent-identity.name", "override-agent");
        System.setProperty("seeker.agent-identity.group", "override-group");

        Properties properties = PropertiesLoader.load();
        AgentIdentityConfig identity = new AgentIdentityConfig(properties);

        assertEquals("override-agent", identity.getAgentName());
        assertEquals("override-group", identity.getAgentGroup());
    }
}
