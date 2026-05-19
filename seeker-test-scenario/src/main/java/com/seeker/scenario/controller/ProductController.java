package com.seeker.scenario.controller;

import com.seeker.scenario.entity.Product;
import com.seeker.scenario.repository.ProductRepository;
import com.seeker.scenario.service.Chaos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository products;
    private final Chaos chaos;

    @GetMapping
    public List<Product> list() {
        chaos.maybeSlow(0.01, 80, 200); // 1% slow
        return products.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        chaos.maybeSlow(0.01, 50, 150);
        return products.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND")));
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody Map<String, Object> body) {
        chaos.maybeSlow(0.01, 80, 200);
        Long productId = ((Number) body.get("productId")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();

        Product p = products.findById(productId)
                .orElseThrow(() -> new RuntimeException("PRODUCT_NOT_FOUND: " + productId));

        if (p.getStock() < quantity) {
            return ResponseEntity.status(400).body(Map.of(
                    "error", "OUT_OF_STOCK",
                    "productId", productId,
                    "available", p.getStock()
            ));
        }

        // 재고는 의도적으로 차감하지 않음 (load 가 빨리 소진하지 않게).
        long amount = p.getPrice() * quantity;
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "name", p.getName(),
                "amount", amount
        ));
    }
}
