<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { createKnowledgeBase, fetchKnowledgeBases, fetchKnowledgeDocuments, triggerDocumentIndex, uploadKnowledgeDocument } from '../../api/knowledge';
import { createFileParse, fetchLatestFileParseRecord } from '../../api/file';
import { useProjectStore } from '../../stores/project';
import { useUserStore } from '../../stores/user';
import type { ID, KnowledgeBase, KnowledgeDocument } from '../../api/types';

const projectStore = useProjectStore();
const userStore = useUserStore();
const loading = ref(false);
const docsLoading = ref(false);
const creating = ref(false);
const uploading = ref(false);
const error = ref('');
const docsError = ref('');
const bases = ref<KnowledgeBase[]>([]);
const docs = ref<KnowledgeDocument[]>([]);
const activeBaseId = ref<ID>('');
const dialogVisible = ref(false);
const selectedFiles = ref<File[]>([]);
const indexingId = ref<ID>('');
const parsingId = ref<ID>('');
const form = reactive({ name: '', description: '' });
const activeBase = computed(() => bases.value.find((item) => String(item.knowledgeBaseId) === String(activeBaseId.value)) || null);
const canManageKnowledge = computed(() => userStore.hasPermission('knowledge:manage'));
const knowledgeManageTip = '当前账号没有知识库管理权限';
const indexableStatuses = new Set(['PENDING', 'FAILED']);
const parseableExts = new Set(['png', 'jpg', 'jpeg', 'webp', 'pdf', 'doc', 'docx']);

function normalizeStatus(status?: string) {
  return (status || '').toUpperCase();
}

function canSubmitIndex(row: KnowledgeDocument) {
  return canManageKnowledge.value && indexableStatuses.has(normalizeStatus(row.indexStatus));
}

function fileExt(name?: string) {
  const matched = (name || '').trim().toLowerCase().match(/\.([a-z0-9]+)$/);
  return matched?.[1] || '';
}

function canParseDocument(row: KnowledgeDocument) {
  return canManageKnowledge.value && Boolean(row.fileId && parseableExts.has(fileExt(row.title)));
}

function parseTargetFormat(row: KnowledgeDocument) {
  return ['png', 'jpg', 'jpeg', 'webp'].includes(fileExt(row.title)) ? 'TEXT' : 'MARKDOWN';
}

function parseDisabledReason(row: KnowledgeDocument) {
  if (!row.fileId) return '文档缺少 fileId，无法创建文件解析任务';
  return `当前文件格式 .${fileExt(row.title) || 'unknown'} 暂不支持解析，无法作为知识库入库来源`;
}

function indexActionText(row: KnowledgeDocument) {
  const status = normalizeStatus(row.indexStatus);
  if (status === 'INDEXING') return '入库中';
  if (status === 'SUCCESS') return '已入库';
  if (status === 'FAILED') return '重新入库';
  return '入库处理';
}

async function loadBases(selectId?: ID) {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const projectId = projectStore.currentProject?.projectId;
    if (!projectId) { bases.value = []; docs.value = []; activeBaseId.value = ''; return; }
    bases.value = await fetchKnowledgeBases(projectId);
    const nextId = selectId || activeBaseId.value;
    const matched = bases.value.find((item) => String(item.knowledgeBaseId) === String(nextId));
    activeBaseId.value = matched ? matched.knowledgeBaseId : (bases.value[0]?.knowledgeBaseId || '');
    if (activeBaseId.value) await loadDocs(activeBaseId.value); else docs.value = [];
  } catch (err) {
    error.value = err instanceof Error ? err.message : '知识库数据加载失败，请检查后端知识库接口。';
  } finally { loading.value = false; }
}

async function loadDocs(baseId: ID) {
  docsLoading.value = true;
  docsError.value = '';
  try { docs.value = (await fetchKnowledgeDocuments(baseId)).records; }
  catch (err) { docsError.value = err instanceof Error ? err.message : '文档列表加载失败，请检查后端知识库文档接口。'; docs.value = []; }
  finally { docsLoading.value = false; }
}

async function submitCreate() {
  if (!canManageKnowledge.value) return ElMessage.warning(knowledgeManageTip);
  if (!form.name.trim()) return ElMessage.warning('请填写知识库名称');
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning('请先选择项目');
  creating.value = true;
  error.value = '';
  try {
    const created = await createKnowledgeBase(projectId, { name: form.name.trim(), description: form.description.trim() });
    dialogVisible.value = false; form.name = ''; form.description = '';
    ElMessage.success('知识库创建成功');
    await loadBases(created.knowledgeBaseId);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`知识库创建失败，请检查后端知识库接口。${detail}`);
  } finally { creating.value = false; }
}

async function uploadDocs() {
  if (!canManageKnowledge.value) return ElMessage.warning(knowledgeManageTip);
  if (!activeBaseId.value) return ElMessage.warning('请先选择知识库');
  if (!selectedFiles.value.length) return ElMessage.warning('请先选择文件');
  uploading.value = true;
  docsError.value = '';
  try {
    const unsupported = selectedFiles.value.filter((file) => !parseableExts.has(fileExt(file.name)));
    if (unsupported.length) {
      ElMessage.error(`以下文件暂不支持知识库解析入库：${unsupported.map((file) => file.name).join('、')}`);
      return;
    }
    for (const file of selectedFiles.value) await uploadKnowledgeDocument(activeBaseId.value, file);
    ElMessage.success('文档上传成功');
    selectedFiles.value = [];
    await loadDocs(activeBaseId.value);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`知识库文档上传失败，请检查后端知识库文档接口或文件存储配置。${detail}`);
  } finally { uploading.value = false; }
}

