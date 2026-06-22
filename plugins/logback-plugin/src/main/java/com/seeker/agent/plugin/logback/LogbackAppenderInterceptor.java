package com.seeker.agent.plugin.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.seeker.agent.core.log.LogCaptureGuard;
import com.seeker.agent.core.log.LogRecord;
import com.seeker.agent.core.sender.LogSenderHolder;
import com.seeker.agent.instrument.interceptor.AroundInterceptor;

/**
 * Logback appender의 doAppend 진입 시점에서 logging event를 수집한다.
 */
public class LogbackAppenderInterceptor implements AroundInterceptor {

    private final LogbackEventConverter converter;

    public LogbackAppenderInterceptor(int minSeverityNumber,
                                      boolean onlyTraced,
                                      boolean mdcEnabled,
                                      String mdcKeys,
                                      int maxMessageLength,
                                      int maxStacktraceLength) {
        this.converter = new LogbackEventConverter(
                minSeverityNumber,
                onlyTraced,
                mdcEnabled,
                mdcKeys,
                maxMessageLength,
                maxStacktraceLength);
    }

    @Override
    public void before(Object target, String className, String methodName, Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof ILoggingEvent)) {
            return;
        }
        if (!LogCaptureGuard.enter()) {
            return;
        }
        try {
            LogRecord record = converter.convert((ILoggingEvent) args[0]);
            if (record != null) {
                LogSenderHolder.getSender().send(record);
            }
        } finally {
            LogCaptureGuard.exit();
        }
    }

    @Override
    public void after(Object target, String className, String methodName, Object[] args, Object result,
            Throwable throwable) {
        // before에서 event를 수집한다. appender 실패가 원본 logging 동작에 영향을 주면 안 된다.
    }
}
