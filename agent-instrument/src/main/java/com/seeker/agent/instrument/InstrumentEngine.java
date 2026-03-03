package com.seeker.agent.instrument;

import com.seeker.agent.instrument.matcher.StandardMatchers;
import com.seeker.agent.instrument.plugin.Plugin;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeker 에이전트 Instrumentation을 위한 클래스
 */
public class InstrumentEngine {

    private final List<Plugin> plugins = new ArrayList<>();

    /**
     * 플러그인을 엔진에 추가합니다.
     * 
     * @param plugin 추가할 플러그인 인스턴스
     */
    public void addPlugin(Plugin plugin) {
        plugins.add(plugin);
    }

    /**
     * Byte Buddy를 사용하여 JVM에 Instrumentation 설치
     * 
     * @param instrumentation JVM에서 제공하는 Instrumentation 인스턴스
     */
    public void install(Instrumentation instrumentation) {
        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .ignore(StandardMatchers.ignoreClasses()); // 공통 제외 규칙 적용

        for (Plugin plugin : plugins) {
            agentBuilder = plugin.transform(agentBuilder);
        }

        agentBuilder.installOn(instrumentation);
    }
}
