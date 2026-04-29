package com.seeker.agent.core.context;

import com.seeker.agent.core.model.Trace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThreadLocalTraceContextTest {

    private ThreadLocalTraceContext threadLocalTraceContext;

    @BeforeEach
    void setUp() {
        threadLocalTraceContext = new ThreadLocalTraceContext();
    }

    @Test
    @DisplayName("새로운 트레이스를 생성하고 ThreadLocal에 보관한다")
    void newTraceObject() {
        Trace trace = threadLocalTraceContext.newTraceObject();

        assertNotNull(trace);
        assertEquals(trace, threadLocalTraceContext.currentTraceObject());
        assertNotNull(trace.getTraceId());

        threadLocalTraceContext.removeTraceObject();
        assertNull(threadLocalTraceContext.currentTraceObject());
    }

    @Test
    @DisplayName("전달받은 traceId/parentSpanId 위에 자기 spanId는 새로 생성하여 트레이스를 이어간다")
    void continueTraceObject() {
        String parentTraceId = "0123456789abcdef0123456789abcdef";
        long parentSpanId = 0xdeadbeefL;

        Trace trace = threadLocalTraceContext.continueTraceObject(parentTraceId, parentSpanId);

        assertNotNull(trace);
        assertEquals(parentTraceId, trace.getTraceId().getTraceId());
        assertEquals(parentSpanId, trace.getTraceId().getParentSpanId());
        // 자기 spanId는 로컬 생성 — parentSpanId와 절대 같지 않아야 한다.
        assertNotEquals(parentSpanId, trace.getTraceId().getSpanId());
        assertEquals(trace, threadLocalTraceContext.currentTraceObject());

        threadLocalTraceContext.removeTraceObject();
    }

    @Test
    @DisplayName("TraceBlock 시작과 종료 시 SpanEvent가 올바르게 쌓인다")
    void traceBlockEvents() {
        Trace trace = threadLocalTraceContext.newTraceObject();

        trace.traceBlockBegin(); // depth 1
        trace.traceBlockBegin(); // depth 2
        trace.traceBlockEnd(); // depth 2 end
        trace.traceBlockEnd(); // depth 1 end

        assertEquals(2, trace.getSpan().getSpanEventList().size());
        assertEquals(1, trace.getSpan().getSpanEventList().get(1).getDepth()); // 순서상 마지막에 끝난 게 리스트 끝으로? (현재 구현 확인 필요)
        // 현재 Trace.java 구현: span.addSpanEvent(event)는 리스트 끝에 추가.
        // depth 2가 먼저 끝남 -> seq 0, depth 2
        // depth 1이 나중에 끝남 -> seq 1, depth 1

        assertEquals(2, trace.getSpan().getSpanEventList().get(0).getDepth());
        assertEquals(1, trace.getSpan().getSpanEventList().get(1).getDepth());

        threadLocalTraceContext.removeTraceObject();
    }
}
