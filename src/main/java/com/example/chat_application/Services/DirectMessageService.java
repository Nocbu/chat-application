package com.example.chat_application.Services;

import com.example.chat_application.Repositories.ChatMessageRepository; // adjust name
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.springframework.stereotype.Service;
import com.example.chat_application.security.cryptoService;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public List<ChatMessage> searchDirectHistory(String conversationId,
                                                 String viewerUsername,
                                                 String text,
                                                 String sender,
                                                 String fileType,
                                                 LocalDate fromDate,
                                                 LocalDate toDate) {
        conversationService.requireMember(conversationId, viewerUsername);

        List<ChatMessage> messages = chatMessageRepository
                .findByScopeAndConversationIdOrderByTimestampAsc("DIRECT", conversationId);

        String normalizedViewer = viewerUsername == null ? "" : viewerUsername.trim().toLowerCase();
        String normalizedText = text == null ? "" : text.trim().toLowerCase();
        String normalizedSender = sender == null ? "" : sender.trim().toLowerCase();
        String normalizedFileType = fileType == null ? "" : fileType.trim().toLowerCase();

        return messages.stream()
                .filter(m -> fromDate == null || (m.getTimestamp() != null && !m.getTimestamp().toLocalDate().isBefore(fromDate)))
                .filter(m -> toDate == null || (m.getTimestamp() != null && !m.getTimestamp().toLocalDate().isAfter(toDate)))
                .filter(m -> normalizedSender.isBlank() || matchesSender(m, normalizedSender))
                .filter(m -> normalizedFileType.isBlank() || matchesFileType(m, normalizedFileType))
                .filter(m -> normalizedText.isBlank() || matchesSearchText(m, normalizedText))
                .map(m -> applyViewerVisibility(m, normalizedViewer))
                .collect(Collectors.toList());
    }

    //mark read
    public List<String> markConversationAsRead(String conversationId, String viewerUsername) {
        conversationService.requireMember(conversationId, viewerUsername);

        String viewer = viewerUsername.trim().toLowerCase();
        List<ChatMessage> messages = chatMessageRepository
                .findByScopeAndConversationIdOrderByTimestampAsc("DIRECT", conversationId);

        List<ChatMessage> toUpdate = messages.stream()
                .filter(m -> !m.isDeletedForEveryone())
                .filter(m -> m.getSenderUsername() != null && !m.getSenderUsername().equalsIgnoreCase(viewer))
                .filter(m -> m.getReadBy() == null || !m.getReadBy().contains(viewer))
                .collect(Collectors.toList());

        for (ChatMessage m : toUpdate) {
            if (m.getReadBy() == null) {
                m.setReadBy(new java.util.HashSet<>());
            }
            m.getReadBy().add(viewer);
            chatMessageRepository.save(m);
        }

        return toUpdate.stream().map(ChatMessage::getId).collect(Collectors.toList());
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

        // only sender can delete for everyone
        if (m.getSenderUsername() == null || !m.getSenderUsername().equalsIgnoreCase(username)) {
            throw new SecurityException("Only sender can delete for everyone.");
        }

        m.setDeletedForEveryone(true);


        m.setContent("");


        return chatMessageRepository.save(m);
    }

    private boolean matchesSender(ChatMessage message, String sender) {
        String senderUsername = message.getSenderUsername() == null ? "" : message.getSenderUsername().trim().toLowerCase();
        String displayName = message.getSender() == null ? "" : message.getSender().trim().toLowerCase();
        return senderUsername.contains(sender) || displayName.contains(sender);
    }

    private boolean matchesFileType(ChatMessage message, String fileType) {
        if (message.getType() != MessageType.FILE) {
            return false;
        }

        String actualFileType = message.getFileType() == null ? "" : message.getFileType().trim().toLowerCase();
        return actualFileType.contains(fileType);
    }

    private boolean matchesSearchText(ChatMessage message, String text) {
        StringBuilder haystack = new StringBuilder();
        haystack.append(decryptForSearch(message.getContent())).append(' ');
        haystack.append(message.getFileName() == null ? "" : message.getFileName()).append(' ');
        haystack.append(message.getReplyToContent() == null ? "" : message.getReplyToContent()).append(' ');
        haystack.append(message.getReplyToSender() == null ? "" : message.getReplyToSender());
        return haystack.toString().toLowerCase().contains(text);
    }

    private ChatMessage applyViewerVisibility(ChatMessage message, String viewerUsername) {
        if (message.isDeletedForEveryone()) {
            message.setContent("");
            return message;
        }

        if (message.getDeletedFor() != null && message.getDeletedFor().stream().anyMatch(u -> u.equalsIgnoreCase(viewerUsername))) {
            message.setContent("");
            return message;
        }

        message.setContent(decryptForSearch(message.getContent()));
        return message;
    }

    private String decryptForSearch(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isBlank()) {
            return "";
        }

        try {
            return cryptoService.decryptToString(encryptedContent);
        } catch (Exception ex) {
            return "";
        }
    }
}