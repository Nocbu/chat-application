package com.example.chat_application.controller;

import com.example.chat_application.Services.DirectMessageService;
import com.example.chat_application.model.ChatMessage;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages/direct")
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public DirectMessageController(DirectMessageService directMessageService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.directMessageService = directMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    // REST: history
    @GetMapping("/{conversationId}")
    public ResponseEntity<?> history(@PathVariable String conversationId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not logged in"));

        List<ChatMessage> msgs = directMessageService.getDirectHistory(conversationId, username);
        return ResponseEntity.ok(msgs);
    }

    // REST: delete for me
    @DeleteMapping("/{messageId}/me")
    public ResponseEntity<?> deleteForMe(@PathVariable String messageId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not logged in"));

        ChatMessage updated = directMessageService.deleteForMe(messageId, username);

        // Notify conversation subscribers (so UI updates)
        messagingTemplate.convertAndSend("/topic/direct/" + updated.getConversationId(), Map.of(
                "type", "UPDATE",
                "action", "DELETED_FOR_ME",
                "messageId", updated.getId(),
                "username", username
        ));

        return ResponseEntity.ok(Map.of("success", true));
    }

    // REST: delete for everyone (sender only)
    @DeleteMapping("/{messageId}/everyone")
    public ResponseEntity<?> deleteForEveryone(@PathVariable String messageId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not logged in"));

        ChatMessage updated = directMessageService.deleteForEveryone(messageId, username);

        messagingTemplate.convertAndSend("/topic/direct/" + updated.getConversationId(), Map.of(
                "type", "UPDATE",
                "action", "DELETED_FOR_EVERYONE",
                "messageId", updated.getId()
        ));

        return ResponseEntity.ok(Map.of("success", true));
    }

    // WS: send direct message
    @MessageMapping("/direct.sendMessage")
    public void wsSendDirectMessage(@Payload ChatMessage msg, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return;

        String conversationId = msg.getConversationId();
        ChatMessage saved = directMessageService.saveDirectMessage(conversationId, msg, username);

        messagingTemplate.convertAndSend("/topic/direct/" + conversationId, saved);
    }

    // WS: send direct file message (metadata already uploaded via REST /api/files/upload)
    @MessageMapping("/direct.sendFile")
    public void wsSendDirectFile(@Payload ChatMessage msg, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return;

        String conversationId = msg.getConversationId();
        // Ensure type FILE
        msg.setType(com.example.chat_application.model.MessageType.FILE);

        ChatMessage saved = directMessageService.saveDirectMessage(conversationId, msg, username);
        messagingTemplate.convertAndSend("/topic/direct/" + conversationId, saved);
    }
}