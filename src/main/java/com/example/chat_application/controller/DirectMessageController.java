package com.example.chat_application.controller;

import com.example.chat_application.Repositories.ConversationRepository;
import com.example.chat_application.Services.DirectMessageService;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages/direct")
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;

    public DirectMessageController(DirectMessageService directMessageService,
                                   SimpMessagingTemplate messagingTemplate,
                                   ConversationRepository conversationRepository) {
        this.directMessageService = directMessageService;
        this.messagingTemplate = messagingTemplate;
        this.conversationRepository = conversationRepository;
    }

    // REST: history
    @GetMapping("/{conversationId}")
    public ResponseEntity<?> history(@PathVariable String conversationId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not logged in"));

        List<ChatMessage> msgs = directMessageService.getDirectHistory(conversationId, username);
        return ResponseEntity.ok(msgs);
    }

    // REST: mark conversation as read
    @PostMapping("/{conversationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String conversationId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not logged in"));

        List<String> readMessageIds = directMessageService.markConversationAsRead(conversationId, username);

        if (!readMessageIds.isEmpty()) {
            // Notify the other user's client so they can upgrade ✓ to ✓✓
            messagingTemplate.convertAndSend("/topic/direct/" + conversationId, Map.of(
                    "type", "UPDATE",
                    "action", "READ_RECEIPT",
                    "readerUsername", username,
                    "messageIds", readMessageIds
            ));
        }

        return ResponseEntity.ok(Map.of("success", true));
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
    public void wsSendDirectMessage(@Payload ChatMessage msg) {
        String senderUsername = msg.getSenderUsername();
        if (senderUsername == null || senderUsername.isBlank()) return;

        String conversationId = msg.getConversationId();
        if (conversationId == null || conversationId.isBlank()) return;

        ChatMessage saved = directMessageService.saveDirectMessage(conversationId, msg, senderUsername.trim().toLowerCase());
        messagingTemplate.convertAndSend("/topic/direct/" + conversationId, saved);
        notifyConversationUpdate(saved);
    }

    // WS: send direct file message (metadata already uploaded via REST /api/files/upload)
    @MessageMapping("/direct.sendFile")
    public void wsSendDirectFile(@Payload ChatMessage msg) {
        String senderUsername = msg.getSenderUsername();
        if (senderUsername == null || senderUsername.isBlank()) return;

        String conversationId = msg.getConversationId();
        if (conversationId == null || conversationId.isBlank()) return;

        msg.setType(MessageType.FILE);

        ChatMessage saved = directMessageService.saveDirectMessage(conversationId, msg, senderUsername.trim().toLowerCase());
        messagingTemplate.convertAndSend("/topic/direct/" + conversationId, saved);
        notifyConversationUpdate(saved);
    }

    /**
     * Sends a CONV_UPDATE notification to each conversation member's personal topic
     * (/topic/user/{username}) so their sidebar can update live without a page refresh.
     */
    private void notifyConversationUpdate(ChatMessage saved) {
        String conversationId = saved.getConversationId();
        if (conversationId == null) return;

        // Build the last-message preview (plain-text, already decrypted in saved)
        String preview;
        if (saved.getType() == MessageType.FILE) {
            preview = "📎 " + (saved.getFileName() != null ? saved.getFileName() : "File");
        } else {
            String content = saved.getContent() != null ? saved.getContent() : "";
            preview = content.length() > 40 ? content.substring(0, 40) + "…" : content;
        }

        String senderUsername = saved.getSenderUsername() != null ? saved.getSenderUsername() : "";
        String senderDisplayName = saved.getSender() != null ? saved.getSender() : senderUsername;
        String timestamp = saved.getTimestamp() != null ? saved.getTimestamp().toString() : null;

        conversationRepository.findById(conversationId).ifPresent(conv -> {
            if (conv.getMemberUsernames() == null) return;

            String otherMember = conv.getMemberUsernames().stream()
                    .filter(m -> !m.equalsIgnoreCase(senderUsername))
                    .findFirst().orElse("");

            // Direct conversations are always 1:1 pairs; iterating both members ensures
            // both get a personalised CONV_UPDATE with the correct "other" info.
            for (String member : conv.getMemberUsernames()) {
                boolean memberIsSender = member.equalsIgnoreCase(senderUsername);
                // From this member's point of view, the "other" person is the opposite side
                String notifyOtherUsername = memberIsSender ? otherMember : senderUsername;
                String notifyOtherDisplayName = memberIsSender ? otherMember : senderDisplayName;

                Map<String, Object> update = new HashMap<>();
                update.put("type", "CONV_UPDATE");
                update.put("conversationId", conversationId);
                update.put("senderUsername", senderUsername);
                update.put("otherUsername", notifyOtherUsername);
                update.put("otherDisplayName", notifyOtherDisplayName);
                update.put("lastPreview", preview);
                update.put("lastTimestamp", timestamp);
                update.put("messageType", saved.getType() != null ? saved.getType().name() : "CHAT");

                messagingTemplate.convertAndSend("/topic/user/" + member, update);
            }
        });
    }
}