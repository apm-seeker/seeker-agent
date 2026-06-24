package com.seeker.agent.plugin.logback;

import com.seeker.agent.core.log.LogSeverity;
import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Logback appender path를 계측해 logging event를 Seeker LogRecord로 변환한다.
 */
public class LogbackPlugin implements Plugin {

    private final String minLevel;
    private final boolean onlyTraced;
    private final boolean mdcEnabled;
    private final String mdcKeys;
    private final int maxMessageLength;
    private final int maxStacktraceLength;

    public LogbackPlugin(String minLevel,
                         boolean onlyTraced,
                         boolean mdcEnabled,
                         String mdcKeys,
                         int maxMessageLength,
                         int maxStacktraceLength) {
        this.minLevel = minLevel;
        this.onlyTraced = onlyTraced;
        this.mdcEnabled = mdcEnabled;
        this.mdcKeys = mdcKeys;
        this.maxMessageLength = maxMessageLength;
        this.maxStacktraceLength = maxStacktraceLength;
    }

    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(named("ch.qos.logback.classic.Logger"))
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "LogbackAppenderInterceptor";
                    InterceptorRegistry.register(interceptorName, new LogbackAppenderInterceptor(
                            LogSeverity.fromText(minLevel).getSeverityNumber(),
                            onlyTraced,
                            mdcEnabled,
                            mdcKeys,
                            maxMessageLength,
                            maxStacktraceLength));

                    return new BaseTransformer(interceptorName, named("callAppenders"))
                            .transform(builder, typeDescription, classLoader, module, pd);
                });
    }
}
