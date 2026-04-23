package com.truong.webchat_cloud.controller;

import com.truong.webchat_cloud.entity.ChatMessage;
import com.truong.webchat_cloud.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ChatController {
    @Autowired
    private MessageRepository messageRepository;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        return messageRepository.save(chatMessage); // Lưu vào DB rồi mới phát đi
    }
    @GetMapping("/api/messages")
    @ResponseBody
    public List<ChatMessage> getChatHistory() {
        return messageRepository.findAll(); // Lấy toàn bộ tin nhắn từ DB
    }
}