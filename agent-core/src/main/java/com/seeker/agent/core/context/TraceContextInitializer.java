package com.seeker.agent.core.context;

import com.seeker.agent.core.context.propagation.PropagatorHolder;
import com.seeker.agent.core.context.propagation.W3CTraceContextPropagator;

public final class TraceContextInitializer {

    private TraceContextInitializer() {
    }

    public static void initialize() {
        TraceContextHolder.setTraceContext(new ThreadLocalTraceContext());
        PropagatorHolder.setPropagator(new W3CTraceContextPropagator());
    }
}
