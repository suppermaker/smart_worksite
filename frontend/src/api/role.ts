import request from '../utils/request';
import type { RoleItem, PermissionItem } from './types';

export function listRoles(keyword?: string) {
  return request.get<RoleItem[]>('/system/roles', { params: { keyword } });
}

export function listPermissions() {
  return request.get<PermissionItem[]>('/system/roles/permissions');
}

export function assignPermissions(roleId: number | string, permissionIds: (number | string)[]) {
  return request.put<RoleItem>(`/system/roles/${roleId}/permissions`, { permissionIds });
}
