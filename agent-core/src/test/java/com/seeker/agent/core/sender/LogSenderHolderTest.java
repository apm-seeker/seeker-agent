package com.seeker.agent.core.sender;

import com.seeker.agent.core.log.LogRecord;
import com.seeker.agent.core.log.LogSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSenderHolderTest {

    @Test
    @DisplayName("LogSenderHolder에 sender를 주입하고 조회한다")
    void setAndGetSender() {
        AtomicReference<LogRecord> captured = new AtomicReference<>();
        LogSender sender = captured::set;

        LogSenderHolder.setSender(sender);

        LogRecord record = LogRecord.builder()
                .severity(LogSeverity.INFO)
                .body("hello")
                .build();

        LogSenderHolder.getSender().send(record);

        assertEquals(record, captured.get());
    }
}
