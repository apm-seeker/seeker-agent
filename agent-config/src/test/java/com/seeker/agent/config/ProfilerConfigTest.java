package com.seeker.agent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ProfilerConfigTest {

    @Test
    @DisplayName("프로퍼티로부터 프로파일러 설정을 올바르게 생성한다")
    void createFromProperties() {
        Properties props = new Properties();
        props.setProperty("seeker.profiler.jdbc.enable", "false");
        props.setProperty("seeker.profiler.http.enable", "true");
        props.setProperty("seeker.profiler.max.span.event.count", "500");

        ProfilerConfig config = new ProfilerConfig(props);

        assertFalse(config.isJdbcEnabled());
        assertTrue(config.isHttpEnabled());
        assertTrue(config.isSpringEnabled()); // 기본값 true 확인
        assertEquals(500, config.getMaxSpanEventCount());
    }

    @Test
    @DisplayName("설정이 없을 경우 기본값을 유지한다")
    void useDefaultValues() {
        ProfilerConfig config = new ProfilerConfig(new Properties());

        assertTrue(config.isJdbcEnabled());
        assertTrue(config.isHttpEnabled());
        assertTrue(config.isSpringEnabled());
        assertEquals(1500, config.getMaxSpanEventCount());
    }
}
