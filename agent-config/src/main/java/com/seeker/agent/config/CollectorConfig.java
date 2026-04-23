package com.seeker.agent.config;

import java.util.Properties;

/**
 * Collector(백엔드) 연결 정보를 관리하는 클래스.
 * 트레이스 데이터를 전송할 대상 서버의 호스트와 gRPC/HTTP 포트를 보관합니다.
 */
public class CollectorConfig {

    private final String host;
    private final int grpcPort;
    private final int httpPort;

    public CollectorConfig(Properties properties) {
        this.host = properties.getProperty("seeker.collector.host", "127.0.0.1");
        this.grpcPort = Integer.parseInt(properties.getProperty("seeker.collector.grpc-port", "9999"));
        this.httpPort = Integer.parseInt(properties.getProperty("seeker.collector.http-port", "8888"));
    }

    public String getHost() {
        return host;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    @Override
    public String toString() {
        return "CollectorConfig{" +
                "host='" + host + '\'' +
                ", grpcPort=" + grpcPort +
                ", httpPort=" + httpPort +
                '}';
    }
}