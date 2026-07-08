package com.xd.smartworksite.ai.dto;

public class RagIndexResponse {
    private Integer indexedDocuments;
    private Integer indexedChunks;
    private String provider;
    private String providerTraceId;
    public Integer getIndexedDocuments() { return indexedDocuments; }
    public void setIndexedDocuments(Integer indexedDocuments) { this.indexedDocuments = indexedDocuments; }
    public Integer getIndexedChunks() { return indexedChunks; }
    public void setIndexedChunks(Integer indexedChunks) { this.indexedChunks = indexedChunks; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
}
