package com.seeker.test.service;

import com.seeker.test.entity.User;
import com.seeker.test.repository.UserRepository;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
        secondStep(name);
        return "Success: " + name;
    }

    private void secondStep(String name) {
        System.out.println("[App] NestedFlow 2단계 진입 - 외부 HTTP 요청 시도");

        // HttpClient를 이용한 자기 자신 호출 (전파 테스트)
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:8080/hello?name=" + name);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity());
                System.out.println("[App] HTTP 요청 결과: " + result);
            }
        } catch (Exception e) {
            System.err.println("[App] HTTP 요청 실패: " + e.getMessage());
        }

        thirdStep(name);
    }

    private void thirdStep(String name) {
        System.out.println("[App] NestedFlow 3단계 진입 (DB 작업 포함)");
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        userRepository.save(user);

        if ("error".equals(name)) {
            throw new RuntimeException("Test exception for Seeker verification");
        }
    }
}
