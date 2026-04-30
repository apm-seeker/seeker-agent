package com.seeker.agent.bootstrap;

import com.seeker.agent.config.AgentIdentityConfig;
import com.seeker.agent.config.CollectorConfig;
import com.seeker.agent.config.ProfilerConfig;
import com.seeker.agent.config.PropertiesLoader;
import com.seeker.agent.core.context.ThreadLocalTraceContext;
import com.seeker.agent.core.context.TraceContextHolder;
import com.seeker.agent.core.context.propagation.PropagatorHolder;
import com.seeker.agent.core.context.propagation.W3CTraceContextPropagator;
import com.seeker.agent.core.metric.MetricRegistry;
import com.seeker.agent.core.sender.MetricSender;
import com.seeker.agent.core.model.AgentInfo;
import com.seeker.agent.core.model.AgentInfoHolder;
import com.seeker.agent.core.sender.AgentInfoSender;
import com.seeker.agent.core.sender.DataSender;
import com.seeker.agent.core.sender.DataSenderHolder;
import com.seeker.agent.instrument.InstrumentEngine;
import com.seeker.agent.metric.collector.JvmClassCollector;
import com.seeker.agent.metric.collector.JvmGcCollector;
import com.seeker.agent.metric.collector.JvmMemoryCollector;
import com.seeker.agent.metric.collector.JvmThreadCollector;
import com.seeker.agent.metric.collector.SystemCpuCollector;
import com.seeker.agent.metric.scheduler.MetricScheduler;
import com.seeker.agent.sender.AsyncSpanDispatcher;
import com.seeker.agent.sender.GrpcMetricSender;
import com.seeker.agent.sender.GrpcSpanTransport;
import com.seeker.agent.sender.HttpAgentInfoSender;
import com.seeker.agent.sender.SpanTransport;
import com.seeker.agent.sender.console.ConsoleAgentInfoSender;
import com.seeker.agent.sender.console.ConsoleMetricSender;
import com.seeker.agent.sender.console.ConsoleSpanTransport;
import com.seeker.agent.plugin.http.HttpClientPlugin;
import com.seeker.agent.plugin.jdbc.JdbcPlugin;
import com.seeker.agent.plugin.service.ServicePlugin;
import com.seeker.agent.plugin.was.tomcat.TomcatPlugin;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.instrument.Instrumentation;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Java Agent의 진입점 클래스입니다.
 */
