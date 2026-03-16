package com.example.chat_application.controller;

//import com.chat.app.model.ChatMessage;
//import com.chat.app.model.MessageType;
import com.example.chat_application.Services.ChatMessageService;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private ChatMessageService messageService;

    /**
     * Handle chat messages — save to DB + broadcast
     */
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        String plain = chatMessage.getContent();  // keep for UI
        chatMessage.setType(MessageType.CHAT);

        messageService.saveMessage(chatMessage);  // encrypts before DB save

        chatMessage.setContent(plain);            // restore plaintext for broadcast
        return chatMessage;
    }

    /**
     * Handle file messages — broadcast file info
     */
    @MessageMapping("/chat.sendFile")
    @SendTo("/topic/public")
    public ChatMessage sendFileMessage(@Payload ChatMessage chatMessage) {
        String plainCaption = chatMessage.getContent(); // optional caption
        chatMessage.setType(MessageType.FILE);

        messageService.saveMessage(chatMessage);        // encrypts caption if present

        chatMessage.setContent(plainCaption);           // restore caption plaintext for broadcast
        return chatMessage;
    }

    /**
     * Handle user join
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {

        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("userEmail", chatMessage.getSenderEmail());

        String plain = chatMessage.getContent();   // might be null, fine
        chatMessage.setType(MessageType.JOIN);

        messageService.saveMessage(chatMessage);   // encrypts if content exists
        chatMessage.setContent(plain);

        return chatMessage;
    }

    /**
     * REST endpoint to load chat history
     * GET /api/messages/history
     */
    @GetMapping("/api/messages/history")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory() {
        return ResponseEntity.ok(messageService.getRecentMessages());
    }
}