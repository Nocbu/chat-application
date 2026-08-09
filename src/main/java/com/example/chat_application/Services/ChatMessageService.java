package com.example.chat_application.Services;

import com.example.chat_application.Repositories.ChatMessageRepository;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import com.example.chat_application.security.cryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private cryptoService cryptoService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public ChatMessage saveMessage(ChatMessage message) {
        if (message.getContent() != null && !message.getContent().isBlank()) {
            message.setContent(cryptoService.encryptToString(message.getContent()));
        }
        return messageRepository.save(message);
    }

    public List<ChatMessage> getRecentMessages() {
        // Only fetch PUBLIC-scoped messages null scope is included for backward
        // compatibility with messages saved before the scope field was introduced
        List<ChatMessage> messages = messageRepository.findTop100ByScopeIsNullOrScopeOrderByTimestampDesc("PUBLIC");
        Collections.reverse(messages);

        //decrypt
        for (ChatMessage m : messages) {
            if (m.getContent() != null && !m.getContent().isBlank()) {
                m.setContent(cryptoService.decryptToString(m.getContent()));
            }
        }
        return messages;
    }

    public Optional<ChatMessage> getMessageById(String id) {
        Optional<ChatMessage> msg = messageRepository.findById(id);
        msg.ifPresent(m -> {
            if (m.getContent() != null && !m.getContent().isBlank()) {
                m.setContent(cryptoService.decryptToString(m.getContent()));
            }
        });
        return msg;
    }

    public boolean deleteMessage(String messageId) {
        Optional<ChatMessage> message = messageRepository.findById(messageId);
        if (message.isPresent()) {
            messageRepository.deleteById(messageId);
            return true;
        }
        return false;
    }

    public void clearAllMessages() {
        messageRepository.deleteAll();
    }

    public List<ChatMessage> searchPublicMessages(String text,
                                                  String sender,
                                                  String fileType,
                                                  LocalDate fromDate,
                                                  LocalDate toDate) {
        boolean hasFilters = (text != null && !text.isBlank())
                || (sender != null && !sender.isBlank())
                || (fileType != null && !fileType.isBlank())
                || fromDate != null
                || toDate != null;

        if (!hasFilters) {
            return getRecentMessages();
        }

        List<Criteria> criteriaParts = new ArrayList<>();
        criteriaParts.add(new Criteria().orOperator(
                Criteria.where("scope").is("PUBLIC"),
                Criteria.where("scope").is(null)
        ));

        if (sender != null && !sender.isBlank()) {
            criteriaParts.add(Criteria.where("sender").regex(Pattern.quote(sender.trim()), "i"));
        }

        if (fromDate != null) {
            criteriaParts.add(Criteria.where("timestamp").gte(fromDate.atStartOfDay()));
        }

        if (toDate != null) {
            criteriaParts.add(Criteria.where("timestamp").lte(toDate.atTime(23, 59, 59, 999_999_999)));
        }

        if (fileType != null && !fileType.isBlank()) {
            criteriaParts.add(Criteria.where("type").is(MessageType.FILE));
            criteriaParts.add(Criteria.where("fileType").regex(Pattern.quote(fileType.trim()), "i"));
        }

        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(criteriaParts.toArray(new Criteria[0])));
        query.with(Sort.by(Sort.Direction.ASC, "timestamp"));

        List<ChatMessage> messages = mongoTemplate.find(query, ChatMessage.class);
        String normalizedText = text == null ? "" : text.trim().toLowerCase();

        return messages.stream()
                .filter(message -> {
                    if (normalizedText.isBlank()) {
                        return true;
                    }

                    String decryptedContent = decryptContentOrEmpty(message.getContent());
                    String searchHaystack = (decryptedContent + " "
                            + safeLower(message.getFileName()) + " "
                            + safeLower(message.getReplyToContent())).toLowerCase();
                    return searchHaystack.contains(normalizedText);
                })
                .map(message -> {
                    if (message.isDeletedForEveryone()) {
                        message.setContent("");
                    } else {
                        message.setContent(decryptContentOrEmpty(message.getContent()));
                    }
                    return message;
                })
                .collect(Collectors.toList());
    }

    private String decryptContentOrEmpty(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isBlank()) {
            return "";
        }

        try {
            return cryptoService.decryptToString(encryptedContent);
        } catch (Exception ex) {
            return "";
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}