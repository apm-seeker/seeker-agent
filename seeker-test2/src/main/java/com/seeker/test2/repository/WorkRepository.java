package com.seeker.test2.repository;

import com.seeker.test2.entity.WorkRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkRepository extends JpaRepository<WorkRecord, Long> {
}
