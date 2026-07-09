<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as projectApi from '../../api/project';
import type { ProjectItem, ProjectCreateForm } from '../../api/types';
import { useProjectStore } from '../../stores/project';

const projectStore = useProjectStore();
const projects = ref<ProjectItem[]>([]);
const total = ref(0);
const loading = ref(false);
const keyword = ref('');
const pageNo = ref(1);
const pageSize = ref(20);

const dialogVisible = ref(false);
const editingId = ref<number | string | null>(null);
const formRef = ref();
const form = ref<ProjectCreateForm>({
  projectName: '',
  projectCode: '',
  location: '',
  description: ''
});

const rules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }]
};

async function fetchList() {
  loading.value = true;
  try {
    const res = await projectApi.fetchProjects({ pageNo: pageNo.value, pageSize: pageSize.value, keyword: keyword.value });
    projects.value = res.records;
    total.value = res.total;
  } catch { /* handled */ } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNo.value = 1;
  fetchList();
}

function openCreate() {
  editingId.value = null;
  form.value = { projectName: '', projectCode: '', location: '', description: '' };
  dialogVisible.value = true;
}

function openEdit(row: ProjectItem) {
  editingId.value = row.projectId;
  form.value = {
    projectName: row.projectName,
    projectCode: row.projectCode,
    location: row.location || '',
    description: row.description || ''
  };
  dialogVisible.value = true;
}

async function submit() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) return;
  }
  try {
    if (editingId.value == null) {
      await projectApi.createProject(form.value);
      ElMessage.success('项目创建成功');
    } else {
      await projectApi.updateProject(editingId.value, form.value);
      ElMessage.success('项目更新成功');
    }
    dialogVisible.value = false;
    fetchList();
    projectStore.fetchProjects();
  } catch { /* handled */ }
}

async function handleToggleStatus(row: ProjectItem) {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const label = newStatus === 'ENABLED' ? '启用' : '停用';
  await ElMessageBox.confirm(`确认${label}项目「${row.projectName}」？`, '提示', { type: 'warning' });
  try {
    await projectApi.updateProjectStatus(row.projectId, newStatus);
    ElMessage.success(`已${label}`);
    fetchList();
    projectStore.fetchProjects();
  } catch { /* handled */ }
}

async function handleDelete(row: ProjectItem) {
  await ElMessageBox.confirm(`确认删除项目「${row.projectName}」？删除后不可恢复。`, '警告', { type: 'error' });
  try {
    await projectApi.deleteProject(row.projectId);
    ElMessage.success('已删除');
    fetchList();
    projectStore.fetchProjects();
  } catch { /* handled */ }
}

function handlePageChange(page: number) {
  pageNo.value = page;
  fetchList();
}

onMounted(fetchList);
</script>

<template>
  <div>
    <div class="page-header">
      <h2>项目管理</h2>
      <div class="header-actions">
        <el-input v-model="keyword" placeholder="搜索项目名称/编号" clearable style="width: 240px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="primary" @click="openCreate">新增项目</el-button>
      </div>
    </div>

    <el-table :data="projects" v-loading="loading" stripe>
      <el-table-column prop="projectName" label="项目名称" min-width="160" />
      <el-table-column prop="projectCode" label="项目编号" width="140" />
      <el-table-column prop="location" label="项目地址" min-width="140" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 'ENABLED' ? 'warning' : 'success'" @click="handleToggleStatus(row)">
            {{ row.status === 'ENABLED' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize" :current-page="pageNo" @current-change="handlePageChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId == null ? '新增项目' : '编辑项目'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" maxlength="128" />
        </el-form-item>
        <el-form-item label="项目编号" prop="projectCode">
          <el-input v-model="form.projectCode" maxlength="64" />
        </el-form-item>
        <el-form-item label="项目地址">
          <el-input v-model="form.location" maxlength="255" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; }
.header-actions { display: flex; gap: 8px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
