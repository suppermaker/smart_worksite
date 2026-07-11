import request from '../utils/request';
import { mockKnowledgeBases, mockKnowledgeDocuments } from '../mocks/knowledge';
import type { ID, KnowledgeBase, KnowledgeDocument, PageQuery, PageResult } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function fetchKnowledgeBases(projectId: ID) {
  if (useMock) return mockKnowledgeBases.filter((item) => String(item.projectId) === String(projectId));
  const page = await request.get<PageResult<KnowledgeBase>>(`/projects/${projectId}/knowledge-bases`);
  return page.records;
}

export async function createKnowledgeBase(projectId: ID, data: Pick<KnowledgeBase, 'name' | 'description' | 'domain'>) {
  if (useMock) return { ...mockKnowledgeBases[0], ...data, knowledgeBaseId: Date.now(), projectId } satisfies KnowledgeBase;
  return request.post<KnowledgeBase>(`/projects/${projectId}/knowledge-bases`, data);
}

export async function uploadKnowledgeDocument(knowledgeBaseId: ID, file: File) {
  if (useMock) return { ...mockKnowledgeDocuments[0], documentId: Date.now(), knowledgeBaseId, title: file.name } satisfies KnowledgeDocument;
  const form = new FormData();
  form.append('file', file);
  return request.post<KnowledgeDocument>(`/knowledge-bases/${knowledgeBaseId}/documents`, form);
}

export async function triggerDocumentIndex(documentId: ID) {
  if (useMock) return { ...mockKnowledgeDocuments[0], documentId, indexStatus: 'QUEUED' } satisfies KnowledgeDocument;
  return request.post<KnowledgeDocument>(`/knowledge-documents/${documentId}/index`);
}

export async function fetchKnowledgeDocuments(knowledgeBaseId: ID, params: PageQuery = {}) {
  const records = mockKnowledgeDocuments.filter((item) => String(item.knowledgeBaseId) === String(knowledgeBaseId));
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: records.length, records } satisfies PageResult<KnowledgeDocument>;
  return request.get<PageResult<KnowledgeDocument>>(`/knowledge-bases/${knowledgeBaseId}/documents`, { params });
}
