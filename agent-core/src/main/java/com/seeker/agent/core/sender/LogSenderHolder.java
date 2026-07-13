package com.seeker.agent.core.sender;

/**
 * Runtime에서 주입되는 {@link LogSender} holder.
 *
 * <p>Log plugin이 sender 모듈을 직접 의존하지 않도록 core에는 holder와 interface만 둔다.
 * 기본값은 아무것도 하지 않는 Null Object라 log sender wiring 전에도 안전하다.
 */
public final class LogSenderHolder {

    private static volatile LogSender sender = record -> {
        // default no-op
    };

    private LogSenderHolder() {
    }

    public static void setSender(LogSender newSender) {
        if (newSender != null) {
            sender = newSender;
        }
    }

    public static LogSender getSender() {
        return sender;
    }
}
