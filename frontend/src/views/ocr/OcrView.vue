<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { deleteOcrRecord, fetchOcrDownloadResult, fetchOcrRecord, fetchOcrRecords, retryOcrRecord, submitOcrRecord, updateOcrFields } from '../../api/ocr';
import { fetchTaskStages } from '../../api/task';
import { useProjectStore } from '../../stores/project';
import { useUserStore } from '../../stores/user';
import type { ID, OcrRecord, TaskStageLog } from '../../api/types';

const projectStore = useProjectStore();
const userStore = useUserStore();
const loading = ref(false);
const recordsLoading = ref(false);
const submitting = ref(false);
const error = ref('');
const notice = ref('');
const record = ref<OcrRecord | null>(null);
const records = ref<OcrRecord[]>([]);
const logs = ref<TaskStageLog[]>([]);
const total = ref(0);
const retryingId = ref<ID | ''>('');
const deletingId = ref<ID | ''>('');
const downloadingId = ref<ID | ''>('');
const ocrType = ref('CUSTOM');
const customFields = ref(JSON.stringify([
  { fieldKey: 'partyA', fieldName: '甲方', description: '合同中的甲方名称', required: true, valueType: 'TEXT' },
  { fieldKey: 'partyB', fieldName: '乙方', description: '合同中的乙方名称', required: true, valueType: 'TEXT' },
  { fieldKey: 'contractAmount', fieldName: '合同金额', description: '合同总金额', required: false, valueType: 'AMOUNT' }
], null, 2));
const file = ref<File | null>(null);
const invoiceType = ref('VAT_SPECIAL');
const query = reactive({ pageNo: 1, pageSize: 10, status: '', ocrType: '' });
const currentProjectId = computed(() => projectStore.currentProject?.projectId);
const canManageOcr = computed(() => userStore.hasPermission('ocr:view'));
const canSubmit = computed(() => Boolean(canManageOcr.value && currentProjectId.value && file.value && !submitting.value));
const ocrTypes = [
  { label: '身份证识别', value: 'ID_CARD' },
  { label: '车牌识别', value: 'LICENSE_PLATE' },
  { label: '发票识别', value: 'INVOICE' },
  { label: '自定义字段识别', value: 'CUSTOM' }
];
const invoiceTypes = [
  { label: '增值税专用发票', value: 'VAT_SPECIAL' },
  { label: '增值税普通发票', value: 'VAT_NORMAL' }
];
const retryableStatuses = new Set(['FAILED']);
const downloadableStatuses = new Set(['SUCCESS']);
const ocrStatuses = ['PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELED'];

function normalizeStatus(status?: string) {
  return (status || '').toUpperCase();
}

function canSaveFields() {
  return Boolean(canManageOcr.value && record.value && normalizeStatus(record.value.status) === 'SUCCESS');
}

function canRetryRecord(item: OcrRecord) {
  return canManageOcr.value && retryableStatuses.has(normalizeStatus(item.status));
}

function canDownloadRecord(item: OcrRecord) {
  return downloadableStatuses.has(normalizeStatus(item.status));
}

async function loadRecords() {
  if (!currentProjectId.value) {
    records.value = [];
    total.value = 0;
    return;
  }
  recordsLoading.value = true;
  error.value = '';
  try {
    const page = await fetchOcrRecords({
      projectId: currentProjectId.value,
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      status: query.status || undefined,
      ocrType: query.ocrType || undefined
    });
    records.value = page.records;
    total.value = page.total;
  } catch (err) {
    records.value = [];
    total.value = 0;
    error.value = err instanceof Error ? err.message : 'OCR 记录加载失败，请检查后端 OCR 接口。';
  } finally {
    recordsLoading.value = false;
  }
}

async function loadRecord(recordId: ID, taskId?: ID) {
  try {
    record.value = await fetchOcrRecord(recordId);
    logs.value = record.value.taskId ? await fetchTaskStages(record.value.taskId) : [];
    notice.value = '';
  } catch (err) {
    record.value = null;
    logs.value = [];
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    notice.value = `OCR 任务已提交，但结果接口暂不可用。${detail}`;
    if (taskId) {
      try {
        logs.value = await fetchTaskStages(taskId);
      } catch (stageError) {
        const stageDetail = stageError instanceof Error && stageError.message ? ` ${stageError.message}` : '';
        notice.value = `${notice.value} 阶段日志也加载失败。${stageDetail}`;
      }
    }
  }
}

