package com.seeker.agent.sender;

import com.seeker.agent.core.log.LogRecord;
import com.seeker.agent.sender.log.GrpcLogMessageConverter;
import com.seeker.agent.sender.log.LogTransport;
import com.seeker.collector.global.grpc.CollectResponse;
import com.seeker.collector.global.grpc.CollectorServiceGrpc;
import com.seeker.collector.global.grpc.DataMessage;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;

/**
 * LogRecord batch를 collector gRPC stream으로 전송하는 transport.
 *
 * <p>trace/metric sender와 같은 {@link GrpcChannelHolder}를 공유하되, log 전송은
 * 별도 stream observer를 사용한다. {@link GrpcChannelHolder#channel()}이 package-private
 * 이므로 이 transport는 sender root package에 둔다.
 */
public class GrpcLogTransport implements LogTransport {

    private final GrpcLogMessageConverter converter = new GrpcLogMessageConverter();
    private final CollectorServiceGrpc.CollectorServiceStub stub;
    private volatile StreamObserver<DataMessage> requestObserver;

    public GrpcLogTransport(GrpcChannelHolder channelHolder) {
        this.stub = CollectorServiceGrpc.newStub(channelHolder.channel());
        System.out.println("[Seeker] GrpcLogTransport 초기화 완료 (channel: " + channelHolder.authority() + ")");
    }

    @Override
    public void send(List<LogRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        try {
            ensureStream();
            requestObserver.onNext(converter.toDataMessage(records));
        } catch (Throwable t) {
            System.err.println("[Seeker] gRPC log 전송 에러: " + t.getMessage());
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
                    System.err.println("[Seeker] gRPC log 스트림 에러 - Code: " + status.getCode()
                            + ", Description: " + status.getDescription());
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
        try {
            StreamObserver<DataMessage> obs = requestObserver;
            if (obs != null) {
                obs.onCompleted();
            }
        } catch (Throwable ignored) {
            // close path must not throw
        }
        resetStream();
    }
}
