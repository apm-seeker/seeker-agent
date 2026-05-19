package com.seeker.scenario.repository;

import com.seeker.scenario.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    List<OrderRecord> findTop20ByUserIdOrderByIdDesc(Long userId);
}
