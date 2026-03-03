package com.seeker.agent.instrument.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * 인스트루멘테이션 대상에서 제외할 표준 규칙들을 정의하는 클래스입니다.
 */
public class StandardMatchers {

    /**
     * 에이전트가 절대로 건드리지 말아야 할 패키지 및 클래스 매처를 반환합니다.
     * 
     * @return 제외 대상 매처
     */
    public static ElementMatcher.Junction<TypeDescription> ignoreClasses() {
        return ElementMatchers.nameStartsWith("java.")
                .or(ElementMatchers.nameStartsWith("javax."))
                .or(ElementMatchers.nameStartsWith("sun."))
                .or(ElementMatchers.nameStartsWith("com.sun."))
                .or(ElementMatchers.nameStartsWith("net.bytebuddy."))
                .or(ElementMatchers.nameStartsWith("com.seeker.agent.")) // 에이전트 자체 패키지 제외
                .or(ElementMatchers.nameContains("ByCGLIB")) // CGLIB 프록시 제외
                .or(ElementMatchers.nameContains("$$")) // 런타임 생성 클래스 (Lambda 등) 제외
                .or(ElementMatchers.isSynthetic()); // 컴파일러 생성 클래스 제외
    }
}