function validateCustomFields() {
  try {
    const parsed = JSON.parse(customFields.value);
    if (!Array.isArray(parsed) || parsed.length === 0) throw new Error('customFields must be a non-empty array');
    return true;
  } catch (err) {
    error.value = err instanceof Error ? `自定义字段 JSON 无效：${err.message}` : '自定义字段 JSON 无效';
    return false;
  }
}

async function startOcr() {
  if (!canManageOcr.value) return ElMessage.warning('当前账号没有 OCR 操作权限');
  if (!file.value) return ElMessage.warning('请先选择识别文件');
  if (!currentProjectId.value) return ElMessage.warning('请先选择项目');
  if (ocrType.value === 'INVOICE' && !invoiceType.value) return ElMessage.warning('请选择发票类型');
  if (ocrType.value === 'CUSTOM' && !validateCustomFields()) return;
  submitting.value = true;
  error.value = '';
  notice.value = '';
  try {
    const result = await submitOcrRecord({
      projectId: currentProjectId.value,
      ocrType: ocrType.value,
      file: file.value,
      invoiceType: ocrType.value === 'INVOICE' ? invoiceType.value : undefined,
      customFields: ocrType.value === 'CUSTOM' ? customFields.value : undefined
    });
    ElMessage.success('OCR 识别任务已提交');
    await loadRecord(result.recordId, result.taskId);
    await loadRecords();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '开始识别失败，请确认后端 OCR 接口是否可用。';
  } finally {
    submitting.value = false;
  }
}

async function saveFields() {
  if (!record.value) return;
  if (!canSaveFields()) return ElMessage.warning(`当前 OCR 状态为 ${record.value.status}，识别成功后才能保存修订`);
  loading.value = true;
  error.value = '';
  try {
    record.value = await updateOcrFields(record.value.recordId, record.value.fields);
    ElMessage.success('修订已保存');
    await loadRecords();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存修订失败';
  } finally {
    loading.value = false;
  }
}

async function selectRecord(row: OcrRecord) {
  await loadRecord(row.recordId, row.taskId);
}

async function retryRecord(row: OcrRecord) {
  if (!canRetryRecord(row)) return ElMessage.warning(`当前 OCR 状态为 ${row.status}，不能重试`);
  retryingId.value = row.recordId;
  error.value = '';
  try {
    const result = await retryOcrRecord(row.recordId);
    ElMessage.success('OCR 重试任务已提交');
    await loadRecord(result.recordId, result.taskId);
    await loadRecords();
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'OCR 重试失败，请检查后端 OCR 接口。';
  } finally {
    retryingId.value = '';
  }
}

async function removeRecord(row: OcrRecord) {
  if (!canManageOcr.value) return ElMessage.warning('当前账号没有 OCR 操作权限');
  try {
    await ElMessageBox.confirm(`确认删除 OCR 记录 #${row.recordId}？`, '删除 OCR 记录', { type: 'warning' });
    deletingId.value = row.recordId;
    await deleteOcrRecord(row.recordId);
    ElMessage.success('OCR 记录已删除');
    if (record.value && String(record.value.recordId) === String(row.recordId)) {
      record.value = null;
      logs.value = [];
    }
    await loadRecords();
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      error.value = err instanceof Error ? err.message : 'OCR 记录删除失败，请检查后端 OCR 接口。';
    }
  } finally {
    deletingId.value = '';
  }
}

