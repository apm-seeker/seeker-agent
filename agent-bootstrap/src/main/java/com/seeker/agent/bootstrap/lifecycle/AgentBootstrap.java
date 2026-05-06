package com.seeker.agent.bootstrap.lifecycle;

import com.seeker.agent.bootstrap.plugin.PluginPackInstaller;
import com.seeker.agent.config.SeekerConfig;
import com.seeker.agent.config.properties.AgentIdentityConfig;
import com.seeker.agent.core.context.TraceContextInitializer;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.model.AgentInfoInitializer;
import com.seeker.agent.metric.MetricModule;
import com.seeker.agent.sender.SenderModule;

import java.lang.instrument.Instrumentation;

/**
 * Agent bootstrap orchestration entry point.
 *
 * <p>{@code AgentMain.premain()}은 JVM entry point로만 남기고, 실제 agent 부팅 순서는
 * 이 클래스가 담당한다. 설정 로드, core holder 초기화, sender/metric/plugin 모듈 연결,
 * shutdown hook 등록을 순서대로 수행한다.
 *
 * <p>여기서 모듈 구현체의 세부 부품을 직접 생성하지 않는다. 각 모듈은
 * {@link SenderModule}, {@link MetricModule}, {@link PluginPackInstaller} 같은
 * 자기 entry point를 통해 내부 wiring을 캡슐화한다. 이 클래스는 모듈 간 연결 순서만
 * 알고 있는 composition root다.
 */
public final class AgentBootstrap {

    public AgentRuntime start(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[Seeker] Seeker Agent 가동 시작...");

        SeekerConfig config = SeekerConfig.load();
        System.out.println("[Seeker] 로드된 설정: " + config);
        logDebugModeIfEnabled(config);

        AgentInfo agentInfo = initializeAgentInfo(config.identity());
        TraceContextInitializer.initialize();

        SenderModule senderModule = SenderModule.create(config.collector(), config.profiler(), agentInfo);
        PluginPackInstaller.install(instrumentation, config.profiler());
        MetricModule metricModule = MetricModule.startIfEnabled(
                config.profiler(),
                senderModule::metricSender,
                agentInfo);

        AgentRuntime runtime = new AgentRuntime(senderModule, metricModule);
        runtime.registerShutdownHook();

        System.out.println("[Seeker] Seeker Agent 설치 완료.");
        return runtime;
    }

    private void logDebugModeIfEnabled(SeekerConfig config) {
        if (config.profiler().isDebugEnabled()) {
            System.out.println("[Seeker] DEBUG MODE ENABLED - collector 통신 차단, 수집 데이터를 콘솔로 출력");
        }
    }

    private AgentInfo initializeAgentInfo(AgentIdentityConfig identity) {
        return AgentInfoInitializer.initialize(
                identity.getAgentId(),
                identity.getAgentName(),
                identity.getAgentType(),
                identity.getAgentGroup(),
                System.currentTimeMillis());
    }
}
