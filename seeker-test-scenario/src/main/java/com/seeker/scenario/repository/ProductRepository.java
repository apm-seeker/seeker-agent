package com.seeker.scenario.repository;

import com.seeker.scenario.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
