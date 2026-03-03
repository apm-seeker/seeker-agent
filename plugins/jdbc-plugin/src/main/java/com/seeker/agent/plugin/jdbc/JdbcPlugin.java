package com.seeker.agent.plugin.jdbc;

import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * JDBC의 PreparedStatement를 인스트루멘테이션하는 플러그인입니다.
 */
public class JdbcPlugin implements Plugin {
    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        // java.sql.PreparedStatement 인터페이스를 구현한 클래스들을 타겟으로 합니다.
        // 단, 인터페이스 자체나 추상 클래스는 제외하고 실제 구현체만 인스트루멘테이션합니다.
        return agentBuilder
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("java.sql.PreparedStatement"))
                        .and(ElementMatchers.not(ElementMatchers.isInterface()))
                        .and(ElementMatchers.not(ElementMatchers.isAbstract())))
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "JdbcPreparedStatementInterceptor";

                    // 인터셉터 등록
                    InterceptorRegistry.register(interceptorName, new PreparedStatementExecuteInterceptor());

                    // execute, executeQuery, executeUpdate 메서드에만 Advice 적용
                    return new BaseTransformer(interceptorName,
                            ElementMatchers.nameStartsWith("execute"))
                            .transform(builder, typeDescription, classLoader, module, pd);
                });
    }
}
