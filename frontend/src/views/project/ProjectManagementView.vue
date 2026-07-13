<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { createProject, deleteProject, fetchProjects, updateProject, updateProjectStatus } from '../../api/project';
import type { ProjectItem } from '../../api/types';
import { useProjectStore } from '../../stores/project';
import { useUserStore } from '../../stores/user';
import { hasSuspiciousText } from '../../utils/textQuality';

const projectStore = useProjectStore();
const userStore = useUserStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const deletingId = ref('');
const error = ref('');
const projects = ref<ProjectItem[]>([]);
const dialogVisible = ref(false);
const canManageProject = computed(() => userStore.hasPermission('project:manage'));
const canManageMembers = computed(() => userStore.hasPermission('project:member:manage'));
const form = reactive({ projectId: '', name: '', code: '', address: '', description: '' });

async function loadProjects() {
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchProjects({ pageNo: 1, pageSize: 100 });
    projects.value = page.records;
    projectStore.projects = projects.value;
    if (!projects.value.length) {
      projectStore.switchProject('');
      return;
    }
    const current = projects.value.find((item) => String(item.projectId) === String(projectStore.currentProjectId)) || projects.value[0];
    if (!projectStore.currentProjectId || !projects.value.some((item) => String(item.projectId) === String(projectStore.currentProjectId))) {
      projectStore.switchProject(current.projectId);
    }
  } catch (err) {
    projects.value = [];
    error.value = err instanceof Error ? err.message : '项目列表加载失败，请检查后端项目接口。';
  } finally {
    loading.value = false;
  }
}

function switchProject(project: ProjectItem) {
  projectStore.projects = projects.value;
  projectStore.switchProject(project.projectId);
}

function openMemberManagement(project: ProjectItem) {
  switchProject(project);
  router.push('/project/members');
}

function openCreate() {
  if (!canManageProject.value) return ElMessage.warning('当前账号没有项目管理权限');
  Object.assign(form, { projectId: '', name: '', code: '', address: '', description: '' });
  dialogVisible.value = true;
}

function openEdit(row: ProjectItem) {
  if (!canManageProject.value) return ElMessage.warning('当前账号没有项目管理权限');
  Object.assign(form, { projectId: String(row.projectId), name: row.name, code: row.code, address: row.address, description: row.description || '' });
  dialogVisible.value = true;
}

async function saveProject() {
  if (!canManageProject.value) return ElMessage.warning('当前账号没有项目管理权限');
  if (!form.name.trim()) return ElMessage.warning('请输入项目名称');
  if (!form.code.trim()) return ElMessage.warning('请输入项目编码');
  saving.value = true;
  try {
    const isCreate = !form.projectId;
    const saved = isCreate ? await createProject(form) : await updateProject(form.projectId, form);
    ElMessage.success(isCreate ? '项目已创建' : '项目已保存');
    dialogVisible.value = false;
    await loadProjects();
    if (isCreate) switchProject(saved);
    if (!isCreate && String(projectStore.currentProjectId) === String(form.projectId)) projectStore.switchProject(form.projectId);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(form.projectId ? `项目更新失败，请检查后端项目接口。${detail}` : `项目创建失败，请检查后端项目接口。${detail}`);
  } finally {
    saving.value = false;
  }
}

async function toggleProjectStatus(row: ProjectItem) {
  if (!canManageProject.value) return ElMessage.warning('当前账号没有项目管理权限');
  const enabled = ['ACTIVE', 'ENABLED'].includes(row.status);
  const nextStatus = enabled ? 'DISABLED' : 'ENABLED';
  const actionText = enabled ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(`确认${actionText}项目“${row.name || row.projectName}”？`, `${actionText}项目`, { type: 'warning' });
    await updateProjectStatus(row.projectId, nextStatus);
    ElMessage.success(`项目已${actionText}`);
    await loadProjects();
  } catch (err) {
    if (err === 'cancel' || err === 'close') return;
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`项目${actionText}失败，请检查后端项目状态接口。${detail}`);
  }
}

async function removeProject(row: ProjectItem) {
  if (!canManageProject.value) return ElMessage.warning('当前账号没有项目管理权限');
  try {
    await ElMessageBox.confirm(`确认删除项目“${row.name || row.projectName}”？`, '删除项目', { type: 'warning' });
    deletingId.value = String(row.projectId);
    await deleteProject(row.projectId);
    ElMessage.success('项目已删除');
    await loadProjects();
  } catch (err) {
    if (err === 'cancel' || err === 'close') return;
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`项目删除失败，请检查后端项目接口。${detail}`);
  } finally {
    deletingId.value = '';
  }
}

onMounted(loadProjects);
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div><h2 class="page-title">项目与权限</h2><p class="page-desc">项目档案、成员角色、项目级权限隔离。</p></div>
      <el-button v-if="canManageProject" type="primary" @click="openCreate">新建项目</el-button>
    </div>
    <el-card class="work-card">
      <template #header><strong>项目列表</strong></template>
      <AppTable :loading="loading" :error="error" :data="projects" :columns="[{ prop: 'name', label: '项目名称', slot: 'name' }, { prop: 'code', label: '编码' }, { prop: 'status', label: '状态', slot: 'status' }]">
        <template #empty><EmptyState description="暂无项目，请先创建项目。" /></template>
        <template #name="{ row }">
          <span>{{ row.name || row.projectName }}</span>
          <el-tag v-if="hasSuspiciousText(row.name || row.projectName)" type="warning" size="small" style="margin-left: 6px">疑似历史乱码数据</el-tag>
        </template>
        <template #status="{ row }"><StatusTag :status="row.status" /></template>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button link type="primary" @click="switchProject(row)">设为当前</el-button>
            <el-button v-if="canManageMembers" link type="primary" @click="openMemberManagement(row)">成员管理</el-button>
            <el-button v-if="canManageProject" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canManageProject" link :type="['ACTIVE', 'ENABLED'].includes(row.status) ? 'warning' : 'success'" @click="toggleProjectStatus(row)">
              {{ ['ACTIVE', 'ENABLED'].includes(row.status) ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="canManageProject" link type="danger" :loading="deletingId === String(row.projectId)" @click="removeProject(row)">删除</el-button>
          </template>
        </el-table-column>
      </AppTable>
    </el-card>
    <el-dialog v-model="dialogVisible" title="项目档案" width="560px">
      <el-form label-width="92px">
        <el-form-item label="项目名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="项目编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="项目地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveProject">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped></style>
