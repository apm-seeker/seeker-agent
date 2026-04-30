package com.seeker.agent.sender.console;

import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.sender.AgentInfoSender;

/**
 * 에이전트 정보를 콘솔에 출력하는 AgentInfoSender 구현체입니다.
 * 디버그 모드에서 Collector HTTP 등록 호출 없이 등록 정보를 확인하는 용도로 사용됩니다.
 */
public class ConsoleAgentInfoSender implements AgentInfoSender {

    @Override
    public void register(AgentInfo info) {
        System.out.println("[Seeker-DEBUG][AGENT_INFO] " + info);
    }
}
