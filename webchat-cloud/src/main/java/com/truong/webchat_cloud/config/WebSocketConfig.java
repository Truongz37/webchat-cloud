package com.truong.webchat_cloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Mở cổng /ws cho client (HTML) kết nối vào
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Tiền tố cho các tin nhắn từ server gửi về client
        registry.enableSimpleBroker("/topic");
        // Tiền tố cho các tin nhắn từ client gửi lên server
        registry.setApplicationDestinationPrefixes("/app");
    }
}