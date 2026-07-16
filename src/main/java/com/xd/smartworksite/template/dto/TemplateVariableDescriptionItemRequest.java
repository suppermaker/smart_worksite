package com.xd.smartworksite.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TemplateVariableDescriptionItemRequest {

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^var_[a-z0-9_]+$")
    private String variableName;

    @NotBlank
    @Size(max = 2000)
    private String description;

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
