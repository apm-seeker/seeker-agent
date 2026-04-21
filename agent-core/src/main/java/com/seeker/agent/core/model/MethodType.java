package com.seeker.agent.core.model;

public enum MethodType {

    // 외부 호출 / DB
    JDBC(2000),
    MYSQL(2100),
    HTTP_CLIENT(9000),

    // 사용자 정의 / 내부 메서드
    USER_METHOD(5000),
    UNKNOWN(-1);

    private final int code;

    MethodType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MethodType fromCode(int code) {
        for (MethodType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
