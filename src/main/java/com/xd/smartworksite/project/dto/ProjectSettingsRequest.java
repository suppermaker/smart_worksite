package com.xd.smartworksite.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ProjectSettingsRequest {
    private Long defaultKnowledgeBaseId;
    private Long defaultReportTemplateId;

    @Min(1)
    @Max(3650)
    private Integer dataRetentionDays;

    @Min(1)
    private Long uploadMaxSizeMb;

    private List<@Size(max = 32) String> allowedFileTypes;

    private Boolean internetPolicyCrawlerEnabled;

    @Size(max = 32)
    private String defaultQaRouteMode;

    @Size(max = 32)
    private String defaultOcrLanguage;

    @Size(max = 16)
    private String defaultReportExportFormat;

    public Long getDefaultKnowledgeBaseId() { return defaultKnowledgeBaseId; }
    public void setDefaultKnowledgeBaseId(Long defaultKnowledgeBaseId) { this.defaultKnowledgeBaseId = defaultKnowledgeBaseId; }
    public Long getDefaultReportTemplateId() { return defaultReportTemplateId; }
    public void setDefaultReportTemplateId(Long defaultReportTemplateId) { this.defaultReportTemplateId = defaultReportTemplateId; }
    public Integer getDataRetentionDays() { return dataRetentionDays; }
    public void setDataRetentionDays(Integer dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }
    public Long getUploadMaxSizeMb() { return uploadMaxSizeMb; }
    public void setUploadMaxSizeMb(Long uploadMaxSizeMb) { this.uploadMaxSizeMb = uploadMaxSizeMb; }
    public List<String> getAllowedFileTypes() { return allowedFileTypes; }
    public void setAllowedFileTypes(List<String> allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }
    public Boolean getInternetPolicyCrawlerEnabled() { return internetPolicyCrawlerEnabled; }
    public void setInternetPolicyCrawlerEnabled(Boolean internetPolicyCrawlerEnabled) { this.internetPolicyCrawlerEnabled = internetPolicyCrawlerEnabled; }
    public String getDefaultQaRouteMode() { return defaultQaRouteMode; }
    public void setDefaultQaRouteMode(String defaultQaRouteMode) { this.defaultQaRouteMode = defaultQaRouteMode; }
    public String getDefaultOcrLanguage() { return defaultOcrLanguage; }
    public void setDefaultOcrLanguage(String defaultOcrLanguage) { this.defaultOcrLanguage = defaultOcrLanguage; }
    public String getDefaultReportExportFormat() { return defaultReportExportFormat; }
    public void setDefaultReportExportFormat(String defaultReportExportFormat) { this.defaultReportExportFormat = defaultReportExportFormat; }
}
