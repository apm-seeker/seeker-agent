package com.seeker.agent.core.model;

/**
 * 에이전트가 추적하는 서비스의 종류를 정의합니다.
 */
public enum ServiceType {
    // WAS
    TOMCAT(1000),
    SPRING_BOOT(1100),

    UNKNOWN(-1);

    private final int code;

    ServiceType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ServiceType fromCode(int code) {
        for (ServiceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
