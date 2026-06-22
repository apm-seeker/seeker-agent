package com.seeker.agent.core.sender;

import com.seeker.agent.core.log.LogRecord;

/**
 * 수집된 로그 이벤트를 sender 모듈로 전달하는 core 경계 인터페이스.
 */
public interface LogSender {

    void send(LogRecord record);
}
