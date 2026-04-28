package com.seeker.agent.sender;

import com.seeker.agent.core.model.Span;

import java.io.Closeable;

/**
 * Span을 외부 수집 시스템으로 보내는 전송 계층.
 * 큐잉/스레딩과 독립적이며, 단일 스레드(AsyncSpanDispatcher 워커)에서 호출되는 것을 가정합니다.
 */
public interface SpanTransport extends Closeable {

    void send(Span span);

    @Override
    void close();
}
