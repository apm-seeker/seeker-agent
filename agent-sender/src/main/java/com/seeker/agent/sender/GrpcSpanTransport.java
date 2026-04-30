package com.seeker.agent.sender;

import com.seeker.agent.core.model.Span;
import com.seeker.collector.global.grpc.CollectResponse;
import com.seeker.collector.global.grpc.CollectorServiceGrpc;
import com.seeker.collector.global.grpc.DataMessage;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * gRPC 스트림으로 Span을 Collector에 전송하는 SpanTransport 구현체입니다.
 *
 * <p>채널은 외부에서 주입받아 다른 sender({@code GrpcMetricSender} 등)와 공유한다.
 * 본 클래스는 자기 stream의 라이프사이클만 책임지며, channel 생성/종료는 호출자(AgentMain) 책임.
 */
public class GrpcSpanTransport implements SpanTransport {

    private final GrpcDataMessageConverter converter;
    private final ManagedChannel channel;
    private final CollectorServiceGrpc.CollectorServiceStub stub;
    private volatile StreamObserver<DataMessage> requestObserver;

    /**
     * @param channel 외부에서 생성/소유하는 gRPC 채널. 본 클래스는 닫지 않는다.
     */
    public GrpcSpanTransport(ManagedChannel channel, String applicationName, String agentId) {
        this.channel = channel;
        this.converter = new GrpcDataMessageConverter(applicationName, agentId);
        this.stub = CollectorServiceGrpc.newStub(channel);
        System.out.println("[Seeker] GrpcSpanTransport 초기화 완료 (channel authority: " + channel.authority() + ")");
    }

    @Override
    public void send(Span span) {
        try {
            ensureStream();
            requestObserver.onNext(converter.toDataMessage(span));
        } catch (Exception e) {
            System.err.println("[Seeker] 데이터 전송 에러: " + e.getMessage());
            resetStream();
        }
    }

    private synchronized void ensureStream() {
        if (requestObserver == null) {
            requestObserver = stub.collect(new StreamObserver<CollectResponse>() {
                @Override
                public void onNext(CollectResponse value) {
                }

                @Override
                public void onError(Throwable t) {
                    Status status = Status.fromThrowable(t);
                    System.err.println("[Seeker] gRPC 스트림 에러 - Code: " + status.getCode()
                            + ", Description: " + status.getDescription()
                            + ", Cause: " + (status.getCause() != null ? status.getCause().getMessage() : "none"));
                    t.printStackTrace(System.err);
                    resetStream();
                }

                @Override
                public void onCompleted() {
                    resetStream();
                }
            });
        }
    }

    private void resetStream() {
        requestObserver = null;
    }

    /**
     * 자기 stream만 정리한다. 채널은 외부 소유이므로 닫지 않음.
     */
    @Override
    public void close() {
        try {
            StreamObserver<DataMessage> obs = requestObserver;
            if (obs != null) {
                obs.onCompleted();
            }
        } catch (Throwable ignored) {
            // close 경로에서는 throw 금지
        }
        resetStream();
    }
}
