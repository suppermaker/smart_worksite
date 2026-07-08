package com.xd.smartworksite.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class ContextPrepareResponse {
    private List<AiMessage> contextMessages = new ArrayList<>();
    private List<String> referencedMessageIds = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();
    private List<String> followUpQuestions = new ArrayList<>();
    private String providerTraceId;
    public List<AiMessage> getContextMessages() { return contextMessages; }
    public void setContextMessages(List<AiMessage> contextMessages) { this.contextMessages = contextMessages; }
    public List<String> getReferencedMessageIds() { return referencedMessageIds; }
    public void setReferencedMessageIds(List<String> referencedMessageIds) { this.referencedMessageIds = referencedMessageIds; }
    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }
    public List<String> getFollowUpQuestions() { return followUpQuestions; }
    public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
}
