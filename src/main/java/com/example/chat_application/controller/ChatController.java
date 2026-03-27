package com.example.chat_application.controller;

//import com.chat.app.model.ChatMessage;
//import com.chat.app.model.MessageType;
import com.example.chat_application.Services.ChatMessageService;
import com.example.chat_application.Services.PresenceService;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ChatController {

    @Autowired
    private ChatMessageService messageService;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    //message save to db
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        String plain = chatMessage.getContent();
        chatMessage.setType(MessageType.CHAT);

        messageService.saveMessage(chatMessage);

        chatMessage.setContent(plain);
        return chatMessage;
    }

    //files
    @MessageMapping("/chat.sendFile")
    @SendTo("/topic/public")
    public ChatMessage sendFileMessage(@Payload ChatMessage chatMessage) {
        String plainCaption = chatMessage.getContent();
        chatMessage.setType(MessageType.FILE);

        messageService.saveMessage(chatMessage);

        chatMessage.setContent(plainCaption);
        return chatMessage;
    }

    //join message (BROADCAST ONLY - DO NOT SAVE)
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("userEmail", chatMessage.getSenderEmail());

        // Store actual username (lowercase) for presence tracking
        String senderUsername = chatMessage.getSenderUsername();
        if (senderUsername != null && !senderUsername.isBlank()) {
            String normalised = senderUsername.trim().toLowerCase();
            headerAccessor.getSessionAttributes().put("senderUsername", normalised);
            presenceService.setOnline(normalised);
            // Broadcast presence: online
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "status", "ONLINE",
                    "username", normalised
            ));
        }

        // keep broadcasting JOIN, but don't persist it
        chatMessage.setType(MessageType.JOIN);
        return chatMessage;
    }

    //history
    @GetMapping("/api/messages/history")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory() {
        return ResponseEntity.ok(messageService.getRecentMessages());
    }

    // Online users (for presence on page load)
    @GetMapping("/api/users/online")
    @ResponseBody
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }
}