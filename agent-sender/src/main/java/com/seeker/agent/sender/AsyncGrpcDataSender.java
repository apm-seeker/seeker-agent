package com.seeker.agent.sender;

import com.seeker.agent.core.model.Span;
import com.seeker.agent.core.sender.DataSender;
import com.seeker.collector.global.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;

/**
 * gRPC를 이용해 비동기로 데이터를 전송하는 DataSender 구현체입니다.
 */
public class AsyncGrpcDataSender implements DataSender {

    private final GrpcDataMessageConverter converter;
    private final MpscBlockingConsumerArrayQueue<Span> queue;
    private final ManagedChannel channel;
    private final CollectorServiceGrpc.CollectorServiceStub stub;
    private volatile StreamObserver<DataMessage> requestObserver;
    private final Thread workerThread;
    private volatile boolean running = true;

    public AsyncGrpcDataSender(String host, int port) {
        this(host, port, "Unknown-App", "Unknown-Agent");
    }

    public AsyncGrpcDataSender(String host, int port, String applicationName, String agentId) {
        this.converter = new GrpcDataMessageConverter(applicationName, agentId);
        this.queue = new MpscBlockingConsumerArrayQueue<>(1024 * 8); // 8k capacity
        // gRPC 서버와 연동
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = CollectorServiceGrpc.newStub(channel);
        // 전송하는 Thread를 생성하고 백그라운드로 실행을 한다.
        this.workerThread = new Thread(this::run, "Seeker-DataSender-Worker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
        System.out.println("[Seeker] AsyncGrpcDataSender 초기화 완료 (Collector: " + host + ":" + port + ")");
    }

    @Override
    public void send(Span span) {
        if (!queue.offer(span)) {
            // Buffer overflow drop logic
        }
    }

    private void run() {
        while (running) {
            try {
                Span span = queue.take();

                ensureStream();
                requestObserver.onNext(converter.toDataMessage(span));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[Seeker] 데이터 전송 에러: " + e.getMessage());
                resetStream();
            }
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

    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
