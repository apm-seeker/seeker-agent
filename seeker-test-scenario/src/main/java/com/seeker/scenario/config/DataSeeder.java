package com.seeker.scenario.config;

import com.seeker.scenario.entity.Product;
import com.seeker.scenario.entity.UserAccount;
import com.seeker.scenario.repository.ProductRepository;
import com.seeker.scenario.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public ApplicationRunner seed(UserRepository users, ProductRepository products) {
        return args -> {
            if (users.count() == 0) {
                users.save(new UserAccount(null, "alice",  "TOKEN-ALICE",  100_000L));
                users.save(new UserAccount(null, "bob",    "TOKEN-BOB",    50_000L));
                users.save(new UserAccount(null, "carol",  "TOKEN-CAROL",  10_000L));
            }
            if (products.count() == 0) {
                products.save(new Product(null, "노트북",    1_500_000L, 1_000_000));
                products.save(new Product(null, "키보드",       80_000L, 1_000_000));
                products.save(new Product(null, "마우스",       35_000L, 1_000_000));
                products.save(new Product(null, "모니터",      450_000L, 1_000_000));
                products.save(new Product(null, "헤드셋",      120_000L, 0));
            }
        };
    }
}
