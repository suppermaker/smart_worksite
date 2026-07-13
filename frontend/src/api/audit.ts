import request from '../utils/request';
import type { AuditLog, ID, PageQuery, PageResult } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_AUDIT_MOCK', false);

const mockLogs: AuditLog[] = [
  { id: 1, projectId: 1001, operatorId: 1, operatorName: '项目管理员', action: 'REPORT_CREATE', objectType: 'REPORT', objectId: 10001, requestId: 'REQ-20260706-001', ipAddress: '192.0.2.10', createdAt: '2026-07-06T10:12:00+08:00' },
  { id: 2, projectId: 1001, operatorId: 2, operatorName: '安全员', action: 'REVIEW_SUBMIT', objectType: 'REVIEW_RECORD', objectId: 7001, requestId: 'REQ-20260706-002', ipAddress: '192.0.2.11', createdAt: '2026-07-06T11:20:00+08:00' }
];

export async function fetchAuditLogs(params: PageQuery & { objectType?: string; action?: string; operatorId?: ID; createdFrom?: string; createdTo?: string } = {}) {
  if (useMock) {
    const records = mockLogs.filter((item) => !params.projectId || String(item.projectId) === String(params.projectId));
    return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: records.length, records } satisfies PageResult<AuditLog>;
  }
  return request.get<PageResult<AuditLog>>('/audit/logs', { params });
}
