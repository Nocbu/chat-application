package com.example.chat_application.Services;

import com.example.chat_application.Repositories.ConversationRepository;
import com.example.chat_application.Repositories.UserRepository;
import com.example.chat_application.model.User;
import com.example.chat_application.model.conversation.Conversation;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public Conversation getOrCreateDirectConversation(String myUsername, String targetUsername) {
        if (myUsername == null || myUsername.isBlank()) {
            throw new IllegalArgumentException("Current username is missing.");
        }
        if (targetUsername == null || targetUsername.isBlank()) {
            throw new IllegalArgumentException("Target username is missing.");
        }
        if (myUsername.equalsIgnoreCase(targetUsername)) {
            throw new IllegalArgumentException("You cannot start a chat with yourself.");
        }

        String a = myUsername.trim().toLowerCase();
        String b = targetUsername.trim().toLowerCase();
        String pairKey = (a.compareTo(b) < 0) ? (a + ":" + b) : (b + ":" + a);

        // Validate target user exists + enabled
        User target = userRepository.findByUsernameIgnoreCase(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUsername));

        // enabled is boolean => no null checks
        if (!target.isEnabled()) {
            throw new IllegalArgumentException("User is disabled/banned.");
        }

        return conversationRepository.findByPairKey(pairKey).orElseGet(() -> {
            List<String> members = Arrays.asList(pairKey.split(":"));
            Conversation c = new Conversation(members, pairKey);
            c.setUpdatedAt(Instant.now());
            return conversationRepository.save(c);
        });
    }

    public Conversation requireMember(String conversationId, String username) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        String u = username == null ? "" : username.trim().toLowerCase();
        boolean isMember = c.getMemberUsernames() != null
                && c.getMemberUsernames().stream().anyMatch(m -> m.equalsIgnoreCase(u));

        if (!isMember) throw new SecurityException("Not a member of this conversation.");

        return c;
    }
}