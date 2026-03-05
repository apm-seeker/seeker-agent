package com.seeker.agent.core.sender;

import com.seeker.agent.core.model.Span;

/**
 * DataSender 인스턴스를 관리하는 홀더 클래스입니다.
 */
public class DataSenderHolder {

    private static volatile DataSender sender = span -> {
        // 기본적으로 아무것도 하지 않는 Null Object 패턴 적용
    };

    public static void setSender(DataSender newSender) {
        if (newSender != null) {
            sender = newSender;
        }
    }

    public static DataSender getSender() {
        return sender;
    }
}
