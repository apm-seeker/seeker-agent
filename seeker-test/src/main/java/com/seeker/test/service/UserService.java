package com.seeker.test.service;

import com.seeker.test.entity.User;
import com.seeker.test.repository.UserRepository;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public List<User> saveAndFindAll(String name) {
        // JPA 테스트 (JdbcPlugin)
        userRepository.save(new User(name));
        return userRepository.findAll();
    }

    public String callExternalHttp() throws Exception {
        // HttpClient 테스트 (HttpClientPlugin)
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://example.com");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return "HTTP Response Status: " + response.getStatusLine();
            }
        }
    }

    public String callTest2() throws Exception {
        // seeker-test2 호출 테스트
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("http://localhost:8889/backend/work?name=From-Seeker-Test");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return "Response from seeker-test2: " + response.getStatusLine();
            }
        }
    }
}
