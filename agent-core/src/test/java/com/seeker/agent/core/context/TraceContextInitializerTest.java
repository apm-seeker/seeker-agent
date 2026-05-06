package com.seeker.agent.core.context;

import com.seeker.agent.core.context.propagation.PropagatorHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceContextInitializerTest {

    @Test
    @DisplayName("TraceContext와 propagator holder를 초기화한다")
    void initializeTraceContext() {
        TraceContextInitializer.initialize();

        assertNotNull(TraceContextHolder.getContext());
        assertNotNull(PropagatorHolder.get());
    }
}
