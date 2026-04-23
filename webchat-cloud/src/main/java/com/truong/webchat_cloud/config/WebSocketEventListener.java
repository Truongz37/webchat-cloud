package com.truong.webchat_cloud.config;

import com.truong.webchat_cloud.entity.ChatMessage;
import com.truong.webchat_cloud.entity.RoomMember;
import com.truong.webchat_cloud.entity.User;
import com.truong.webchat_cloud.repository.RoomMemberRepository;
import com.truong.webchat_cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Optional;

@Component
public class WebSocketEventListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = null;
        if (headerAccessor.getUser() != null) {
            username = headerAccessor.getUser().getName();
        }

        if (username != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setOnline(true);
                userRepository.save(user);

                broadcastStatusChange(user, "ONLINE");
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = null;
        if (headerAccessor.getUser() != null) {
            username = headerAccessor.getUser().getName();
        }

        if (username != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setOnline(false);
                userRepository.save(user);

                broadcastStatusChange(user, "OFFLINE");
            }
        }
    }

    private void broadcastStatusChange(User user, String status) {
        // Tìm tất cả các phòng mà user này đang tham gia
        List<RoomMember> memberships = roomMemberRepository.findByUser(user);
        for (RoomMember membership : memberships) {
            Long roomId = membership.getRoom().getId();
            
            ChatMessage msg = new ChatMessage("Hệ thống", "STATUS_" + status + ":" + user.getUsername());
            msg.setRoomId(roomId);
            msg.setMessageType("SYSTEM");
            
            messagingTemplate.convertAndSend("/topic/room/" + roomId, msg);
        }
    }
}
