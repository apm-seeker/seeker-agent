package com.seeker.agent.core.metric;

/**
 * 메트릭 값의 의미를 명시하는 enum.
 *
 * <p>Pinpoint는 metric 이름으로 서버가 추론하는 비공식 규약이지만,
 * 본 에이전트는 wire에 명시 필드로 박아 차트/저장 측의 추론 부담을 없앤다.
 *
 * <p>차트/집계 측이 이 값을 보고:
 * <ul>
 *   <li>{@link #GAUGE}     — 현재값 그대로 표시 (heap_used, threadCount, cpu_load)</li>
 *   <li>{@link #CUMULATIVE} — 인접 두 시점 차분으로 표시 (gc_count, classes_loaded)</li>
 *   <li>{@link #DELTA}     — 에이전트가 이미 차분을 보냈으므로 그대로 표시 (transaction)</li>
 *   <li>{@link #WINDOW}    — 한 cycle 동안의 윈도우값. 다음 cycle에 reset됨 (response_time)</li>
 * </ul>
 */
public enum MetricValueType {
    /** 폴링 시점의 현재값. 누적/차분 처리 없음. */
    GAUGE,

    /** 누적값. 인접 두 시점의 차분이 의미있는 값. (서버에서 차분) */
    CUMULATIVE,

    /** 에이전트에서 이미 직전 cycle 대비 차분으로 변환된 값. */
    DELTA,

    /** 한 cycle 동안의 윈도우값(예: avg/max). 다음 cycle에서 reset된다. */
    WINDOW
}
