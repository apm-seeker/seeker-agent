package com.seeker.agent.bootstrap;

import com.seeker.agent.bootstrap.lifecycle.AgentBootstrap;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBootstrap().start(agentArgs, inst);
    }
}
