package com.seeker.agent.plugin.jdbc;

import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * JDBC의 PreparedStatement를 인스트루멘테이션하는 플러그인입니다.
 */
import com.seeker.agent.core.context.SqlMetadataAccessor;
import net.bytebuddy.description.modifier.Visibility;

/**
 * JDBC의 PreparedStatement 및 Connection을 인스트루멘테이션하는 플러그인입니다.
 */
public class JdbcPlugin implements Plugin {
    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        // 1. Connection.prepareStatement 가로채기
        agentBuilder = agentBuilder
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("java.sql.Connection"))
                        .and(ElementMatchers.not(ElementMatchers.isInterface()))
                        .and(ElementMatchers.not(ElementMatchers.isAbstract())))
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "JdbcConnectionInterceptor";
                    InterceptorRegistry.register(interceptorName, new ConnectionPrepareStatementInterceptor());

                    return new BaseTransformer(interceptorName,
                            ElementMatchers.named("prepareStatement")
                                    .or(ElementMatchers.named("prepareCall")))
                            .transform(builder, typeDescription, classLoader, module, pd);
                });

        // 2. PreparedStatement에 SQL 저장용 필드 추가 및 execute 가로채기
        return agentBuilder
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("java.sql.PreparedStatement"))
                        .and(ElementMatchers.not(ElementMatchers.isInterface()))
                        .and(ElementMatchers.not(ElementMatchers.isAbstract())))
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "JdbcPreparedStatementInterceptor";
                    InterceptorRegistry.register(interceptorName, new PreparedStatementExecuteInterceptor());

                    // 필드 추가 및 인터페이스 구현
                    builder = builder.defineField("_$seeker$sql", String.class, Visibility.PRIVATE)
                            .implement(SqlMetadataAccessor.class)
                            .intercept(net.bytebuddy.implementation.FieldAccessor.ofField("_$seeker$sql"));

                    // execute, executeQuery, executeUpdate, executeBatch, executeLargeUpdate 메서드만
                    // Advice 적용
                    return new BaseTransformer(interceptorName,
                            ElementMatchers.named("execute")
                                    .or(ElementMatchers.named("executeQuery"))
                                    .or(ElementMatchers.named("executeUpdate"))
                                    .or(ElementMatchers.named("executeBatch"))
                                    .or(ElementMatchers.named("executeLargeUpdate")))
                            .transform(builder, typeDescription, classLoader, module, pd);
                });
    }
}
