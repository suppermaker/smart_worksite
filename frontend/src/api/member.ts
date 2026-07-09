import request from '../utils/request';
import type { ProjectMemberItem } from './types';

export function listMembers(projectId: number | string) {
  return request.get<ProjectMemberItem[]>(`/projects/${projectId}/members`);
}

export function addMember(projectId: number | string, data: { userId: number | string; projectRole: string }) {
  return request.post<ProjectMemberItem>(`/projects/${projectId}/members`, data);
}

export function updateMember(projectId: number | string, userId: number | string, data: { userId: number | string; projectRole: string }) {
  return request.put<ProjectMemberItem>(`/projects/${projectId}/members/${userId}`, data);
}

export function removeMember(projectId: number | string, userId: number | string) {
  return request.delete<void>(`/projects/${projectId}/members/${userId}`);
}
