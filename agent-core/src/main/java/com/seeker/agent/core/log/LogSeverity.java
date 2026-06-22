package com.seeker.agent.core.log;

import java.util.Locale;

/**
 * Logging framework level을 OpenTelemetry severity number 범위에 맞춰 단순 매핑한다.
 */
public enum LogSeverity {

    TRACE("TRACE", 1),
    DEBUG("DEBUG", 5),
    INFO("INFO", 9),
    WARN("WARN", 13),
    ERROR("ERROR", 17),
    FATAL("FATAL", 21),
    UNSPECIFIED("UNSPECIFIED", 0);

    private final String severityText;
    private final int severityNumber;

    LogSeverity(String severityText, int severityNumber) {
        this.severityText = severityText;
        this.severityNumber = severityNumber;
    }

    public String getSeverityText() {
        return severityText;
    }

    public int getSeverityNumber() {
        return severityNumber;
    }

    public static LogSeverity fromText(String level) {
        if (level == null || level.isEmpty()) {
            return UNSPECIFIED;
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        if ("WARNING".equals(normalized)) {
            return WARN;
        }
        if ("SEVERE".equals(normalized)) {
            return ERROR;
        }
        for (LogSeverity severity : values()) {
            if (severity.severityText.equals(normalized)) {
                return severity;
            }
        }
        return UNSPECIFIED;
    }
}
