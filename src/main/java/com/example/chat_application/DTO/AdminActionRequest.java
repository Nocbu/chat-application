package com.example.chat_application.DTO;

public class AdminActionRequest {

    private String action;          // "DELETE_MESSAGE", "CLEAR_CHAT", "BAN_USER", "UNBAN_USER", "KICK_USER"
    private String messageId;
    private String targetUsername;   // admin uses display name, NOT email
    private String reason;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}