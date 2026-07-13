import type { DataSourceItem } from '../api/types';

export const mockSources: DataSourceItem[] = [
  { dataSourceId: 1, projectId: 1001, name: '项目质量安全库', dbType: 'MYSQL', jdbcUrl: 'jdbc:mysql://db.example.invalid:3306/worksite_quality', username: 'readonly', status: 'ENABLED', createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-01T09:00:00+08:00' }
];
