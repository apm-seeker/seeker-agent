package com.seeker.agent.config;

import java.util.Properties;

/**
 * Collector(gRPC 백엔드) 연결 정보를 관리하는 클래스.
 * 트레이스 데이터를 전송할 대상 서버의 호스트와 포트를 보관합니다.
 */
public class CollectorConfig {

    private final String host;
    private final int port;

    public CollectorConfig(Properties properties) {
        this.host = properties.getProperty("seeker.collector.host", "127.0.0.1");
        this.port = Integer.parseInt(properties.getProperty("seeker.collector.port", "9999"));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "CollectorConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
