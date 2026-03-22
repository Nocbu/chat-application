package com.example.chat_application.controller;

import com.example.chat_application.Repositories.UserRepository;
import com.example.chat_application.Services.ConversationService;
import com.example.chat_application.model.User;
import com.example.chat_application.model.conversation.Conversation;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/direct")
    public ResponseEntity<?> startDirect(@RequestBody Map<String, String> body, HttpSession session) {
        String myUsername = (String) session.getAttribute("username");

        // Recover username if missing in session (common after refresh)
        if (myUsername == null || myUsername.isBlank()) {
            String email = (String) session.getAttribute("userEmail");
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated."));
            }

            User me = userRepository.findByEmail(email).orElse(null);
            if (me == null || me.getUsername() == null || me.getUsername().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username not set. Please choose a username."));
            }

            myUsername = me.getUsername().trim().toLowerCase();
            session.setAttribute("username", myUsername);
        }

        String targetUsername = body.get("targetUsername");
        Conversation c = conversationService.getOrCreateDirectConversation(myUsername, targetUsername);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", c.getId(),
                "pairKey", c.getPairKey()
        ));
    }
}