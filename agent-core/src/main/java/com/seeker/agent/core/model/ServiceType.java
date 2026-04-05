package com.seeker.agent.core.model;

/**
 * 에이전트가 추적하는 서비스의 종류를 정의합니다.
 */
public enum ServiceType {
    // WAS
    TOMCAT(1000),
    SPRING_BOOT(1100),

    // 외부 호출 / DB
    JDBC(2000),
    MYSQL(2100),
    HTTP_CLIENT(9000),

    // 사용자 정의 / 내부 메서드
    USER_METHOD(5000),
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
