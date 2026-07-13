<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import AppTable from '../../components/common/AppTable.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchAuditLogs } from '../../api/audit';
import { useProjectStore } from '../../stores/project';
import type { AuditLog } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const rows = ref<AuditLog[]>([]);
const error = ref('');
const pager = reactive({ pageNo: 1, pageSize: 10, total: 0, objectType: '', action: '' });
const projectId = computed(() => projectStore.currentProject?.projectId);

async function loadRows() {
  if (!projectId.value) {
    rows.value = [];
    pager.total = 0;
    error.value = '请先选择项目';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchAuditLogs({ projectId: projectId.value, pageNo: pager.pageNo, pageSize: pager.pageSize, objectType: pager.objectType || undefined, action: pager.action || undefined });
    rows.value = page.records;
    pager.total = page.total;
  } catch (err) {
    rows.value = [];
    pager.total = 0;
    error.value = err instanceof Error ? err.message : '审计日志加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  if (!projectStore.currentProject) await projectStore.fetchProjects();
  await loadRows();
});
</script>

<template>
  <div class="page">
    <div class="page-header"><div><h2 class="page-title">审计日志</h2><p class="page-desc">用户操作、外部调用、关键业务动作留痕。</p></div></div>
    <el-card class="work-card"><template #header><div class="filters"><strong>日志列表</strong><div><el-input v-model="pager.objectType" clearable placeholder="对象类型" style="width:140px" /><el-input v-model="pager.action" clearable placeholder="动作" style="width:160px;margin-left:8px" /><el-button type="primary" style="margin-left:8px" @click="loadRows">查询</el-button></div></div></template>
      <AppTable :loading="loading" :error="error" :data="rows" :total="pager.total" :page-no="pager.pageNo" :page-size="pager.pageSize" :columns="[{ prop: 'createdAt', label: '时间', width: 190 }, { prop: 'operatorId', label: '操作人ID', width: 110 }, { prop: 'action', label: '动作' }, { prop: 'objectType', label: '对象类型' }, { prop: 'objectId', label: '对象ID', width: 100 }, { prop: 'requestId', label: '请求ID', width: 180 }, { prop: 'ipAddress', label: 'IP', width: 130 }]" @page-change="(p, s) => { pager.pageNo = p; pager.pageSize = s; loadRows(); }">
        <template #empty><EmptyState description="暂无审计日志" /></template>
      </AppTable>
    </el-card>
  </div>
</template>
<style scoped>.filters{display:flex;align-items:center;justify-content:space-between;}</style>
