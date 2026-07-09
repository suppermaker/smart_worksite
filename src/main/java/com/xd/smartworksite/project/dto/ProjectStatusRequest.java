package com.xd.smartworksite.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ProjectStatusRequest {

    @NotBlank
    @Pattern(regexp = "ENABLED|DISABLED")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
