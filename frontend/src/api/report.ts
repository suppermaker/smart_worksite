import request, { downloadFile } from '../utils/request';
import { mockReports } from '../mocks/report';
import type { ID, PageQuery, PageResult, ReportItem } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function uploadReportTemplate(data: { projectId: ID; templateName: string; templateType: string; file: File; scenario?: string; versionNo?: string; description?: string }) {
  if (useMock) return { templateId: Date.now(), projectId: data.projectId, fileId: 4300, status: 'SUCCESS' };
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('templateName', data.templateName);
  form.append('templateType', data.templateType);
  if (data.scenario) form.append('scenario', data.scenario);
  if (data.versionNo) form.append('versionNo', data.versionNo);
  if (data.description) form.append('description', data.description);
  form.append('file', data.file);
  return request.post('/report/templates', form);
}

export async function fetchReportTemplateVariables(templateId: ID) {
  if (useMock) return [{ name: 'projectName', label: '项目名称' }, { name: 'month', label: '月份' }];
  return request.get(`/report/templates/${templateId}/variables`);
}

export async function createReport(data: { projectId: ID; reportName: string; reportType: string; templateId: ID; knowledgeBaseIds?: ID[]; dataSourceIds?: ID[]; referenceFileIds?: ID[]; variables?: Record<string, unknown> }) {
  if (useMock) return { reportId: 10003, taskId: 9503, status: 'PROCESSING' };
  return request.post<{ reportId: ID; taskId: ID; status: string }>('/reports', data);
}

export async function fetchReports(params: PageQuery = {}) {
  const records = mockReports.filter((item) => !params.projectId || String(item.projectId) === String(params.projectId));
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: records.length, records } satisfies PageResult<ReportItem>;
  return request.get<PageResult<ReportItem>>('/reports', { params });
}

export async function fetchReportDetail(reportId: ID) {
  if (useMock) return mockReports.find((item) => String(item.reportId) === String(reportId)) || mockReports[0];
  return request.get<ReportItem>(`/reports/${reportId}`);
}

export async function regenerateReport(reportId: ID) {
  if (useMock) return { reportId, taskId: 9504, status: 'PROCESSING' };
  return request.post(`/reports/${reportId}/regenerate`);
}

export function downloadReport(reportId: ID, format: 'WORD' | 'PDF' = 'WORD', filename?: string) {
  if (useMock) return downloadFile('', { filename: filename || `report-${reportId}.${format === 'PDF' ? 'pdf' : 'docx'}`, data: 'mock report content' });
  return downloadFile(`/reports/${reportId}/download`, { params: { format }, filename });
}
