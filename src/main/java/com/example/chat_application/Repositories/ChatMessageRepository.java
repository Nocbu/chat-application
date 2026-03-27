package com.example.chat_application.Repositories;


import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findTop100ByOrderByTimestampDesc();
    List<ChatMessage> findTop100ByScopeIsNullOrScopeOrderByTimestampDesc(String scope);
    List<ChatMessage> findByType(MessageType type);
    List<ChatMessage> findBySenderEmail(String senderEmail);
    List<ChatMessage> findByScopeAndConversationIdOrderByTimestampAsc(String scope, String conversationId);
    void deleteAllByType(MessageType type);
}
