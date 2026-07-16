package com.xd.smartworksite.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TemplateVariableDescriptionUpsertRequest {

    @NotNull
    @Size(max = 1000)
    @Valid
    private List<@Valid TemplateVariableDescriptionItemRequest> variables;

    public List<TemplateVariableDescriptionItemRequest> getVariables() {
        return variables;
    }

    public void setVariables(List<TemplateVariableDescriptionItemRequest> variables) {
        this.variables = variables;
    }
}
