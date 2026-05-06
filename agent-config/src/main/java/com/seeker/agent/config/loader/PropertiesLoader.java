package com.seeker.agent.config.loader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 에이전트 설정 프로퍼티를 로드하는 유틸리티.
 * 클래스패스의 seeker.config, 외부 파일(-Dseeker.config=...), 시스템 프로퍼티 순으로 병합합니다.
 */
public final class PropertiesLoader {

    private static final String DEFAULT_CONFIG_FILE = "seeker.config";
    private static final String CONFIG_PATH_PROPERTY = "seeker.config";

    private PropertiesLoader() {
    }

    public static Properties load() {
        Properties properties = new Properties();

        // 1. 클래스패스에서 기본 설정 로드
        try (InputStream is = PropertiesLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            System.err.println("[Seeker] Failed to load classpath config: " + DEFAULT_CONFIG_FILE + " (" + e.getMessage() + ")");
        }

        // 2. 외부 설정 파일 로드 (시스템 프로퍼티 -Dseeker.config=...)
        String externalPath = System.getProperty(CONFIG_PATH_PROPERTY);
        if (externalPath != null) {
            try (InputStream is = new FileInputStream(externalPath)) {
                properties.load(is);
            } catch (IOException e) {
                System.err.println("[Seeker] Failed to load external config: " + externalPath + " (" + e.getMessage() + ")");
            }
        }

        // 3. 시스템 프로퍼티로 개별 설정 덮어쓰기
        properties.putAll(System.getProperties());

        return properties;
    }
}
