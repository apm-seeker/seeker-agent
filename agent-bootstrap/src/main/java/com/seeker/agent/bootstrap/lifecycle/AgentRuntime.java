package com.seeker.agent.bootstrap.lifecycle;

import com.seeker.agent.metric.MetricModule;
import com.seeker.agent.sender.SenderModule;

/**
 * Started agent runtime resources and shutdown lifecycle owner.
 *
 * <p>Agent가 시작된 뒤 종료해야 하는 module runtime들을 보관하고, JVM shutdown hook에서
 * 닫을 순서를 한 곳에 모은다. {@code AgentBootstrap}은 runtime을 생성해 hook만 등록하고,
 * 실제 종료 순서는 이 클래스가 소유한다.
 *
 * <p>종료 순서는 metric scheduler 중지 후 sender 리소스 종료다. metric sender는
 * {@link SenderModule}이 소유하므로 sender close 경로에서 함께 정리된다.
 */
public final class AgentRuntime implements AutoCloseable {

    private final SenderModule senderModule;
    private final MetricModule metricModule;
    private boolean closed;

    AgentRuntime(SenderModule senderModule, MetricModule metricModule) {
        this.senderModule = senderModule;
        this.metricModule = metricModule;
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "seeker-agent-shutdown"));
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (metricModule != null) {
            metricModule.close();
        }
        senderModule.close();
    }
}
