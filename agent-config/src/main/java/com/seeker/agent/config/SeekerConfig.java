package com.seeker.agent.config;

import com.seeker.agent.config.loader.PropertiesLoader;
import com.seeker.agent.config.properties.AgentIdentityConfig;
import com.seeker.agent.config.properties.CollectorConfig;
import com.seeker.agent.config.properties.ProfilerConfig;

import java.util.Properties;

/**
 * Seeker agent의 전체 설정을 묶는 config facade.
 *
 * <p>외부 모듈은 {@link PropertiesLoader}와 개별 {@code Properties} 기반 설정 객체를
 * 직접 조립하지 않고 이 클래스의 {@link #load()}를 통해 필요한 설정 묶음을 얻는다.
 * 실제 properties 병합 규칙은 {@link PropertiesLoader}에 유지하고, 이 클래스는 병합된
 * {@link Properties}를 agent identity, collector, profiler 설정으로 변환하는 경계 역할만 한다.
 *
 * <p>설정 로드 순서는 {@link PropertiesLoader}의 정책을 따른다.
 * 현재 정책은 classpath {@code seeker.config}, 외부 파일 {@code -Dseeker.config=...},
 * JVM system properties 순서로 병합한다.
 */
public final class SeekerConfig {

    private final AgentIdentityConfig identity;
    private final CollectorConfig collector;
    private final ProfilerConfig profiler;

    private SeekerConfig(AgentIdentityConfig identity,
                         CollectorConfig collector,
                         ProfilerConfig profiler) {
        this.identity = identity;
        this.collector = collector;
        this.profiler = profiler;
    }

    /**
     * 현재 JVM 환경에서 Seeker agent 설정을 로드한다.
     *
     * @return agent bootstrap 과정에서 사용할 typed config 묶음
     */
    public static SeekerConfig load() {
        Properties properties = PropertiesLoader.load();
        return new SeekerConfig(
                new AgentIdentityConfig(properties),
                new CollectorConfig(properties),
                new ProfilerConfig(properties));
    }

    /**
     * agent id, name, type, group 설정을 반환한다.
     */
    public AgentIdentityConfig identity() {
        return identity;
    }

    /**
     * collector host와 HTTP/gRPC port 설정을 반환한다.
     */
    public CollectorConfig collector() {
        return collector;
    }

    /**
     * profiler, plugin, metric 관련 설정을 반환한다.
     */
    public ProfilerConfig profiler() {
        return profiler;
    }

    @Override
    public String toString() {
        return "SeekerConfig{" +
                "identity=" + identity +
                ", collector=" + collector +
                ", profiler=" + profiler +
                '}';
    }
}
