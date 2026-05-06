package com.seeker.agent.config.properties;

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
    // 디버그 모드: Collector에 연결하지 않고 수집 데이터를 콘솔에 출력
    private final boolean debugEnabled;
    // 메트릭 수집 활성화 여부 (전체 on/off)
    private final boolean metricEnabled;
    // 메트릭 수집 주기. MetricScheduler에서 [1000, 10000]로 클램프됨.
    private final long metricIntervalMs;
    // N cycle 누적 후 batch 전송. 5초 인터벌 × 6 = 30초마다 송신.
    private final int metricBatchSize;

    public ProfilerConfig(Properties properties) {
        this.jdbcEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.jdbc.enabled", "true"));
        this.httpEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.http.enabled", "true"));
        this.springEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.spring.enabled", "true"));
        this.maxSpanEventCount = Integer
                .parseInt(properties.getProperty("seeker.profiler.max-span-event-count", "1500"));
        this.samplingRate = Double.parseDouble(properties.getProperty("seeker.profiler.sampling-rate", "1.0"));
        this.basePackages = properties.getProperty("seeker.profiler.base-packages", "");
        this.debugEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.debug.enabled", "false"));
        this.metricEnabled = Boolean.parseBoolean(properties.getProperty("seeker.metric.enabled", "true"));
        this.metricIntervalMs = Long.parseLong(properties.getProperty("seeker.metric.interval.ms", "5000"));
        this.metricBatchSize = Integer.parseInt(properties.getProperty("seeker.metric.batch.size", "6"));
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

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isMetricEnabled() {
        return metricEnabled;
    }

    public long getMetricIntervalMs() {
        return metricIntervalMs;
    }

    public int getMetricBatchSize() {
        return metricBatchSize;
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
                ", debugEnabled=" + debugEnabled +
                ", metricEnabled=" + metricEnabled +
                ", metricIntervalMs=" + metricIntervalMs +
                ", metricBatchSize=" + metricBatchSize +
                '}';
    }
}
