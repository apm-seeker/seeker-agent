package com.seeker.agent.sender.log;

import com.seeker.agent.core.log.LogRecord;
import com.seeker.collector.global.grpc.DataMessage;
import com.seeker.collector.global.grpc.LogBatch;
import com.seeker.collector.global.grpc.LogMessage;
import com.seeker.collector.global.grpc.TraceId;

import java.util.List;

/**
 * 도메인 {@link LogRecord} batch를 collector gRPC proto 메시지로 변환한다.
 */
public class GrpcLogMessageConverter {

    public DataMessage toDataMessage(List<LogRecord> records) {
        LogBatch.Builder batchBuilder = LogBatch.newBuilder();
        if (records != null) {
            for (LogRecord record : records) {
                if (record != null) {
                    batchBuilder.addLogs(toLogMessage(record));
                }
            }
        }
        return DataMessage.newBuilder()
                .setLogBatch(batchBuilder.build())
                .build();
    }

    private LogMessage toLogMessage(LogRecord record) {
        LogMessage.Builder builder = LogMessage.newBuilder()
                .setTimestamp(record.getTimestamp())
                .setObservedTimestamp(record.getObservedTimestamp())
                .setTraceId(TraceId.newBuilder()
                        .setTraceId(nullSafe(record.getTraceId()))
                        .setSpanId(record.getSpanId())
                        .setParentSpanId(record.getParentSpanId())
                        .setFlags(record.getTraceFlags())
                        .build())
                .setTraceFlags(record.getTraceFlags())
                .setAgentId(nullSafe(record.getAgentId()))
                .setServiceName(nullSafe(record.getServiceName()))
                .setAgentGroup(nullSafe(record.getAgentGroup()))
                .setLoggerName(nullSafe(record.getLoggerName()))
                .setThreadName(nullSafe(record.getThreadName()))
                .setSeverityText(nullSafe(record.getSeverityText()))
                .setSeverityNumber(record.getSeverityNumber())
                .setBody(nullSafe(record.getBody()))
                .setExceptionType(nullSafe(record.getExceptionType()))
                .setExceptionMessage(nullSafe(record.getExceptionMessage()))
                .setExceptionStacktrace(nullSafe(record.getExceptionStacktrace()));

        if (record.getAttributes() != null && !record.getAttributes().isEmpty()) {
            builder.putAllAttributes(record.getAttributes());
        }
        return builder.build();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
