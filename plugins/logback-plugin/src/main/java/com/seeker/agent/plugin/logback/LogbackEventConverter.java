package com.seeker.agent.plugin.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.model.AgentInfoHolder;
import com.seeker.agent.core.model.Trace;
import com.seeker.agent.core.log.LogRecord;
import com.seeker.agent.core.log.LogRecordBuilder;
import com.seeker.agent.core.log.LogSeverity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Logback ILoggingEvent를 Seeker LogRecord로 변환한다.
 */
public class LogbackEventConverter {

    private final int minSeverityNumber;
    private final boolean onlyTraced;
    private final boolean mdcEnabled;
    private final Set<String> mdcKeys;
    private final int maxMessageLength;
    private final int maxStacktraceLength;

    public LogbackEventConverter(int minSeverityNumber,
                                 boolean onlyTraced,
                                 boolean mdcEnabled,
                                 String mdcKeys,
                                 int maxMessageLength,
                                 int maxStacktraceLength) {
        this.minSeverityNumber = minSeverityNumber;
        this.onlyTraced = onlyTraced;
        this.mdcEnabled = mdcEnabled;
        this.mdcKeys = parseKeys(mdcKeys);
        this.maxMessageLength = Math.max(0, maxMessageLength);
        this.maxStacktraceLength = Math.max(0, maxStacktraceLength);
    }

    public LogRecord convert(ILoggingEvent event) {
        if (event == null || isAgentLogger(event.getLoggerName())) {
            return null;
        }

        LogSeverity severity = LogSeverity.fromText(event.getLevel() == null ? null : event.getLevel().toString());
        if (severity.getSeverityNumber() < minSeverityNumber) {
            return null;
        }

        Trace trace = TraceContextHolder.getContext().currentTraceObject();
        if (onlyTraced && trace == null) {
            return null;
        }

        AgentInfo agentInfo = AgentInfoHolder.get();
        LogRecordBuilder builder = LogRecord.builder()
                .timestamp(event.getTimeStamp())
                .observedTimestamp(System.currentTimeMillis())
                .loggerName(event.getLoggerName())
                .threadName(event.getThreadName())
                .severity(severity)
                .body(truncate(event.getFormattedMessage(), maxMessageLength))
                .attribute("log.framework", "logback");

        if (trace != null) {
            builder.traceId(trace.getTraceId().getTraceId())
                    .spanId(trace.getTraceId().getSpanId())
                    .parentSpanId(trace.getTraceId().getParentSpanId());
        }
        if (agentInfo != null) {
            builder.agentId(agentInfo.getAgentId())
                    .serviceName(agentInfo.getAgentName())
                    .agentGroup(agentInfo.getAgentGroup());
        }

        addMdc(builder, event.getMDCPropertyMap());
        addThrowable(builder, event.getThrowableProxy());

        return builder.build();
    }

    private boolean isAgentLogger(String loggerName) {
        return loggerName != null &&
                (loggerName.startsWith("com.seeker.agent.")
                        || loggerName.startsWith("io.grpc.")
                        || loggerName.startsWith("io.netty."));
    }

    private void addMdc(LogRecordBuilder builder, Map<String, String> mdc) {
        if (!mdcEnabled || mdc == null || mdc.isEmpty() || mdcKeys.isEmpty()) {
            return;
        }
        for (String key : mdcKeys) {
            String value = mdc.get(key);
            if (value != null) {
                builder.attribute("mdc." + key, value);
            }
        }
    }

    private void addThrowable(LogRecordBuilder builder, IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return;
        }
        builder.exceptionType(throwableProxy.getClassName())
                .exceptionMessage(throwableProxy.getMessage())
                .exceptionStacktrace(truncate(ThrowableProxyUtil.asString(throwableProxy), maxStacktraceLength));
    }

    private Set<String> parseKeys(String keys) {
        Set<String> out = new HashSet<>();
        if (keys == null || keys.trim().isEmpty()) {
            return out;
        }
        Arrays.stream(keys.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(out::add);
        return out;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
