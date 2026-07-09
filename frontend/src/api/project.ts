import request from '../utils/request';
import { mockProjects } from '../mocks/project';
import type { PageQuery, PageResult, ProjectItem, ProjectCreateForm, ProjectUpdateForm } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function fetchProjects(params: PageQuery = {}) {
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: mockProjects.length, records: mockProjects } satisfies PageResult<ProjectItem>;
  return request.get<PageResult<ProjectItem>>('/projects', { params });
}

export async function fetchProjectDetail(projectId: string | number) {
  if (useMock) return mockProjects.find((item) => String(item.projectId) === String(projectId)) || mockProjects[0];
  return request.get<ProjectItem>(`/projects/${projectId}`);
}

export function createProject(data: ProjectCreateForm) {
  return request.post<ProjectItem>('/projects', data);
}

export function updateProject(projectId: string | number, data: ProjectUpdateForm) {
  return request.put<ProjectItem>(`/projects/${projectId}`, data);
}

export function deleteProject(projectId: string | number) {
  return request.delete<void>(`/projects/${projectId}`);
}

export function updateProjectStatus(projectId: string | number, status: string) {
  return request.put<void>(`/projects/${projectId}/status`, { status });
}