async function downloadRecord(row: OcrRecord) {
  if (!canDownloadRecord(row)) return ElMessage.warning(`当前 OCR 状态为 ${row.status}，识别成功后才能下载结果`);
  downloadingId.value = row.recordId;
  try {
    const result = await fetchOcrDownloadResult(row.recordId);
    const json = JSON.stringify(result, null, 2);
    const blob = new Blob([json], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    try {
      link.href = url;
      link.download = `ocr-${row.recordId}.json`;
      document.body.appendChild(link);
      link.click();
    } finally {
      if (link.parentNode) document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'OCR 结果下载失败，请检查后端 OCR 接口。';
  } finally {
    downloadingId.value = '';
  }
}

watch(currentProjectId, () => {
  query.pageNo = 1;
  record.value = null;
  logs.value = [];
  void loadRecords();
});

onMounted(async () => {
  if (!projectStore.currentProject) await projectStore.fetchProjects();
  await loadRecords();
});
</script>

<template>
  <div class="page">
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <el-alert v-if="notice" :title="notice" type="info" show-icon :closable="false" />
    <div class="page-header">
      <div>
        <h2 class="page-title">OCR 识别</h2>
        <p class="page-desc">图片/文档上传、识别进度、结构化字段、人工修订和 JSON 下载。</p>
      </div>
      <el-button type="primary" :loading="loading" :disabled="!canSaveFields()" @click="saveFields">保存修订</el-button>
    </div>

    <el-card class="work-card">
      <template #header>
        <div class="table-head">
          <strong>OCR 记录</strong>
          <div>
            <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px" @change="loadRecords">
              <el-option v-for="item in ocrStatuses" :key="item" :label="item" :value="item" />
            </el-select>
            <el-select v-model="query.ocrType" clearable placeholder="类型" style="width: 160px" @change="loadRecords">
              <el-option v-for="item in ocrTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button @click="loadRecords">刷新</el-button>
          </div>
        </div>
      </template>
      <AppTable
        :loading="recordsLoading"
        :data="records"
        :total="total"
        :page-no="query.pageNo"
        :page-size="query.pageSize"
        :columns="[
          { prop: 'recordId', label: 'ID', width: 90 },
          { prop: 'ocrType', label: '类型', width: 150 },
          { prop: 'status', label: '状态', slot: 'status', width: 110 },
          { prop: 'progress', label: '进度', width: 90 },
          { prop: 'updatedAt', label: '更新时间', width: 180 }
        ]"
        @page-change="(p, s) => { query.pageNo = p; query.pageSize = s; loadRecords(); }"
      >
        <template #empty><EmptyState description="暂无 OCR 记录" /></template>
        <template #status="{ row }"><StatusTag :status="row.status" /></template>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button link type="primary" @click="selectRecord(row)">详情</el-button>
            <el-button link :loading="retryingId === row.recordId" :disabled="!canRetryRecord(row)" @click="retryRecord(row)">重试</el-button>
            <el-button link :loading="downloadingId === row.recordId" :disabled="!canDownloadRecord(row)" @click="downloadRecord(row)">下载</el-button>
            <el-button link type="danger" :loading="deletingId === row.recordId" :disabled="!canManageOcr" @click="removeRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </AppTable>
    </el-card>

    <div class="two-col">
      <el-card class="work-card">
        <h3 class="panel-title">文件上传与识别设置</h3>
        <el-form label-width="88px">
          <el-form-item label="OCR 类型">
            <el-select v-model="ocrType" style="width: 100%">
              <el-option v-for="item in ocrTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="ocrType === 'CUSTOM'" label="自定义字段">
            <el-input
              v-model="customFields"
              type="textarea"
              :rows="8"
              placeholder="请输入后端要求的 JSON 数组，例如合同编号、甲方、金额等字段定义"
            />
          </el-form-item>
          <el-form-item v-if="ocrType === 'INVOICE'" label="发票类型">
            <el-select v-model="invoiceType" style="width: 100%">
              <el-option v-for="item in invoiceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-form>
        <AppUpload
          :model-value="file ? [file] : []"
          accept=".jpg,.jpeg,.png,.pdf"
          :multiple="false"
          tip="支持身份证、车牌、发票和自定义字段识别"
          :uploading="submitting"
          @update:model-value="file = $event[0] || null"
        />
        <el-button type="primary" style="margin-top: 12px" :loading="submitting" :disabled="!canSubmit" @click="startOcr">开始识别</el-button>
        <div class="preview">{{ file?.name || '文件预览区' }}</div>
      </el-card>

      <el-card class="work-card" v-loading="loading || submitting">
        <h3 class="panel-title">识别字段</h3>
        <TaskProgress v-if="record" :percentage="record.progress" :status="record.status" :logs="logs" />
        <EmptyState v-if="!record" description="暂无 OCR 记录，请上传文件后开始识别" />
        <AppTable
          v-else
          :data="record.fields"
          :columns="[
            { prop: 'fieldName', label: '字段' },
            { prop: 'fieldValue', label: '识别值' },
            { prop: 'confidence', label: '置信度' },
            { prop: 'location', label: '位置' }
          ]"
        >
          <template #empty><EmptyState description="暂无识别字段" /></template>
          <el-table-column label="修订">
            <template #default="{ row }"><el-input v-model="row.fieldValue" :disabled="!canSaveFields()" /></template>
          </el-table-column>
        </AppTable>
      </el-card>
    </div>

    <JsonViewer v-if="record" :value="record" title="OCR JSON 结果" />
  </div>
</template>

<style scoped>
.table-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.table-head > div { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.preview { height: 180px; margin-top: 14px; border: 1px dashed var(--sw-border); border-radius: 12px; display: grid; place-items: center; color: var(--sw-muted); background: #f8fafc; }
@media (max-width: 768px) {
  .table-head { align-items: flex-start; flex-direction: column; }
}
</style>
