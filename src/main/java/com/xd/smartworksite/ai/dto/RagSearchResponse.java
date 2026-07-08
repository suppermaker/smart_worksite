package com.xd.smartworksite.ai.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RagSearchResponse {
    private List<Record> records = new ArrayList<>();
    private String providerTraceId;
    public List<Record> getRecords() { return records; }
    public void setRecords(List<Record> records) { this.records = records; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
    public static class Record {
        private String title;
        private String contentSnippet;
        private String sourceType;
        private String sourceId;
        private Double score;
        private Double rerankScore;
        private Map<String, Object> metadata = new LinkedHashMap<>();
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContentSnippet() { return contentSnippet; }
        public void setContentSnippet(String contentSnippet) { this.contentSnippet = contentSnippet; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public Double getRerankScore() { return rerankScore; }
        public void setRerankScore(Double rerankScore) { this.rerankScore = rerankScore; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
