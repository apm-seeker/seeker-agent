package com.seeker.agent.bootstrap;

import com.seeker.agent.instrument.InstrumentEngine;
import com.seeker.agent.plugin.http.HttpClientPlugin;
import com.seeker.agent.plugin.jdbc.JdbcPlugin;
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

        InstrumentEngine engine = new InstrumentEngine();

        // TODO 기존의 하드코딩 방식의 Plugins 주입 방식을 config 방식으로 수정
        // 플러그인 등록
        engine.addPlugin(new TomcatPlugin());
        engine.addPlugin(new HttpClientPlugin());
        engine.addPlugin(new JdbcPlugin());

        // instrumentation 설치
        engine.install(inst);

        System.out.println("[Seeker] Seeker Agent 설치 완료.");
    }
}
