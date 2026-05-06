package com.seeker.agent.sender;

import com.seeker.agent.config.properties.CollectorConfig;
import com.seeker.agent.config.properties.ProfilerConfig;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.sender.AgentInfoSender;
import com.seeker.agent.core.sender.DataSender;
import com.seeker.agent.core.sender.DataSenderHolder;
import com.seeker.agent.core.sender.MetricSender;
import com.seeker.agent.sender.console.ConsoleAgentInfoSender;
import com.seeker.agent.sender.console.ConsoleMetricSender;
import com.seeker.agent.sender.console.ConsoleSpanTransport;

import java.io.Closeable;

/**
 * Sender 모듈의 public wiring entry point.
 *
 * <p>이 클래스는 agent-bootstrap이 sender 내부 구현체를 직접 알지 않도록
 * gRPC channel, agent info sender, span transport, dispatcher, metric sender 조립을
 * 한 곳에 캡슐화한다. bootstrap에는 {@link SenderModule} 자체만 노출되며
 * {@link GrpcChannelHolder}, {@link GrpcSpanTransport}, {@link AsyncSpanDispatcher} 같은
 * 세부 구현은 sender 모듈 내부에 남긴다.
 *
 * <p>bootstrap에서 이 타입을 직접 참조하는 이유는 모듈 간 연결 지점이 필요하기 때문이다.
 * metric 모듈은 {@link MetricSender}를 사용하지만 생성 책임은 sender 모듈에 있다.
 * 따라서 bootstrap은 {@link #metricSender()}를 통해 metric 모듈에 넘길 sender만 얻고,
 * 생성 방식과 close 순서는 이 클래스가 소유한다.
 */
public final class SenderModule implements Closeable {

    private static final int DEFAULT_SPAN_QUEUE_CAPACITY = 1024 * 8;

    private final boolean debugEnabled;
    private final GrpcChannelHolder grpcChannelHolder;
    private final DataSender dataSender;

    private MetricSender metricSender;
    private boolean closed;

    private SenderModule(boolean debugEnabled,
                         GrpcChannelHolder grpcChannelHolder,
                         DataSender dataSender) {
        this.debugEnabled = debugEnabled;
        this.grpcChannelHolder = grpcChannelHolder;
        this.dataSender = dataSender;
    }

    public static SenderModule create(CollectorConfig collectorConfig,
                                      ProfilerConfig profilerConfig,
                                      AgentInfo agentInfo) {
        boolean debugEnabled = profilerConfig.isDebugEnabled();
        GrpcChannelHolder grpcChannelHolder = debugEnabled
                ? null
                : new GrpcChannelHolder(collectorConfig.getHost(), collectorConfig.getGrpcPort());

        AgentInfoSender agentInfoSender = debugEnabled
                ? new ConsoleAgentInfoSender()
                : new HttpAgentInfoSender(collectorConfig.getHost(), collectorConfig.getHttpPort());
        agentInfoSender.register(agentInfo);

        SpanTransport transport = debugEnabled
                ? new ConsoleSpanTransport()
                : new GrpcSpanTransport(
                        grpcChannelHolder,
                        agentInfo.getAgentName(),
                        agentInfo.getAgentId());
        DataSender dataSender = new AsyncSpanDispatcher(transport, DEFAULT_SPAN_QUEUE_CAPACITY);
        DataSenderHolder.setSender(dataSender);

        return new SenderModule(debugEnabled, grpcChannelHolder, dataSender);
    }

    /**
     * Metric 전송에 사용할 sender를 lazy 생성한다.
     *
     * <p>metric 기능이 비활성화된 경우 gRPC metric stream이나 console metric sender를
     * 만들 필요가 없으므로, {@code MetricModule}이 실제로 시작될 때만 생성한다.
     */
    public synchronized MetricSender metricSender() {
        if (metricSender == null) {
            metricSender = debugEnabled
                    ? new ConsoleMetricSender()
                    : new GrpcMetricSender(grpcChannelHolder);
        }
        return metricSender;
    }

    /**
     * sender 모듈이 소유한 전송 리소스를 닫는다.
     *
     * <p>종료 순서는 span dispatcher, metric sender, shared gRPC channel 순서다.
     * trace/metric sender는 각각 자기 stream을 닫고, 마지막에 공유 channel을 닫는다.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeIfPossible(dataSender);
        closeIfPossible(metricSender);
        closeIfPossible(grpcChannelHolder);
    }

    private void closeIfPossible(Object target) {
        if (target instanceof Closeable) {
            try {
                ((Closeable) target).close();
            } catch (Exception ignored) {
                // shutdown path must not throw
            }
        }
    }
}
