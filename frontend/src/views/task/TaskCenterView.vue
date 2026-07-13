<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AppTable from '../../components/common/AppTable.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import { cancelTask, fetchTaskDetail, fetchTasks, fetchTaskStages, fetchTaskStatistics, retryTask } from '../../api/task';
import { useProjectStore } from '../../stores/project';
import type { ID, TaskDetail, TaskStageLog, TaskStatistics } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const rows = ref<TaskDetail[]>([]);
const total = ref(0);
const selectedTask = ref<TaskDetail | null>(null);
const logs = ref<TaskStageLog[]>([]);
const statistics = ref<TaskStatistics | null>(null);
const retryingId = ref<ID | ''>('');
const cancelingId = ref<ID | ''>('');
const query = reactive({ pageNo: 1, pageSize: 20, status: '', taskType: '', taskId: '' });
const projectId = computed(() => projectStore.currentProject?.projectId || '');
const retryableStatuses = new Set(['FAILED']);
const cancelableStatuses = new Set(['PENDING', 'QUEUED', 'RUNNING', 'RETRYING']);
const taskStatuses = ['PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'RETRYING', 'CANCELED'];

function normalizeStatus(status?: string) {
  return (status || '').toUpperCase();
}

function canRetry(task: TaskDetail) {
  return retryableStatuses.has(normalizeStatus(task.status)) && (task.retryCount || 0) < (task.maxRetryCount || 0);
}

function canCancel(task: TaskDetail) {
  return cancelableStatuses.has(normalizeStatus(task.status));
}

async function loadStatistics() {
  try {
    statistics.value = projectId.value ? await fetchTaskStatistics(projectId.value) : null;
  } catch (err) {
    statistics.value = null;
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    error.value = error.value || `任务统计加载失败，请检查后端任务统计接口。${detail}`;
  }
}

async function loadTasks() {
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchTasks({ projectId: projectId.value || undefined, pageNo: query.pageNo, pageSize: query.pageSize, status: query.status, taskType: query.taskType });
    rows.value = page.records;
    total.value = page.total;
    await loadStatistics();
  } catch (err) {
    rows.value = [];
    total.value = 0;
    error.value = err instanceof Error ? err.message : '任务列表加载失败，请检查后端任务接口。';
  } finally {
    loading.value = false;
  }
}

async function loadTaskById(taskId: ID) {
  detailLoading.value = true;
  logs.value = [];
  try {
    selectedTask.value = await fetchTaskDetail(taskId);
    logs.value = await fetchTaskStages(taskId);
  } catch (err) {
    selectedTask.value = null;
    ElMessage.error(err instanceof Error ? err.message : '任务详情加载失败，请检查后端任务接口。');
  } finally {
    detailLoading.value = false;
  }
}

async function searchTask() {
  if (!query.taskId) return ElMessage.warning('请输入 taskId');
  await loadTaskById(query.taskId);
}

async function retry(task: TaskDetail) {
  if (!canRetry(task)) return ElMessage.warning(`当前任务状态为 ${task.status}，不能重试`);
  retryingId.value = task.taskId;
  try {
    await retryTask(task.taskId);
    ElMessage.success('任务已重新入队');
    await loadTasks();
    await loadTaskById(task.taskId);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '任务重试失败，请检查后端任务接口。');
  } finally {
    retryingId.value = '';
  }
}

async function cancel(task: TaskDetail) {
  if (!canCancel(task)) return ElMessage.warning(`当前任务状态为 ${task.status}，不能取消`);
  cancelingId.value = task.taskId;
  try {
    await cancelTask(task.taskId);
    ElMessage.success('取消请求已提交');
    await loadTasks();
    await loadTaskById(task.taskId);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '任务取消失败，请检查后端任务接口。');
  } finally {
    cancelingId.value = '';
  }
}

onMounted(async () => {
  if (!projectStore.currentProject) await projectStore.fetchProjects();
  await loadTasks();
});
</script>

