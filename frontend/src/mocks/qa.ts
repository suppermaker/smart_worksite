import type { QaMessage, QaSession } from '../api/types';

export const mockQaSessions: QaSession[] = [
  { sessionId: 5001, projectId: 1001, title: '安全规范问答', status: 'ACTIVE', createdAt: '2026-07-04T09:00:00+08:00', updatedAt: '2026-07-04T09:30:00+08:00' },
  { sessionId: 5002, projectId: 1001, title: '质量验收咨询', status: 'ACTIVE', createdAt: '2026-07-04T10:00:00+08:00', updatedAt: '2026-07-04T10:30:00+08:00' }
];

export const mockQaMessages: QaMessage[] = [
  { messageId: 6001, sessionId: 5001, projectId: 1001, question: '系统可以回答哪些问题？', answer: '请围绕当前项目知识库提问，我会返回来源引用和可追溯结果。', routeMode: 'MIXED', references: [], status: 'SUCCESS', createdAt: '2026-07-04T09:01:00+08:00', updatedAt: '2026-07-04T09:01:00+08:00' }
];
