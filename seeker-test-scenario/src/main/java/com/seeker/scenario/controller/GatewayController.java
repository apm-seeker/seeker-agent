package com.seeker.scenario.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seeker.scenario.service.HttpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 운영 e-commerce 게이트웨이.
 *
 *   POST /api/auth/login         body { username, password }
 *   GET  /api/products           목록
 *   GET  /api/products/{id}      단건
 *   GET  /api/users/me           Header Authorization: Bearer <token>
 *   GET  /api/orders             Header Authorization: Bearer <token>
 *   POST /api/orders             Header Authorization: Bearer <token>  body { productId, quantity }
 *
 * 에러/지연은 downstream 의 Chaos 가 자동 확률로 발생 — 호출자는 일반 호출만 함.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GatewayController {

    private final HttpService http;
    private final ObjectMapper json;

    @Value("${downstream.auth}")
    private String authUrl;
    @Value("${downstream.order}")
    private String orderUrl;
    @Value("${downstream.product}")
    private String productUrl;

    // ===== auth =====

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return passThrough(() -> http.post(authUrl + "/auth/login",
                toJson(body), null));
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return passThrough(() -> {
            JsonNode userNode = verifyToken(auth);
            return userNode.toString();
        });
    }

    // ===== products =====

    @GetMapping("/products")
    public ResponseEntity<?> products() {
        return passThrough(() -> http.get(productUrl + "/product", null));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> product(@PathVariable Long id) {
        return passThrough(() -> http.get(productUrl + "/product/" + id, null));
    }

    // ===== orders =====

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@RequestHeader(value = "Authorization", required = false) String auth) {
        return passThrough(() -> {
            JsonNode userNode = verifyToken(auth);
            long userId = userNode.get("userId").asLong();
            return http.get(orderUrl + "/order/list?userId=" + userId, null);
        });
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body
    ) {
        return passThrough(() -> {
            JsonNode userNode = verifyToken(auth);
            long userId = userNode.get("userId").asLong();
            Object productId = body.get("productId");
            Object quantity = body.getOrDefault("quantity", 1);
            String payload = "{\"userId\":" + userId
                    + ",\"productId\":" + productId
                    + ",\"quantity\":" + quantity + "}";
            return http.post(orderUrl + "/order/create", payload, null);
        });
    }

    // ===== helpers =====

    private JsonNode verifyToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new HttpService.DownstreamException(401, "{\"error\":\"MISSING_TOKEN\"}");
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        String res = http.post(authUrl + "/auth/verify",
                "{\"token\":\"" + token + "\"}", null);
        try {
            return json.readTree(res);
        } catch (Exception e) {
            throw new HttpService.HttpServiceException(e);
        }
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new HttpService.HttpServiceException(e);
        }
    }

    /** downstream 응답을 그대로 전달. 4xx 는 4xx, 그 외 예외는 500. */
    private ResponseEntity<?> passThrough(SupplierWithThrows<String> action) {
        try {
            String body = action.get();
            return ResponseEntity.ok(body);
        } catch (HttpService.DownstreamException e) {
            return ResponseEntity.status(e.status).body(e.body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", String.valueOf(e.getMessage())
            ));
        }
    }

    @FunctionalInterface
    interface SupplierWithThrows<T> {
        T get() throws Exception;
    }
}
