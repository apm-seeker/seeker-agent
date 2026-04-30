package com.seeker.agent.core.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 모든 {@link MetricCollector}를 모아 한 번에 수집하는 composite.
 *
 * <p>핵심 책임:
 * <ul>
 *   <li>등록 시 {@link MetricCollector#isAvailable()} 검사로 dead collector 걸러냄.</li>
 *   <li>수집 시 per-collector try-catch로 한 컬렉터 실패가 다른 컬렉터를 막지 않게 격리.</li>
 * </ul>
 *
 * <p>Pinpoint의 Composite + per-collector 격리 패턴과 동일.
 *
 * <p>스레드 세이프: {@link CopyOnWriteArrayList}로 등록과 순회를 안전하게 분리. 등록은 부팅 시만 일어나므로
 * 복사 비용은 무시 가능, 순회(매 cycle)는 락 없이 빠르다.
 */
public class MetricRegistry {

    private final List<MetricCollector> collectors = new CopyOnWriteArrayList<>();

    /**
     * 컬렉터를 등록한다. {@link MetricCollector#isAvailable()}이 false면 무시하고 로그만 남긴다.
     */
    public void register(MetricCollector collector) {
        if (collector == null) return;
        if (collector.isAvailable()) {
            collectors.add(collector);
            System.out.println("[Seeker] metric collector registered: " + collector.name());
        } else {
            System.out.println("[Seeker] metric collector unavailable, skipped: " + collector.name());
        }
    }

    /**
     * 등록된 모든 컬렉터에서 메트릭을 수집한다.
     * 한 컬렉터가 throw 해도 다른 컬렉터의 결과는 보존된다.
     */
    public List<Metric> collectAll(long now) {
        List<Metric> all = new ArrayList<>();
        for (MetricCollector c : collectors) {
            try {
                List<Metric> metrics = c.collect(now);
                if (metrics != null && !metrics.isEmpty()) {
                    all.addAll(metrics);
                }
            } catch (Throwable t) {
                // 한 컬렉터 실패가 다른 컬렉터를 막지 않게 격리.
                // 모니터링 실패가 비즈니스 앱에 영향을 주는 일은 절대 없어야 한다.
                System.err.println("[Seeker] metric collector failed: " + c.name() + " - " + t);
            }
        }
        return all;
    }

    /** 등록된 컬렉터 수. 디버깅/로그용. */
    public int size() {
        return collectors.size();
    }
}
