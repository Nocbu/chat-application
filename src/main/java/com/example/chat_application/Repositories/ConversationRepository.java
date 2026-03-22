package com.example.chat_application.Repositories;

import com.example.chat_application.model.conversation.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByPairKey(String pairKey);
}