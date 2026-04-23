package com.truong.webchat_cloud.repository;

import com.truong.webchat_cloud.entity.RoomMember;
import com.truong.webchat_cloud.entity.Room;
import com.truong.webchat_cloud.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomId(Long roomId);
    List<RoomMember> findByUser(User user);
    Optional<RoomMember> findByRoomAndUser(Room room, User user);
    long countByRoomId(Long roomId);
    void deleteByRoomId(Long roomId);
}
