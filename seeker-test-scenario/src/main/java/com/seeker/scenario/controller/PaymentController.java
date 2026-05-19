package com.seeker.scenario.controller;

import com.seeker.scenario.entity.Payment;
import com.seeker.scenario.repository.PaymentRepository;
import com.seeker.scenario.service.Chaos;
import com.seeker.scenario.service.HttpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository payments;
    private final HttpService http;
    private final Chaos chaos;

    @Value("${downstream.pgmock}")
    private String pgUrl;

    @PostMapping("/charge")
    public ResponseEntity<?> charge(@RequestBody Map<String, Object> body) {
        Long orderId = ((Number) body.get("orderId")).longValue();
        Long amount = ((Number) body.get("amount")).longValue();

        chaos.maybeSlow(0.02, 400, 1200);    // 2% P95 spike
        chaos.maybeNpe(0.015, "payment-charge"); // 1.5% NPE

        String pgRes = http.post(pgUrl + "/pg/pay",
                "{\"amount\":" + amount + "}",
                null);

        Payment p = new Payment(
                null, orderId, amount, "APPROVED", extractTxnId(pgRes), LocalDateTime.now()
        );
        payments.save(p);

        return ResponseEntity.ok(Map.of(
                "paymentId", p.getId(),
                "status", "APPROVED",
                "pg", pgRes
        ));
    }

    private String extractTxnId(String json) {
        int i = json.indexOf("\"txnId\":\"");
        if (i < 0) return null;
        int start = i + 9;
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }
}
