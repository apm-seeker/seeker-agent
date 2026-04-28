package com.seeker.agent.sender;

import com.seeker.agent.core.model.Span;
import com.seeker.agent.core.sender.DataSender;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;

import java.io.Closeable;

/**
 * Span을 비동기 큐에 적재하고, 단일 워커 스레드에서 SpanTransport로 전달하는 DataSender 구현체입니다.
 * 큐잉/백프레셔 책임만 가지며 실제 전송은 주입받은 SpanTransport에 위임합니다.
 */
public class AsyncSpanDispatcher implements DataSender, Closeable {

    private final MpscBlockingConsumerArrayQueue<Span> queue;
    private final SpanTransport transport;
    private final Thread workerThread;
    private volatile boolean running = true;

    public AsyncSpanDispatcher(SpanTransport transport, int capacity) {
        this.transport = transport;
        this.queue = new MpscBlockingConsumerArrayQueue<>(capacity);
        this.workerThread = new Thread(this::run, "Seeker-DataSender-Worker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    @Override
    public void send(Span span) {
        if (!queue.offer(span)) {
            // Buffer overflow drop logic
        }
    }

    private void run() {
        while (running) {
            try {
                Span span = queue.take();
                transport.send(span);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // SpanTransport는 예외를 throw하지 않는 것이 계약 — 여기로 들어왔다면 transport 구현 버그
                System.err.println("[Seeker] BUG: SpanTransport가 예외를 throw함 - " + e);
                e.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        try {
            transport.close();
        } catch (Exception e) {
            System.err.println("[Seeker] transport close 에러: " + e.getMessage());
        }
    }
}
