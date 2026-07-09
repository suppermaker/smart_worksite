<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchReviewRecord, fetchReviewTemplates, submitReviewRecord } from '../../api/review';
import { fetchTaskStages } from '../../api/task';
import { useProjectStore } from '../../stores/project';
import type { ID, ReviewRecord, ReviewTemplate, TaskStageLog } from '../../api/types';

const router = useRouter();
const projectStore = useProjectStore();
const loading = ref(false);
const submitting = ref(false);
const templateError = ref('');
const submitError = ref('');
const resultNotice = ref('');
const stageNotice = ref('');
const templates = ref<ReviewTemplate[]>([]);
const selectedTemplateId = ref<ID>('');
const file = ref<File | null>(null);
const currentRecord = ref<ReviewRecord | null>(null);
const submittedInfo = ref<{ recordId?: ID; taskId?: ID; status?: string } | null>(null);
const logs = ref<TaskStageLog[]>([]);
const canSubmit = computed(() => Boolean(templates.value.length && selectedTemplateId.value && file.value && !submitting.value));

function t(text: string) { return text; }
function goTemplates() { router.push('/templates'); }

async function loadTemplates() {
  loading.value = true;
  templateError.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const projectId = projectStore.currentProject?.projectId;
    templates.value = projectId ? await fetchReviewTemplates(projectId) : [];
    if (!selectedTemplateId.value && templates.value[0]) selectedTemplateId.value = templates.value[0].templateId;
  } catch {
    templateError.value = t('\u5ba1\u67e5\u6a21\u677f\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u6a21\u677f\u63a5\u53e3\u3002');
  } finally {
    loading.value = false;
  }
}

async function loadStages(taskId?: ID) {
  stageNotice.value = '';
  if (!taskId) {
    logs.value = [];
    return;
  }
  try {
    logs.value = await fetchTaskStages(taskId);
  } catch {
    logs.value = [];
    stageNotice.value = t('\u9636\u6bb5\u65e5\u5fd7\u6682\u4e0d\u53ef\u7528');
  }
}

async function loadRecord(recordId: ID, taskId?: ID, status?: string) {
  resultNotice.value = '';
  submittedInfo.value = { recordId, taskId, status };
  try {
    currentRecord.value = await fetchReviewRecord(recordId);
    submittedInfo.value = { recordId: currentRecord.value.recordId, taskId: currentRecord.value.taskId, status: currentRecord.value.status };
    await loadStages(currentRecord.value.taskId || taskId);
  } catch {
    currentRecord.value = null;
    await loadStages(taskId);
    resultNotice.value = t('\u5ba1\u67e5\u4efb\u52a1\u5df2\u63d0\u4ea4\uff0c\u4f46\u7ed3\u679c\u63a5\u53e3\u6682\u4e0d\u53ef\u7528\uff0c\u8bf7\u7a0d\u540e\u5237\u65b0\u6216\u8054\u7cfb\u540e\u7aef\u786e\u8ba4\u3002');
  }
}

async function submit() {
  submitError.value = '';
  if (!templates.value.length) return ElMessage.warning(t('\u5f53\u524d\u9879\u76ee\u6682\u65e0\u5ba1\u67e5\u6a21\u677f\uff0c\u8bf7\u5148\u5230\u6a21\u677f\u4e2d\u5fc3\u4e0a\u4f20\u5ba1\u67e5\u6a21\u677f\u3002'));
  if (!selectedTemplateId.value) return ElMessage.warning(t('\u8bf7\u9009\u62e9\u5ba1\u67e5\u6a21\u677f'));
  if (!file.value) return ElMessage.warning(t('\u8bf7\u5148\u9009\u62e9\u5ba1\u67e5\u6587\u4ef6'));
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning(t('\u8bf7\u5148\u9009\u62e9\u9879\u76ee'));
  submitting.value = true;
  resultNotice.value = '';
  stageNotice.value = '';
  try {
    const result = await submitReviewRecord({ projectId, templateId: selectedTemplateId.value, file: file.value });
    submittedInfo.value = result;
    ElMessage.success(t('\u5ba1\u67e5\u4efb\u52a1\u5df2\u63d0\u4ea4'));
    await loadRecord(result.recordId, result.taskId, result.status);
  } catch (err) {
    submitError.value = err instanceof Error ? err.message : t('\u5ba1\u67e5\u63d0\u4ea4\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u5ba1\u67e5\u63a5\u53e3\u3002');
  } finally {
    submitting.value = false;
  }
}

onMounted(loadTemplates);
</script>