<template>
  <div class="page">
    <div class="page-header"><div><h2 class="page-title">任务中心</h2><p class="page-desc">统一查看报告生成、OCR、知识入库、文件解析等长任务状态和阶段日志。</p></div></div>

    <div class="stat-row" v-if="statistics"><el-card><strong>{{ statistics.queuedCount }}</strong><span>排队中</span></el-card><el-card><strong>{{ statistics.runningCount }}</strong><span>运行中</span></el-card><el-card><strong>{{ statistics.failedCount }}</strong><span>失败</span></el-card></div>

    <el-card class="work-card">
      <template #header><div class="table-head"><strong>任务列表</strong><div><el-select v-model="query.status" clearable placeholder="状态" style="width: 140px" @change="loadTasks"><el-option v-for="s in taskStatuses" :key="s" :label="s" :value="s" /></el-select><el-input v-model="query.taskId" placeholder="输入 taskId 精确查询" style="width: 240px" @keyup.enter="searchTask"><template #append><el-button :loading="detailLoading" @click="searchTask">查询</el-button></template></el-input></div></div></template>
      <AppTable :loading="loading" :error="error" :data="rows" :total="total" :page-no="query.pageNo" :page-size="query.pageSize" :columns="[{ prop: 'taskId', label: 'ID', width: 100 }, { prop: 'taskType', label: '类型', width: 150 }, { prop: 'bizType', label: '业务', width: 120 }, { prop: 'status', label: '状态', slot: 'status', width: 110 }, { prop: 'currentStage', label: '当前阶段' }, { prop: 'updatedAt', label: '更新时间', width: 180 }]" @page-change="(p, s) => { query.pageNo = p; query.pageSize = s; loadTasks(); }">
        <template #empty><EmptyState description="暂无任务" /></template><template #status="{ row }"><StatusTag :status="row.status" /></template>
        <el-table-column label="操作" width="230"><template #default="{ row }"><el-button link type="primary" @click="loadTaskById(row.taskId)">详情</el-button><el-button link :loading="retryingId === row.taskId" :disabled="!canRetry(row)" @click="retry(row)">重试</el-button><el-button link type="warning" :loading="cancelingId === row.taskId" :disabled="!canCancel(row)" @click="cancel(row)">取消</el-button></template></el-table-column>
      </AppTable>
    </el-card>

    <el-card class="work-card" v-loading="detailLoading">
      <template #header><strong>{{ selectedTask ? `${selectedTask.taskType} #${selectedTask.taskId}` : '任务详情' }}</strong></template>
      <EmptyState v-if="!selectedTask" description="请选择任务或输入 taskId 查询详情" />
      <template v-else><div class="detail-head"><StatusTag :status="selectedTask.status" /><span v-if="selectedTask.errorMessage" class="error-text">{{ selectedTask.errorMessage }}</span></div><TaskProgress :percentage="selectedTask.progress || 0" :status="selectedTask.status" :logs="logs" /></template>
    </el-card>

    <el-card class="work-card"><template #header><strong>阶段日志</strong></template><AppTable :loading="detailLoading" :data="logs" :columns="[{ prop: 'stageName', label: '阶段' }, { prop: 'status', label: '状态', slot: 'status' }, { prop: 'message', label: '说明' }, { prop: 'errorMessage', label: '错误' }, { prop: 'createdAt', label: '时间' }]"><template #empty><EmptyState description="选择任务后查看阶段日志" /></template><template #status="{ row }"><StatusTag :status="row.status" /></template></AppTable></el-card>
  </div>
</template>

<style scoped>.table-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.table-head>div{display:flex;gap:8px}.stat-row{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:16px}.stat-row :deep(.el-card__body){display:flex;align-items:baseline;gap:8px}.stat-row strong{font-size:28px;color:var(--sw-primary)}.detail-head{display:flex;align-items:center;gap:12px;margin-bottom:12px}.error-text{color:#b42318}</style>
