<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AppTable from '../../components/common/AppTable.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import { createDataSource, deleteDataSource, disableDataSource, enableDataSource, fetchDataSources, inspectDataSourceSchema, queryDataSource, testDataSource, updateDataSource } from '../../api/datasource';
import { useProjectStore } from '../../stores/project';
import { useUserStore } from '../../stores/user';
import type { DataSourceItem, DataSourceQueryResult, DataSourceSchema, ID } from '../../api/types';

const projectStore = useProjectStore();
const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const querying = ref(false);
const error = ref('');
const rows = ref<DataSourceItem[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const schemaVisible = ref(false);
const schemaLoading = ref(false);
const selectedId = ref<ID | ''>('');
const testingId = ref<ID | ''>('');
const togglingId = ref<ID | ''>('');
const deletingId = ref<ID | ''>('');
const result = ref<DataSourceQueryResult | null>(null);
const schema = ref<DataSourceSchema | null>(null);
const question = ref('本月未闭环安全问题有多少？');
const query = reactive({ pageNo: 1, pageSize: 20, keyword: '', status: '' });
const form = reactive({ dataSourceId: '' as ID | '', name: '', dbType: 'MYSQL', jdbcUrl: '', username: '', password: '' });
const projectId = computed(() => projectStore.currentProject?.projectId || '');
const canManageDataSource = computed(() => userStore.hasPermission('datasource:manage'));

function resetForm() {
  Object.assign(form, { dataSourceId: '', name: '', dbType: 'MYSQL', jdbcUrl: '', username: '', password: '' });
}

function ensureProject() {
  if (!projectId.value) {
    error.value = '请先选择项目';
    return false;
  }
  return true;
}

async function loadRows() {
  if (!ensureProject()) return;
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchDataSources({ projectId: projectId.value, pageNo: query.pageNo, pageSize: query.pageSize, keyword: query.keyword, status: query.status });
    rows.value = page.records;
    total.value = page.total;
  } catch (err) {
    rows.value = [];
    total.value = 0;
    error.value = err instanceof Error ? err.message : '数据源列表加载失败，请检查后端数据源接口。';
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  if (!canManageDataSource.value) return ElMessage.warning('当前账号没有数据源管理权限');
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: DataSourceItem) {
  if (!canManageDataSource.value) return ElMessage.warning('当前账号没有数据源管理权限');
  Object.assign(form, { dataSourceId: row.dataSourceId, name: row.name, dbType: row.dbType, jdbcUrl: row.jdbcUrl, username: row.username || '', password: '' });
  dialogVisible.value = true;
}

async function save() {
  if (!canManageDataSource.value) return ElMessage.warning('当前账号没有数据源管理权限');
  if (!ensureProject()) return;
  if (!form.name.trim()) return ElMessage.warning('请输入数据源名称');
  if (!form.jdbcUrl.trim()) return ElMessage.warning('请输入 JDBC URL');
  if (!form.username.trim()) return ElMessage.warning('请输入用户名');
  if (!form.dataSourceId && !form.password.trim()) return ElMessage.warning('新增数据源必须输入密码');
  saving.value = true;
  try {
    const payload = { name: form.name.trim(), dbType: form.dbType, jdbcUrl: form.jdbcUrl.trim(), username: form.username.trim(), password: form.password || undefined };
    if (form.dataSourceId) await updateDataSource(form.dataSourceId, payload);
    else await createDataSource({ projectId: projectId.value, ...payload, password: form.password });
    ElMessage.success(form.dataSourceId ? '数据源已更新' : '数据源已创建');
    dialogVisible.value = false;
    await loadRows();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '数据源保存失败，请检查后端接口。');
  } finally {
    saving.value = false;
  }
}

async function test(row: DataSourceItem) {
  testingId.value = row.dataSourceId;
  try {
    const res = await testDataSource(row.dataSourceId);
    ElMessage[res.success ? 'success' : 'error'](res.message || (res.success ? '连接成功' : '连接失败'));
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '连接测试失败，请检查后端数据源接口。');
  } finally {
    testingId.value = '';
  }
}

async function showSchema(row: DataSourceItem) {
  schemaVisible.value = true;
  schemaLoading.value = true;
  schema.value = null;
  try {
    schema.value = await inspectDataSourceSchema(row.dataSourceId);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : 'Schema 查询失败，请检查后端数据源接口。');
  } finally {
    schemaLoading.value = false;
  }
}

async function toggleStatus(row: DataSourceItem) {
  if (!canManageDataSource.value) return ElMessage.warning('当前账号没有数据源管理权限');
  const enabled = ['ENABLED', 'ACTIVE'].includes(String(row.status).toUpperCase());
  const action = enabled ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(`确认${action}数据源「${row.name}」？`, `${action}数据源`, { type: 'warning' });
    togglingId.value = row.dataSourceId;
    if (enabled) await disableDataSource(row.dataSourceId);
    else await enableDataSource(row.dataSourceId);
    ElMessage.success(`数据源已${action}`);
    await loadRows();
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err instanceof Error ? err.message : `数据源${action}失败，请检查后端数据源接口。`);
    }
  } finally {
    togglingId.value = '';
  }
}

