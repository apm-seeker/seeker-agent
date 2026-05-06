package com.seeker.agent.core.sender;

import com.seeker.agent.core.metric.MetricSnapshot;
import java.util.List;

/**
 * 수집된 {@link MetricSnapshot}을 외부(콘솔/gRPC collector)로 내보내는 책임의 인터페이스.
 *
 * <p>구현체는 agent-sender 모듈에 둔다 — DataSender(span)와 동일 패턴.
 * <ul>
 *   <li>{@code ConsoleMetricSender} — 디버그 모드용</li>
 *   <li>{@code GrpcMetricSender}    — proto 변환 + gRPC 스트림 전송 (Phase 1.5)</li>
 * </ul>
 *
 * <p>스레드 모델: scheduler 데몬 스레드에서 단일 호출되므로 별도 동기화 불필요.
 */
public interface MetricSender {

    /**
     * batch로 묶인 스냅샷들을 송신한다.
     *
     * <p>구현 시 주의:
     * <ul>
     *   <li>네트워크/IO 실패해도 절대 throw 하지 않는다 — 스케줄러 cycle을 깨뜨리면 안 됨.</li>
     *   <li>blocking 호출은 가능하면 짧게. 길어지면 다음 cycle이 밀린다.</li>
     * </ul>
     */
    void send(List<MetricSnapshot> snapshots);
}
