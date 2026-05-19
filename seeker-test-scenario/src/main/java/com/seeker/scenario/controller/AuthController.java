package com.seeker.scenario.controller;

import com.seeker.scenario.entity.UserAccount;
import com.seeker.scenario.repository.UserRepository;
import com.seeker.scenario.service.Chaos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository users;
    private final Chaos chaos;

    /** username → token + userId */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        chaos.maybeSlow(0.01, 150, 400); // 1% slow (login DB lookup)
        String username = body.get("username");
        UserAccount user = users.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_CREDENTIALS"));
        }
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "token", user.getToken()
        ));
    }

    /** token → userId (세션 검증) */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        chaos.maybeSlow(0.005, 100, 250); // 0.5% slow
        String token = body.get("token");
        UserAccount user = users.findByToken(token).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_TOKEN"));
        }
        return ResponseEntity.ok(Map.of("userId", user.getId(), "username", user.getUsername()));
    }
}
