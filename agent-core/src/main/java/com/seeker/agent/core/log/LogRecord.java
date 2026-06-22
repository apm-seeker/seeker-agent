package com.seeker.agent.core.log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logging framework에서 관찰한 단일 로그 이벤트의 agent 내부 표현.
 *
 * <p>OpenTelemetry Logs Data Model의 핵심 필드(timestamp, trace/span correlation,
 * severity, body, attributes)를 현재 Seeker trace 모델에 맞춰 단순화했다.
 * Logback/Log4j2 같은 plugin은 framework event를 이 객체로 변환한 뒤 sender에 넘긴다.
 */
public final class LogRecord {

    /** Logging framework가 로그 이벤트를 생성한 시각(ms). */
    private final long timestamp;

    /** Seeker agent가 로그 이벤트를 관찰해 LogRecord로 변환한 시각(ms). */
    private final long observedTimestamp;

    /** 분산 trace 전체에서 공유되는 32-char hex trace id. trace 밖 로그면 null일 수 있다. */
    private final String traceId;

    /** 현재 서비스에서 생성한 local span id. trace 밖 로그면 -1. */
    private final long spanId;

    /** 현재 span의 parent span id. root span이거나 trace 밖 로그면 -1. */
    private final long parentSpanId;

    /** W3C trace flags. 초기 구현은 sampled=1로 둔다. */
    private final byte traceFlags;

    /** Collector와 dashboard에서 agent instance를 식별하는 id. */
    private final String agentId;

    /** 사용자가 설정한 service/application 이름. */
    private final String serviceName;

    /** 같은 역할의 agent instance를 묶는 group 이름. */
    private final String agentGroup;

    /** 로그를 발생시킨 logger 이름. 보통 class/package 이름이다. */
    private final String loggerName;

    /** 로그 이벤트가 발생한 thread 이름. */
    private final String threadName;

    /** 원본 logging framework의 level 문자열. 예: INFO, WARN, ERROR. */
    private final String severityText;

    /** severityText를 OpenTelemetry severity number 범위에 맞춰 변환한 값. */
    private final int severityNumber;

    /** 최종 formatting이 끝난 로그 메시지 본문. */
    private final String body;

    /** MDC allowlist, framework 이름, marker 등 로그 부가 정보. */
    private final Map<String, String> attributes;

    /** 예외가 함께 기록된 경우 예외 class 이름. */
    private final String exceptionType;

    /** 예외가 함께 기록된 경우 예외 메시지. */
    private final String exceptionMessage;

    /** 예외가 함께 기록된 경우 문자열화한 stacktrace. 길이 제한은 converter/sender 단계에서 적용한다. */
    private final String exceptionStacktrace;

    LogRecord(LogRecordBuilder builder) {
        this.timestamp = builder.timestamp;
        this.observedTimestamp = builder.observedTimestamp;
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.parentSpanId = builder.parentSpanId;
        this.traceFlags = builder.traceFlags;
        this.agentId = builder.agentId;
        this.serviceName = builder.serviceName;
        this.agentGroup = builder.agentGroup;
        this.loggerName = builder.loggerName;
        this.threadName = builder.threadName;
        this.severityText = builder.severityText;
        this.severityNumber = builder.severityNumber;
        this.body = builder.body;
        this.attributes = builder.attributes == null || builder.attributes.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
        this.exceptionType = builder.exceptionType;
        this.exceptionMessage = builder.exceptionMessage;
        this.exceptionStacktrace = builder.exceptionStacktrace;
    }

    public static LogRecordBuilder builder() {
        return new LogRecordBuilder();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getObservedTimestamp() {
        return observedTimestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public long getSpanId() {
        return spanId;
    }

    public long getParentSpanId() {
        return parentSpanId;
    }

    public byte getTraceFlags() {
        return traceFlags;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getAgentGroup() {
        return agentGroup;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getSeverityText() {
        return severityText;
    }

    public int getSeverityNumber() {
        return severityNumber;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getExceptionStacktrace() {
        return exceptionStacktrace;
    }

    public boolean hasTraceContext() {
        return traceId != null && !traceId.isEmpty();
    }

    @Override
    public String toString() {
        return "LogRecord{" +
                "timestamp=" + timestamp +
                ", observedTimestamp=" + observedTimestamp +
                ", traceId='" + traceId + '\'' +
                ", spanId=" + spanId +
                ", parentSpanId=" + parentSpanId +
                ", agentId='" + agentId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", loggerName='" + loggerName + '\'' +
                ", threadName='" + threadName + '\'' +
                ", severityText='" + severityText + '\'' +
                ", severityNumber=" + severityNumber +
                ", body='" + body + '\'' +
                ", attributes=" + attributes +
                ", exceptionType='" + exceptionType + '\'' +
                '}';
    }
}
