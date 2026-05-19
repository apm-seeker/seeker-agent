package com.seeker.scenario.controller;

import com.seeker.scenario.service.Chaos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pg")
@RequiredArgsConstructor
public class PgMockController {

    private final Chaos chaos;

    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestBody Map<String, Object> body) {
        chaos.maybeTimeout(0.01, 3_000); // 1% — 호출자(2초 sockTO) timeout 유발
        chaos.maybeNpe(0.005, "pg-pay");  // 0.5% NPE
        return ResponseEntity.ok(Map.of(
                "txnId", "PG-" + UUID.randomUUID().toString().substring(0, 8),
                "amount", body.get("amount")
        ));
    }
}
