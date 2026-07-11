package com.xd.smartworksite.project.dto;

public class ProjectStatisticsResponse {
    private Long projectId;
    private long memberCount;
    private long knowledgeBaseCount;
    private long reportCount;
    private long dataSourceCount;
    private long qaCount;
    private long reviewCount;
    private long ocrCount;
    private long fileStorageBytes;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public long getMemberCount() { return memberCount; }
    public void setMemberCount(long memberCount) { this.memberCount = memberCount; }
    public long getKnowledgeBaseCount() { return knowledgeBaseCount; }
    public void setKnowledgeBaseCount(long knowledgeBaseCount) { this.knowledgeBaseCount = knowledgeBaseCount; }
    public long getReportCount() { return reportCount; }
    public void setReportCount(long reportCount) { this.reportCount = reportCount; }
    public long getDataSourceCount() { return dataSourceCount; }
    public void setDataSourceCount(long dataSourceCount) { this.dataSourceCount = dataSourceCount; }
    public long getQaCount() { return qaCount; }
    public void setQaCount(long qaCount) { this.qaCount = qaCount; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    public long getOcrCount() { return ocrCount; }
    public void setOcrCount(long ocrCount) { this.ocrCount = ocrCount; }
    public long getFileStorageBytes() { return fileStorageBytes; }
    public void setFileStorageBytes(long fileStorageBytes) { this.fileStorageBytes = fileStorageBytes; }
}
