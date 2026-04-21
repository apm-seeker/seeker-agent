package com.seeker.agent.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentConfigTest {

    @BeforeEach
    void setUp() {
        // 테스트 전 관련 시스템 프로퍼티 초기화
        System.clearProperty("seeker.agentId");
        System.clearProperty("seeker.applicationName");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("seeker.agentId");
        System.clearProperty("seeker.applicationName");
    }

    @Test
    @DisplayName("기본 설정 파일(seeker.config)을 올바르게 로드한다")
    void loadDefaultConfig() {
        AgentConfig config = AgentConfig.load();

        assertNotNull(config);
        // seeker.config에 정의된 기본값들 확인 (구현 시 넣은 값 기준)
        assertEquals("seeker-test-agent", config.getAgentId());
        assertEquals("seeker-test-app", config.getApplicationName());
        assertEquals("127.0.0.1", config.getCollectorHost());
        assertEquals(9991, config.getCollectorPort());
        assertEquals(1.0, config.getSamplingRate());
    }

    @Test
    @DisplayName("시스템 프로퍼티가 설정 파일보다 우선순위가 높다")
    void systemPropertyOverride() {
        System.setProperty("seeker.agentId", "overridden-agent");
        System.setProperty("seeker.applicationName", "overridden-app");

        AgentConfig config = AgentConfig.load();

        assertEquals("overridden-agent", config.getAgentId());
        assertEquals("overridden-app", config.getApplicationName());
    }
}
