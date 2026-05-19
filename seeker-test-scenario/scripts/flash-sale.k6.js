// k6 시나리오 — 이벤트성 flash sale 부하.
//
// 동작:
//   t=0s   : 평소 트래픽 (browse 30 req/s) 만 흐름
//   t=30s  : 이벤트 시작 → flash_sale 시나리오가 0 → 500 req/s 까지 5초간 ramp
//   t=35s  : 500 → 2000 req/s 로 5초간 추가 spike (계산대 몰림)
//   t=40s  : 2000 req/s 유지 20초 (피크)
//   t=60s  : 2000 → 100 req/s 로 20초 진정 (꼬리 트래픽)
//   t=80s  : 100 → 0 마무리 10초
//
// 평소 트래픽은 전 구간 (2분) 동안 background 로 흘러서 "이벤트 전 / 중 / 후"
// 의 대시보드 변화를 한 화면에서 비교할 수 있게 한다.
//
// 사용:
//   k6 run scripts\flash-sale.k6.js
//   k6 run -e GW=http://10.0.0.5:8090 scripts\flash-sale.k6.js
//   k6 run -e PRODUCT=2 scripts\flash-sale.k6.js     # 다른 상품으로 몰리기

import http from 'k6/http'
import { sleep } from 'k6'

const GW = __ENV.GW || 'http://localhost:8090'
const FLASH_PRODUCT = parseInt(__ENV.PRODUCT || '4', 10)   // 모니터 (재고 충분)
const TOKENS = ['TOKEN-ALICE', 'TOKEN-BOB', 'TOKEN-CAROL']

function pickToken() {
    return TOKENS[Math.floor(Math.random() * TOKENS.length)]
}

export const options = {
    scenarios: {
        // 평상시 트래픽 — 항상 흐름. 카탈로그 둘러보기 위주.
        baseline: {
            executor: 'constant-arrival-rate',
            rate: 30,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 50,
            maxVUs: 200,
            exec: 'browse',
            tags: { phase: 'baseline' },
        },
        // 이벤트성 spike — 30초 후 시작.
        flash_sale: {
            executor: 'ramping-arrival-rate',
            startTime: '30s',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 300,
            maxVUs: 2500,
            stages: [
                { target: 100,  duration: '5s'  },   // 이벤트 시작
                { target: 500, duration: '5s'  },   // spike
                { target: 500, duration: '20s' },   // 피크 유지
                { target: 40,  duration: '20s' },   // 진정
                { target: 0,    duration: '10s' },   // 마무리
            ],
            exec: 'flashSale',
            tags: { phase: 'flash' },
        },
    },
    thresholds: {
        // 평소는 1초 안에 95% 처리되어야 함
        'http_req_duration{phase:baseline}': ['p(95)<1000'],
        // flash 는 부하가 심하니 느슨하게
        'http_req_duration{phase:flash}':    ['p(99)<8000'],
        'http_req_failed{phase:flash}':      ['rate<0.5'],
    },
}

// ---- 평상시 트래픽 ----
export function browse() {
    const r = Math.random()
    if (r < 0.45) {
        http.get(`${GW}/api/products`, { tags: { name: 'GET /api/products' } })
    } else if (r < 0.70) {
        const id = 1 + Math.floor(Math.random() * 5)
        http.get(`${GW}/api/products/${id}`, { tags: { name: 'GET /api/products/{id}' } })
    } else if (r < 0.85) {
        const tok = pickToken()
        http.get(`${GW}/api/users/me`, {
            headers: { Authorization: `Bearer ${tok}` },
            tags: { name: 'GET /api/users/me' },
        })
    } else if (r < 0.95) {
        const tok = pickToken()
        http.get(`${GW}/api/orders`, {
            headers: { Authorization: `Bearer ${tok}` },
            tags: { name: 'GET /api/orders' },
        })
    } else {
        const tok = pickToken()
        http.post(
            `${GW}/api/auth/login`,
            JSON.stringify({ username: tok.replace('TOKEN-', '').toLowerCase(), password: 'x' }),
            { headers: { 'Content-Type': 'application/json' }, tags: { name: 'POST /api/auth/login' } },
        )
    }
    sleep(Math.random() * 0.3)
}

// ---- 이벤트 시나리오 — 한 상품 사려고 몰림 ----
export function flashSale() {
    const tok = pickToken()
    http.post(
        `${GW}/api/orders`,
        JSON.stringify({ productId: FLASH_PRODUCT, quantity: 1 }),
        {
            headers: {
                Authorization: `Bearer ${tok}`,
                'Content-Type': 'application/json',
            },
            tags: { name: 'POST /api/orders (flash)' },
        },
    )
}
