package com.seeker.agent.core.context.propagation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * W3C Trace Context (https://www.w3.org/TR/trace-context/) 기반 propagator 구현체.
 *
 * <p>3개 헤더를 다룬다.
 * <ul>
 *   <li>{@code traceparent} : {@code 00-{traceId}-{spanId}-{flags}} 고정 포맷.
 *       이때 spanId 자리는 <em>호출자의 현재 spanId</em>이며, 수신자에게는 자기 입장의 parentSpanId가 된다.</li>
 *   <li>{@code tracestate}  : {@code seeker=k1:v1;k2:v2,vendor2=...} — 벤더 전용 키-값(예: {@code pAgentId})</li>
 *   <li>{@code baggage}     : {@code k1=v1,k2=v2} — 애플리케이션 컨텍스트</li>
 * </ul>
 *
 * <p>수신자는 {@code traceparent}의 spanId를 자기 parentSpanId로 사용하고, 자기 spanId는 로컬에서 새로 생성한다
 * — 이른바 W3C self-generation 모델. sender 측 pre-allocation 구조가 아님.
 *
 * <p>동작 원칙:
 * <ul>
 *   <li>실패해도 throw 하지 않는다 — 인터셉터의 예외는 비즈니스 로직을 깨뜨릴 수 있으므로 항상 안전 반환.</li>
 *   <li>스레드-세이프 — 모든 메서드가 인스턴스 상태를 변경하지 않음 (불변).</li>
 * </ul>
 */
public class W3CTraceContextPropagator implements TraceContextPropagator {

    // ===== W3C Trace Context 표준 헤더 이름 =====

    public static final String TRACEPARENT = "traceparent";
    public static final String TRACESTATE = "tracestate";
    public static final String BAGGAGE = "baggage";

    /**
     * tracestate 안에서 우리 벤더 영역을 식별하는 키.
     * 형태: {@code seeker=key:value;...} — 다른 벤더(예: {@code dd=...}, {@code nr=...})와 공존 가능.
     */
    public static final String VENDOR_KEY = "seeker";

    /**
     * traceparent 포맷 버전. 현재 표준은 {@code "00"}. 향후 표준이 바뀌면 새 버전 핸들링 필요.
     */
    private static final String VERSION = "00";

    /**
     * trace-flags 값.
     * <ul>
     *   <li>{@code "00"}: not sampled (수집되지 않음)</li>
     *   <li>{@code "01"}: sampled (수집됨)</li>
     * </ul>
     * 현재는 항상 sampled로 고정. 추후 샘플링 정책이 들어오면 이 값을 동적으로 설정해야 한다.
     */
    private static final String SAMPLED_FLAGS = "01";

    // ============================================================
    // inject — 현재 컨텍스트를 헤더로 직렬화
    // ============================================================

    @Override
    public <C> void inject(PropagationContext context, C carrier, HeaderSetter<C> setter) {
        // 방어적 null/empty 체크 — 호출부 실수로 NPE가 비즈니스 코드까지 전파되지 않도록.
        if (context == null || context.isEmpty() || carrier == null || setter == null) {
            return;
        }
        String traceId = context.getTraceId();
        if (traceId == null) {
            return;
        }

        // 1) traceparent: 항상 발신
        setter.setHeader(carrier, TRACEPARENT, formatTraceparent(traceId, context.getParentSpanId()));

        // 2) tracestate: 벤더 메타데이터가 있을 때만 발신 (빈 헤더 보내지 않음)
        String tracestate = formatTracestate(context.getTraceState());
        if (tracestate != null) {
            setter.setHeader(carrier, TRACESTATE, tracestate);
        }

        // 3) baggage: 애플리케이션 컨텍스트가 있을 때만 발신
        if (!context.getBaggage().isEmpty()) {
            setter.setHeader(carrier, BAGGAGE, formatBaggage(context.getBaggage()));
        }
    }

    // ============================================================
    // extract — 헤더에서 컨텍스트 복원
    // ============================================================

    @Override
    public <C> PropagationContext extract(C carrier, HeaderGetter<C> getter) {
        if (carrier == null || getter == null) {
            return PropagationContext.empty();
        }

        // 1) traceparent 필수 — 없으면 추적 컨텍스트 없음으로 판단
        String traceparent = getter.getHeader(carrier, TRACEPARENT);
        if (traceparent == null || traceparent.isEmpty()) {
            return PropagationContext.empty();
        }

        // 2) traceparent 파싱. 길이/숫자 검증 실패 시 empty 반환 (헤더 손상 방어)
        ParsedTraceparent parsed = parseTraceparent(traceparent);
        if (parsed == null) {
            return PropagationContext.empty();
        }

        // 3) 빌더에 traceId + parentSpanId 채움.
        //    wire의 spanId가 곧 수신자 입장의 parentSpanId.
        PropagationContext.Builder builder = PropagationContext.builder()
                .traceId(parsed.traceId)
                .parentSpanId(parsed.spanId);

        // 4) tracestate 파싱 (선택적) — seeker= 키 영역만 가져온다.
        Map<String, String> tracestateMap = parseTracestate(getter.getHeader(carrier, TRACESTATE));
        for (Map.Entry<String, String> e : tracestateMap.entrySet()) {
            builder.putTraceState(e.getKey(), e.getValue());
        }

        // 5) baggage 파싱 (선택적)
        Map<String, String> baggage = parseBaggage(getter.getHeader(carrier, BAGGAGE));
        for (Map.Entry<String, String> e : baggage.entrySet()) {
            builder.putBaggage(e.getKey(), e.getValue());
        }

        return builder.build();
    }

    // ============================================================
    // traceparent 직렬화 / 파싱
    // ============================================================

    /**
     * traceparent 헤더 값 문자열 생성.
     * 결과 예: {@code 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01}
     */
    private static String formatTraceparent(String traceId, long spanId) {
        return VERSION + "-"
                + normalizeTraceId(traceId) + "-"   // 32-char hex 보장
                + toSpanIdHex(spanId) + "-"         // long → 16-char hex
                + SAMPLED_FLAGS;
    }

    /**
     * traceparent 문자열을 (traceId, spanId)로 파싱.
     *
     * <p>표준 검증:
     * <ul>
     *   <li>4개 부분(version-traceId-spanId-flags)이 있어야 함</li>
     *   <li>traceId는 정확히 32-char hex</li>
     *   <li>spanId는 정확히 16-char hex</li>
     * </ul>
     *
     * @return 파싱된 결과. 형식 위반 시 {@code null} (호출자가 empty로 처리)
     */
    private static ParsedTraceparent parseTraceparent(String traceparent) {
        // 표준에 맞게 트림 후 split.
        String[] parts = traceparent.trim().split("-");
        if (parts.length < 4) {
            // version만 미래에 늘어나도 (parts >= 4) 인 경우는 받아들임. 너무 짧으면 손상으로 판단.
            return null;
        }
        String traceIdHex = parts[1];
        String spanIdHex = parts[2];
        // 길이 검증 — W3C 표준은 정확히 이 길이를 요구한다.
        if (traceIdHex.length() != 32 || spanIdHex.length() != 16) {
            return null;
        }
        try {
            // unsigned 파싱: hex가 0x80...이상이면 long의 부호 비트가 1로 되어
            // signed parseLong은 NumberFormatException을 던지기 때문.
            long spanId = Long.parseUnsignedLong(spanIdHex, 16);
            return new ParsedTraceparent(traceIdHex, spanId);
        } catch (NumberFormatException e) {
            // hex가 아닌 문자가 섞여 있는 경우 등.
            return null;
        }
    }

    /**
     * traceId를 32-char lowercase hex로 정규화.
     *
     * <p>주된 용도는 inject 직전 안전망. 본 에이전트의 {@code TraceId}는 항상 32-char hex를 생성하므로
     * 정상 경로에서는 입력이 이미 맞는 길이이다. 하지만 다음 케이스에서 안전망 역할:
     * <ul>
     *   <li>레거시 UUID 포맷이 외부에서 들어오는 경우 (구버전 에이전트 등)</li>
     *   <li>테스트에서 짧은 문자열을 임의로 넣는 경우</li>
     * </ul>
     *
     * <p>처리 규칙:
     * <ul>
     *   <li>32자 → 그대로</li>
     *   <li>32자보다 길면 → 뒤쪽 32자 사용</li>
     *   <li>32자보다 짧으면 → 앞에 0으로 패딩</li>
     *   <li>null → 모두 0인 32자</li>
     * </ul>
     */
    private static String normalizeTraceId(String traceId) {
        if (traceId == null) {
            return "00000000000000000000000000000000";
        }
        // UUID 포맷("xxxxxxxx-xxxx-...")의 하이픈 제거 + 소문자 통일.
        String hex = traceId.replace("-", "").toLowerCase();
        if (hex.length() == 32) {
            return hex;
        }
        if (hex.length() > 32) {
            // 더 긴 ID가 들어오면 뒷부분을 사용 (앞부분 손실보다 뒤가 보통 더 분산성이 좋음)
            return hex.substring(hex.length() - 32);
        }
        // 짧으면 leading zero 패딩.
        StringBuilder sb = new StringBuilder(32);
        for (int i = hex.length(); i < 32; i++) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }

    /**
     * long spanId를 16-char lowercase hex로 변환.
     * {@code %016x} : 16자리 폭, 부호 없는 hex, 부족하면 0 패딩.
     */
    private static String toSpanIdHex(long spanId) {
        return String.format("%016x", spanId);
    }

    // ============================================================
    // tracestate 직렬화 / 파싱
    // ============================================================

    /**
     * vendor 영역 키-값 맵을 tracestate 헤더 값으로 직렬화.
     *
     * <p>출력 예: {@code seeker=pAgentId:agent01;pAppName:order-service}
     *
     * <p>구분자 선택:
     * <ul>
     *   <li>엔트리(벤더 간) 구분: {@code ,} (W3C 표준)</li>
     *   <li>벤더 내부 키-값 페어 구분: {@code ;} (콤마는 표준 구분자라 사용 불가)</li>
     *   <li>키-값 사이: {@code :}</li>
     * </ul>
     *
     * @return tracestate 헤더 값. vendorEntries가 비어있으면 {@code null} (헤더 자체를 보내지 않음)
     */
    private static String formatTracestate(Map<String, String> vendorEntries) {
        if (vendorEntries == null || vendorEntries.isEmpty()) {
            return null;
        }
        StringBuilder vendorValue = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : vendorEntries.entrySet()) {
            if (!first) {
                vendorValue.append(';');  // 두 번째 이후 엔트리부터 구분자 추가
            }
            first = false;
            vendorValue.append(sanitizeKey(e.getKey()))
                    .append(':')
                    .append(sanitizeValue(e.getValue()));
        }
        // "seeker=" 접두로 우리 벤더 영역임을 명시.
        return VENDOR_KEY + "=" + vendorValue;
    }

    /**
     * tracestate 헤더 값을 파싱하여 우리 벤더 영역의 키-값 맵을 추출.
     *
     * <p>다른 벤더 영역(예: {@code dd=...}, {@code nr=...})은 무시한다 — 우리 코드가 해석할 의무 없음.
     * 다만 inject 시점에 패스스루 하려면 별도 보존이 필요할 수 있는데, 현재는 단순화를 위해 미구현.
     */
    private static Map<String, String> parseTracestate(String tracestate) {
        Map<String, String> result = new LinkedHashMap<>();
        if (tracestate == null || tracestate.isEmpty()) {
            return result;
        }
        // 콤마로 벤더 엔트리 분리. 예: "seeker=...,dd=...,vendor2=..."
        for (String entry : tracestate.split(",")) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;  // "=" 없거나 키가 비어있으면 손상 — 건너뜀
            }
            String key = entry.substring(0, eq).trim();
            String value = entry.substring(eq + 1).trim();
            // 우리 벤더 영역만 처리. 그 외는 무시.
            if (!VENDOR_KEY.equals(key)) {
                continue;
            }
            // 벤더 내부의 키-값 페어 분해. 세미콜론으로 split, 콜론으로 키-값 분리.
            for (String pair : value.split(";")) {
                int colon = pair.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                result.put(pair.substring(0, colon).trim(), pair.substring(colon + 1).trim());
            }
        }
        return result;
    }

    // ============================================================
    // baggage 직렬화 / 파싱
    // ============================================================

    /**
     * baggage 키-값 맵을 헤더 값으로 직렬화.
     * 출력 예: {@code userId=alice,tenantId=acme}
     *
     * <p>tracestate와 달리 baggage는 벤더 키 없이 평면적인 key=value 리스트.
     */
    private static String formatBaggage(Map<String, String> baggage) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : baggage.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(sanitizeKey(e.getKey())).append('=').append(sanitizeValue(e.getValue()));
        }
        return sb.toString();
    }

    /**
     * baggage 헤더를 파싱하여 키-값 맵으로 반환.
     */
    private static Map<String, String> parseBaggage(String baggage) {
        Map<String, String> result = new LinkedHashMap<>();
        if (baggage == null || baggage.isEmpty()) {
            return result;
        }
        for (String entry : baggage.split(",")) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            result.put(entry.substring(0, eq).trim(), entry.substring(eq + 1).trim());
        }
        return result;
    }

    // ============================================================
    // sanitization — 헤더 구분자 충돌 방지
    // ============================================================

    /**
     * 키에서 W3C 헤더의 구분자/공백을 {@code _}로 치환.
     * 키에 콤마/세미콜론/등호/콜론/공백이 들어가면 파싱이 깨지므로 미리 정리.
     */
    private static String sanitizeKey(String s) {
        return s.replaceAll("[,;=:\\s]", "_");
    }

    /**
     * 값에서 헤더 구분자(콤마/세미콜론/콜론)를 {@code _}로 치환.
     * 값은 키와 달리 등호/공백은 허용해도 무방하므로 더 적은 문자만 치환.
     */
    private static String sanitizeValue(String s) {
        return s.replaceAll("[,;:]", "_");
    }

    // ============================================================
    // 내부 자료구조
    // ============================================================

    /**
     * traceparent 파싱 결과를 묶기 위한 내부 값 객체.
     * 메서드 두 개의 반환값을 묶어 전달하는 용도 — 외부 노출 안 함.
     */
    private static final class ParsedTraceparent {
        /** 32-char hex traceId */
        final String traceId;
        /** wire의 spanId 자리 = 수신자에게는 parentSpanId */
        final long spanId;

        ParsedTraceparent(String traceId, long spanId) {
            this.traceId = traceId;
            this.spanId = spanId;
        }
    }
}
