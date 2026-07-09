<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchOcrRecord, submitOcrRecord, updateOcrFields } from '../../api/ocr';
import { fetchTaskStages } from '../../api/task';
import { useProjectStore } from '../../stores/project';
import type { ID, OcrRecord, TaskStageLog } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const submitting = ref(false);
const error = ref('');
const notice = ref('');
const record = ref<OcrRecord | null>(null);
const logs = ref<TaskStageLog[]>([]);
const ocrType = ref('CONTRACT');
const customFields = ref('');
const file = ref<File | null>(null);
const canSubmit = computed(() => Boolean(file.value && !submitting.value));
const ocrTypes = [
  { label: '身份证', value: 'ID_CARD' },
  { label: '车牌', value: 'LICENSE_PLATE' },
  { label: '发票', value: 'INVOICE' },
  { label: '合同', value: 'CONTRACT' },
  { label: '自定义', value: 'CUSTOM' }
];

async function loadRecord(recordId: ID, taskId?: ID) {
  try {
    record.value = await fetchOcrRecord(recordId);
    logs.value = record.value.taskId ? await fetchTaskStages(record.value.taskId) : [];
    notice.value = '';
  } catch (err) {
    record.value = null;
    logs.value = taskId ? await fetchTaskStages(taskId).catch(() => []) : [];
    notice.value = 'OCR 任务已提交，但结果接口暂不可用/后端待提供。';
  }
}

async function startOcr() {
  if (!file.value) return ElMessage.warning('请先选择识别文件');
  submitting.value = true;
  error.value = '';
  notice.value = '';
  try {
    const result = await submitOcrRecord({
      projectId: projectStore.currentProject?.projectId || 0,
      ocrType: ocrType.value,
      file: file.value,
      customFields: customFields.value
    });
    ElMessage.success('OCR 识别任务已提交');
    await loadRecord(result.recordId, result.taskId);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '开始识别失败，请确认后端接口是否可用';
  } finally {
    submitting.value = false;
  }
}

async function saveFields() {
  if (!record.value) return;
  loading.value = true;
  error.value = '';
  try {
    await updateOcrFields(record.value.recordId, record.value.fields);
    ElMessage.success('修订已保存');
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存修订失败';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="page">
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <el-alert v-if="notice" :title="notice" type="info" show-icon :closable="false" />
    <div class="page-header">
      <div>
        <h2 class="page-title">OCR识别</h2>
        <p class="page-desc">图片/文档上传、识别进度、结构化字段、人工修订和 JSON 下载。</p>
      </div>
      <el-button type="primary" :loading="loading" :disabled="!record" @click="saveFields">保存修订</el-button>
    </div>

    <div class="two-col">
      <el-card class="work-card">
        <h3 class="panel-title">文件上传与识别设置</h3>
        <el-form label-width="88px">
          <el-form-item label="OCR类型">
            <el-select v-model="ocrType" style="width: 100%">
              <el-option v-for="item in ocrTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="ocrType === 'CUSTOM'" label="自定义字段">
            <el-input v-model="customFields" placeholder="例如：合同编号、甲方、金额" />
          </el-form-item>
        </el-form>
        <AppUpload accept=".jpg,.jpeg,.png,.pdf" tip="支持身份证、车牌、发票、合同和自定义文档" :uploading="submitting" @change="file = $event[0] || null" />
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
          <el-table-column label="修订"><template #default="{ row }"><el-input v-model="row.fieldValue" /></template></el-table-column>
        </AppTable>
      </el-card>
    </div>

    <JsonViewer v-if="record" :value="record" title="OCR JSON结果" />
  </div>
</template>

<style scoped>
.preview { height: 180px; margin-top: 14px; border: 1px dashed var(--sw-border); border-radius: 12px; display: grid; place-items: center; color: var(--sw-muted); background: #f8fafc; }
</style>
