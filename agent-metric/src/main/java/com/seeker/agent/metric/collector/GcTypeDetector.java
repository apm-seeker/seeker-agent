package com.seeker.agent.metric.collector;

import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

/**
 * 실행 중인 JVM의 GC 알고리즘을 자동으로 감지해 분류 라벨을 반환하는 유틸리티.
 *
 * <p>{@link GarbageCollectorMXBean#getName()}이 반환하는 이름을 매핑한다.
 * Pinpoint의 {@code GarbageCollectorMetricProvider}와 동일 패턴 — 매칭 실패 시 {@code UNKNOWN} fallback.
 *
 * <p>사용처: GC count/time 메트릭의 tags에 GC 타입을 함께 실어 보내, 차트에서 "G1_OLD vs G1_YOUNG" 같은 분리 가능.
 */
public final class GcTypeDetector {

    /** GC 알고리즘 분류. tags 값으로 사용된다. */
    public enum GcType {
        SERIAL, PARALLEL, CMS, G1, ZGC, SHENANDOAH, UNKNOWN;

        /** 표기 라벨(소문자 hyphen). 차트에서 그대로 사용 가능. */
        public String label() { return name().toLowerCase().replace('_', '-'); }
    }

    /**
     * GarbageCollectorMXBean의 {@code getName()} 결과를 GcType으로 분류.
     *
     * <p>대표 매핑:
     * <ul>
     *   <li>"Copy", "MarkSweepCompact" → SERIAL</li>
     *   <li>"PS Scavenge", "PS MarkSweep" → PARALLEL</li>
     *   <li>"ParNew", "ConcurrentMarkSweep" → CMS</li>
     *   <li>"G1 Young Generation", "G1 Old Generation" → G1</li>
     *   <li>"ZGC Minor Pauses", "ZGC Major Pauses", "ZGC" → ZGC</li>
     *   <li>"Shenandoah Pauses", "Shenandoah Cycles" → SHENANDOAH</li>
     * </ul>
     */
    public static GcType classify(String gcName) {
        if (gcName == null) return GcType.UNKNOWN;
        String n = gcName.toLowerCase();
        if (n.contains("g1")) return GcType.G1;
        if (n.contains("zgc") || n.equals("zgc")) return GcType.ZGC;
        if (n.contains("shenandoah")) return GcType.SHENANDOAH;
        if (n.contains("ps ")) return GcType.PARALLEL;
        if (n.contains("parnew") || n.contains("concurrentmarksweep")) return GcType.CMS;
        if (n.contains("copy") || n.contains("marksweepcompact")) return GcType.SERIAL;
        return GcType.UNKNOWN;
    }

    /**
     * 등록된 모든 GC bean에서 가장 대표적인 GC 알고리즘을 추정한다.
     * 부팅 시 1회 호출 후 캐싱하는 용도. (Pinpoint:46 패턴)
     */
    public static GcType detectPrimary(List<GarbageCollectorMXBean> beans) {
        if (beans == null || beans.isEmpty()) return GcType.UNKNOWN;
        for (GarbageCollectorMXBean bean : beans) {
            GcType type = classify(bean.getName());
            if (type != GcType.UNKNOWN) return type;
        }
        return GcType.UNKNOWN;
    }

    private GcTypeDetector() {}
}
