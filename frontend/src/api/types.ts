export type ID = string | number;
export type Status = 'PENDING' | 'QUEUED' | 'RUNNING' | 'PROCESSING' | 'COMPLETED' | 'SUCCESS' | 'FAILED' | 'RETRYING' | 'CANCELED' | 'ACTIVE' | 'ENABLED' | 'DISABLED' | 'ARCHIVED';

export interface PageQuery {
  pageNo?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  projectId?: ID;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface UserInfo {
  id: ID;
  username: string;
  realName: string;
  roles: string[];
  permissions: string[];
  buttonPermissions?: string[];
  projects?: UserProject[];
  defaultProjectId?: ID;
  createdAt: string;
  updatedAt: string;
}

export interface UserProject {
  projectId: ID;
  projectName: string;
  projectCode: string;
  status: string;
  projectRole: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: UserInfo;
}

export interface ProjectItem {
  projectId: ID;
  projectName: string;
  projectCode: string;
  location?: string;
  status: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectCreateForm {
  projectName: string;
  projectCode: string;
  location?: string;
  description?: string;
}

export interface ProjectUpdateForm {
  projectName: string;
  projectCode: string;
  location?: string;
  description?: string;
}

export interface FileObject {
  fileId: ID;
  objectName?: string;
  projectId: ID;
  bizType?: string;
  bizId?: ID;
  fileName: string;
  fileExt?: string;
  contentType?: string;
  fileSize: number;
  fileHash?: string;
  status: Status;
  metadata?: string;
  previewSupported?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FileAccessUrl {
  fileId: ID;
  url: string;
  expiresAt: string;
  previewSupported?: boolean;
}

export interface KnowledgeBase {
  knowledgeBaseId: ID;
  projectId: ID;
  domain?: string;
  name: string;
  description: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocument {
  documentId: ID;
  projectId: ID;
  knowledgeBaseId: ID;
  fileId?: ID;
  title: string;
  sourceType?: string;
  indexStatus: Status;
  taskId?: ID;
  errorMessage?: string;
  versionNo?: number;
  createdAt: string;
  updatedAt: string;
}

export interface QaSession {
  sessionId: ID;
  projectId: ID;
  title: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface QaReference {
  title: string;
  sourceType: 'KNOWLEDGE' | 'DATABASE' | 'POLICY';
  page?: string;
  score: number;
  documentId?: ID;
}

export interface QaMessage {
  messageId: ID;
  sessionId: ID;
  projectId: ID;
  question?: string;
  answer?: string;
  routeMode?: 'AUTO' | 'MODEL' | 'KNOWLEDGE' | 'DATABASE' | 'MIXED';
  references?: QaReference[];
  feedback?: Record<string, unknown>;
  status: Status;
  needClarification?: boolean;
  clarificationQuestions?: string[];
  providerTraceId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewTemplate {
  id: ID;
  templateId: ID;
  projectId: ID;
  taskId?: ID;
  fileId: ID;
  templateName: string;
  templateType: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewIssue {
  issueId: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  location: string;
  ruleName: string;
  description: string;
  suggestion: string;
}

export interface ReviewRecord {
  recordId: ID;
  projectId: ID;
  taskId?: ID;
  fileId?: ID;
  templateId: ID;
  status: Status;
  issues: ReviewIssue[];
  result?: Record<string, unknown>;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReportItem {
  id: ID;
  reportId: ID;
  projectId: ID;
  taskId: ID;
  fileId?: ID;
  reportName: string;
  reportType: string;
  templateId: ID;
  version: string;
  status: Status;
  progress: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserItem {
  id: ID;
  username: string;
  displayName: string;
  phone?: string;
  email?: string;
  status: string;
  roles: string[];
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserCreateForm {
  username: string;
  password: string;
  displayName: string;
  phone?: string;
  email?: string;
  roleCodes?: string[];
}

export interface UserUpdateForm {
  displayName: string;
  phone?: string;
  email?: string;
  roleCodes?: string[];
}

export interface RoleItem {
  id: ID;
  roleCode: string;
  roleName: string;
  description?: string;
  status: string;
  permissionIds: ID[];
  createdAt: string;
  updatedAt: string;
}

export interface RoleCreateForm {
  roleCode: string;
  roleName: string;
  description?: string;
  permissionIds?: ID[];
}

export interface RoleUpdateForm {
  roleName: string;
  description?: string;
  permissionIds?: ID[];
}

export interface PermissionItem {
  id: ID;
  permissionCode: string;
  permissionName: string;
  permissionType: string;
  parentId?: ID;
}

export interface ProjectMemberItem {
  id: ID;
  projectId: ID;
  userId: ID;
  username: string;
  displayName: string;
  projectRole: string;
  status: string;
  createdAt: string;
}

export interface OcrField {
  fieldName: string;
  fieldValue: string;
  confidence: number;
  location: string;
}

export interface OcrRecord {
  id: ID;
  recordId: ID;
  projectId: ID;
  taskId: ID;
  fileId: ID;
  ocrType: 'ID_CARD' | 'LICENSE_PLATE' | 'INVOICE' | 'CONTRACT' | 'CUSTOM';
  status: Status;
  progress: number;
  fields: OcrField[];
  createdAt: string;
  updatedAt: string;
}

export interface TaskStageLog {
  id: ID;
  taskId: ID;
  attemptNo?: number;
  stageCode: string;
  status: Status;
  inputSummary?: string;
  outputSummary?: string;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  costMs?: number;
  createdAt: string;
}

export interface TaskDetail {
  taskId: ID;
  projectId: ID;
  taskType: string;
  bizType?: string;
  bizId?: ID;
  status: Status;
  currentStage?: string;
  retryCount?: number;
  maxRetryCount?: number;
  cancelRequested?: boolean;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
  updatedAt: string;
}
