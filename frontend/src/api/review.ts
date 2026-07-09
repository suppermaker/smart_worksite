
import request from '../utils/request';
import { mockReviewRecord, mockReviewTemplates } from '../mocks/review';
import type { ID, ReviewRecord, ReviewTemplate } from './types';
import { useModuleMock } from './mock';
import { fetchLocalTemplates } from './template';

const useReviewRecordMock = useModuleMock('VITE_USE_REVIEW_RECORD_MOCK', true);
const useReviewTemplateMock = useModuleMock('VITE_USE_REVIEW_TEMPLATE_MOCK', false);
const mockRecords: ReviewRecord[] = [mockReviewRecord];

function mockId() {
  return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`);
}

function filterMockReviewTemplates(projectId?: ID) {
  return projectId ? mockReviewTemplates.filter((item) => String(item.projectId) === String(projectId)) : mockReviewTemplates;
}

export async function uploadReviewTemplate(data: { projectId: ID; templateName: string; templateType: string; file: File }) {
  if (useReviewTemplateMock) return { ...mockReviewTemplates[0], ...data, fileId: 4101, id: mockId(), templateId: mockId() } satisfies ReviewTemplate;
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('templateName', data.templateName);
  form.append('templateType', data.templateType);
  form.append('file', data.file);
  return request.post<ReviewTemplate>('/review/templates', form);
}

export async function fetchReviewTemplates(projectId?: ID) {
  if (useReviewTemplateMock) return filterMockReviewTemplates(projectId);
  try {
    const remote = await request.get<ReviewTemplate[]>('/review/templates', { params: { projectId } });
    return [...fetchLocalTemplates({ projectId, templateCategory: 'REVIEW' }), ...remote] as ReviewTemplate[];
  } catch {
    return [...fetchLocalTemplates({ projectId, templateCategory: 'REVIEW' }), ...filterMockReviewTemplates(projectId)] as ReviewTemplate[];
  }
}

export async function submitReviewRecord(data: { projectId: ID; templateId: ID; file: File }) {
  if (useReviewRecordMock) {
    const now = new Date().toISOString();
    const id = mockId();
    const record = { ...mockReviewRecord, id, recordId: id, taskId: id + 1, projectId: data.projectId, templateId: data.templateId, createdAt: now, updatedAt: now } satisfies ReviewRecord;
    mockRecords.unshift(record);
    return { recordId: record.recordId, taskId: record.taskId, status: record.status };
  }
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('templateId', String(data.templateId));
  form.append('file', data.file);
  return request.post<{ recordId: ID; taskId: ID; status: string }>('/review/records', form);
}

export async function fetchReviewRecord(recordId: ID) {
  if (useReviewRecordMock) return mockRecords.find((item) => String(item.recordId) === String(recordId)) || { ...mockReviewRecord, recordId } satisfies ReviewRecord;
  return request.get<ReviewRecord>(`/review/records/${recordId}`);
}
