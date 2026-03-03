package com.seeker.agent.plugin.http;

import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Apache HttpClient의 요청을 인스트루멘테이션하는 플러그인입니다.
 */
public class HttpClientPlugin implements Plugin {
    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        return agentBuilder
                .type(ElementMatchers.named("org.apache.http.impl.client.InternalHttpClient"))
                .transform((builder, typeDescription, classLoader, module, pd) -> {
                    String interceptorName = "HttpClientInterceptor";

                    // 인터셉터 등록
                    InterceptorRegistry.register(interceptorName, new HttpClientExecuteInterceptor());

                    // doExecute 메서드에 Advice 적용 (매개변수 타입으로 정확히 매칭하는 것이 좋음)
                    return new BaseTransformer(interceptorName)
                            .transform(builder, typeDescription, classLoader, module, pd);
                });
    }
}
