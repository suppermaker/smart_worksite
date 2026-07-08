package com.xd.smartworksite.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AiMessage {
    private String messageId;
    @NotBlank
    private String role;
    @NotBlank
    private String content;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
