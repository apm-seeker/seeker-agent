package com.seeker.agent.metric.collector;

import com.seeker.agent.core.metric.Metric;
import com.seeker.agent.core.metric.MetricCollector;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM/시스템 CPU 사용률 + 시스템 부하를 수집하는 컬렉터.
 *
 * <p>측정 항목 (모두 GAUGE):
 * <ul>
 *   <li>{@code system.cpu.jvm_load}    — JVM 프로세스 CPU 사용률 (0.0~1.0)</li>
 *   <li>{@code system.cpu.system_load} — 시스템 전체 CPU 사용률 (0.0~1.0)</li>
 *   <li>{@code system.cpu.load_average} — system load average / CPU 코어 수 (Unix만)</li>
 * </ul>
 *
 * <p>{@code com.sun.management.OperatingSystemMXBean}이 제공하는 메서드를 reflection으로 호출 — Oracle/OpenJDK
 * 외 일부 JDK(IBM J9 등)에서 메서드가 없을 수 있어 graceful degradation을 위해 직접 cast 대신 reflection 사용.
 * Pinpoint의 vendor 분기 패턴과 동일 의도.
 *
 * <p>한 번이라도 메서드 lookup 실패하면 해당 metric만 빠지고 나머지는 정상 수집된다.
 */
public class SystemCpuCollector implements MetricCollector {

    private static final String METRIC_NAME = "system.cpu";

    private final OperatingSystemMXBean osBean;
    private final Method getProcessCpuLoad;   // com.sun.management.OperatingSystemMXBean#getProcessCpuLoad
    private final Method getSystemCpuLoad;    // com.sun.management.OperatingSystemMXBean#getCpuLoad (JDK 14+)
                                              // 또는 #getSystemCpuLoad (이전)
    private final int availableProcessors;

    public SystemCpuCollector() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.availableProcessors = (osBean != null) ? osBean.getAvailableProcessors() : 1;
        this.getProcessCpuLoad = lookup(osBean, "getProcessCpuLoad");
        // JDK 14+에서는 getCpuLoad, 이전에는 getSystemCpuLoad. 둘 다 시도.
        Method m = lookup(osBean, "getCpuLoad");
        if (m == null) m = lookup(osBean, "getSystemCpuLoad");
        this.getSystemCpuLoad = m;
    }

    /**
     * 메서드를 reflection으로 안전하게 lookup. 없으면 null 반환 (graceful degradation).
     */
    private static Method lookup(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * lookup된 메서드를 호출해 double 결과를 안전하게 반환.
     * 호출 실패 시 -1 반환 (수집 불가 표시).
     */
    private double invokeDouble(Method m) {
        if (m == null) return -1;
        try {
            Object result = m.invoke(osBean);
            if (result instanceof Double) {
                double v = (Double) result;
                // 메서드 명세상 -1 또는 NaN이 나올 수 있음 — 그대로 -1로 통일.
                if (Double.isNaN(v) || v < 0) return -1;
                return v;
            }
            return -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    public String name() {
        return METRIC_NAME;
    }

    @Override
    public boolean isAvailable() {
        // 두 메서드 중 하나라도 있으면 의미있는 값 수집 가능.
        return osBean != null && (getProcessCpuLoad != null || getSystemCpuLoad != null);
    }

    @Override
    public List<Metric> collect(long now) {
        List<Metric> out = new ArrayList<>(3);

        double jvmLoad = invokeDouble(getProcessCpuLoad);
        if (jvmLoad >= 0) {
            out.add(Metric.gauge(METRIC_NAME, "jvm_load", jvmLoad, now));
        }

        double systemLoad = invokeDouble(getSystemCpuLoad);
        if (systemLoad >= 0) {
            out.add(Metric.gauge(METRIC_NAME, "system_load", systemLoad, now));
        }

        // load average — Unix에서만 의미있음 (Windows는 -1 반환)
        double loadAvg = osBean.getSystemLoadAverage();
        if (loadAvg >= 0 && availableProcessors > 0) {
            // 코어 수로 나눠 1.0 = 100% 부하 의미로 정규화. 차트 친화적.
            out.add(Metric.gauge(METRIC_NAME, "load_average", loadAvg / availableProcessors, now));
        }

        return out;
    }
}
