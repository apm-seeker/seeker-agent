package com.seeker.agent.core.log;

/**
 * Logging event 수집 중 agent 내부 로그가 다시 수집되는 재귀를 막는 ThreadLocal guard.
 */
public final class LogCaptureGuard {

    private static final ThreadLocal<Boolean> CAPTURING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private LogCaptureGuard() {
    }

    /**
     * @return 현재 스레드에서 log capture에 진입할 수 있으면 true, 이미 capture 중이면 false
     */
    public static boolean enter() {
        if (Boolean.TRUE.equals(CAPTURING.get())) {
            return false;
        }
        CAPTURING.set(Boolean.TRUE);
        return true;
    }

    public static void exit() {
        CAPTURING.set(Boolean.FALSE);
    }

    public static boolean isCapturing() {
        return Boolean.TRUE.equals(CAPTURING.get());
    }
}
