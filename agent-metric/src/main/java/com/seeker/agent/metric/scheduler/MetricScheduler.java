package com.seeker.agent.metric.scheduler;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricRegistry;
import com.seeker.agent.core.sender.MetricSender;
import com.seeker.agent.core.metric.MetricSnapshot;
import com.seeker.agent.core.model.AgentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 메트릭 수집 cycle을 주기적으로 실행하고 batch로 송신하는 스케줄러.
 *
 * <p>Pinpoint의 {@code DefaultAgentStatMonitor} + {@code CollectJob} 패턴 차용:
 * <ul>
 *   <li>단일 <strong>데몬 스레드</strong> (jstack 식별성을 위해 이름 박음 — {@code seeker-stat-monitor})</li>
 *   <li>인터벌은 [1s, 10s]로 clamp — 너무 짧으면 부하, 너무 길면 dashboard 무의미</li>
 *   <li><strong>wall-clock measured interval</strong>: 스케줄 인터벌 신뢰 X. GC pause로 8초 됐으면 8000을 보냄.
 *       서버의 TPS 계산 정확성에 영향.</li>
 *   <li><strong>N cycle 누적 후 batch flush</strong> (default 6) — gRPC 호출 횟수 절감</li>
 *   <li><strong>list-swap</strong>: flush 시 새 ArrayList를 할당하고 기존 리스트는 sender에 넘김 → lock-free</li>
 *   <li><strong>pre-load class trick</strong>: start() 직전 collectAll()을 미리 1~2회 호출.
 *       JDK 6 시절 classloader deadlock(#2881) 회피용 안전 보험.</li>
 *   <li><strong>per-cycle try-catch</strong>: 한 cycle 실패가 다음 cycle에 영향 없도록.</li>
 * </ul>
 */
public class MetricScheduler {

    /** 인터벌 하한. 너무 짧으면 수집/송신 비용이 비즈니스 처리 영향. */
    public static final long MIN_INTERVAL_MS = 1000;

    /** 인터벌 상한. 너무 길면 메트릭이 사실상 무의미. */
    public static final long MAX_INTERVAL_MS = 10_000;

    /** 기본 batch size — 5초 인터벌이면 30초마다 송신. */
    public static final int DEFAULT_BATCH_SIZE = 6;

    private final ScheduledExecutorService executor;
    private final MetricRegistry registry;
    private final MetricSender sender;
    private final AgentInfo agentInfo;
    private final long intervalMs;
    private final int batchSize;

    /** 직전 cycle의 timestamp. wall-clock measured interval 계산용. */
    private long prevTimestamp = -1;

    /** N cycle 누적 버퍼. flush 시 새 인스턴스로 swap. */
    private List<MetricSnapshot> buffer;

    private ScheduledFuture<?> future;

    public MetricScheduler(MetricRegistry registry, MetricSender sender, AgentInfo agentInfo,
                           long intervalMs, int batchSize) {
        this.registry = registry;
        this.sender = sender;
        this.agentInfo = agentInfo;
        // 인터벌 clamp — 외부 설정이 극단치여도 안전한 범위 강제.
        this.intervalMs = Math.max(MIN_INTERVAL_MS, Math.min(MAX_INTERVAL_MS, intervalMs));
        this.batchSize = (batchSize <= 0) ? DEFAULT_BATCH_SIZE : batchSize;
        this.buffer = new ArrayList<>(this.batchSize);

        // 데몬 스레드 + 식별 가능한 이름.
        // 데몬: 메트릭 스레드가 비-데몬이면 JVM 종료를 막을 수 있음.
        // 이름: jstack 떠서 즉시 식별 가능하게.
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "seeker-stat-monitor");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 스케줄러를 시작한다. {@link #start()}가 한 번 호출된 후 재호출은 무시.
     */
    public synchronized void start() {
        if (future != null) {
            return;  // 이미 시작됨
        }

        // pre-load class trick (Pinpoint #2881 회피용 안전 보험).
        // 스케줄러 첫 가동 시점에 일어나는 클래스 로딩이 다른 스레드와 충돌하지 않도록 사전 워밍업.
        try { registry.collectAll(System.currentTimeMillis()); } catch (Throwable ignored) {}
        try { registry.collectAll(System.currentTimeMillis()); } catch (Throwable ignored) {}

        future = executor.scheduleAtFixedRate(this::runOnce,
                intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        System.out.println("[Seeker] metric scheduler started: interval=" + intervalMs
                + "ms batch=" + batchSize + " collectors=" + registry.size());
    }

    /**
     * 한 cycle 처리. ScheduledExecutor에서 단일 스레드로만 호출되므로 락 불필요.
     */
    private void runOnce() {
        try {
            long now = System.currentTimeMillis();

            // wall-clock measured interval. 첫 호출은 0으로 둔다 (에이전트 시작 직후 spike 방지).
            long measuredInterval = (prevTimestamp < 0) ? 0 : (now - prevTimestamp);
            prevTimestamp = now;

            List<Metric> metrics = registry.collectAll(now);
            MetricSnapshot snapshot = MetricSnapshot.of(agentInfo, now, measuredInterval, metrics);
            buffer.add(snapshot);

            // batchSize에 도달하면 flush.
            if (buffer.size() >= batchSize) {
                flush();
            }
        } catch (Throwable t) {
            // 한 cycle 실패가 다음 cycle을 막지 않게. 모니터링 실패 → 비즈니스 영향은 절대 금지.
            System.err.println("[Seeker] metric cycle failed: " + t);
        }
    }

    /**
     * 버퍼를 swap하여 sender에게 넘기고 새 버퍼를 할당.
     * list-swap 패턴: producer(scheduler thread)가 새 리스트 만들고, consumer(sender)는 기존 리스트를 받음.
     * 락 없이 producer/consumer 분리.
     */
    private void flush() {
        if (buffer.isEmpty()) return;
        List<MetricSnapshot> toSend = buffer;
        buffer = new ArrayList<>(batchSize);
        try {
            sender.send(toSend);
        } catch (Throwable t) {
            // sender 자체가 throw 하지 않도록 구현하지만 안전망.
            System.err.println("[Seeker] metric send failed: " + t);
        }
    }

    /**
     * 스케줄러 정지. JVM 종료 hook이나 테스트에서 호출. 남은 버퍼는 마지막으로 flush 시도.
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        // 마지막 잔여 버퍼 flush (best-effort).
        try { flush(); } catch (Throwable ignored) {}
    }
}
