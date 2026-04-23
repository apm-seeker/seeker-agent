package com.seeker.agent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectorConfigTest {

    @Nested
    @DisplayName("host")
    class Host {
        @Test
        @DisplayName("프로퍼티 값을 그대로 보관한다")
        void usesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.collector.host", "10.0.0.1");

            CollectorConfig config = new CollectorConfig(props);

            assertEquals("10.0.0.1", config.getHost());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 127.0.0.1을 사용한다")
        void defaultsToLocalhost() {
            CollectorConfig config = new CollectorConfig(new Properties());

            assertEquals("127.0.0.1", config.getHost());
        }
    }

    @Nested
    @DisplayName("grpcPort")
    class GrpcPort {
        @Test
        @DisplayName("프로퍼티 값을 int로 파싱한다")
        void parsesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.collector.grpc-port", "8080");

            CollectorConfig config = new CollectorConfig(props);

            assertEquals(8080, config.getGrpcPort());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 9999를 사용한다")
        void defaultsTo9999() {
            CollectorConfig config = new CollectorConfig(new Properties());

            assertEquals(9999, config.getGrpcPort());
        }
    }

    @Nested
    @DisplayName("httpPort")
    class HttpPort {
        @Test
        @DisplayName("프로퍼티 값을 int로 파싱한다")
        void parsesProperty() {
            Properties props = new Properties();
            props.setProperty("seeker.collector.http-port", "9090");

            CollectorConfig config = new CollectorConfig(props);

            assertEquals(9090, config.getHttpPort());
        }

        @Test
        @DisplayName("프로퍼티가 없으면 기본값 8081을 사용한다")
        void defaultsTo8888() {
            CollectorConfig config = new CollectorConfig(new Properties());

            assertEquals(8081, config.getHttpPort());
        }
    }
}