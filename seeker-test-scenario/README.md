# seeker-test-scenario — 분산 추적 검증용 멀티 서비스

기존 `seeker-test`, `seeker-test2` 는 1~2개 서비스만 띄워 추적이 가능한지만 확인.
이 모듈은 **현실적인 결제 시나리오** 를 5개 서비스 + 1개 mock PG 로 재현해서
**분산 추적 전파, P95 spike, 다양한 에러 분기** 를 한 번에 확인할 수 있게 합니다.

---

## 서비스 토폴로지

```
[Client]
   |
   v POST /api/checkout/{scenario}
[API-GATEWAY    :8090]
   |
   +-> [AUTH-SERVICE   :8091]      (토큰 검증)
   |
   +-> [ORDER-SERVICE  :8092]
        |
        +-> [PRODUCT-SERVICE :8093] (재고 차감)
        +-> [PAYMENT-SERVICE :8094]
             |
             +-> [PG-MOCK    :8095] (외부 PG 모의)
```

모든 서비스는 **단일 jar (`scenario.jar`)** 를 다른 포트 + 다른 `seeker.config` 로 띄운 동일 프로세스.
agent 가 보는 이름만 다를 뿐 코드는 한 곳. seeker-test2 의 Gateway/Backend 패턴 그대로 확장.

---

## Public API (Gateway :8090)

운영 e-commerce 외형. 에러/지연은 downstream 의 **Chaos** 가 자체 확률로 발생 — path 에 `error-*` 같은 힌트 없음.

| Method | Path                       | 인증            | 호출 체인                                                       |
|--------|----------------------------|-----------------|----------------------------------------------------------------|
| POST   | `/api/auth/login`          | -               | Gateway → Auth(DB)                                              |
| GET    | `/api/users/me`            | Bearer token    | Gateway → Auth(DB)                                              |
| GET    | `/api/products`            | -               | Gateway → Product(DB)                                           |
| GET    | `/api/products/{id}`       | -               | Gateway → Product(DB)                                           |
| GET    | `/api/orders`              | Bearer token    | Gateway → Auth(DB) → Order(DB)                                  |
| POST   | `/api/orders`              | Bearer token    | Gateway → Auth(DB) → Order(DB) → Product(DB) → Payment(DB) → PG |

**알려진 토큰** (DataSeeder seed):
`TOKEN-ALICE` · `TOKEN-BOB` · `TOKEN-CAROL`

## Chaos 자동 발생 표

각 downstream 컨트롤러가 `ThreadLocalRandom` 으로 확률 발생.

| Service  | 종류                             | 확률 | 결과                                              |
|----------|----------------------------------|------|--------------------------------------------------|
| AUTH     | login slow                       | 1%   | 150~400ms 지연                                    |
| AUTH     | verify slow                      | 0.5% | 100~250ms 지연                                    |
| PRODUCT  | 모든 endpoint slow               | 1%   | 50~200ms 지연                                     |
| ORDER    | DB NOT NULL 위반                 | 0.3% | DataIntegrityViolationException → 500             |
| PAYMENT  | P95 spike                        | 2%   | 400~1200ms 지연                                   |
| PAYMENT  | NullPointerException             | 1.5% | 500 — exception 패널에 stacktrace                  |
| PG-MOCK  | timeout (3s hang, 호출자 2s sockTO) | 1% | SocketTimeoutException → PAYMENT 500              |
| PG-MOCK  | NullPointerException             | 0.5% | 500                                              |
| PRODUCT  | OUT_OF_STOCK (id=5 헤드셋)        | 항상 | 400 — `POST /api/orders` 가 productId=5 일 때      |

위 확률 합산으로 **정상 트래픽 95%+ / 가끔 fault** — 운영 환경에 가까운 패턴.

---

## 사전 준비

### 1. MySQL 띄우기

```powershell
cd C:\Users\SSAFY\seok\final_project\seeker-agent\seeker-test-scenario
docker compose up -d
```

`seeker_scenario` schema 자동 생성. seed 데이터 (`alice/bob/carol`, 상품 5종) 는 첫 부팅 시 `DataSeeder` 가 채움.

### 2. seeker 인프라 (collector / kafka / clickhouse / 웹)

루트 `docker-compose.yml` + `seeker-collector` / `seeker-web` 가 동작 중이어야 함.
**collector 가 `127.0.0.1:9999` (gRPC) + `127.0.0.1:8081` (HTTP) 에서 listen** 해야 agent 가 연결.

---

## 빌드 + 실행

```powershell
cd C:\Users\SSAFY\seok\final_project\seeker-agent\seeker-test-scenario
.\scripts\run-all.ps1        # gradle 빌드 + 6개 인스턴스 시작
```

옵션:
- `.\scripts\run-all.ps1 -NoBuild` — 이미 빌드돼 있으면 빌드 건너뛰기
- `.\scripts\stop-all.ps1` — 8080-8085 포트의 java 프로세스 일괄 종료

