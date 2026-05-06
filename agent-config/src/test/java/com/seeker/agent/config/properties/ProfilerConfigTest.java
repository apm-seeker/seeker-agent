package com.seeker.agent.config.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilerConfigTest {

    @Nested
    @DisplayName("jdbcEnabled")
    class JdbcEnabled {
        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "true, true",
                "false, false",
                "TRUE, true",
                "invalid, false"
        })
        @DisplayName("프로퍼티 값을 boolean으로 파싱한다")
        void parsesProperty(String input, boolean expected) {
            Properties props = new Properties();
            props.setProperty("seeker.profiler.jdbc.enabled", input);

            ProfilerConfig config = new ProfilerConfig(props);

            assertEquals(expected, config.isJdbcEnabled());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 true를 사용한다")
        void defaultsToTrue() {
            ProfilerConfig config = new ProfilerConfig(new Properties());

            assertTrue(config.isJdbcEnabled());
        }
    }

    @Nested
    @DisplayName("httpEnabled")
    class HttpEnabled {
        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "true, true",
                "false, false",
                "TRUE, true",
                "invalid, false"
        })
        @DisplayName("프로퍼티 값을 boolean으로 파싱한다")
        void parsesProperty(String input, boolean expected) {
            Properties props = new Properties();
            props.setProperty("seeker.profiler.http.enabled", input);

            ProfilerConfig config = new ProfilerConfig(props);

            assertEquals(expected, config.isHttpEnabled());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 true를 사용한다")
        void defaultsToTrue() {
            ProfilerConfig config = new ProfilerConfig(new Properties());

            assertTrue(config.isHttpEnabled());
        }
    }

    @Nested
    @DisplayName("springEnabled")
    class SpringEnabled {
        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @CsvSource({
                "true, true",
                "false, false",
                "TRUE, true",
                "invalid, false"
        })
        @DisplayName("프로퍼티 값을 boolean으로 파싱한다")
        void parsesProperty(String input, boolean expected) {
            Properties props = new Properties();
            props.setProperty("seeker.profiler.spring.enabled", input);

            ProfilerConfig config = new ProfilerConfig(props);

            assertEquals(expected, config.isSpringEnabled());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 true를 사용한다")
        void defaultsToTrue() {
            ProfilerConfig config = new ProfilerConfig(new Properties());

            assertTrue(config.isSpringEnabled());
        }
    }

    @Nested
    @DisplayName("maxSpanEventCount")
    class MaxSpanEventCount {
        @Test
        @DisplayName("프로퍼티 값을 int로 파싱한다")
        void parsesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.profiler.max-span-event-count", "500");

            ProfilerConfig config = new ProfilerConfig(props);

            assertEquals(500, config.getMaxSpanEventCount());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 1500을 사용한다")
        void defaultsTo1500() {
            ProfilerConfig config = new ProfilerConfig(new Properties());

            assertEquals(1500, config.getMaxSpanEventCount());
        }
    }

    @Nested
    @DisplayName("samplingRate")
    class SamplingRate {
        @Test
        @DisplayName("프로퍼티 값을 double로 파싱한다")
        void parsesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.profiler.sampling-rate", "0.5");

            ProfilerConfig config = new ProfilerConfig(props);

            assertEquals(0.5, config.getSamplingRate());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 1.0을 사용한다")
        void defaultsTo1() {
            ProfilerConfig config = new ProfilerConfig(new Properties());

            assertEquals(1.0, config.getSamplingRate());
        }
    }

    @Nested
    @DisplayName("basePackages")
    class BasePackages {
        @Test
        @DisplayName("프로퍼티 값을 그대로 보관한다")
        void usesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.profiler.base-packages", "com.example.a,com.example.b");

            ProfilerConfig config = new ProfilerConfig(props);

            assertEquals("com.example.a,com.example.b", config.getBasePackages());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 빈 문자열을 사용한다")
        void defaultsToEmpty() {
            ProfilerConfig config = new ProfilerConfig(new Properties());

            assertEquals("", config.getBasePackages());
        }
    }
}
