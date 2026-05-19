package com.seeker.scenario.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 운영 환경처럼 보이게 하는 자동 fault 주입.
 * X-Scenario 헤더 같은 힌트 없이 작은 확률 (0.5~2%) 로 발생.
 */
@Component
public class Chaos {

    /** prob 확률로 minMs~maxMs 만큼 잠. */
    public boolean maybeSlow(double prob, long minMs, long maxMs) {
        if (ThreadLocalRandom.current().nextDouble() >= prob) return false;
        long ms = ThreadLocalRandom.current().nextLong(minMs, maxMs + 1);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    /** prob 확률로 NPE 던짐 — {@code label} 위치 식별용. */
    public void maybeNpe(double prob, String label) {
        if (ThreadLocalRandom.current().nextDouble() >= prob) return;
        String x = null;
        //noinspection ConstantConditions,DataFlowIssue
        x.length(); // 의도된 NullPointerException
    }

    /** prob 확률로 임의 RuntimeException 던짐. */
    public void maybeFail(double prob, String message) {
        if (ThreadLocalRandom.current().nextDouble() >= prob) return;
        throw new RuntimeException(message);
    }

    /** prob 확률로 hangMs 만큼 (호출자 timeout 유발) 잠. */
    public void maybeTimeout(double prob, long hangMs) {
        if (ThreadLocalRandom.current().nextDouble() >= prob) return;
        try {
            Thread.sleep(hangMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
