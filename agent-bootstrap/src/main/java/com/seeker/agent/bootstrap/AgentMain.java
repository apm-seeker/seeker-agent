package com.seeker.agent.bootstrap;

import com.seeker.agent.config.AgentConfig;
import com.seeker.agent.core.context.ThreadLocalTraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.instrument.InstrumentEngine;
import com.seeker.agent.plugin.http.HttpClientPlugin;
import com.seeker.agent.plugin.jdbc.JdbcPlugin;
import com.seeker.agent.plugin.service.ServicePlugin;
import com.seeker.agent.plugin.was.tomcat.TomcatPlugin;

import java.lang.instrument.Instrumentation;

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
        AgentConfig config = AgentConfig.load();
        System.out.println("[Seeker] 로드된 설정: " + config);

        // TraceContext 초기화 및 Holder에 등록
        TraceContextHolder.setTraceContext(new ThreadLocalTraceContext());

        // DataSender 초기화 및 등록
        /* 기존 하드코딩 방식 - 주석 처리
        String appName = System.getProperty("seeker.application.name", "Default-App");
        String agentId = System.getProperty("seeker.agent.id", "Default-Agent");
        com.seeker.agent.core.sender.DataSender sender = new com.seeker.agent.sender.AsyncGrpcDataSender("localhost",
                9999, appName, agentId);
        */
        com.seeker.agent.core.sender.DataSender sender = new com.seeker.agent.sender.AsyncGrpcDataSender(
                config.getCollectorHost(),
                config.getCollectorPort(),
                config.getApplicationName(),
                config.getAgentId());
        com.seeker.agent.core.sender.DataSenderHolder.setSender(sender);

        InstrumentEngine engine = new InstrumentEngine();

        // 플러그인 등록
        engine.addPlugin(new TomcatPlugin());
        engine.addPlugin(new HttpClientPlugin());
        engine.addPlugin(new JdbcPlugin());

        /* 기존 하드코딩 방식 - 주석 처리
        engine.addPlugin(new ServicePlugin("com.seeker.test"));
        */

        // 범용 서비스 플러그인 등록
        String basePackages = config.getBasePackages();
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
