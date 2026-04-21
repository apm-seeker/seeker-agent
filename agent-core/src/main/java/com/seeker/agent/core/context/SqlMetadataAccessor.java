package com.seeker.agent.core.context;

/**
 * PreparedStatement 객체에 SQL 문장을 보관하고 꺼내기 위한 인터페이스입니다.
 * 런타임에 PreparedStatement 구현체에 이 인터페이스가 추가됩니다.
 */
public interface SqlMetadataAccessor {
    void _$seeker$setSql(String sql);

    String _$seeker$getSql();
}
