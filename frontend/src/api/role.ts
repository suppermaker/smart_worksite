import request from '../utils/request';
import type { RoleItem, PermissionItem, RoleCreateForm, RoleUpdateForm } from './types';

export function listRoles(keyword?: string) {
  return request.get<RoleItem[]>('/system/roles', { params: { keyword } });
}

export function listPermissions() {
  return request.get<PermissionItem[]>('/system/roles/permissions');
}

export function assignPermissions(roleId: number | string, permissionIds: (number | string)[]) {
  return request.put<RoleItem>(`/system/roles/${roleId}/permissions`, { permissionIds });
}

export function createRole(data: RoleCreateForm) {
  return request.post<RoleItem>('/system/roles', data);
}

export function updateRole(roleId: number | string, data: RoleUpdateForm) {
  return request.put<RoleItem>(`/system/roles/${roleId}`, data);
}

export function updateRoleStatus(roleId: number | string, status: 'ENABLED' | 'DISABLED') {
  return request.put<RoleItem>(`/system/roles/${roleId}/status`, null, { params: { status } });
}

export function deleteRole(roleId: number | string) {
  return request.delete<void>(`/system/roles/${roleId}`);
}
