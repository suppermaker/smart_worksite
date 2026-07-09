import request from '../utils/request';
import { mockOcrRecord } from '../mocks/ocr';
import type { ID, OcrField, OcrRecord } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_OCR_MOCK', true);
const mockRecords: OcrRecord[] = [mockOcrRecord];

function mockId() {
  return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`);
}

export async function submitOcrRecord(data: { projectId: ID; ocrType: string; file: File; customFields?: string }) {
  if (useMock) {
    const now = new Date().toISOString();
    const id = mockId();
    const custom = data.ocrType === 'CUSTOM' && data.customFields
      ? data.customFields.split(/[?,]/).map((name, index) => ({ fieldName: name.trim(), fieldValue: '', confidence: 0, location: `\u81ea\u5b9a\u4e49${index + 1}` })).filter((item) => item.fieldName)
      : [];
    const record = { ...mockOcrRecord, id, recordId: id, taskId: id + 1, projectId: data.projectId, ocrType: data.ocrType as OcrRecord['ocrType'], fields: custom.length ? custom : mockOcrRecord.fields.map((item) => ({ ...item })), createdAt: now, updatedAt: now } satisfies OcrRecord;
    mockRecords.unshift(record);
    return { recordId: record.recordId, taskId: record.taskId, status: record.status };
  }
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('ocrType', data.ocrType);
  form.append('file', data.file);
  if (data.customFields) form.append('customFields', data.customFields);
  return request.post<{ recordId: ID; taskId: ID; status: string }>('/ocr/records', form);
}

export async function fetchOcrRecord(recordId: ID) {
  if (useMock) return mockRecords.find((item) => String(item.recordId) === String(recordId)) || { ...mockOcrRecord, recordId } satisfies OcrRecord;
  return request.get<OcrRecord>(`/ocr/records/${recordId}`);
}

export async function updateOcrFields(recordId: ID, fields: OcrField[]) {
  if (useMock) {
    const item = mockRecords.find((record) => String(record.recordId) === String(recordId));
    if (item) {
      item.fields = fields.map((field) => ({ ...field }));
      item.updatedAt = new Date().toISOString();
    }
    return { recordId, fields, status: 'SUCCESS' };
  }
  return request.put(`/ocr/records/${recordId}/fields`, { fields });
}
