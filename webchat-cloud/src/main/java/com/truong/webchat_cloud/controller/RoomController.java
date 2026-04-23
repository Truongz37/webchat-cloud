package com.truong.webchat_cloud.controller;

import com.truong.webchat_cloud.entity.ActivityLog;
import com.truong.webchat_cloud.entity.ChatMessage;
import com.truong.webchat_cloud.entity.Room;
import com.truong.webchat_cloud.entity.RoomMember;
import com.truong.webchat_cloud.entity.User;
import com.truong.webchat_cloud.repository.ActivityLogRepository;
import com.truong.webchat_cloud.repository.MessageRepository;
import com.truong.webchat_cloud.repository.RoomMemberRepository;
import com.truong.webchat_cloud.repository.RoomRepository;
import com.truong.webchat_cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    private void sendSystemMessage(Long roomId, String content) {
        ChatMessage msg = new ChatMessage("Hệ thống", content);
        msg.setRoomId(roomId);
        msg.setMessageType("SYSTEM");
        messageRepository.save(msg);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, msg);
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestParam String name, @RequestParam(required = false) String password) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Room room = new Room();
        room.setName(name);
        room.setPassword(password);
        room.setRoomCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        room = roomRepository.save(room);

        RoomMember member = new RoomMember(currentUser, room, "LEADER");
        roomMemberRepository.save(member);

        activityLogRepository.save(new ActivityLog(currentUser.getUsername() + " created room " + room.getRoomCode(), currentUser.getUsername()));

        return ResponseEntity.ok(room);
    }

    @PostMapping("/join-code")
    public ResponseEntity<?> joinRoomByCode(@RequestParam String roomCode, @RequestParam(required = false) String password) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Room> roomOpt = roomRepository.findByRoomCode(roomCode);
        if (roomOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mã phòng không tồn tại");
            return ResponseEntity.badRequest().body(response);
        }

        Room room = roomOpt.get();

        if (room.getPassword() != null && !room.getPassword().isEmpty() && !room.getPassword().equals(password)) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mật khẩu phòng không đúng");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomAndUser(room, currentUser);
        if (existingMember.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bạn đã tham gia phòng này rồi");
            return ResponseEntity.badRequest().body(response);
        }

        RoomMember member = new RoomMember(currentUser, room, "MEMBER");
        roomMemberRepository.save(member);

        activityLogRepository.save(new ActivityLog(currentUser.getUsername() + " joined room " + room.getRoomCode(), currentUser.getUsername()));
        sendSystemMessage(room.getId(), currentUser.getDisplayName() + " đã tham gia phòng.");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tham gia thành công");
        response.put("roomId", room.getId());
        return ResponseEntity.ok(response);
    }

    private void handleMemberLeave(Room room, RoomMember leavingMember) {
        roomMemberRepository.delete(leavingMember);
        List<RoomMember> remainingMembers = roomMemberRepository.findByRoomId(room.getId());
        
        if (remainingMembers.isEmpty()) {
            roomRepository.delete(room);
        } else if ("LEADER".equals(leavingMember.getRole())) {
            // Find oldest member by ID ascending
            RoomMember nextLeader = remainingMembers.stream()
                .min((m1, m2) -> m1.getId().compareTo(m2.getId()))
                .orElse(remainingMembers.get(0));
            nextLeader.setRole("LEADER");
            roomMemberRepository.save(nextLeader);
            sendSystemMessage(room.getId(), nextLeader.getUser().getDisplayName() + " đã trở thành Trưởng nhóm mới.");
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Room> roomOpt = roomRepository.findById(id);
        if (roomOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<RoomMember> existingMember = roomMemberRepository.findByRoomAndUser(roomOpt.get(), currentUser);
        if (existingMember.isPresent()) {
            Room room = roomOpt.get();
            sendSystemMessage(room.getId(), currentUser.getDisplayName() + " đã rời phòng.");
            handleMemberLeave(room, existingMember.get());
            activityLogRepository.save(new ActivityLog(currentUser.getUsername() + " left room " + room.getRoomCode(), currentUser.getUsername()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Rời phòng thành công");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Không phải thành viên của phòng");
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{roomId}/kick/{userId}")
    public ResponseEntity<?> kickMember(@PathVariable Long roomId, @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) return ResponseEntity.notFound().build();

        boolean isAdmin = currentUser.getRole().equals("ROLE_ADMIN");
        boolean isLeader = false;

        Optional<RoomMember> currentMemberOpt = roomMemberRepository.findByRoomAndUser(roomOpt.get(), currentUser);
        if (currentMemberOpt.isPresent() && "LEADER".equals(currentMemberOpt.get().getRole())) {
            isLeader = true;
        }

        if (!isAdmin && !isLeader) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Chỉ Trưởng nhóm mới có quyền này");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Optional<User> targetUserOpt = userRepository.findById(userId);
        if (targetUserOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<RoomMember> targetMemberOpt = roomMemberRepository.findByRoomAndUser(roomOpt.get(), targetUserOpt.get());
        if (targetMemberOpt.isPresent()) {
            if ("LEADER".equals(targetMemberOpt.get().getRole()) && !isAdmin) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Không thể Kick Trưởng nhóm");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            Room room = roomOpt.get();
            User targetUser = targetUserOpt.get();
            
            handleMemberLeave(room, targetMemberOpt.get());
            sendSystemMessage(room.getId(), targetUser.getDisplayName() + " đã bị mời ra khỏi phòng.");
            activityLogRepository.save(new ActivityLog(currentUser.getUsername() + " kicked " + targetUser.getUsername() + " from room " + room.getRoomCode(), currentUser.getUsername()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Kick thành công");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Người dùng không có trong phòng");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyRooms() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<RoomMember> memberships = roomMemberRepository.findByUser(currentUser);
        List<Map<String, Object>> rooms = memberships.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getRoom().getId());
            map.put("name", m.getRoom().getName());
            map.put("roomCode", m.getRoom().getRoomCode());
            map.put("role", m.getRole());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getRoomMembers(@PathVariable Long id) {
        List<RoomMember> members = roomMemberRepository.findByRoomId(id);
        List<Map<String, Object>> memberInfo = members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getUser().getId());
            map.put("username", m.getUser().getUsername());
            map.put("displayName", m.getUser().getDisplayName());
            map.put("role", m.getRole());
            map.put("isOnline", m.getUser().isOnline());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(memberInfo);
    }
}
