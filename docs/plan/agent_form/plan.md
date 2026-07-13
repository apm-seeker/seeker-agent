### 구현을 할 단계를 표시

1.  agent-config
2.  agent-core
3.  agent-sender
4.  agent-instrument 
5. 5.agent-bootstrap

단계별 구현 과정

- 1단계 agent-config부터 하는 이유 -> 모든 모듈이 설정값을 참조하기 때문에 제일 먼저 만들어야 다른 모듈 작업할 때 실제 값을 넣어서 테스트
- 2단계 agent-core는 Span, SpanEvent 같은 데이터 모델과 TraceContext가 여기 있어서, instrument나 sender 모두 이걸 참조
  - **세부순서**
    - **model (Span, SpanEvent, SpanChunk)**
    - → TraceId 
    - → sampler (Sampler, RateSampler) 해당 sampler는 추후 구현 예정
    - → recorder (SpanRecorder, SpanEventRecorder)
    - → storage (BufferedStorage)
    - → context (TraceContext)
    - → model/Trace (recorder, storage 다 만든 후에)
- 3단계 agent-sender는 Span 모델이 완성된 후에 변환하고 전송하는 로직 / 처음엔 gRPC 연동 전에 **테스트로 콘솔 출력용 MockSender로 대체**
- 4단계 agent-instrument는 실제 ByteBuddy 작업인데, core와 sender가 다 있어야 인터셉터 안에서 의미있는 동작을 확인 가능
  - InstrumentEngine (ByteBuddy 설정)
  - → AroundInterceptor 인터페이스
  - → AbstractWasInterceptor
  - → StandardHostValveInterceptor (Tomcat)
  - → TomcatPlugin
  - → WasPluginRegistry
  - → 나머지 플러그인 (Http, Jdbc, Spring)
- 5단계 agent-bootstrap은 마지막에 전체를 조립하는 단계라 앞의 모든 게 준비된 후에 진행