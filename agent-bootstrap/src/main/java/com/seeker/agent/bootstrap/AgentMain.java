package com.seeker.agent.bootstrap;

import com.seeker.agent.instrument.InstrumentEngine;
import com.seeker.agent.instrument.sample.TestPlugin;

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

        // 테스트용 플러그인 등록
        engine.addPlugin(new TestPlugin());

        // instrumentation 설치
        engine.install(inst);

        System.out.println("[Seeker] Seeker Agent 설치 완료.");
    }
}
