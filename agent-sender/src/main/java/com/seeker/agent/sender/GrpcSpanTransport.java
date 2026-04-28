package com.seeker.agent.sender;

import com.seeker.agent.core.model.Span;
import com.seeker.collector.global.grpc.CollectResponse;
import com.seeker.collector.global.grpc.CollectorServiceGrpc;
import com.seeker.collector.global.grpc.DataMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * gRPC 스트림으로 Span을 Collector에 전송하는 SpanTransport 구현체입니다.
 * 채널/스트림 라이프사이클과 재연결 책임을 가집니다.
 */
public class GrpcSpanTransport implements SpanTransport {

    private final GrpcDataMessageConverter converter;
    private final ManagedChannel channel;
    private final CollectorServiceGrpc.CollectorServiceStub stub;
    private volatile StreamObserver<DataMessage> requestObserver;

    public GrpcSpanTransport(String host, int port, String applicationName, String agentId) {
        this.converter = new GrpcDataMessageConverter(applicationName, agentId);
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = CollectorServiceGrpc.newStub(channel);
        System.out.println("[Seeker] GrpcSpanTransport 초기화 완료 (Collector: " + host + ":" + port + ")");
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

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
