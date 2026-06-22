package com.seeker.agent.core.log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LogRecord} builder.
 *
 * <p>Framework별 log event converter가 필요한 필드만 단계적으로 채울 수 있도록 둔다.
 * 기본 trace flag는 sampled(1)로 둔다. 실제 sampling 정책이 들어오면 converter에서
 * 현재 trace의 sampling 결과를 명시적으로 설정한다.
 */
public final class LogRecordBuilder {

    long timestamp;
    long observedTimestamp;

    String traceId;
    long spanId = -1;
    long parentSpanId = -1;
    byte traceFlags = 1;

    String agentId;
    String serviceName;
    String agentGroup;

    String loggerName;
    String threadName;
    String severityText;
    int severityNumber = LogSeverity.UNSPECIFIED.getSeverityNumber();
    String body;

    Map<String, String> attributes;

    String exceptionType;
    String exceptionMessage;
    String exceptionStacktrace;

    LogRecordBuilder() {
    }

    public LogRecordBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LogRecordBuilder observedTimestamp(long observedTimestamp) {
        this.observedTimestamp = observedTimestamp;
        return this;
    }

    public LogRecordBuilder traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public LogRecordBuilder spanId(long spanId) {
        this.spanId = spanId;
        return this;
    }

    public LogRecordBuilder parentSpanId(long parentSpanId) {
        this.parentSpanId = parentSpanId;
        return this;
    }

    public LogRecordBuilder traceFlags(byte traceFlags) {
        this.traceFlags = traceFlags;
        return this;
    }

    public LogRecordBuilder agentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public LogRecordBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public LogRecordBuilder agentGroup(String agentGroup) {
        this.agentGroup = agentGroup;
        return this;
    }

    public LogRecordBuilder loggerName(String loggerName) {
        this.loggerName = loggerName;
        return this;
    }

    public LogRecordBuilder threadName(String threadName) {
        this.threadName = threadName;
        return this;
    }

    public LogRecordBuilder severity(LogSeverity severity) {
        if (severity != null) {
            this.severityText = severity.getSeverityText();
            this.severityNumber = severity.getSeverityNumber();
        }
        return this;
    }

    public LogRecordBuilder severity(String severityText, int severityNumber) {
        this.severityText = severityText;
        this.severityNumber = severityNumber;
        return this;
    }

    public LogRecordBuilder severityText(String severityText) {
        this.severityText = severityText;
        return this;
    }

    public LogRecordBuilder severityNumber(int severityNumber) {
        this.severityNumber = severityNumber;
        return this;
    }

    public LogRecordBuilder body(String body) {
        this.body = body;
        return this;
    }

    public LogRecordBuilder attributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            this.attributes = null;
        } else {
            this.attributes = new LinkedHashMap<>(attributes);
        }
        return this;
    }

    public LogRecordBuilder attribute(String key, String value) {
        if (key == null || value == null) {
            return this;
        }
        if (this.attributes == null) {
            this.attributes = new LinkedHashMap<>();
        }
        this.attributes.put(key, value);
        return this;
    }

    public LogRecordBuilder exceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
        return this;
    }

    public LogRecordBuilder exceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    public LogRecordBuilder exceptionStacktrace(String exceptionStacktrace) {
        this.exceptionStacktrace = exceptionStacktrace;
        return this;
    }

    public LogRecord build() {
        long now = System.currentTimeMillis();
        if (timestamp <= 0) {
            timestamp = now;
        }
        if (observedTimestamp <= 0) {
            observedTimestamp = now;
        }
        return new LogRecord(this);
    }
}
