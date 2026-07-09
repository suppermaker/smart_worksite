package com.xd.smartworksite.auth.dto;

import java.util.List;

public class RoleAssignRequest {

    private List<Long> permissionIds;

    public List<Long> getPermissionIds() { return permissionIds; }
    public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }
}
