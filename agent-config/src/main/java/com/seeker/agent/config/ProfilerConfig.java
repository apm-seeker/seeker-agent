package com.seeker.agent.config;

import java.util.Properties;

/**
 * 프로파일러 세부 설정을 관리하는 클래스.
 * 플러그인 활성화 여부나 버퍼 크기 등을 제어합니다.
 */
public class ProfilerConfig {

    private final boolean jdbcEnabled;
    private final boolean httpEnabled;
    private final boolean springEnabled;
    private final int maxSpanEventCount;

    public ProfilerConfig(Properties properties) {
        this.jdbcEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.jdbc.enable", "true"));
        this.httpEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.http.enable", "true"));
        this.springEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.spring.enable", "true"));
        this.maxSpanEventCount = Integer
                .parseInt(properties.getProperty("seeker.profiler.max.span.event.count", "1500"));
    }

    public boolean isJdbcEnabled() {
        return jdbcEnabled;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public boolean isSpringEnabled() {
        return springEnabled;
    }

    public int getMaxSpanEventCount() {
        return maxSpanEventCount;
    }
}
