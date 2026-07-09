import request from '../utils/request';
import { mockQaMessages, mockQaSessions, type QaMessageWithExtra } from '../mocks/qa';
import type { ID, QaMessage, QaSession } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_QA_MOCK', true);
const mockSessions = [...mockQaSessions];
const mockMessages: QaMessageWithExtra[] = [...mockQaMessages];
const feedbackState: Record<string, boolean> = {};

function mockId() {
  return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`);
}

function buildAssistantMessage(sessionId: ID, projectId: ID, question: string): QaMessageWithExtra {
  const now = new Date().toISOString();
  const id = mockId();
  return {
    id,
    messageId: id,
    sessionId,
    projectId,
    taskId: 9301,
    fileId: 0,
    role: 'assistant',
    question,
    answer: `\u5df2\u7ed3\u5408\u9879\u76ee\u77e5\u8bc6\u5e93\u5206\u6790\uff1a${question}\u3002\u5efa\u8bae\u4f18\u5148\u6838\u67e5\u65b9\u6848\u4ea4\u5e95\u3001\u73b0\u573a\u9a8c\u6536\u8bb0\u5f55\u548c\u6574\u6539\u95ed\u73af\u60c5\u51b5\u3002`,
    content: `\u5df2\u7ed3\u5408\u9879\u76ee\u77e5\u8bc6\u5e93\u5206\u6790\uff1a${question}\u3002\u5efa\u8bae\u4f18\u5148\u6838\u67e5\u65b9\u6848\u4ea4\u5e95\u3001\u73b0\u573a\u9a8c\u6536\u8bb0\u5f55\u548c\u6574\u6539\u95ed\u73af\u60c5\u51b5\u3002`,
    routeMode: 'MIXED',
    status: 'SUCCESS',
    createdAt: now,
    updatedAt: now,
    references: [
      { title: '\u9879\u76ee\u5b89\u5168\u89c4\u8303\u5e93', sourceType: 'KNOWLEDGE', page: '\u7b2c5\u9875', score: 0.91, documentId: 3001 },
      { title: '\u65bd\u5de5\u8d28\u91cf\u9a8c\u6536\u5e93', sourceType: 'KNOWLEDGE', page: '\u7b2c2\u7ae0', score: 0.84, documentId: 3002 }
    ],
    sqlResult: { sql: 'select risk_level,count(*) as total from check_items group by risk_level', table: 'check_items', rows: [{ risk_level: 'HIGH', total: 2 }, { risk_level: 'MEDIUM', total: 5 }] }
  };
}

export async function createQaSession(data: { projectId: ID; title?: string }) {
  if (useMock) {
    const now = new Date().toISOString();
    const id = mockId();
    const created = { id, sessionId: id, projectId: data.projectId, taskId: 0, fileId: 0, title: data.title || '\u65b0\u5efa\u4f1a\u8bdd', status: 'ACTIVE', createdAt: now, updatedAt: now } satisfies QaSession;
    mockSessions.unshift(created);
    return created;
  }
  return request.post<QaSession>('/qa/sessions', data);
}

export async function fetchQaSessions(projectId: ID) {
  if (useMock) return mockSessions.filter((item) => String(item.projectId) === String(projectId));
  return request.get<QaSession[]>('/qa/sessions', { params: { projectId } });
}

export async function fetchQaMessages(sessionId: ID) {
  if (useMock) return mockMessages.filter((item) => String(item.sessionId) === String(sessionId));
  return request.get<QaMessage[]>(`/qa/sessions/${sessionId}/messages`);
}

export async function sendQuestion(sessionId: ID, data: { projectId: ID; question: string; routeMode?: string; dataSourceIds?: ID[]; knowledgeBaseIds?: ID[] }) {
  if (useMock) {
    const answer = buildAssistantMessage(sessionId, data.projectId, data.question);
    mockMessages.push(answer);
    return answer;
  }
  return request.post<QaMessage>(`/qa/sessions/${sessionId}/messages`, data);
}

export async function submitFeedback(messageId: ID, useful: boolean) {
  if (useMock) {
    feedbackState[String(messageId)] = useful;
    return { messageId, useful };
  }
  return request.post(`/qa/messages/${messageId}/feedback`, { useful });
}
