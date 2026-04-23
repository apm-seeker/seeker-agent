package com.seeker.agent.config;

import java.util.Properties;

/**
 * 프로파일러 세부 설정을 관리하는 클래스.
 * 플러그인 활성화 여부, 버퍼 크기, 샘플링 비율, 계측 대상 패키지 등을 제어합니다.
 */
public class ProfilerConfig {

    private final boolean jdbcEnabled;
    private final boolean httpEnabled;
    private final boolean springEnabled;
    private final int maxSpanEventCount;
    // TODO 추후 sample 구현 예정
    private final double samplingRate;
    // 특정 패키지 하위의 클래스를 추적할 base packages
    private final String basePackages;

    public ProfilerConfig(Properties properties) {
        this.jdbcEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.jdbc.enabled", "true"));
        this.httpEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.http.enabled", "true"));
        this.springEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.spring.enabled", "true"));
        this.maxSpanEventCount = Integer
                .parseInt(properties.getProperty("seeker.profiler.max-span-event-count", "1500"));
        this.samplingRate = Double.parseDouble(properties.getProperty("seeker.profiler.sampling-rate", "1.0"));
        this.basePackages = properties.getProperty("seeker.profiler.base-packages", "");
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

    public double getSamplingRate() {
        return samplingRate;
    }

    public String getBasePackages() {
        return basePackages;
    }

    @Override
    public String toString() {
        return "ProfilerConfig{" +
                "jdbcEnabled=" + jdbcEnabled +
                ", httpEnabled=" + httpEnabled +
                ", springEnabled=" + springEnabled +
                ", maxSpanEventCount=" + maxSpanEventCount +
                ", samplingRate=" + samplingRate +
                ", basePackages='" + basePackages + '\'' +
                '}';
    }
}
