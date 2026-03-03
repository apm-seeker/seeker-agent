package com.seeker.agent.plugin.was.tomcat;

import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Tomcat의 핵심 밸브인 StandardHostValve를 인스트루멘테이션하는 플러그인입니다.
 */
public class TomcatPlugin implements Plugin {
    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(ElementMatchers.named("org.apache.catalina.core.StandardHostValve"))
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "TomcatStandardHostValveInterceptor";

                    // 인터셉터 등록
                    InterceptorRegistry.register(interceptorName, new StandardHostValveInvokeInterceptor());

                    // invoke 메서드에 Advice 적용
                    return new BaseTransformer(interceptorName)
                            .transform(builder, typeDescription, classLoader, module, pd);
                });
    }
}
