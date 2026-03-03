package com.seeker.agent.instrument.transformer;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

import com.seeker.agent.instrument.interceptor.AroundInterceptor;
import com.seeker.agent.instrument.interceptor.InterceptorRegistry;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

/**
 * Byte Buddy Advice를 사용하여 대상 메서드에 인터셉터를 적용하는 기본 트랜스포머입니다.
 */
public class BaseTransformer implements Transformer {

    private final String interceptorClassName;

    public BaseTransformer(String interceptorClassName) {
        this.interceptorClassName = interceptorClassName;
    }

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassLoader classLoader,
            JavaModule module,
            ProtectionDomain protectionDomain) {

        return builder.visit(Advice.withCustomMapping()
                .bind(InterceptorName.class, interceptorClassName)
                .to(InterceptorAdvice.class)
                .on(ElementMatchers.any()));
    }

    /**
     * 실제 메서드 바이트코드에 삽입될 Advice 로직입니다.
     */
    public static class InterceptorAdvice {
        /**
         * 메서드 실행 진입 시점에 호출됩니다.
         */
        @Advice.OnMethodEnter
        public static void onEnter(@InterceptorName String interceptorName,
                @Advice.This(optional = true) Object target,
                @Advice.AllArguments Object[] args) {
            AroundInterceptor interceptor = InterceptorRegistry.getInterceptor(interceptorName);
            if (interceptor != null) {
                interceptor.before(target, args);
            }
        }

        /**
         * 메서드 실행 종료 시점에 호출됩니다. (예외 발생 시에도 호출됨)
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void onExit(@InterceptorName String interceptorName,
                @Advice.This(optional = true) Object target,
                @Advice.AllArguments Object[] args,
                @Advice.Return(readOnly = false, typing = DYNAMIC) Object result,
                @Advice.Thrown Throwable throwable) {
            AroundInterceptor interceptor = InterceptorRegistry.getInterceptor(interceptorName);
            if (interceptor != null) {
                interceptor.after(target, args, result, throwable);
            }
        }
    }

    /**
     * Advice에 인터셉터 이름을 전달하기 위한 커스텀 어노테이션입니다.
     */
    @java.lang.annotation.Retention(RUNTIME)
    public @interface InterceptorName {
    }
}
