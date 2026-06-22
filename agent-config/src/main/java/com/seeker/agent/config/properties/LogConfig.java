package com.seeker.agent.config.properties;

import java.util.Properties;

/**
 * Log trace 수집 설정.
 *
 * <p>초기 기본값은 보수적으로 둔다. 로그는 trace/span보다 데이터량이 크기 때문에
 * 명시적으로 켠 경우에만 수집하고, 기본적으로 trace 안의 ERROR 이상 로그만 대상으로 한다.
 */
public class LogConfig {

    private final boolean enabled;
    private final boolean logbackEnabled;
    private final boolean log4j2Enabled;
    private final boolean julEnabled;
    private final String minLevel;
    private final boolean onlyTraced;
    private final boolean mdcEnabled;
    private final String mdcKeys;
    private final int maxMessageLength;
    private final int maxStacktraceLength;
    private final int queueCapacity;
    private final int batchSize;
    private final long flushIntervalMs;

    public LogConfig(Properties properties) {
        this.enabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.log.enabled", "false"));
        this.logbackEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.log.logback.enabled", "true"));
        this.log4j2Enabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.log.log4j2.enabled", "false"));
        this.julEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.log.jul.enabled", "false"));
        this.minLevel = properties.getProperty("seeker.profiler.log.min-level", "ERROR");
        this.onlyTraced = Boolean.parseBoolean(properties.getProperty("seeker.profiler.log.only-traced", "true"));
        this.mdcEnabled = Boolean.parseBoolean(properties.getProperty("seeker.profiler.log.mdc.enabled", "false"));
        this.mdcKeys = properties.getProperty("seeker.profiler.log.mdc.keys", "");
        this.maxMessageLength = parseInt(properties, "seeker.profiler.log.max-message-length", 4096);
        this.maxStacktraceLength = parseInt(properties, "seeker.profiler.log.max-stacktrace-length", 8192);
        this.queueCapacity = parseInt(properties, "seeker.profiler.log.queue.capacity", 8192);
        this.batchSize = parseInt(properties, "seeker.profiler.log.batch.size", 100);
        this.flushIntervalMs = parseLong(properties, "seeker.profiler.log.flush.interval.ms", 1000L);
    }

    private int parseInt(Properties properties, String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private long parseLong(Properties properties, String key, long defaultValue) {
        try {
            return Long.parseLong(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLogbackEnabled() {
        return logbackEnabled;
    }

    public boolean isLog4j2Enabled() {
        return log4j2Enabled;
    }

    public boolean isJulEnabled() {
        return julEnabled;
    }

    public String getMinLevel() {
        return minLevel;
    }

    public boolean isOnlyTraced() {
        return onlyTraced;
    }

    public boolean isMdcEnabled() {
        return mdcEnabled;
    }

    public String getMdcKeys() {
        return mdcKeys;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public int getMaxStacktraceLength() {
        return maxStacktraceLength;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    @Override
    public String toString() {
        return "LogConfig{" +
                "enabled=" + enabled +
                ", logbackEnabled=" + logbackEnabled +
                ", log4j2Enabled=" + log4j2Enabled +
                ", julEnabled=" + julEnabled +
                ", minLevel='" + minLevel + '\'' +
                ", onlyTraced=" + onlyTraced +
                ", mdcEnabled=" + mdcEnabled +
                ", mdcKeys='" + mdcKeys + '\'' +
                ", maxMessageLength=" + maxMessageLength +
                ", maxStacktraceLength=" + maxStacktraceLength +
                ", queueCapacity=" + queueCapacity +
                ", batchSize=" + batchSize +
                ", flushIntervalMs=" + flushIntervalMs +
                '}';
    }
}
