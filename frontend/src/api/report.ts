import request from '../utils/request';
import { mockReports } from '../mocks/report';
import type { ID, PageQuery, PageResult, ReportItem } from './types';
import { fetchLocalTemplates, type TemplateItem } from './template';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_REPORT_MOCK', false);

interface ReportCreateRequest {
  projectId: ID;
  reportName?: string;
  reportType: string;
  templateId: ID;
  knowledgeBaseIds?: ID[];
  dataSourceIds?: ID[];
  referenceFileIds?: ID[];
  variables?: Record<string, unknown>;
}

export type ReportTemplate = TemplateItem;
type DownloadUrlResponse = string | { url: string; [key: string]: unknown };

export async function uploadReportTemplate(file: File, projectId: ID) {
  if (useMock) return { templateId: Date.now(), projectId, fileId: 4300, status: 'SUCCESS' } satisfies Partial<ReportTemplate>;
  const form = new FormData();
  form.append('projectId', String(projectId));
  form.append('file', file);
  return request.post<ReportTemplate>('/report/templates', form);
}

export async function fetchReportTemplates(projectId?: ID) {
  if (useMock) return [] as ReportTemplate[];
  try {
    const remote = await request.get<ReportTemplate[]>('/report/templates', { params: { projectId } });
    return [...fetchLocalTemplates({ projectId, templateCategory: 'REPORT' }), ...remote] as ReportTemplate[];
  } catch {
    return fetchLocalTemplates({ projectId, templateCategory: 'REPORT' }) as ReportTemplate[];
  }
}

export async function fetchReportTemplateVariables(templateId: ID) {
  return request.get<string[]>(`/report/templates/${templateId}/variables`);
}

export async function createReport(data: ReportCreateRequest) {
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
  return request.post<{ reportId: ID; taskId: ID; status: string }>(`/reports/${reportId}/regenerate`);
}

export async function fetchReportDownloadUrl(reportId: ID, format: 'WORD' | 'PDF' = 'WORD') {
  const result = await request.get<DownloadUrlResponse>(`/reports/${reportId}/download`, { params: { format } });
  return typeof result === 'string' ? result : result.url;
}

export async function downloadReport(reportId: ID, format: 'WORD' | 'PDF' = 'WORD', filename?: string) {
  if (useMock) {
    const blob = new Blob(['mock report content'], { type: 'text/plain;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename || `report-${reportId}.${format === 'PDF' ? 'pdf' : 'docx'}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
    return;
  }
  const url = await fetchReportDownloadUrl(reportId, format);
  if (!url) throw new Error('\u62a5\u544a\u4e0b\u8f7d\u5730\u5740\u4e3a\u7a7a\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u62a5\u544a\u4e0b\u8f7d\u63a5\u53e3');
  const link = document.createElement('a');
  link.href = url;
  if (filename) link.download = filename;
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}