async function handleParse(row: KnowledgeDocument) {
  if (!canManageKnowledge.value) return ElMessage.warning(knowledgeManageTip);
  if (!row.fileId) return ElMessage.warning('文档缺少 fileId，无法解析');
  if (!canParseDocument(row)) return ElMessage.warning(parseDisabledReason(row));
  parsingId.value = row.documentId;
  docsError.value = '';
  try {
    await createFileParse(row.fileId, { projectId: row.projectId, targetFormat: parseTargetFormat(row) });
    ElMessage.success('文件解析任务已提交，解析成功后再执行入库');
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`文件解析任务提交失败。${detail}`);
  } finally {
    parsingId.value = '';
  }
}

async function handleIndex(row: KnowledgeDocument) {
  if (!canManageKnowledge.value) return ElMessage.warning(knowledgeManageTip);
  const documentId = row.documentId;
  if (!documentId) return ElMessage.warning('文档ID缺失，无法触发入库');
  if (!row.fileId) return ElMessage.warning('文档缺少 fileId，无法触发入库');
  if (!canSubmitIndex(row)) return ElMessage.warning(`当前文档状态为 ${row.indexStatus}，不能重复提交入库任务`);
  try {
    const latestParse = await fetchLatestFileParseRecord(row.fileId, row.projectId);
    if (normalizeStatus(latestParse.status) !== 'SUCCESS') {
      ElMessage.warning(`最新文件解析状态为 ${latestParse.status}，请等待解析成功后再入库`);
      return;
    }
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`入库前未找到可用的成功解析结果，请先点击“解析文件”。${detail}`);
    return;
  }
  indexingId.value = documentId;
  docsError.value = '';
  try { await triggerDocumentIndex(documentId); ElMessage.success('入库任务已提交'); await loadDocs(activeBaseId.value); }
  catch (err) { const detail = err instanceof Error && err.message ? ` ${err.message}` : ''; ElMessage.error(`入库任务提交失败，请检查后端知识库入库接口。${detail}`); }
  finally { indexingId.value = ''; }
}

watch(activeBaseId, (id) => { if (id) loadDocs(id); });
onMounted(loadBases);
</script>

<template>
  <div class="page" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <div class="page-header"><div><h2 class="page-title">知识库管理</h2><p class="page-desc">项目级知识库、文档解析状态和入库进度。</p></div><el-button v-if="canManageKnowledge" type="primary" @click="dialogVisible = true">新建知识库</el-button></div>
    <EmptyState v-if="!loading && !bases.length" description="暂无知识库，请联系知识库管理员创建。" :action-text="canManageKnowledge ? '创建知识库' : undefined" @action="dialogVisible = true" />
    <template v-else>
      <el-card class="work-card"><div class="base-list"><button v-for="base in bases" :key="base.knowledgeBaseId" type="button" class="base-card" :class="{ active: String(activeBaseId) === String(base.knowledgeBaseId) }" @click="activeBaseId = base.knowledgeBaseId"><strong>{{ base.name }}</strong><span>{{ base.description || '暂无描述' }}</span><small>领域：{{ base.domain || '-' }}</small></button></div><p v-if="activeBase" class="muted">当前知识库：{{ activeBase.name }} / <StatusTag :status="activeBase.status" /></p></el-card>
      <el-card class="work-card"><h3 class="panel-title">上传文档</h3><AppUpload v-model="selectedFiles" accept=".doc,.docx,.pdf,.jpg,.jpeg,.png,.webp" tip="知识库入库依赖文件解析结果，当前支持 Word、PDF 和图片文件" :uploading="uploading"  /><el-button type="primary" style="margin-top: 12px" :loading="uploading" :disabled="!activeBaseId" @click="uploadDocs">上传到当前知识库</el-button></el-card>
      <el-card class="work-card"><h3 class="panel-title">文档处理状态</h3><el-alert v-if="docsError" :title="docsError" type="error" show-icon :closable="false" style="margin-bottom: 12px" /><AppTable :loading="docsLoading" :data="docs" :columns="[{ prop: 'title', label: '文档名称' }, { prop: 'sourceType', label: '来源类型', width: 120 }, { prop: 'indexStatus', label: '入库状态', slot: 'index', width: 110 }, { prop: 'errorMessage', label: '说明' }, { prop: 'createdAt', label: '创建时间', width: 180 }]"><template #empty><EmptyState description="暂无知识库文档，可先上传项目资料。" /></template><template #index="{ row }"><StatusTag :status="row.indexStatus" /></template><el-table-column label="操作" width="240"><template #default="{ row }"><el-tooltip :disabled="canParseDocument(row)" :content="parseDisabledReason(row)"><span><el-button link type="primary" :loading="String(parsingId) === String(row.documentId)" :disabled="!canParseDocument(row)" @click="handleParse(row)">解析文件</el-button></span></el-tooltip><el-button link type="primary" :loading="String(indexingId) === String(row.documentId)" :disabled="!canSubmitIndex(row)" @click="handleIndex(row)">{{ indexActionText(row) }}</el-button></template></el-table-column></AppTable></el-card>
    </template>
    <el-dialog v-model="dialogVisible" title="新建知识库" width="520px"><el-form label-width="96px"><el-form-item label="知识库名称"><el-input v-model="form.name" placeholder="请输入知识库名称" /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea" placeholder="请输入知识库描述" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.base-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.base-card { text-align: left; border: 1px solid var(--sw-border); border-radius: 12px; background: #fff; padding: 14px; cursor: pointer; display: grid; gap: 8px; color: inherit; }
.base-card.active { border-color: var(--sw-primary); box-shadow: 0 0 0 3px rgba(30, 94, 255, 0.12); }
.base-card span, .base-card small, .muted { color: var(--sw-muted); }
.panel-title { margin: 0 0 12px; font-size: 16px; }
</style>
