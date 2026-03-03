package com.seeker.test.controller;

import com.seeker.test.entity.User;
import com.seeker.test.service.NestedService;
import com.seeker.test.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserService userService;

    @Autowired
    private NestedService nestedService;

    @GetMapping("/hello")
    public String hello(@RequestParam(name = "name", defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/db")
    public List<User> db() {
        return userService.saveAndFindAll("Seeker-JPA-User");
    }

    @GetMapping("/http")
    public String http() throws Exception {
        return userService.callExternalHttp();
    }

    @GetMapping("/nested")
    public String nested(@RequestParam(name = "name", defaultValue = "Seeker") String name) {
        return nestedService.startNestedFlow(name);
    }
}
