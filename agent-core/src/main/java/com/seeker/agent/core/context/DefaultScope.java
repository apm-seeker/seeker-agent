package com.seeker.agent.core.context;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadLocal 기반의 기본 스코프 구현체.
 */
public class DefaultScope implements Scope {
    private final String name;
    private final ThreadLocal<AtomicInteger> depth = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    public DefaultScope(String name) {
        this.name = name;
    }

    @Override
    public boolean tryEnter() {
        if (depth.get().getAndIncrement() == 0) {
            return true;
        }
        return false;
    }

    @Override
    public void leave() {
        int d = depth.get().decrementAndGet();
        if (d < 0) {
            depth.get().set(0);
        }
    }

    @Override
    public boolean isRoot() {
        return depth.get().get() == 1;
    }

    @Override
    public String getName() {
        return name;
    }
}
