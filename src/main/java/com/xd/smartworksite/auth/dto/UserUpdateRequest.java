package com.xd.smartworksite.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class UserUpdateRequest {

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String phone;
    private String email;
    private List<String> roleCodes;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getRoleCodes() { return roleCodes; }
    public void setRoleCodes(List<String> roleCodes) { this.roleCodes = roleCodes; }
}
