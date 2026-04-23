package com.truong.webchat_cloud.repository;
import com.truong.webchat_cloud.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
}