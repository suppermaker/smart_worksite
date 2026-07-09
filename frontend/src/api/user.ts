import request from '../utils/request';
import type { PageResult, UserItem, UserCreateForm, UserUpdateForm } from './types';

export function listUsers(params: { keyword?: string; status?: string; pageNo?: number; pageSize?: number }) {
  return request.get<PageResult<UserItem>>('/system/users', { params });
}

export function getUser(userId: number | string) {
  return request.get<UserItem>(`/system/users/${userId}`);
}

export function createUser(data: UserCreateForm) {
  return request.post<UserItem>('/system/users', data);
}

export function updateUser(userId: number | string, data: UserUpdateForm) {
  return request.put<UserItem>(`/system/users/${userId}`, data);
}

export function updateUserStatus(userId: number | string, status: string) {
  return request.put<void>(`/system/users/${userId}/status`, null, { params: { status } });
}

export function resetPassword(userId: number | string, newPassword: string) {
  return request.put<void>(`/system/users/${userId}/password`, { newPassword });
}
