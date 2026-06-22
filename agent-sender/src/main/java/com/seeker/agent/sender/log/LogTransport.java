package com.seeker.agent.sender.log;

import com.seeker.agent.core.log.LogRecord;

import java.io.Closeable;
import java.util.List;

/**
 * LogRecord batch를 실제 sink(console, gRPC 등)로 전달하는 transport 경계.
 */
public interface LogTransport extends Closeable {

    void send(List<LogRecord> records);
}
