package com.seeker.agent.instrument.sample;

import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * com.example.DemoService 클래스의 메서드를 가로채는 테스트용 플러그인입니다.
 */
public class TestPlugin implements Plugin {
    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(ElementMatchers.named("com.example.DemoService")) // 대상 클래스 지정
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "TestInterceptor";

                    // 인터셉터 레지스트리에 등록
                    InterceptorRegistry.register(interceptorName, new TestInterceptor());

                    // BaseTransformer를 사용하여 Advice 적용
                    return new BaseTransformer(interceptorName)
                            .transform(builder, typeDescription, classLoader, module, pd);
                });
    }
}
