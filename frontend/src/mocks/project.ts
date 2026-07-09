import type { ProjectItem } from '../api/types';

export const mockProjects: ProjectItem[] = [
  { projectId: 1001, projectName: '城北智慧工地一期', projectCode: 'CB-2026-01', status: 'ENABLED', location: '杭州市城北片区', description: '城北片区智慧工地一期项目', createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:00:00+08:00' },
  { projectId: 1002, projectName: '轨交站点综合体', projectCode: 'GJ-ZD-02', status: 'ENABLED', location: '地铁二号线站点', description: '轨交站点综合体建设项目', createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:10:00+08:00' },
  { projectId: 1003, projectName: '产业园改造工程', projectCode: 'CY-GZ-03', status: 'DISABLED', location: '高新区产业园', description: '高新区产业园改造工程', createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:20:00+08:00' }
];
