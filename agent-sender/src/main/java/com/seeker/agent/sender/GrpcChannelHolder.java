package com.seeker.agent.sender;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * trace/metric 송신기들이 공유하는 gRPC {@link ManagedChannel}을 감싸는 wrapper.
 *
 * <p>이 클래스의 존재 이유는 {@code io.grpc} 의존성을 sender 모듈 내부로 격리시키는 것.
 * agent-bootstrap 등 외부 모듈은 {@link ManagedChannel}을 직접 보지 않고 본 holder만 다룬다.
 *
 * <p>채널 접근 메서드 {@link #channel()}은 <strong>package-private</strong>으로 선언되어
 * 같은 패키지({@code com.seeker.agent.sender})의 sender 구현체만 호출할 수 있다.
 * 외부 모듈에서 {@code holder.channel()}을 호출하려 하면 컴파일 에러.
 *
 * <p>라이프사이클:
 * <ul>
 *   <li>생성자에서 채널 즉시 build</li>
 *   <li>{@link #close()}에서 graceful shutdown(2초) 후 강제 종료</li>
 * </ul>
 */
public class GrpcChannelHolder implements Closeable {

    private static final long SHUTDOWN_AWAIT_SECONDS = 2;

    private final ManagedChannel channel;

    public GrpcChannelHolder(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    /**
     * 같은 패키지의 sender만 접근 가능한 채널 핸들.
     * <strong>외부 모듈에서 호출 불가 (package-private).</strong>
     */
    ManagedChannel channel() {
        return channel;
    }

    /**
     * 진행 중 RPC가 정리될 시간을 짧게 준 뒤 강제 종료.
     * shutdown hook 등에서 1회만 호출하면 된다.
     */
    @Override
    public void close() {
        try {
            channel.shutdown();
            if (!channel.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        } catch (Throwable t) {
            // close 경로에서 throw 금지
            System.err.println("[Seeker] grpc channel close 에러: " + t.getMessage());
        }
    }

    /** 디버깅용 — 채널 인증 권한 문자열 (host:port). */
    public String authority() {
        return channel.authority();
    }
}
