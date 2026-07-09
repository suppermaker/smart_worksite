<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { createKnowledgeBase, fetchKnowledgeBases, fetchKnowledgeDocuments, triggerDocumentIndex, uploadKnowledgeDocument } from '../../api/knowledge';
import { useProjectStore } from '../../stores/project';
import type { ID, KnowledgeBase, KnowledgeDocument } from '../../api/types';

const projectStore = useProjectStore();
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
const form = reactive({ name: '', description: '' });
const activeBase = computed(() => bases.value.find((item) => String(getBaseId(item)) === String(activeBaseId.value)) || null);

function getBaseId(base: KnowledgeBase) {
  return (base as KnowledgeBase & { knowledgeBaseId?: ID }).knowledgeBaseId || base.id;
}

async function loadBases(selectId?: ID) {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const projectId = projectStore.currentProject?.projectId;
    if (!projectId) {
      bases.value = [];
      docs.value = [];
      activeBaseId.value = '';
      return;
    }
    bases.value = await fetchKnowledgeBases(projectId);
    const nextId = selectId || activeBaseId.value;
    const matched = bases.value.find((item) => String(getBaseId(item)) === String(nextId));
    activeBaseId.value = matched ? getBaseId(matched) : (bases.value[0] ? getBaseId(bases.value[0]) : '');
    if (activeBaseId.value) await loadDocs(activeBaseId.value);
    else docs.value = [];
  } catch (err) {
    error.value = err instanceof Error ? err.message : '知识库数据加载失败，请检查后端知识库接口。';
  } finally {
    loading.value = false;
  }
}

async function loadDocs(baseId: ID) {
  docsLoading.value = true;
  docsError.value = '';
  try {
    docs.value = (await fetchKnowledgeDocuments(baseId)).records;
  } catch (err) {
    docsError.value = err instanceof Error ? err.message : '文档列表加载失败，请检查后端知识库文档接口。';
    docs.value = [];
  } finally {
    docsLoading.value = false;
  }
}

async function submitCreate() {
  if (!form.name.trim()) return ElMessage.warning('请填写知识库名称');
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning('请先选择项目');
  creating.value = true;
  error.value = '';
  try {
    const created = await createKnowledgeBase(projectId, { name: form.name.trim(), description: form.description.trim() });
    dialogVisible.value = false;
    form.name = '';
    form.description = '';
    ElMessage.success('知识库创建成功');
    await loadBases(getBaseId(created));
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`知识库创建失败，请检查后端知识库接口。${detail}`);
  } finally {
    creating.value = false;
  }
}

async function uploadDocs() {
  if (!activeBaseId.value) return ElMessage.warning('请先选择知识库');
  if (!selectedFiles.value.length) return ElMessage.warning('请先选择文件');
  uploading.value = true;
  docsError.value = '';
  try {
    for (const file of selectedFiles.value) await uploadKnowledgeDocument(activeBaseId.value, file);
    ElMessage.success('文档上传成功');
    selectedFiles.value = [];
    await loadDocs(activeBaseId.value);
    await loadBases(activeBaseId.value);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`知识库文档上传失败，请检查后端知识库文档接口或文件存储配置。${detail}`);
  } finally {
    uploading.value = false;
  }
}

async function handleIndex(row: KnowledgeDocument) {
  const documentId = row.documentId || row.id;
  if (!documentId) return ElMessage.warning('文档ID缺失，无法触发入库');
  indexingId.value = documentId;
  docsError.value = '';
  try {
    await triggerDocumentIndex(documentId);
    ElMessage.success('入库任务已提交');
    await loadDocs(activeBaseId.value);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`入库任务提交失败，请检查后端知识库入库接口。${detail}`);
  } finally {
    indexingId.value = '';
  }
}

watch(activeBaseId, (id) => { if (id) loadDocs(id); });
onMounted(loadBases);
</script>

<template>
  <div class="page" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <div class="page-header">
      <div>
        <h2 class="page-title">知识库管理</h2>
        <p class="page-desc">项目级知识库、文档解析状态和入库进度。</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true">新建知识库</el-button>
    </div>

    <EmptyState v-if="!loading && !bases.length" description="暂无知识库，请先创建知识库。" action-text="创建知识库" @action="dialogVisible = true" />
    <template v-else>
      <el-card class="work-card">
        <div class="base-list">
          <button v-for="base in bases" :key="getBaseId(base)" type="button" class="base-card" :class="{ active: String(activeBaseId) === String(getBaseId(base)) }" @click="activeBaseId = getBaseId(base)">
            <strong>{{ base.name }}</strong>
            <span>{{ base.description || '暂无描述' }}</span>
            <small>文档数量：{{ base.documentCount || 0 }}</small>
          </button>
        </div>
        <p v-if="activeBase" class="muted">
          当前知识库：{{ activeBase.name }} / <StatusTag :status="activeBase.status" />
        </p>
        <p class="muted">启用、停用、删除知识库：后端接口待提供。</p>
      </el-card>

      <el-card class="work-card">
        <h3 class="panel-title">上传文档</h3>
        <AppUpload accept=".doc,.docx,.pdf,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.txt,.md" :uploading="uploading" @change="selectedFiles = $event" />
        <el-button type="primary" style="margin-top: 12px" :loading="uploading" :disabled="!activeBaseId" @click="uploadDocs">上传到当前知识库</el-button>
      </el-card>

      <el-card class="work-card">
        <h3 class="panel-title">文档处理状态</h3>
        <el-alert v-if="docsError" :title="docsError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
        <AppTable
          :loading="docsLoading"
          :data="docs"
          :columns="[
            { prop: 'fileName', label: '文件名' },
            { prop: 'parseStatus', label: '解析状态', slot: 'parse', width: 110 },
            { prop: 'indexStatus', label: '入库状态', slot: 'index', width: 110 },
            { prop: 'failReason', label: '失败原因' },
            { prop: 'createdAt', label: '创建时间', width: 180 }
          ]"
        >
          <template #empty><EmptyState description="暂无知识库文档，可先上传项目资料。" /></template>
          <template #parse="{ row }"><StatusTag :status="row.parseStatus" /></template>
          <template #index="{ row }"><StatusTag :status="row.indexStatus" /></template>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" :loading="String(indexingId) === String(row.documentId || row.id)" @click="handleIndex(row)">入库处理</el-button>
            </template>
          </el-table-column>
        </AppTable>
      </el-card>
    </template>

    <el-dialog v-model="dialogVisible" title="新建知识库" width="520px">
      <el-form label-width="96px">
        <el-form-item label="知识库名称"><el-input v-model="form.name" placeholder="请输入知识库名称" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" placeholder="请输入知识库描述" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.base-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.base-card { text-align: left; border: 1px solid var(--sw-border); border-radius: 12px; background: #fff; padding: 14px; cursor: pointer; display: grid; gap: 8px; color: inherit; }
.base-card.active { border-color: var(--sw-primary); box-shadow: 0 0 0 3px rgba(30, 94, 255, 0.12); }
.base-card span, .base-card small, .muted { color: var(--sw-muted); }
.panel-title { margin: 0 0 12px; font-size: 16px; }
</style>
