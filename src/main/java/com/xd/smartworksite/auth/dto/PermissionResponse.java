package com.xd.smartworksite.auth.dto;

public class PermissionResponse {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }

    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }

    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
