package com.seeker.agent.core.metric;

import java.util.List;

/**
 * 단일 메트릭 종류(JVM GC, JVM Memory, System CPU 등)를 수집하는 책임의 인터페이스.
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>구현체는 부팅 시 1회만 MXBean을 lookup 하고 인스턴스를 캐싱해야 한다 (매 cycle 새로 lookup 금지).</li>
 *   <li>{@link #isAvailable()}로 graceful degradation을 인터페이스에 강제. JDK/벤더 메서드가 없으면 false 반환.</li>
 *   <li>{@link #collect(long)}은 절대 throw 하지 않는 게 이상적 — 호출 측({@link MetricRegistry})에서도 잡지만,
 *       구현체 자체에서 NoSuchMethodError 등을 잡아 빈 리스트 반환하는 패턴이 권장.</li>
 * </ul>
 */
public interface MetricCollector {

    /**
     * 컬렉터의 식별 이름. 로그 출력 시 어느 collector가 실패했는지 추적용.
     * 예: "jvm.gc", "jvm.memory", "system.cpu"
     */
    String name();

    /**
     * 이 컬렉터가 현재 환경에서 동작 가능한지 여부.
     *
     * <p>예: {@code com.sun.management.OperatingSystemMXBean}이 제공되지 않는 JDK에서는 false.
     * 부팅 시 {@link MetricRegistry}가 false인 컬렉터를 등록 거부할 수 있다.
     *
     * <p>이 시그니처를 인터페이스에 박은 이유: Pinpoint도 vendor 분기 시 graceful degradation을 채택했지만
     * (try-catch로 UNSUPPORTED 교체) 패턴화는 안 했다. 우리는 인터페이스에 명시해 모든 컬렉터가 강제 준수하게 함.
     */
    boolean isAvailable();

    /**
     * 한 시점의 메트릭들을 수집해 반환한다.
     *
     * @param now 수집 시각 (Epoch ms). 이 cycle의 모든 metric이 같은 timestamp를 갖도록 호출자가 전달.
     * @return 수집된 메트릭 목록. 비어있을 수 있음. {@code null} 반환 금지.
     */
    List<Metric> collect(long now);
}
