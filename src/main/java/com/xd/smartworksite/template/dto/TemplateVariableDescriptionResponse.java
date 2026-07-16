package com.xd.smartworksite.template.dto;

public class TemplateVariableDescriptionResponse {

    private String variableName;
    private String description;

    public TemplateVariableDescriptionResponse() {
    }

    public TemplateVariableDescriptionResponse(String variableName, String description) {
        this.variableName = variableName;
        this.description = description;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
