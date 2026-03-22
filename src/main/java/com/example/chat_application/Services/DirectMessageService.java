package com.example.chat_application.Services;

import com.example.chat_application.Repositories.ChatMessageRepository; // adjust name
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.springframework.stereotype.Service;
import com.example.chat_application.security.cryptoService;

import java.time.Instant;
import java.util.List;

@Service
public class DirectMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final cryptoService cryptoService;
    private final ConversationService conversationService;

    public DirectMessageService(ChatMessageRepository chatMessageRepository,
                                cryptoService cryptoService,
                                ConversationService conversationService) {
        this.chatMessageRepository = chatMessageRepository;
        this.cryptoService = cryptoService;
        this.conversationService = conversationService;
    }

    public ChatMessage saveDirectMessage(String conversationId, ChatMessage msg, String senderUsername) {
        conversationService.requireMember(conversationId, senderUsername);

        msg.setScope("DIRECT");
        msg.setConversationId(conversationId);
        msg.setSenderUsername(senderUsername);

        if (msg.getType() == null) {
            msg.setType(MessageType.CHAT);
        }

        // Encrypt content/caption before saving
        String plain = msg.getContent();
        msg.setContent(cryptoService.encryptToString(plain));
        msg.setTimestamp(java.time.LocalDateTime.now());

        ChatMessage saved = chatMessageRepository.save(msg);

        // Return plaintext to broadcast
        saved.setContent(plain);
        return saved;
    }

    public List<ChatMessage> getDirectHistory(String conversationId, String viewerUsername) {
        conversationService.requireMember(conversationId, viewerUsername);

        // You need a repo query for this:
        // find by conversationId + scope DIRECT ordered by timestamp asc/desc
        List<ChatMessage> messages = chatMessageRepository
                .findByScopeAndConversationIdOrderByTimestampAsc("DIRECT", conversationId);

        // Decrypt + apply delete rules
        for (ChatMessage m : messages) {
            // If deleted for everyone => keep placeholder (frontend can show)
            if (m.isDeletedForEveryone()) {
                m.setContent(""); // no content
                continue;
            }

            // If deleted for me => hide content or mark; simplest is blank content
            if (m.getDeletedFor() != null && m.getDeletedFor().stream().anyMatch(u -> u.equalsIgnoreCase(viewerUsername))) {
                m.setContent(""); // frontend can hide if empty + flags
                continue;
            }

            // Normal decrypt
            m.setContent(cryptoService.decryptToString(m.getContent()));
        }

        return messages;
    }

    public ChatMessage deleteForMe(String messageId, String username) {
        ChatMessage m = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        if (!"DIRECT".equalsIgnoreCase(m.getScope())) {
            throw new IllegalArgumentException("Not a direct message.");
        }
        conversationService.requireMember(m.getConversationId(), username);

        m.getDeletedFor().add(username.toLowerCase());
        return chatMessageRepository.save(m);
    }

    public ChatMessage deleteForEveryone(String messageId, String username) {
        ChatMessage m = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found."));

        if (!"DIRECT".equalsIgnoreCase(m.getScope())) {
            throw new IllegalArgumentException("Not a direct message.");
        }
        conversationService.requireMember(m.getConversationId(), username);

        // Only sender can delete for everyone
        if (m.getSenderUsername() == null || !m.getSenderUsername().equalsIgnoreCase(username)) {
            throw new SecurityException("Only sender can delete for everyone.");
        }

        m.setDeletedForEveryone(true);

        // Optional: wipe content so even DB won't keep it (stronger privacy)
        m.setContent("");
        // If you wipe content, decryption should not be attempted later.

        return chatMessageRepository.save(m);
    }
}