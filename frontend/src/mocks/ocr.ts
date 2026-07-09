import type { OcrRecord } from '../api/types';

export const mockOcrRecord: OcrRecord = {
  id: 11001,
  recordId: 11001,
  projectId: 1001,
  taskId: 9601,
  fileId: 4401,
  ocrType: 'CONTRACT',
  status: 'PROCESSING',
  progress: 88,
  fields: [
    { fieldName: '\u5408\u540c\u7f16\u53f7', fieldValue: 'HT-2026-001', confidence: 0.98, location: '\u7b2c1\u9875' },
    { fieldName: '\u7532\u65b9', fieldValue: '\u67d0\u67d0\u5efa\u8bbe\u5355\u4f4d', confidence: 0.94, location: '\u7b2c1\u9875' },
    { fieldName: '\u4e59\u65b9', fieldValue: '\u67d0\u67d0\u65bd\u5de5\u5355\u4f4d', confidence: 0.91, location: '\u7b2c1\u9875' },
    { fieldName: '\u5408\u540c\u91d1\u989d', fieldValue: '12800.00', confidence: 0.84, location: '\u7b2c2\u9875' }
  ],
  createdAt: '2026-07-04T10:00:00+08:00',
  updatedAt: '2026-07-04T10:08:00+08:00'
};
