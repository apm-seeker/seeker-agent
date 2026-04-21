package com.seeker.agent.plugin.service;

import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import com.seeker.agent.instrument.plugin.Plugin;
import com.seeker.agent.instrument.transformer.BaseTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * 특정 패키지 하위의 모든 클래스를 추적하는 범용 서비스 플러그인.
 */
public class ServicePlugin implements Plugin {

    private final String packagePrefix;

    public ServicePlugin(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    @Override
    public AgentBuilder transform(AgentBuilder agentBuilder) {
        // 인터셉터 등록
        InterceptorRegistry.register("serviceInterceptor", new ServiceInterceptor());

        // 매칭 로직:
        // 1. 지정된 패키지 하위의 클래스
        // 2. public 메서드
        // 3. 생성자 제외, static 메서드 제외
        // 4. toString, hashCode, equals 등 기본 메서드 제외
        return agentBuilder.type(nameStartsWith(packagePrefix))
                .transform(new BaseTransformer("serviceInterceptor", getMethodMatcher()));
    }

    private ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return not(isStatic())
                .and(not(isConstructor()))
                .and(not(named("toString")))
                .and(not(named("hashCode")))
                .and(not(named("equals")))
                .and(not(named("clone")));
    }
}
