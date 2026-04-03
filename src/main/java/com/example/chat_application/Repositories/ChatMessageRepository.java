package com.example.chat_application.Repositories;
import com.example.chat_application.model.ChatMessage;
import com.example.chat_application.model.MessageType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findTop100ByOrderByTimestampDesc();
    List<ChatMessage> findTop100ByScopeIsNullOrScopeOrderByTimestampDesc(String scope);
    List<ChatMessage> findByType(MessageType type);
    List<ChatMessage> findBySenderEmail(String senderEmail);
    List<ChatMessage> findByScopeAndConversationIdOrderByTimestampAsc(String scope, String conversationId);
    Optional<ChatMessage> findTop1ByScopeAndConversationIdOrderByTimestampDesc(String scope, String conversationId);
    void deleteAllByType(MessageType type);

    @Query(value = "{'scope': 'DIRECT', 'conversationId': ?0, 'senderUsername': {$ne: ?1}, "
            + "'deletedForEveryone': false, "
            + "'deletedFor': {$not: {$elemMatch: {$eq: ?1}}}, "
            + "'readBy': {$not: {$elemMatch: {$eq: ?1}}}}", count = true)
    long countUnreadInConversation(String conversationId, String viewerUsername);
}
