# Seeker Agent Configuration Package

이 패키지는 Seeker 에이전트의 설정을 로드하고 관리하는 역할을 담당합니다.

## 주요 구성 요소

1. **AgentConfig.java**:
   - 에이전트 전역 설정을 로드합니다 (Agent ID, Application Name, Collector 주소 등).
   - 로드 우선순위:
     1. classpath의 `seeker.config` (기본값)
     2. `-Dseeker.config` 시스템 프로퍼티로 지정된 외부 설정 파일
     3. 개별 `-Dseeker.xxx` 시스템 프로퍼티 (가장 높은 우선순위)

2. **ProfilerConfig.java**:
   - 세부 프로파일링 설정을 관리합니다 (JDBC/HTTP/Spring 플러그인 활성 여부, 최대 이벤트 개수 등).

3. **seeker.config** (resources):
   - 에이전트의 기본 설정값을 담고 있는 텍스트 파일입니다.

## 진행 사항 (1단계: Config 설계 및 패키지 구조 재구성)

- **아키텍처 맞춤 패키지 이동**:
  - 기존 `com.seeker.agent.context` 패키지의 `Trace`, `Span`, `SpanEvent` 모델을 `com.seeker.agent.core.model` 패키지로 이동하여 계층형 구조를 명확히 했습니다.
- **설정 로직 구현**:
  - 파일 및 시스템 프로퍼티로부터 동적으로 설정을 읽어오는 `AgentConfig`를 구현했습니다.
  - 플러그인별 활성화 제어를 위한 `ProfilerConfig`의 토대를 마련했습니다.
