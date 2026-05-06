package com.seeker.agent.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeekerConfigTest {

    private static final String[] KEYS = {
            "seeker.agent-identity.name",
            "seeker.agent-identity.group",
            "seeker.collector.host",
            "seeker.collector.grpc-port",
            "seeker.collector.http-port",
            "seeker.profiler.debug.enabled"
    };

    @BeforeEach
    void setUp() {
        clearProperties();
    }

    @AfterEach
    void tearDown() {
        clearProperties();
    }

    @Test
    @DisplayName("로드된 Properties를 하위 설정 객체로 묶는다")
    void loadCreatesNestedConfigObjects() {
        System.setProperty("seeker.agent-identity.name", "seeker-test-agent");
        System.setProperty("seeker.agent-identity.group", "seeker-test-group");
        System.setProperty("seeker.collector.host", "collector.test");
        System.setProperty("seeker.collector.grpc-port", "19099");
        System.setProperty("seeker.collector.http-port", "18081");
        System.setProperty("seeker.profiler.debug.enabled", "true");

        SeekerConfig config = SeekerConfig.load();

        assertEquals("seeker-test-agent", config.identity().getAgentName());
        assertEquals("seeker-test-group", config.identity().getAgentGroup());
        assertEquals("collector.test", config.collector().getHost());
        assertEquals(19099, config.collector().getGrpcPort());
        assertEquals(18081, config.collector().getHttpPort());
        assertTrue(config.profiler().isDebugEnabled());
    }

    private void clearProperties() {
        for (String key : KEYS) {
            System.clearProperty(key);
        }
    }
}
