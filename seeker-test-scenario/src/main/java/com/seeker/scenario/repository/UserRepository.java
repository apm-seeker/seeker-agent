package com.seeker.scenario.repository;

import com.seeker.scenario.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByToken(String token);
    Optional<UserAccount> findByUsername(String username);
}
