package com.truong.webchat_cloud.controller;

import com.truong.webchat_cloud.entity.ActivityLog;
import com.truong.webchat_cloud.entity.Room;
import com.truong.webchat_cloud.entity.User;
import com.truong.webchat_cloud.repository.ActivityLogRepository;
import com.truong.webchat_cloud.repository.RoomMemberRepository;
import com.truong.webchat_cloud.repository.RoomRepository;
import com.truong.webchat_cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate != null && endDate != null) {
            List<User> users = userRepository.findByCreatedAtBetween(startDate, endDate)
                    .stream().filter(u -> "ROLE_USER".equals(u.getRole())).collect(Collectors.toList());
            return ResponseEntity.ok(users);
        }
        List<User> users = userRepository.findAll()
                .stream().filter(u -> "ROLE_USER".equals(u.getRole())).collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Username already exists");
            return ResponseEntity.badRequest().body(response);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        activityLogRepository.save(new ActivityLog("Admin created user " + username, "ADMIN"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/lock")
    public ResponseEntity<?> toggleLockUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ("ROLE_ADMIN".equals(user.getRole())) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Cannot modify ADMIN status");
                return ResponseEntity.status(403).body(response);
            }
            boolean newLockStatus = !user.isLocked();
            user.setLocked(newLockStatus);
            userRepository.save(user);
            
            activityLogRepository.save(new ActivityLog("Admin " + (newLockStatus ? "locked" : "unlocked") + " user " + user.getUsername(), "ADMIN"));

            if (newLockStatus) {
                // Ép Frontend của user đó logout
                Map<String, Object> lockMsg = new HashMap<>();
                lockMsg.put("type", "LOCK");
                messagingTemplate.convertAndSend("/topic/user/" + user.getUsername(), lockMsg);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User lock status updated");
            response.put("isLocked", user.isLocked());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRooms() {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> roomStats = rooms.stream().map(room -> {
            long memberCount = roomMemberRepository.countByRoomId(room.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getId());
            map.put("name", room.getName());
            map.put("roomCode", room.getRoomCode());
            map.put("createdAt", room.getCreatedAt());
            map.put("memberCount", memberCount);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(roomStats);
    }

    @DeleteMapping("/rooms/{id}")
    @Transactional
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        Optional<Room> roomOpt = roomRepository.findById(id);
        if (roomOpt.isPresent()) {
            String roomName = roomOpt.get().getName();
            roomMemberRepository.deleteByRoomId(id);
            roomRepository.deleteById(id);
            
            activityLogRepository.save(new ActivityLog("Admin deleted room " + roomName, "ADMIN"));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Room deleted successfully");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/logs")
    public ResponseEntity<List<ActivityLog>> getLogs() {
        return ResponseEntity.ok(activityLogRepository.findAllByOrderByTimestampDesc());
    }
}
