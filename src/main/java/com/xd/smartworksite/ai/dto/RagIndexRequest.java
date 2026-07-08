package com.xd.smartworksite.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class RagIndexRequest {
    @NotNull
    private Long projectId;
    private Long knowledgeBaseId;
    @Valid
    private List<RagDocumentRequest> documents = new ArrayList<>();
    private Integer chunkSize;
    private Integer chunkOverlap;
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public List<RagDocumentRequest> getDocuments() { return documents; }
    public void setDocuments(List<RagDocumentRequest> documents) { this.documents = documents; }
    public Integer getChunkSize() { return chunkSize; }
    public void setChunkSize(Integer chunkSize) { this.chunkSize = chunkSize; }
    public Integer getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
