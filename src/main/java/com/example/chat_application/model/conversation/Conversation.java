package com.example.chat_application.model.conversation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    private ConversationType type = ConversationType.DIRECT;

    /**
     * Always exactly 2 usernames, sorted ascending.
     * Example: ["alice","bob"]
     */
    private List<String> memberUsernames;

    /**
     * Unique key for direct chats: "alice:bob" (sorted).
     */
    @Indexed(unique = true)
    private String pairKey;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Conversation() {}

    public Conversation(List<String> memberUsernames, String pairKey) {
        this.memberUsernames = memberUsernames;
        this.pairKey = pairKey;
    }

    // --- getters/setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ConversationType getType() { return type; }
    public void setType(ConversationType type) { this.type = type; }

    public List<String> getMemberUsernames() { return memberUsernames; }
    public void setMemberUsernames(List<String> memberUsernames) { this.memberUsernames = memberUsernames; }

    public String getPairKey() { return pairKey; }
    public void setPairKey(String pairKey) { this.pairKey = pairKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}