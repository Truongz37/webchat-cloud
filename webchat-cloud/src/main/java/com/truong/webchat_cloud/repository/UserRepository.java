package com.truong.webchat_cloud.repository;
import com.truong.webchat_cloud.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}