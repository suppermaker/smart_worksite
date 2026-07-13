import request from '../utils/request';
import { mockTaskDetail, mockTaskStages } from '../mocks/task';
import type { ID, PageQuery, PageResult, TaskDetail, TaskStageLog, TaskStatistics } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_TASK_MOCK', false);

function findMockTask(taskId: ID) {
  if (String(mockTaskDetail.taskId) !== String(taskId)) throw new Error(`任务不存在：${taskId}`);
  return mockTaskDetail;
}

export async function fetchTasks(params: PageQuery & { taskType?: string; bizType?: string } = {}) {
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: 1, records: [mockTaskDetail] } satisfies PageResult<TaskDetail>;
  return request.get<PageResult<TaskDetail>>('/tasks', { params });
}

export async function fetchTaskStatistics(projectId?: ID) {
  if (useMock) return { projectId, statusCounts: { QUEUED: 1, RUNNING: 1, FAILED: 0 }, queuedCount: 1, runningCount: 1, failedCount: 0 } satisfies TaskStatistics;
  return request.get<TaskStatistics>('/tasks/statistics', { params: { projectId } });
}

export async function fetchTaskDetail(taskId: ID) {
  if (useMock) return findMockTask(taskId);
  return request.get<TaskDetail>(`/tasks/${taskId}`);
}

export async function fetchTaskStages(taskId: ID) {
  if (useMock) return mockTaskStages.map((item) => ({ ...item, taskId })) satisfies TaskStageLog[];
  return request.get<TaskStageLog[]>(`/tasks/${taskId}/stages`);
}

export async function retryTask(taskId: ID) {
  if (useMock) return { ...findMockTask(taskId), status: 'QUEUED' } satisfies TaskDetail;
  return request.post<TaskDetail>(`/tasks/${taskId}/retry`);
}

export async function cancelTask(taskId: ID) {
  if (useMock) return { ...findMockTask(taskId), status: 'CANCELED' } satisfies TaskDetail;
  return request.post<TaskDetail>(`/tasks/${taskId}/cancel`);
}
