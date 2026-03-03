package com.seeker.test.service;

import com.seeker.test.entity.User;
import com.seeker.test.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NestedService {

    private final UserRepository userRepository;

    public NestedService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public String startNestedFlow(String name) {
        System.out.println("[App] NestedFlow 시작: " + name);
        return secondStep(name);
    }

    private String secondStep(String name) {
        System.out.println("[App] NestedFlow 2단계 진입");
        return thirdStep(name);
    }

    private String thirdStep(String name) {
        System.out.println("[App] NestedFlow 3단계 진입 (DB 작업 포함)");
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        userRepository.save(user);
        return "Success: " + name;
    }
}
