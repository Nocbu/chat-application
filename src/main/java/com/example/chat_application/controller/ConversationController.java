package com.example.chat_application.controller;

import com.example.chat_application.Services.ConversationService;
import com.example.chat_application.model.conversation.Conversation;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct")
    public ResponseEntity<?> startDirect(@RequestBody Map<String, String> body, HttpSession session) {
        String myUsername = (String) session.getAttribute("username"); // you may not have this yet!
        // If you don't store username in session, store it at login, OR send from frontend.
        if (myUsername == null) {
            // fallback: try displayName? NOT recommended
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No username in session."));
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