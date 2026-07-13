package com.seeker.agent.core.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogCaptureGuardTest {

    @AfterEach
    void tearDown() {
        LogCaptureGuard.exit();
    }

    @Test
    @DisplayName("같은 스레드에서 재진입을 막고 exit 후 다시 진입 가능하다")
    void enterAndExit() {
        assertFalse(LogCaptureGuard.isCapturing());

        assertTrue(LogCaptureGuard.enter());
        assertTrue(LogCaptureGuard.isCapturing());
        assertFalse(LogCaptureGuard.enter());

        LogCaptureGuard.exit();

        assertFalse(LogCaptureGuard.isCapturing());
        assertTrue(LogCaptureGuard.enter());
    }
}
