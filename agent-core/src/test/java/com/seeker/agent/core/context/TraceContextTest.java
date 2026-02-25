package com.seeker.agent.core.context;

import com.seeker.agent.core.model.Trace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraceContextTest {

    private TraceContext traceContext;

    @BeforeEach
    void setUp() {
        traceContext = new TraceContext();
    }

    @Test
    @DisplayName("새로운 트레이스를 생성하고 ThreadLocal에 보관한다")
    void newTraceObject() {
        Trace trace = traceContext.newTraceObject();

        assertNotNull(trace);
        assertEquals(trace, traceContext.currentTraceObject());
        assertNotNull(trace.getTraceId());

        traceContext.removeTraceObject();
        assertNull(traceContext.currentTraceObject());
    }

    @Test
    @DisplayName("전달받은 TraceId를 사용하여 트레이스를 이어간다")
    void continueTraceObject() {
        TraceId parentId = new TraceId();
        Trace trace = traceContext.continueTraceObject(parentId);

        assertNotNull(trace);
        assertEquals(parentId, trace.getTraceId());
        assertEquals(trace, traceContext.currentTraceObject());

        traceContext.removeTraceObject();
    }

    @Test
    @DisplayName("TraceBlock 시작과 종료 시 SpanEvent가 올바르게 쌓인다")
    void traceBlockEvents() {
        Trace trace = traceContext.newTraceObject();

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

        traceContext.removeTraceObject();
    }
}