부팅에 약 10-20초. 6개 콘솔 창이 minimize 로 뜸.

---

## 분산 추적 확인

### 단발 호출

```powershell
# 무인증
curl.exe http://localhost:8090/api/products
curl.exe http://localhost:8090/api/products/1

# 로그인 (token 받기)
curl.exe -X POST http://localhost:8090/api/auth/login -H 'Content-Type: application/json' -d '{"username":"alice","password":"x"}'

# Bearer 인증 후
curl.exe -H 'Authorization: Bearer TOKEN-ALICE' http://localhost:8090/api/users/me
curl.exe -H 'Authorization: Bearer TOKEN-ALICE' http://localhost:8090/api/orders
curl.exe -X POST -H 'Authorization: Bearer TOKEN-ALICE' -H 'Content-Type: application/json' http://localhost:8090/api/orders -d '{"productId":1,"quantity":1}'
```

### 부하 러너 (운영 e-commerce 트래픽 모사)

```powershell
.\scripts\load-runner.ps1                       # 무한 반복, 600ms 간격
.\scripts\load-runner.ps1 -IntervalMs 300       # 더 빠르게
.\scripts\load-runner.ps1 -Count 500            # 500번만
```

호출 가중치 (총 100):
- GET /api/products 30 · GET /api/products/{id} 20
- GET /api/users/me 15 · GET /api/orders 15
- POST /api/auth/login 10 · POST /api/orders 10

### k6 — 이벤트성 flash sale spike (한 번에 몰리는 부하)

운영에서 한정수량 이벤트가 시작되는 순간처럼 **동시 접속이 급증하는 패턴** 을 재현.
평소 트래픽을 background 로 깔아두고, 30초 후 이벤트가 터지면서 2000 req/s 까지 ramp.

```powershell
# k6 설치 (Windows)
choco install k6 -y
# 또는 https://k6.io/docs/get-started/installation/ 에서 binary 받기

# 실행
k6 run .\scripts\flash-sale.k6.js

# 다른 상품으로 몰리기
k6 run -e PRODUCT=2 .\scripts\flash-sale.k6.js

# Gateway 가 다른 호스트면
k6 run -e GW=http://10.0.0.5:8090 .\scripts\flash-sale.k6.js
```

**시나리오 타임라인 (총 2분)**

| 시각 | baseline (browse) | flash_sale (POST /api/orders) |
|------|-------------------|-------------------------------|
| 0s   | 30 req/s 시작     | -                             |
| 30s  | 30 req/s          | 0 → 500 req/s ramp 5s         |
| 35s  | 30 req/s          | 500 → **2000** req/s ramp 5s  |
| 40s  | 30 req/s          | **2000 req/s 유지** 20s        |
| 60s  | 30 req/s          | 2000 → 100 진정 20s            |
| 80s  | 30 req/s          | 100 → 0 마무리 10s             |
| 120s | 종료              | (이미 종료)                    |

**대시보드/트레이스에서 보일 것**
- Scatter: t=35s 부근에 점이 폭발적으로 증가
- Metric: PAYMENT/PG-MOCK 의 P99 spike, throughput 곡선 산봉우리
- Traces: `POST /api/orders` 가 평소 50ms 에서 수초까지 늘어지는 trace 가 다수
- Topology 엣지: ORDER→PAYMENT, PAYMENT→PG-MOCK 의 호출량이 빨간색 (high traffic)

---

## 검증 체크리스트

- [ ] 대시보드 토폴로지에 6 노드 (USER 포함) + 5 엣지 표시
- [ ] `/traces` 검색에서 `agentIds=API-GATEWAY` 필터링 동작
- [ ] 부하 러너 1~2분 돌리면 `exceptionClass=NullPointerException` trace 가 가끔 발견 (PAYMENT 1.5% / PG-MOCK 0.5%)
- [ ] PAYMENT span duration ≥ 400ms 가 가끔 발견 (2% spike)
- [ ] SocketTimeoutException trace 가 가끔 발견 (PG-MOCK timeout 1%)
- [ ] AUTH, ORDER, PRODUCT, PAYMENT 각 service 별 JVM metric 페이지 표시

---

## 제약

- **분산 전파는 Apache HttpClient 4.x 만 추적**. RestTemplate/WebClient 호출은 trace 가 끊김 (현재 plugin 한계).
- `error-db` 는 ORDER 단에서 JPA flush 시점에 예외. JDBC plugin 이 캡처하는 SQL 은 valid statement 만이라 NOT NULL 위반 자체는 stacktrace 만 보임.
- 단일 jar 라서 모든 인스턴스가 같은 entity/repository 를 로드. PRODUCT/AUTH 인스턴스에도 OrderRecord 테이블 매핑이 들어가지만 사용 안 함 (불필요한 ddl-auto 영향 없음).
