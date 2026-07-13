import request from '../utils/request';
import { mockQaMessages, mockQaSessions, type QaMessageWithExtra } from '../mocks/qa';
import type { ID, PageResult, QaMessage, QaSession } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_QA_MOCK', false);
const mockSessions = [...mockQaSessions];
const mockMessages: QaMessageWithExtra[] = [...mockQaMessages];
const feedbackState: Record<string, boolean> = {};

function mockId() { return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`); }

function buildAssistantMessage(sessionId: ID, projectId: ID, question: string): QaMessageWithExtra {
  const now = new Date().toISOString();
  const id = mockId();
  return {
    messageId: id,
    sessionId,
    projectId,
    question,
    answer: `已结合项目知识库分析：${question}。建议优先核查方案交底、现场验收记录和整改闭环情况。`,
    routeMode: 'MIXED',
    status: 'SUCCESS',
    createdAt: now,
    updatedAt: now,
    references: [
      { title: '项目安全规范库', sourceType: 'KNOWLEDGE', page: '第5页', score: 0.91, documentId: 3001 },
      { title: '施工质量验收库', sourceType: 'KNOWLEDGE', page: '第2章', score: 0.84, documentId: 3002 }
    ],
    sqlResult: { sql: 'select risk_level,count(*) as total from check_items group by risk_level', table: 'check_items', rows: [{ risk_level: 'HIGH', total: 2 }, { risk_level: 'MEDIUM', total: 5 }] }
  };
}

export async function createQaSession(data: { projectId: ID; title?: string }) {
  if (useMock) {
    const now = new Date().toISOString();
    const id = mockId();
    const created = { sessionId: id, projectId: data.projectId, title: data.title || '新建会话', status: 'ACTIVE', createdAt: now, updatedAt: now } satisfies QaSession;
    mockSessions.unshift(created);
    return created;
  }
  return request.post<QaSession>('/qa/sessions', data);
}

export async function fetchQaSessions(projectId: ID) {
  if (useMock) return mockSessions.filter((item) => String(item.projectId) === String(projectId));
  const page = await request.get<PageResult<QaSession>>('/qa/sessions', { params: { projectId } });
  return page.records;
}

export async function fetchQaMessages(sessionId: ID) {
  if (useMock) return mockMessages.filter((item) => String(item.sessionId) === String(sessionId));
  return request.get<QaMessage[]>(`/qa/sessions/${sessionId}/messages`);
}

export async function sendQuestion(sessionId: ID, data: { question: string; routeMode?: string; dataSourceIds?: ID[]; knowledgeBaseIds?: ID[] }, projectId?: ID) {
  if (useMock) {
    const mockProjectId = projectId || mockSessions.find((item) => String(item.sessionId) === String(sessionId))?.projectId;
    if (!mockProjectId) throw new Error(`问答 mock 会话缺少项目编号：${sessionId}`);
    const answer = buildAssistantMessage(sessionId, mockProjectId, data.question);
    mockMessages.push(answer);
    return answer;
  }
  return request.post<QaMessage>(`/qa/sessions/${sessionId}/messages`, data);
}

export async function submitFeedback(messageId: ID, useful: boolean) {
  if (useMock) { feedbackState[String(messageId)] = useful; return { messageId, useful }; }
  return request.post(`/qa/messages/${messageId}/feedback`, { feedbackType: useful ? 'LIKE' : 'DISLIKE', extra: { useful } });
}
