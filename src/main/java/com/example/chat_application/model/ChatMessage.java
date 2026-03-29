package com.example.chat_application.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "messages")
public class ChatMessage {

    @Id
    private String id;

    private MessageType type;
    private String content;

    //group-chat fields
    private String sender;       // display name (what you show in UI)
    private String senderEmail;

    //for dm
    private String senderUsername;

    // scope message
    private String scope = "PUBLIC";

    // conv. id for dm
    private String conversationId;

    // files
    private String fileId;       // reference to FileAttachment if type=FILE
    private String fileName;
    private String fileType;
    private long fileSize;

    // delete flags
    private boolean deletedForEveryone = false;
    private Set<String> deletedFor = new HashSet<>();

    // Read receipts (DIRECT messages)
    private Set<String> readBy = new HashSet<>();

    // Reply-to metadata
    private String replyToMessageId;
    private String replyToContent;
    private String replyToSender;

    private LocalDateTime timestamp;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(MessageType type, String content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public boolean isDeletedForEveryone() { return deletedForEveryone; }
    public void setDeletedForEveryone(boolean deletedForEveryone) { this.deletedForEveryone = deletedForEveryone; }

    public Set<String> getDeletedFor() { return deletedFor; }
    public void setDeletedFor(Set<String> deletedFor) {
        this.deletedFor = (deletedFor == null) ? new HashSet<>() : deletedFor;
    }

    public Set<String> getReadBy() { return readBy; }
    public void setReadBy(Set<String> readBy) {
        this.readBy = (readBy == null) ? new HashSet<>() : readBy;
    }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public String getReplyToSender() { return replyToSender; }
    public void setReplyToSender(String replyToSender) { this.replyToSender = replyToSender; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}