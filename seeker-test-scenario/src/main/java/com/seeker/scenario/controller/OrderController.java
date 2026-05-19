package com.seeker.scenario.controller;

import com.seeker.scenario.entity.OrderRecord;
import com.seeker.scenario.repository.OrderRepository;
import com.seeker.scenario.service.Chaos;
import com.seeker.scenario.service.HttpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orders;
    private final HttpService http;
    private final Chaos chaos;

    @Value("${downstream.product}")
    private String productUrl;
    @Value("${downstream.payment}")
    private String paymentUrl;

    @GetMapping("/list")
    public List<OrderRecord> list(@RequestParam Long userId) {
        chaos.maybeSlow(0.01, 50, 150);
        return orders.findTop20ByUserIdOrderByIdDesc(userId);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long productId = ((Number) body.get("productId")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();

        // 0.3% 확률 — 의도적 DB 무결성 위반 (NOT NULL 위반)
        if (ThreadLocalRandom.current().nextDouble() < 0.003) {
            OrderRecord bad = new OrderRecord();
            orders.save(bad); // DataIntegrityViolationException
        }

        String prodRes = http.post(productUrl + "/product/reserve",
                "{\"productId\":" + productId + ",\"quantity\":" + quantity + "}",
                null);

        long totalAmount = extractAmount(prodRes);
        OrderRecord order = new OrderRecord(
                null, userId, productId, quantity, totalAmount, "CREATED", LocalDateTime.now()
        );
        orders.save(order);

        String payRes = http.post(paymentUrl + "/payment/charge",
                "{\"orderId\":" + order.getId() + ",\"amount\":" + totalAmount + "}",
                null);

        order.setStatus("PAID");
        orders.save(order);

        return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "totalAmount", totalAmount,
                "payment", payRes
        ));
    }

    private long extractAmount(String json) {
        int i = json.indexOf("\"amount\":");
        if (i < 0) return 0L;
        int start = i + 9;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }
}
