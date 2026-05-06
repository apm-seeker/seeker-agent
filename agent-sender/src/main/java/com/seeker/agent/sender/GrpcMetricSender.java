package com.seeker.agent.sender;

import com.seeker.agent.core.metric.MetricSnapshot;
import com.seeker.agent.core.sender.MetricSender;
import com.seeker.collector.global.grpc.CollectResponse;
import com.seeker.collector.global.grpc.CollectorServiceGrpc;
import com.seeker.collector.global.grpc.DataMessage;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.Closeable;
import java.util.List;

/**
 * gRPC 스트림으로 {@link MetricSnapshot} batch를 collector에 전송하는 {@link MetricSender} 구현.
 *
 * <p>채널은 외부 주입(트레이스 송신과 공유)이며, <strong>자기만의 별도 stream</strong>을 연다.
 * → 채널 자원은 1회만 맺지만, trace 폭주가 metric stream을 직접 막지 않음 (gRPC 멀티플렉싱).
 *
 * <p>{@code GrpcSpanTransport}와 동일한 stream 라이프사이클 패턴:
 * <ul>
 *   <li>lazy {@link StreamObserver} 생성</li>
 *   <li>에러 시 {@code resetStream()}으로 다음 호출 때 재연결</li>
 *   <li>예외 절대 throw 안 함 — 스케줄러 cycle 보호</li>
 * </ul>
 *
 * <p>채널 라이프사이클은 호출자(AgentMain) 책임. 본 클래스의 {@link #close()}는 자기 stream만 정리.
 */
public class GrpcMetricSender implements MetricSender, Closeable {

    private final GrpcMetricConverter converter;
    private final CollectorServiceGrpc.CollectorServiceStub stub;

    /**
     * lazy 생성되는 stream. 에러 시 null로 reset되고 다음 send 호출 때 재생성된다.
     * volatile은 send와 shutdown hook 간 가시성 보장.
     */
    private volatile StreamObserver<DataMessage> requestObserver;

    /**
     * @param channelHolder 외부에서 생성/소유하는 gRPC 채널의 holder. trace 송신과 공유 가능.
     */
    public GrpcMetricSender(GrpcChannelHolder channelHolder) {
        this.converter = new GrpcMetricConverter();
        // channel() 호출은 같은 패키지(com.seeker.agent.sender)에서만 가능 — io.grpc 캡슐화 유지.
        this.stub = CollectorServiceGrpc.newStub(channelHolder.channel());
        System.out.println("[Seeker] GrpcMetricSender 초기화 완료 (channel: " + channelHolder.authority() + ")");
    }

    @Override
    public void send(List<MetricSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return;
        try {
            ensureStream();
            // batch 안의 모든 snapshot을 같은 stream에 연속 push.
            // gRPC bidirectional stream은 onNext 호출 순서를 보장한다.
            for (MetricSnapshot s : snapshots) {
                requestObserver.onNext(converter.toDataMessage(s));
            }
        } catch (Throwable t) {
            // 절대 throw 안 함 — 스케줄러 cycle을 깨면 안 됨.
            System.err.println("[Seeker] gRPC metric 전송 에러: " + t.getMessage());
            resetStream();
        }
    }

    /**
     * stream이 없거나 reset된 상태면 새로 연다.
     * synchronized: 일반 호출은 단일 데몬 스레드지만, shutdown hook이 close()를 동시에 호출할 수 있음.
     */
    private synchronized void ensureStream() {
        if (requestObserver == null) {
            requestObserver = stub.collect(new StreamObserver<CollectResponse>() {
                @Override
                public void onNext(CollectResponse value) {
                    // collector 응답 무시 (성공 카운트는 디버그용 의미만)
                }

                @Override
                public void onError(Throwable t) {
                    Status status = Status.fromThrowable(t);
                    System.err.println("[Seeker] gRPC metric 스트림 에러 - Code: " + status.getCode()
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
