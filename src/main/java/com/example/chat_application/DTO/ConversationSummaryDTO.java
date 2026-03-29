package com.example.chat_application.DTO;

public class ConversationSummaryDTO {

    private String conversationId;
    private String otherUsername;
    private String otherDisplayName;
    private String lastMessagePreview;
    private String lastMessageTimestamp; // ISO string from LocalDateTime
    private long unreadCount;

    public ConversationSummaryDTO() {}

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }

    public String getOtherDisplayName() { return otherDisplayName; }
    public void setOtherDisplayName(String otherDisplayName) { this.otherDisplayName = otherDisplayName; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public String getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(String lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}
