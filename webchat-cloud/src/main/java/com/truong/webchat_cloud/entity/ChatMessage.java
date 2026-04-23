package com.truong.webchat_cloud.entity;

import java.time.LocalDateTime;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "messages")
public class ChatMessage {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String sender;
    private String content;

    private Long roomId;
    private String messageType; // TEXT, IMAGE, FILE, SYSTEM
    private String fileUrl;

    private LocalDateTime timestamp;

    // Constructor trống bắt buộc phải có
    public ChatMessage() {
    }

    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.messageType = "TEXT";
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    // Các hàm Getter và Setter (Alt + Insert để tạo tự động)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}