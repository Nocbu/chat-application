package com.example.chat_application.controller;

import com.example.chat_application.DTO.AdminActionRequest;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import com.example.chat_application.model.User;
import com.example.chat_application.Services.ChatMessageService;
import com.example.chat_application.Services.FileStorageService;
import com.example.chat_application.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ChatMessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @PostMapping("/action")
    public ResponseEntity<Map<String, Object>> performAction(
            @RequestBody AdminActionRequest request,
            @RequestHeader("X-Admin-Username") String username,
            @RequestHeader("X-Admin-Password") String password) {

        Map<String, Object> response = new HashMap<>();

        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            response.put("success", false);
            response.put("message", "Unauthorized. Invalid admin credentials.");
            return ResponseEntity.status(403).body(response);
        }

        switch (request.getAction().toUpperCase()) {

            case "DELETE_MESSAGE":
                boolean deleted = messageService.deleteMessage(request.getMessageId());
                if (deleted) {
                    ChatMessage systemMsg = new ChatMessage();
                    systemMsg.setType(MessageType.SYSTEM);
                    systemMsg.setSender("Admin");
                    systemMsg.setContent("MESSAGE_DELETED:" + request.getMessageId());
                    messagingTemplate.convertAndSend("/topic/public", systemMsg);

                    response.put("success", true);
                    response.put("message", "Message deleted");
                } else {
                    response.put("success", false);
                    response.put("message", "Message not found");
                }
                break;

            case "CLEAR_CHAT":
                messageService.clearAllMessages();
                ChatMessage clearMsg = new ChatMessage();
                clearMsg.setType(MessageType.SYSTEM);
                clearMsg.setSender("Admin");
                clearMsg.setContent("CHAT_CLEARED");
                messagingTemplate.convertAndSend("/topic/public", clearMsg);

                response.put("success", true);
                response.put("message", "All messages cleared");
                break;

            case "BAN_USER":
                // Now uses displayName instead of email
                boolean banned = userService.banUserByUsername(request.getTargetUsername());
                if (banned) {
                    // Get the user's email to notify via WebSocket
                    String bannedEmail = userService.getEmailByUsername(request.getTargetUsername());

                    ChatMessage banMsg = new ChatMessage();
                    banMsg.setType(MessageType.SYSTEM);
                    banMsg.setSender("Admin");
                    banMsg.setContent("USER_BANNED:" + (bannedEmail != null ? bannedEmail : request.getTargetUsername()));
                    messagingTemplate.convertAndSend("/topic/public", banMsg);

                    response.put("success", true);
                    response.put("message", "User banned: " + request.getTargetUsername());
                } else {
                    response.put("success", false);
                    response.put("message", "User not found with username: " + request.getTargetUsername());
                }
                break;

            case "UNBAN_USER":
                boolean unbanned = userService.unbanUserByUsername(request.getTargetUsername());
                response.put("success", unbanned);
                response.put("message", unbanned ? "User unbanned: " + request.getTargetUsername() : "User not found");
                break;

            case "KICK_USER":
                String kickedEmail = userService.getEmailByUsername(request.getTargetUsername());
                ChatMessage kickMsg = new ChatMessage();
                kickMsg.setType(MessageType.SYSTEM);
                kickMsg.setSender("Admin");
                kickMsg.setContent("USER_KICKED:" + (kickedEmail != null ? kickedEmail : request.getTargetUsername()));
                messagingTemplate.convertAndSend("/topic/public", kickMsg);

                response.put("success", true);
                response.put("message", "Kick signal sent for: " + request.getTargetUsername());
                break;

            default:
                response.put("success", false);
                response.put("message", "Unknown action: " + request.getAction());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(
            @RequestHeader("X-Admin-Username") String username,
            @RequestHeader("X-Admin-Password") String password) {

        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestHeader("X-Admin-Username") String username,
            @RequestHeader("X-Admin-Password") String password) {

        if (!adminUsername.equals(username) || !adminPassword.equals(password)) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getAllUsers().size());
        stats.put("totalMessages", messageService.getRecentMessages().size());
        return ResponseEntity.ok(stats);
    }
}