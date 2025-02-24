package com.example.AdvancedAnalysisCW;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsById(Long id);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
}