public class AgentMain {
    /**
     * JVM 시작 시 실행되는 메서드
     *
     * @param agentArgs 에이전트 인자
     * @param inst      JVM Instrumentation 인스턴스
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Seeker] Seeker Agent 가동 시작...");

        // 설정 로드
        Properties properties = PropertiesLoader.load();
        AgentIdentityConfig identityConfig = new AgentIdentityConfig(properties);
        CollectorConfig collectorConfig = new CollectorConfig(properties);
        ProfilerConfig profilerConfig = new ProfilerConfig(properties);
        System.out.println("[Seeker] 로드된 설정: " + identityConfig + ", " + collectorConfig + ", " + profilerConfig);

        boolean debugEnabled = profilerConfig.isDebugEnabled();
        if (debugEnabled) {
            System.out.println("[Seeker] DEBUG MODE ENABLED — collector 통신 차단, 수집 데이터를 콘솔에 출력");
        }

        // 에이전트 등록 (디버그 모드 시 콘솔, 평소 HTTP)
        long startTime = System.currentTimeMillis();
        AgentInfo agentInfo = new AgentInfo(
                identityConfig.getAgentId(),
                identityConfig.getAgentName(),
                identityConfig.getAgentType(),
                identityConfig.getAgentGroup(),
                startTime);
        AgentInfoSender agentInfoSender = debugEnabled
                ? new ConsoleAgentInfoSender()
                : new HttpAgentInfoSender(collectorConfig.getHost(), collectorConfig.getHttpPort());
        agentInfoSender.register(agentInfo);

        // 인터셉터가 분산 추적 헤더에 자기 agentId를 실어보내려면 전역 접근이 필요
        AgentInfoHolder.set(agentInfo);

        // TraceContext 초기화 및 Holder에 등록
        TraceContextHolder.setTraceContext(new ThreadLocalTraceContext());

        // 분산 추적 헤더 propagator 등록 (기본: W3C Trace Context)
        PropagatorHolder.setPropagator(new W3CTraceContextPropagator());

        // gRPC 채널을 1개만 생성하여 trace/metric 송신기가 공유한다.
        // 채널 위에 각자 별도 stream을 열어 격리하면서 채널 자원·인증은 한 번만 맺는다.
        // 채널 라이프사이클은 AgentMain이 책임지며, 마지막 shutdown hook에서 종료한다.
        final ManagedChannel grpcChannel = debugEnabled
                ? null
                : ManagedChannelBuilder.forAddress(collectorConfig.getHost(), collectorConfig.getGrpcPort())
                        .usePlaintext()
                        .build();

        // DataSender 초기화 및 등록 (Dispatcher + Transport 조합, 디버그 모드 시 Transport는 콘솔)
        SpanTransport transport = debugEnabled
                ? new ConsoleSpanTransport()
                : new GrpcSpanTransport(
                        grpcChannel,
                        identityConfig.getAgentName(),
                        identityConfig.getAgentId());
        DataSender sender = new AsyncSpanDispatcher(transport, 1024 * 8);
        DataSenderHolder.setSender(sender);

        InstrumentEngine engine = new InstrumentEngine();

        // 플러그인 등록
        engine.addPlugin(new TomcatPlugin());
        engine.addPlugin(new HttpClientPlugin());
        engine.addPlugin(new JdbcPlugin());

        // 범용 서비스 플러그인 등록
        String basePackages = profilerConfig.getBasePackages();
        if (basePackages != null && !basePackages.isEmpty()) {
            String[] packages = basePackages.split(",");
            for (String pkg : packages) {
                engine.addPlugin(new ServicePlugin(pkg.trim()));
            }
        }

        // instrumentation 설치
        engine.install(inst);

        // ──────────────── JVM 메트릭 수집 가동 ────────────────
        if (profilerConfig.isMetricEnabled()) {
            startMetricMonitor(profilerConfig, grpcChannel, debugEnabled, agentInfo);
        }

        // 공유 gRPC 채널 종료 hook — trace/metric 종료 후 마지막에 채널 정리.
        // 다른 sender들의 close()는 자기 stream만 정리하고 채널은 안 닫으므로 여기서 일괄 종료.
        if (grpcChannel != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    grpcChannel.shutdown();
                    if (!grpcChannel.awaitTermination(2, TimeUnit.SECONDS)) {
                        grpcChannel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    grpcChannel.shutdownNow();
                }
            }, "seeker-grpc-shutdown"));
        }

        System.out.println("[Seeker] Seeker Agent 설치 완료.");
    }

    /**
     * 메트릭 수집기들을 등록하고 스케줄러를 가동한다.
     * 디버그 모드 → ConsoleMetricSender, 정상 모드 → GrpcMetricSender (공유 채널 + 별도 stream).
     */
    private static void startMetricMonitor(ProfilerConfig profilerConfig,
                                           ManagedChannel grpcChannel,
                                           boolean debugEnabled,
                                           AgentInfo agentInfo) {
        MetricRegistry registry = new MetricRegistry();
        registry.register(new JvmGcCollector());
        registry.register(new JvmMemoryCollector());
        registry.register(new JvmThreadCollector());
        registry.register(new JvmClassCollector());
        registry.register(new SystemCpuCollector());

        // 송신기 분기 — 정상 모드는 trace와 채널 공유, stream만 별도.
        MetricSender metricSender = debugEnabled
                ? new ConsoleMetricSender()
                : new GrpcMetricSender(grpcChannel);

        MetricScheduler scheduler = new MetricScheduler(
                registry, metricSender, agentInfo,
                profilerConfig.getMetricIntervalMs(),
                profilerConfig.getMetricBatchSize());
        scheduler.start();

        // JVM 종료 시 순서 (이 hook):
        //   1) scheduler.stop()  — 다음 cycle 취소 + 진행 중 cycle 종료 + 잔여 버퍼 flush 시도
        //   2) sender.close()    — 자기 stream만 정리 (채널은 안 닫음)
        // 공유 채널 자체는 별도 hook("seeker-grpc-shutdown")에서 일괄 종료된다.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.stop();
            if (metricSender instanceof java.io.Closeable) {
                try {
                    ((java.io.Closeable) metricSender).close();
                } catch (Exception ignored) {
                    // shutdown 경로에서는 throw 금지
                }
            }
        }, "seeker-stat-shutdown"));
    }
}
