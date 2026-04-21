package com.seeker.agent.core.context;

import com.seeker.agent.core.model.Trace;

public interface TraceContext {
    Trace newTraceObject();

    Trace newTraceObject(TraceId traceId);

    Trace currentTraceObject();

    void setTraceObject(Trace trace);

    void removeTraceObject();

    Scope getScope(String name);
}
