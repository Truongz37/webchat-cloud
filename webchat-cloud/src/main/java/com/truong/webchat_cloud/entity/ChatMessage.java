package com.truong.webchat_cloud.entity;


@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "messages")
public class ChatMessage {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String sender;
    private String content;

    // Constructor trống bắt buộc phải có
    public ChatMessage() {
    }

    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    // Các hàm Getter và Setter (Alt + Insert để tạo tự động)
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
}