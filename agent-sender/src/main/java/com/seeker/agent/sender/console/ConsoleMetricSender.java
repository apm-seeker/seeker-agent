package com.seeker.agent.sender.console;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.sender.MetricSender;
import com.seeker.agent.core.metric.MetricSnapshot;

import java.util.List;

/**
 * 디버그 모드용 {@link MetricSender} 구현체.
 * collector 통신 없이 수집된 메트릭을 콘솔에 출력한다.
 *
 * <p>{@code ConsoleSpanTransport}와 동일한 디버그 보조 도구. 학습/테스트 환경에서
 * 메트릭 파이프라인 동작 확인용.
 *
 * <p>실제 운영에서는 gRPC 기반 구현체(Phase 1.5의 {@code GrpcMetricSender})로 교체된다.
 */
public class ConsoleMetricSender implements MetricSender {

    public ConsoleMetricSender() {
        System.out.println("[Seeker] ConsoleMetricSender 초기화 (collector 통신 없음)");
    }

    @Override
    public void send(List<MetricSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return;
        for (MetricSnapshot snapshot : snapshots) {
            // batch 단위로 헤더 한 줄 + metric 목록 들여쓰기 출력. 가독성 우선.
            System.out.println("[Seeker-DEBUG][METRIC] " + snapshot);
            for (Metric m : snapshot.getMetrics()) {
                System.out.println("    " + m);
            }
        }
    }
}