async function remove(row: DataSourceItem) {
  if (!canManageDataSource.value) return ElMessage.warning('当前账号没有数据源管理权限');
  try {
    await ElMessageBox.confirm(`确认删除数据源「${row.name}」？`, '删除数据源', { type: 'warning' });
    deletingId.value = row.dataSourceId;
    await deleteDataSource(row.dataSourceId);
    ElMessage.success('数据源已删除');
    await loadRows();
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error(err instanceof Error ? err.message : '数据源删除失败，请检查后端数据源接口。');
    }
  } finally {
    deletingId.value = '';
  }
}

async function ask() {
  if (!ensureProject()) return;
  if (!question.value.trim()) return ElMessage.warning('请输入业务数据问题');
  querying.value = true;
  result.value = null;
  try {
    if (!selectedId.value) return ElMessage.warning('请选择一个启用的数据源');
    result.value = await queryDataSource({ projectId: projectId.value, question: question.value.trim(), dataSourceId: selectedId.value });
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '数据库问答失败，请检查 AI 数据库问答接口。');
  } finally {
    querying.value = false;
  }
}

onMounted(async () => {
  if (!projectStore.currentProject) await projectStore.fetchProjects();
  await loadRows();
});
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div><h2 class="page-title">数据源管理</h2><p class="page-desc">配置业务数据库，统一通过后端安全校验执行只读数据库问答。</p></div>
      <el-button type="primary" @click="openCreate">新增数据源</el-button>
    </div>

    <el-card class="work-card">
      <template #header>
        <div class="table-head"><strong>数据源列表</strong><div><el-input v-model="query.keyword" clearable placeholder="搜索名称" style="width: 220px" @keyup.enter="loadRows" @clear="loadRows" /><el-button type="primary" @click="loadRows">查询</el-button></div></div>
      </template>
      <AppTable :loading="loading" :error="error" :data="rows" :total="total" :page-no="query.pageNo" :page-size="query.pageSize" :columns="[{prop:'name',label:'名称'},{prop:'dbType',label:'类型',width:110},{prop:'jdbcUrl',label:'JDBC URL'},{prop:'username',label:'用户',width:120},{prop:'status',label:'状态',slot:'status',width:100}]" @page-change="(p,s)=>{query.pageNo=p;query.pageSize=s;loadRows()}">
        <template #empty><EmptyState description="暂无数据源" /></template>
        <template #status="{ row }"><StatusTag :status="row.status" /></template>
        <el-table-column label="问答" width="76"><template #default="{ row }"><el-radio v-model="selectedId" :value="row.dataSourceId" :disabled="!['ENABLED','ACTIVE'].includes(String(row.status).toUpperCase())">选择</el-radio></template></el-table-column>
        <el-table-column label="操作" width="260"><template #default="{ row }"><el-button link type="primary" :loading="testingId === row.dataSourceId" @click="test(row)">测试</el-button><el-button link @click="showSchema(row)">Schema</el-button><el-button link :disabled="!canManageDataSource" @click="openEdit(row)">编辑</el-button><el-button link :disabled="!canManageDataSource" :loading="togglingId === row.dataSourceId" :type="['ENABLED','ACTIVE'].includes(String(row.status).toUpperCase()) ? 'warning' : 'success'" @click="toggleStatus(row)">{{ ['ENABLED','ACTIVE'].includes(String(row.status).toUpperCase()) ? '停用' : '启用' }}</el-button><el-button link type="danger" :disabled="!canManageDataSource" :loading="deletingId === row.dataSourceId" @click="remove(row)">删除</el-button></template></el-table-column>
      </AppTable>
    </el-card>

    <el-card class="work-card">
      <template #header><strong>数据库问答</strong></template>
      <el-alert title="数据库问答由后端调用 Python 生成 SQL，Java 后端只执行安全只读 SQL；失败会直接显示真实错误。" type="info" show-icon :closable="false" style="margin-bottom: 12px" />
      <el-input v-model="question" type="textarea" :rows="3" placeholder="请输入业务数据问题" />
      <el-button type="primary" :loading="querying" style="margin-top:12px" @click="ask">生成查询</el-button>
      <div v-if="result" class="result"><el-alert v-if="result.summary" :title="result.summary" type="success" show-icon /><pre v-if="result.sql">{{ result.sql }}</pre><JsonViewer :value="result.rows" /></div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="数据源配置" width="640px">
      <el-form label-width="96px"><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="类型"><el-select v-model="form.dbType"><el-option label="MySQL" value="MYSQL" /><el-option label="PostgreSQL" value="POSTGRESQL" /><el-option label="Kingbase" value="KINGBASE" /></el-select></el-form-item><el-form-item label="JDBC URL"><el-input v-model="form.jdbcUrl" placeholder="jdbc:mysql://db.example.invalid:3306/db" /></el-form-item><el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item><el-form-item label="密码"><el-input v-model="form.password" type="password" show-password :placeholder="form.dataSourceId ? '留空则不修改密码' : '请输入密码'" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="schemaVisible" title="数据源 Schema" width="760px">
      <div v-loading="schemaLoading"><EmptyState v-if="!schema" description="暂无 Schema" /><JsonViewer v-else :value="schema" /></div>
    </el-dialog>
  </div>
</template>

<style scoped>.table-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.table-head>div{display:flex;gap:8px}.result{margin-top:14px;display:flex;flex-direction:column;gap:12px}pre{background:#0f172a;color:#d1fae5;padding:12px;border-radius:10px;overflow:auto}</style>
