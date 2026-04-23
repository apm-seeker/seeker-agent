package com.seeker.agent.core.sender;

import com.seeker.agent.core.model.AgentInfo;

/**
 * 에이전트 식별 정보를 Collector에 등록하는 인터페이스입니다.
 * agent-core 모듈은 이 인터페이스에만 의존하며, 실제 구현체는 실행 시점에 주입됩니다.
 */
public interface AgentInfoSender {

    /**
     * 에이전트 정보를 Collector에 등록합니다.
     *
     * @param info 등록할 에이전트 정보
     */
    void register(AgentInfo info);
}
