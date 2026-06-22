package com.seeker.agent.core.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogRecordTest {

    @Test
    @DisplayName("LogRecord builder가 필드를 채우고 attributes를 불변 복사한다")
    void buildLogRecord() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("log.framework", "logback");

        LogRecord record = LogRecord.builder()
                .timestamp(1000L)
                .observedTimestamp(2000L)
                .traceId("0123456789abcdef0123456789abcdef")
                .spanId(10L)
                .parentSpanId(5L)
                .agentId("agent-1")
                .serviceName("order-service")
                .agentGroup("order")
                .loggerName("com.example.OrderService")
                .threadName("http-nio-8080-exec-1")
                .severity(LogSeverity.ERROR)
                .body("payment failed")
                .attributes(attributes)
                .exceptionType("java.lang.IllegalStateException")
                .exceptionMessage("failed")
                .exceptionStacktrace("stacktrace")
                .build();

        attributes.put("log.framework", "changed");

        assertEquals(1000L, record.getTimestamp());
        assertEquals(2000L, record.getObservedTimestamp());
        assertEquals("0123456789abcdef0123456789abcdef", record.getTraceId());
        assertEquals(10L, record.getSpanId());
        assertEquals(5L, record.getParentSpanId());
        assertEquals("agent-1", record.getAgentId());
        assertEquals("order-service", record.getServiceName());
        assertEquals("order", record.getAgentGroup());
        assertEquals("com.example.OrderService", record.getLoggerName());
        assertEquals("http-nio-8080-exec-1", record.getThreadName());
        assertEquals("ERROR", record.getSeverityText());
        assertEquals(17, record.getSeverityNumber());
        assertEquals("payment failed", record.getBody());
        assertEquals("logback", record.getAttributes().get("log.framework"));
        assertThrows(UnsupportedOperationException.class,
                () -> record.getAttributes().put("another", "value"));
        assertEquals("java.lang.IllegalStateException", record.getExceptionType());
        assertEquals("failed", record.getExceptionMessage());
        assertEquals("stacktrace", record.getExceptionStacktrace());
        assertTrue(record.hasTraceContext());
    }

    @Test
    @DisplayName("timestamp가 없으면 build 시 현재 시각 기반 기본값을 채운다")
    void defaultTimestamps() {
        long before = System.currentTimeMillis();

        LogRecord record = LogRecord.builder()
                .severity(LogSeverity.INFO)
                .body("hello")
                .build();

        long after = System.currentTimeMillis();

        assertTrue(record.getTimestamp() >= before);
        assertTrue(record.getTimestamp() <= after);
        assertTrue(record.getObservedTimestamp() >= before);
        assertTrue(record.getObservedTimestamp() <= after);
    }
}
