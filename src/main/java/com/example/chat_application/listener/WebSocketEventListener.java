package com.example.chat_application.listener;

import com.example.chat_application.Services.ChatMessageService;
import com.example.chat_application.Services.PresenceService;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private ChatMessageService messageService; // can be removed if you want, but not required

    @Autowired
    private PresenceService presenceService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String senderUsername = (String) headerAccessor.getSessionAttributes().get("senderUsername");

        if (username != null) {
            logger.info("User disconnected: {}", username);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(MessageType.LEAVE);
            chatMessage.setSender(username);
            chatMessage.setContent(username + " left the chat!");

            // public chat
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        }

        // presence
        if (senderUsername != null && !senderUsername.isBlank()) {
            presenceService.setOffline(senderUsername);
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "status", "OFFLINE",
                    "username", senderUsername.toLowerCase()
            ));
        }
    }
}