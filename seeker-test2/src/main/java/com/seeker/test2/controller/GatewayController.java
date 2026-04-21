package com.seeker.test2.controller;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @GetMapping("/call")
    public String callBackend(@RequestParam(defaultValue = "SeekerRequest") String name) {
        String backendUrl = "http://localhost:8082/backend/work?name=" + name;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(backendUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity());
                return "Gateway received from Backend: " + result;
            }
        } catch (Exception e) {
            return "Gateway error calling Backend: " + e.getMessage();
        }
    }
}
