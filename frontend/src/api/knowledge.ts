import request from '../utils/request';
import { mockKnowledgeBases, mockKnowledgeDocuments } from '../mocks/knowledge';
import type { ID, KnowledgeBase, KnowledgeDocument, PageQuery, PageResult } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_KNOWLEDGE_MOCK', true);
const mockBaseStorageKey = 'smart_worksite_mock_knowledge_bases';
const mockDocumentStorageKey = 'smart_worksite_mock_knowledge_documents';

function readMockState<T>(key: string, fallback: T[]) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) as T[] : [...fallback];
  } catch {
    return [...fallback];
  }
}

function saveMockState() {
  localStorage.setItem(mockBaseStorageKey, JSON.stringify(mockBaseState));
  localStorage.setItem(mockDocumentStorageKey, JSON.stringify(mockDocumentState));
}

function createMockId() {
  return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`);
}

const mockBaseState = readMockState<KnowledgeBase>(mockBaseStorageKey, mockKnowledgeBases);
const mockDocumentState = readMockState<KnowledgeDocument>(mockDocumentStorageKey, mockKnowledgeDocuments);

export async function fetchKnowledgeBases(projectId: ID) {
  if (useMock) return mockBaseState.filter((item) => String(item.projectId) === String(projectId));
  return request.get<KnowledgeBase[]>(`/projects/${projectId}/knowledge-bases`);
}

export async function createKnowledgeBase(projectId: ID, data: Pick<KnowledgeBase, 'name' | 'description'>) {
  if (useMock) {
    const now = new Date().toISOString();
    const id = createMockId();
    const created = { id, projectId, taskId: id + 1, fileId: 0, name: data.name, description: data.description || '', status: 'ACTIVE', documentCount: 0, createdAt: now, updatedAt: now } satisfies KnowledgeBase;
    mockBaseState.unshift(created);
    saveMockState();
    return created;
  }
  return request.post<KnowledgeBase>(`/projects/${projectId}/knowledge-bases`, data);
}

export async function uploadKnowledgeDocument(knowledgeBaseId: ID, file: File) {
  if (useMock) {
    const now = new Date().toISOString();
    const base = mockBaseState.find((item) => String(item.id) === String(knowledgeBaseId));
    const id = createMockId();
    const created = {
      id,
      documentId: id,
      projectId: base?.projectId || 0,
      knowledgeBaseId,
      taskId: id + 1,
      fileId: id + 2,
      fileName: file.name,
      parseStatus: 'SUCCESS',
      indexStatus: 'PENDING',
      status: 'PENDING',
      failReason: '',
      createdAt: now,
      updatedAt: now
    } satisfies KnowledgeDocument;
    mockDocumentState.unshift(created);
    if (base) base.documentCount = mockDocumentState.filter((item) => String(item.knowledgeBaseId) === String(knowledgeBaseId)).length;
    saveMockState();
    return created;
  }
  const form = new FormData();
  form.append('file', file);
  return request.post<KnowledgeDocument>(`/knowledge-bases/${knowledgeBaseId}/documents`, form);
}

export async function triggerDocumentIndex(documentId: ID) {
  if (useMock) {
    const item = mockDocumentState.find((doc) => String(doc.documentId || doc.id) === String(documentId));
    if (item) {
      item.indexStatus = 'PROCESSING';
      item.status = 'PROCESSING';
      item.updatedAt = new Date().toISOString();
      saveMockState();
    }
    return { taskId: 9202, status: 'PROCESSING' };
  }
  return request.post<{ taskId: ID; status: string }>(`/knowledge-documents/${documentId}/index`);
}

export async function fetchKnowledgeDocuments(knowledgeBaseId: ID, params: PageQuery = {}) {
  const records = mockDocumentState.filter((item) => String(item.knowledgeBaseId) === String(knowledgeBaseId));
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: records.length, records } satisfies PageResult<KnowledgeDocument>;
  return request.get<PageResult<KnowledgeDocument>>(`/knowledge-bases/${knowledgeBaseId}/documents`, { params });
}
