package com.example.chat_application.Services;

import com.example.chat_application.DTO.ConversationSummaryDTO;
import com.example.chat_application.Repositories.ChatMessageRepository;
import com.example.chat_application.Repositories.ConversationRepository;
import com.example.chat_application.Repositories.UserRepository;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import com.example.chat_application.model.User;
import com.example.chat_application.model.conversation.Conversation;
import com.example.chat_application.security.cryptoService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final cryptoService cryptoService;

    public ConversationService(ConversationRepository conversationRepository,
                               UserRepository userRepository,
                               ChatMessageRepository chatMessageRepository,
                               cryptoService cryptoService) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.cryptoService = cryptoService;
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

    /**
     * Returns a summary list of all direct conversations the user has, sorted by most recent first.
     * Each summary includes the other participant's name, last message preview, and unread count.
     */
    public List<ConversationSummaryDTO> getConversationSummaries(String viewerUsername) {
        final int PREVIEW_MAX_LENGTH = 40;
        String viewer = viewerUsername.trim().toLowerCase();

        List<Conversation> conversations = conversationRepository.findByMemberUsernamesContaining(viewer);

        List<ConversationSummaryDTO> summaries = new ArrayList<>();

        for (Conversation conv : conversations) {
            // Determine the other participant
            String otherUsername = null;
            if (conv.getMemberUsernames() != null) {
                for (String m : conv.getMemberUsernames()) {
                    if (!m.equalsIgnoreCase(viewer)) {
                        otherUsername = m;
                        break;
                    }
                }
            }
            if (otherUsername == null) continue;

            // Resolve display name
            final String finalOther = otherUsername;
            String otherDisplayName = userRepository.findByUsernameIgnoreCase(finalOther)
                    .map(User::getDisplayName)
                    .orElse(finalOther);

            // Get last message for preview
            String lastPreview = "";
            String lastTimestamp = null;
            Optional<ChatMessage> lastMsgOpt = chatMessageRepository
                    .findTop1ByScopeAndConversationIdOrderByTimestampDesc("DIRECT", conv.getId());

            if (lastMsgOpt.isPresent()) {
                ChatMessage last = lastMsgOpt.get();
                lastTimestamp = last.getTimestamp() != null ? last.getTimestamp().toString() : null;

                if (last.isDeletedForEveryone()) {
                    lastPreview = "This message was deleted";
                } else if (last.getType() == MessageType.FILE) {
                    lastPreview = "📎 " + (last.getFileName() != null ? last.getFileName() : "File");
                } else {
                    try {
                        String decrypted = cryptoService.decryptToString(last.getContent());
                        if (decrypted != null && decrypted.length() > PREVIEW_MAX_LENGTH) {
                            lastPreview = decrypted.substring(0, PREVIEW_MAX_LENGTH) + "…";
                        } else {
                            lastPreview = decrypted != null ? decrypted : "";
                        }
                    } catch (Exception e) {
                        lastPreview = "";
                    }
                }
            }

            // Count unread messages using an efficient DB-level count query
            long unreadCount = chatMessageRepository.countUnreadInConversation(conv.getId(), viewer);

            ConversationSummaryDTO dto = new ConversationSummaryDTO();
            dto.setConversationId(conv.getId());
            dto.setOtherUsername(finalOther);
            dto.setOtherDisplayName(otherDisplayName);
            dto.setLastMessagePreview(lastPreview);
            dto.setLastMessageTimestamp(lastTimestamp);
            dto.setUnreadCount(unreadCount);

            summaries.add(dto);
        }

        // Sort: conversations with messages first (most recent), then by creation order
        summaries.sort(Comparator.comparing(ConversationSummaryDTO::getLastMessageTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return summaries;
    }
}