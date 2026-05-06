package com.seeker.agent.bootstrap.plugin;

import com.seeker.agent.config.properties.ProfilerConfig;
import com.seeker.agent.instrument.InstrumentEngine;
import com.seeker.agent.plugin.http.HttpClientPlugin;
import com.seeker.agent.plugin.jdbc.JdbcPlugin;
import com.seeker.agent.plugin.service.ServicePlugin;
import com.seeker.agent.plugin.was.tomcat.TomcatPlugin;

import java.lang.instrument.Instrumentation;

/**
 * Built-in plugin pack installer for the bootstrap module.
 *
 * <p>{@code agent-instrument}는 Byte Buddy engine만 제공하고 plugin 구현체를 알지 않는다.
 * plugin 모듈들은 bootstrap이 최종 agent jar를 구성할 때 함께 묶이므로, built-in plugin
 * 목록과 profiler flag 기반 등록 정책은 bootstrap의 이 단일 지점에 둔다.
 */
public final class PluginPackInstaller {

    private PluginPackInstaller() {
    }

    public static void install(Instrumentation instrumentation, ProfilerConfig profilerConfig) {
        InstrumentEngine engine = new InstrumentEngine();

        engine.addPlugin(new TomcatPlugin());
        if (profilerConfig.isHttpEnabled()) {
            engine.addPlugin(new HttpClientPlugin());
        }
        if (profilerConfig.isJdbcEnabled()) {
            engine.addPlugin(new JdbcPlugin());
        }
        if (profilerConfig.isSpringEnabled()) {
            addServicePlugins(engine, profilerConfig.getBasePackages());
        }

        engine.install(instrumentation);
    }

    private static void addServicePlugins(InstrumentEngine engine, String basePackages) {
        if (basePackages == null || basePackages.isEmpty()) {
            return;
        }
        String[] packages = basePackages.split(",");
        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (!trimmed.isEmpty()) {
                engine.addPlugin(new ServicePlugin(trimmed));
            }
        }
    }
}
