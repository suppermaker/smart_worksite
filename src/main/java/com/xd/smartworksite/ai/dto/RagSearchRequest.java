package com.xd.smartworksite.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class RagSearchRequest {
    @NotNull
    private Long projectId;
    @NotBlank
    private String query;
    private List<Long> knowledgeBaseIds = new ArrayList<>();
    private List<String> libraryTypes = new ArrayList<>();
    private Integer topK = 5;
    private Double scoreThreshold;
    private Boolean rerankEnabled = true;
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public List<Long> getKnowledgeBaseIds() { return knowledgeBaseIds; }
    public void setKnowledgeBaseIds(List<Long> knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
    public List<String> getLibraryTypes() { return libraryTypes; }
    public void setLibraryTypes(List<String> libraryTypes) { this.libraryTypes = libraryTypes; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Double getScoreThreshold() { return scoreThreshold; }
    public void setScoreThreshold(Double scoreThreshold) { this.scoreThreshold = scoreThreshold; }
    public Boolean getRerankEnabled() { return rerankEnabled; }
    public void setRerankEnabled(Boolean rerankEnabled) { this.rerankEnabled = rerankEnabled; }
}
