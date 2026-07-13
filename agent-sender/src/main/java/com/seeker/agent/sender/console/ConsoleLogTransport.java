package com.seeker.agent.sender.console;

import com.seeker.agent.core.log.LogRecord;

import com.seeker.agent.sender.log.LogTransport;
import java.util.List;

/**
 * Debug mode에서 LogRecord를 console로 출력하는 transport.
 */
public class ConsoleLogTransport implements LogTransport {

    @Override
    public void send(List<LogRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (LogRecord record : records) {
            System.out.println("[Seeker][Log] " + record);
        }
    }

    @Override
    public void close() {
        // no resources
    }
}