<template>
  <div class="page" v-loading="loading">
    <el-alert v-if="templateError" :title="templateError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
    <el-alert v-if="submitError" :title="submitError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
    <el-alert v-if="resultNotice" :title="resultNotice" type="info" show-icon :closable="false" style="margin-bottom: 12px" />
    <el-alert v-if="stageNotice" :title="stageNotice" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />

    <div class="page-header">
      <div>
        <h2 class="page-title">{{ t('\u5408\u89c4\u5ba1\u67e5') }}</h2>
        <p class="page-desc">{{ t('\u4e0a\u4f20\u65b9\u6848\u6216\u5408\u540c\uff0c\u6309\u6a21\u677f\u751f\u6210\u95ee\u9898\u5b9a\u4f4d\u3001\u4fee\u6539\u5efa\u8bae\u548c JSON \u7ed3\u679c\u3002') }}</p>
      </div>
      <el-tooltip :content="t('\u5ba1\u67e5\u7ed3\u679c\u5bfc\u51fa\u63a5\u53e3\u5f85\u540e\u7aef\u63d0\u4f9b')">
        <el-button disabled>{{ t('\u5bfc\u51fa\u5ba1\u67e5\u7ed3\u679c') }}</el-button>
      </el-tooltip>
    </div>

    <el-card class="work-card">
      <el-empty v-if="!loading && !templates.length" :description="t('\u5f53\u524d\u9879\u76ee\u6682\u65e0\u5ba1\u67e5\u6a21\u677f\uff0c\u8bf7\u5148\u5230\u6a21\u677f\u4e2d\u5fc3\u4e0a\u4f20\u5ba1\u67e5\u6a21\u677f\u3002')">
        <el-button type="primary" @click="goTemplates">{{ t('\u53bb\u6a21\u677f\u4e2d\u5fc3') }}</el-button>
      </el-empty>
      <template v-else>
        <el-form inline>
          <el-form-item :label="t('\u5ba1\u67e5\u6a21\u677f')">
            <el-select v-model="selectedTemplateId" style="width: 260px" :placeholder="t('\u8bf7\u9009\u62e9\u6a21\u677f')">
              <el-option v-for="item in templates" :key="item.templateId" :label="item.templateName" :value="item.templateId" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">{{ t('\u53d1\u8d77\u5ba1\u67e5') }}</el-button>
          </el-form-item>
        </el-form>
        <AppUpload accept=".doc,.docx,.pdf" :uploading="submitting" @change="file = $event[0] || null" />
      </template>
    </el-card>

    <el-card v-if="submittedInfo && !currentRecord" class="work-card">
      <h3 class="panel-title">{{ t('\u5df2\u63d0\u4ea4\u4efb\u52a1') }}</h3>
      <p>recordId: {{ submittedInfo.recordId || '-' }}</p>
      <p>taskId: {{ submittedInfo.taskId || '-' }}</p>
      <p>status: {{ submittedInfo.status || '-' }}</p>
    </el-card>

    <EmptyState v-if="!loading && !resultNotice && !currentRecord && !submittedInfo" :description="t('\u6682\u65e0\u5ba1\u67e5\u8bb0\u5f55\uff0c\u8bf7\u4e0a\u4f20\u6587\u4ef6\u540e\u53d1\u8d77\u5ba1\u67e5\u3002')" />
    <template v-else-if="currentRecord">
      <el-card class="work-card">
        <h3 class="panel-title">{{ t('\u5ba1\u67e5\u8fdb\u5ea6') }}</h3>
        <TaskProgress :percentage="currentRecord.progress" :status="currentRecord.status" :logs="logs" />
      </el-card>
      <div class="two-col">
        <el-card class="work-card">
          <h3 class="panel-title">{{ t('\u95ee\u9898\u5217\u8868') }}</h3>
          <AppTable
            :data="currentRecord.issues || []"
            :columns="[
              { prop: 'severity', label: t('\u4e25\u91cd\u7a0b\u5ea6') },
              { prop: 'location', label: t('\u95ee\u9898\u5b9a\u4f4d') },
              { prop: 'ruleName', label: t('\u89c4\u5219\u540d\u79f0') },
              { prop: 'description', label: t('\u95ee\u9898\u63cf\u8ff0') },
              { prop: 'suggestion', label: t('\u4fee\u6539\u5efa\u8bae') }
            ]"
          >
            <template #empty><EmptyState :description="t('\u6682\u65e0\u5ba1\u67e5\u95ee\u9898\u3002')" /></template>
            <el-table-column :label="t('\u72b6\u6001')" width="110"><template #default><StatusTag :status="currentRecord?.status" /></template></el-table-column>
          </AppTable>
        </el-card>
        <JsonViewer :value="currentRecord" :title="t('\u5ba1\u67e5 JSON \u7ed3\u679c')" />
      </div>
    </template>
  </div>
</template>
