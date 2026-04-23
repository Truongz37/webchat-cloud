package com.truong.webchat_cloud.controller;

import com.truong.webchat_cloud.entity.ChatMessage;
import com.truong.webchat_cloud.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ChatController {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.truong.webchat_cloud.repository.UserRepository userRepository;

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload ChatMessage chatMessage) {
        // Kiểm tra user có bị khóa không
        java.util.Optional<com.truong.webchat_cloud.entity.User> userOpt = userRepository.findByUsername(chatMessage.getSender());
        if (userOpt.isPresent() && userOpt.get().isLocked()) {
            return; // Từ chối gửi
        }

        chatMessage.setRoomId(roomId);
        ChatMessage savedMessage = messageRepository.save(chatMessage); // Lưu vào DB
        
        // Gửi tin nhắn đến các client đăng ký vào phòng cụ thể
        messagingTemplate.convertAndSend("/topic/room/" + roomId, savedMessage);
    }
    
    @GetMapping("/api/messages/{roomId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable Long roomId) {
        return messageRepository.findByRoomIdOrderByIdAsc(roomId); // Lấy tin nhắn theo phòng
    }
}