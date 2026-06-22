package com.seeker.agent.sender.log;

import com.seeker.agent.core.log.LogRecord;
import com.seeker.agent.core.sender.LogSender;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LogRecord를 bounded queue에 적재하고 별도 worker thread에서 batch로 flush하는 sender.
 *
 * <p>logging framework의 appender path에서 network I/O가 일어나지 않도록 분리한다.
 */
public class AsyncLogDispatcher implements LogSender, Closeable {

    private static final int DEFAULT_QUEUE_CAPACITY = 8192;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 1000;

    private final MpscBlockingConsumerArrayQueue<LogRecord> queue;
    private final LogTransport transport;
    private final int batchSize;
    private final long flushIntervalMs;
    private final Thread workerThread;
    private final AtomicLong droppedCount = new AtomicLong();

    private volatile boolean running = true;

    public AsyncLogDispatcher(LogTransport transport) {
        this(transport, DEFAULT_QUEUE_CAPACITY, DEFAULT_BATCH_SIZE, DEFAULT_FLUSH_INTERVAL_MS);
    }

    public AsyncLogDispatcher(LogTransport transport, int queueCapacity, int batchSize, long flushIntervalMs) {
        this.transport = transport;
        this.queue = new MpscBlockingConsumerArrayQueue<>(Math.max(1, queueCapacity));
        this.batchSize = Math.max(1, batchSize);
        this.flushIntervalMs = Math.max(100, flushIntervalMs);
        this.workerThread = new Thread(this::run, "Seeker-LogSender-Worker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    @Override
    public void send(LogRecord record) {
        if (record == null) {
            return;
        }
        if (!queue.offer(record)) {
            droppedCount.incrementAndGet();
        }
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }

    private void run() {
        List<LogRecord> batch = new ArrayList<>(batchSize);
        while (running) {
            try {
                LogRecord first = queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    queue.drain(batch::add, batchSize - batch.size());
                }
                if (!batch.isEmpty()) {
                    flush(batch);
                    batch = new ArrayList<>(batchSize);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                System.err.println("[Seeker] log dispatch failed: " + t.getMessage());
            }
        }
        drainAndFlush(batch);
    }

    private void drainAndFlush(List<LogRecord> batch) {
        LogRecord record;
        while ((record = queue.poll()) != null) {
            batch.add(record);
            if (batch.size() >= batchSize) {
                flush(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            flush(batch);
        }
    }

    private void flush(List<LogRecord> batch) {
        try {
            transport.send(batch);
        } catch (Throwable t) {
            System.err.println("[Seeker] log transport send failed: " + t.getMessage());
        }
    }

    @Override
    public void close() {
        running = false;
        workerThread.interrupt();
        try {
            workerThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            transport.close();
        } catch (Exception e) {
            System.err.println("[Seeker] log transport close failed: " + e.getMessage());
        }
    }
}
