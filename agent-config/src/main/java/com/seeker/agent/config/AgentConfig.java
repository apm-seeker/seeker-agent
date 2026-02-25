package com.seeker.agent.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 에이전트 전역 설정을 로드하고 관리하는 클래스.
 * seeker.config 파일 및 시스템 프로퍼티로부터 설정을 읽어옵니다.
 */
public class AgentConfig {

    private static final String DEFAULT_CONFIG_FILE = "seeker.config";
    private static final String CONFIG_PATH_PROPERTY = "seeker.config";

    private final String agentId;
    // 해당 appliaction의 이름을 설정을 한다.
    private final String applicationName;
    // 연동할 collector host를 설정을 해줍니다.
    private final String collectorHost;
    // 연결할 collectore의 port를 설정해줍니다.
    private final int collectorPort;
    // TODO 추후 sample 구현 예정
    private final double samplingRate;

    private AgentConfig(Properties properties) {
        this.agentId = properties.getProperty("seeker.agentId", "unnamed-agent");
        this.applicationName = properties.getProperty("seeker.applicationName", "unnamed-application");
        this.collectorHost = properties.getProperty("seeker.collector.host", "127.0.0.1");
        this.collectorPort = Integer.parseInt(properties.getProperty("seeker.collector.port", "9991"));
        this.samplingRate = Double.parseDouble(properties.getProperty("seeker.sampling.rate", "1.0"));
    }

    public static AgentConfig load() {
        Properties properties = new Properties();

        // 1. 클래스패스에서 기본 설정 로드
        try (InputStream is = AgentConfig.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException ignored) {

        }

        // 2. 외부 설정 파일 로드 (시스템 프로퍼티 -Dseeker.config=...)
        String externalPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (externalPath != null) {
            try (InputStream is = new FileInputStream(externalPath)) {
                properties.load(is);
            } catch (IOException e) {
                System.err.println("[Seeker] Failed to load external config: " + externalPath);
            }
        }

        // 3. 시스템 프로퍼티로 개별 설정 덮어쓰기
        properties.putAll(System.getProperties());

        return new AgentConfig(properties);
    }

    public String getAgentId() {
        return agentId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getCollectorHost() {
        return collectorHost;
    }

    public int getCollectorPort() {
        return collectorPort;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "agentId='" + agentId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", collectorHost='" + collectorHost + '\'' +
                ", collectorPort=" + collectorPort +
                ", samplingRate=" + samplingRate +
                '}';
    }
}
