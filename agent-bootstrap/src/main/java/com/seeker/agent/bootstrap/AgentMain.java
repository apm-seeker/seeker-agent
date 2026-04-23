package com.seeker.agent.bootstrap;

import com.seeker.agent.config.AgentIdentityConfig;
import com.seeker.agent.config.CollectorConfig;
import com.seeker.agent.config.ProfilerConfig;
import com.seeker.agent.config.PropertiesLoader;
import com.seeker.agent.core.context.ThreadLocalTraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.instrument.InstrumentEngine;
import com.seeker.agent.sender.HttpAgentInfoSender;
import com.seeker.agent.plugin.http.HttpClientPlugin;
import com.seeker.agent.plugin.jdbc.JdbcPlugin;
import com.seeker.agent.plugin.service.ServicePlugin;
import com.seeker.agent.plugin.was.tomcat.TomcatPlugin;

import java.lang.instrument.Instrumentation;
import java.util.Properties;

/**
 * Java Agent의 진입점 클래스입니다.
 */
public class AgentMain {
    /**
     * JVM 시작 시 실행되는 메서드
     *
     * @param agentArgs 에이전트 인자
     * @param inst      JVM Instrumentation 인스턴스
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Seeker] Seeker Agent 가동 시작...");

        // 설정 로드
        Properties properties = PropertiesLoader.load();
        AgentIdentityConfig identityConfig = new AgentIdentityConfig(properties);
        CollectorConfig collectorConfig = new CollectorConfig(properties);
        ProfilerConfig profilerConfig = new ProfilerConfig(properties);
        System.out.println("[Seeker] 로드된 설정: " + identityConfig + ", " + collectorConfig + ", " + profilerConfig);

        // Collector에 에이전트 등록 (HTTP)
        long startTime = System.currentTimeMillis();
        AgentInfo agentInfo = new AgentInfo(
                identityConfig.getAgentId(),
                identityConfig.getAgentName(),
                identityConfig.getAgentType(),
                identityConfig.getAgentGroup(),
                startTime);
        new HttpAgentInfoSender(collectorConfig.getHost(), collectorConfig.getHttpPort()).register(agentInfo);

        // TraceContext 초기화 및 Holder에 등록
        TraceContextHolder.setTraceContext(new ThreadLocalTraceContext());

        // DataSender 초기화 및 등록
        com.seeker.agent.core.sender.DataSender sender = new com.seeker.agent.sender.AsyncGrpcDataSender(
                collectorConfig.getHost(),
                collectorConfig.getGrpcPort(),
                identityConfig.getAgentName(),
                identityConfig.getAgentId());
        com.seeker.agent.core.sender.DataSenderHolder.setSender(sender);

        InstrumentEngine engine = new InstrumentEngine();

        // 플러그인 등록
        engine.addPlugin(new TomcatPlugin());
        engine.addPlugin(new HttpClientPlugin());
        engine.addPlugin(new JdbcPlugin());

        // 범용 서비스 플러그인 등록
        String basePackages = profilerConfig.getBasePackages();
        if (basePackages != null && !basePackages.isEmpty()) {
            String[] packages = basePackages.split(",");
            for (String pkg : packages) {
                engine.addPlugin(new ServicePlugin(pkg.trim()));
            }
        }

        // instrumentation 설치
        engine.install(inst);

        System.out.println("[Seeker] Seeker Agent 설치 완료.");
    }
}
