package com.seeker.agent.core.sender;

import com.seeker.agent.core.model.Span;

/**
 * 데이터를 수집 서버로 전동하는 인터페이스입니다.
 * agent-core 모듈은 이 인터페이스에만 의존하며, 실제 구현체는 실행 시점에 주입됩니다.
 */
public interface DataSender {
    /**
     * 수집된 Span 데이터를 전송합니다.
     * 
     * @param span 전송할 Span 객체
     */
    void send(Span span);
}
