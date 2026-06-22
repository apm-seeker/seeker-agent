package com.seeker.agent.core.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSeverityTest {

    @Test
    @DisplayName("일반 로그 레벨을 severity number로 매핑한다")
    void fromText() {
        assertEquals(LogSeverity.TRACE, LogSeverity.fromText("trace"));
        assertEquals(LogSeverity.DEBUG, LogSeverity.fromText("DEBUG"));
        assertEquals(LogSeverity.INFO, LogSeverity.fromText("info"));
        assertEquals(LogSeverity.WARN, LogSeverity.fromText("WARN"));
        assertEquals(LogSeverity.ERROR, LogSeverity.fromText("ERROR"));
        assertEquals(LogSeverity.FATAL, LogSeverity.fromText("fatal"));
    }

    @Test
    @DisplayName("JUL 레벨 별칭을 매핑한다")
    void aliases() {
        assertEquals(LogSeverity.WARN, LogSeverity.fromText("WARNING"));
        assertEquals(LogSeverity.ERROR, LogSeverity.fromText("SEVERE"));
    }

    @Test
    @DisplayName("알 수 없는 레벨은 UNSPECIFIED로 처리한다")
    void unknownLevel() {
        assertEquals(LogSeverity.UNSPECIFIED, LogSeverity.fromText(null));
        assertEquals(LogSeverity.UNSPECIFIED, LogSeverity.fromText(""));
        assertEquals(LogSeverity.UNSPECIFIED, LogSeverity.fromText("NOTICE"));
    }
}
