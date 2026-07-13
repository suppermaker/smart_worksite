<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as roleApi from '../../api/role';
import type { RoleItem, PermissionItem } from '../../api/types';

const roles = ref<RoleItem[]>([]);
const permissions = ref<PermissionItem[]>([]);
const loading = ref(false);
const saving = ref(false);
const actionLoading = ref(false);
const error = ref('');

const selectedRole = ref<RoleItem | null>(null);
const checkedPermissionIds = ref<(string | number)[]>([]);
const dialogPermissionIds = ref<(string | number)[]>([]);
const keyword = ref('');
const dialogVisible = ref(false);
const editingRoleId = ref<string | number | null>(null);
const formRef = ref();
const form = reactive({ roleCode: '', roleName: '', description: '', status: 'ENABLED' as 'ENABLED' | 'DISABLED' });
const builtInRoleCodes = new Set(['PLATFORM_ADMIN', 'PROJECT_ADMIN', 'BUSINESS_USER', 'VIEWER']);

const groupedPermissions = computed(() => {
  const groups: Record<string, PermissionItem[]> = {};
  for (const p of permissions.value) {
    const group = p.permissionCode.split(':')[0] || 'other';
    if (!groups[group]) groups[group] = [];
    groups[group].push(p);
  }
  return groups;
});

const groupLabels: Record<string, string> = {
  dashboard: '工作台', knowledge: '知识库', qa: '知识问答', review: '合规审查',
  report: '报告管理', ocr: 'OCR识别', system: '系统管理', project: '项目管理', file: '文件管理',
  template: '模板中心', task: '任务中心', audit: '审计日志', datasource: '数据源管理'
};

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

async function fetchData() {
  loading.value = true;
  error.value = '';
  try {
    [roles.value, permissions.value] = await Promise.all([roleApi.listRoles(keyword.value.trim() || undefined), roleApi.listPermissions()]);
    if (roles.value.length) {
      const current = selectedRole.value ? roles.value.find((role) => String(role.id) === String(selectedRole.value?.id)) : roles.value[0];
      selectRole(current || roles.value[0]);
    } else {
      selectedRole.value = null;
      checkedPermissionIds.value = [];
    }
  } catch (err) {
    roles.value = [];
    permissions.value = [];
    selectedRole.value = null;
    checkedPermissionIds.value = [];
    error.value = getErrorMessage(err, '角色权限数据加载失败');
    ElMessage.error(error.value);
  } finally { loading.value = false; }
}

function selectRole(role: RoleItem) {
  selectedRole.value = role;
  checkedPermissionIds.value = [...role.permissionIds];
}

function isBuiltInRole(role: RoleItem | null) {
  return !!role && builtInRoleCodes.has(role.roleCode);
}

async function savePermissions() {
  if (!selectedRole.value) return;
  if (isBuiltInRole(selectedRole.value)) {
    ElMessage.warning('内置角色权限由系统种子数据维护，不能在前端修改');
    return;
  }
  saving.value = true;
  try {
    const updated = await roleApi.assignPermissions(selectedRole.value.id, checkedPermissionIds.value);
    const idx = roles.value.findIndex(r => r.id === updated.id);
    if (idx >= 0) roles.value[idx] = updated;
    selectedRole.value = updated;
    checkedPermissionIds.value = [...updated.permissionIds];
    ElMessage.success('权限保存成功');
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '权限保存失败'));
  } finally { saving.value = false; }
}

function openCreate() {
  editingRoleId.value = null;
  Object.assign(form, { roleCode: '', roleName: '', description: '', status: 'ENABLED' });
  dialogPermissionIds.value = [];
  dialogVisible.value = true;
}

function openEdit(role: RoleItem) {
  if (isBuiltInRole(role)) {
    ElMessage.warning('内置角色不能修改、停用或删除');
    return;
  }
  editingRoleId.value = role.id;
  Object.assign(form, { roleCode: role.roleCode, roleName: role.roleName, description: role.description || '', status: role.status as 'ENABLED' | 'DISABLED' });
  dialogPermissionIds.value = [...role.permissionIds];
  dialogVisible.value = true;
}

async function submitRole() {
  await formRef.value?.validate();
  actionLoading.value = true;
  try {
    if (editingRoleId.value == null) {
      const created = await roleApi.createRole({
        roleCode: form.roleCode,
        roleName: form.roleName,
        description: form.description || undefined,
        permissionIds: dialogPermissionIds.value
      });
      ElMessage.success('角色创建成功');
      selectedRole.value = created;
    } else {
      const updated = await roleApi.updateRole(editingRoleId.value, {
        roleName: form.roleName,
        description: form.description || undefined,
        permissionIds: dialogPermissionIds.value
      });
      ElMessage.success('角色更新成功');
      selectedRole.value = updated;
    }
    dialogVisible.value = false;
    await fetchData();
  } catch (error) {
    ElMessage.error(getErrorMessage(error, editingRoleId.value == null ? '角色创建失败' : '角色更新失败'));
  } finally {
    actionLoading.value = false;
  }
}

async function toggleStatus(role: RoleItem) {
  if (isBuiltInRole(role)) {
    ElMessage.warning('内置角色不能修改、停用或删除');
    return;
  }
  const next = role.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const label = next === 'ENABLED' ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(`确认${label}角色“${role.roleName}”？`, '提示', { type: 'warning' });
    await roleApi.updateRoleStatus(role.id, next);
    ElMessage.success(`${label}成功`);
    await fetchData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, `${label}角色失败`));
    }
  }
}

async function deleteRole(role: RoleItem) {
  if (isBuiltInRole(role)) {
    ElMessage.warning('内置角色不能修改、停用或删除');
    return;
  }
  try {
    await ElMessageBox.confirm(`确认删除角色“${role.roleName}”？该操作不可恢复。`, '提示', { type: 'warning' });
    await roleApi.deleteRole(role.id);
    ElMessage.success('角色删除成功');
    await fetchData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '角色删除失败'));
    }
  }
}

onMounted(fetchData);

const roleMap: Record<string, string> = {
  PLATFORM_ADMIN: '平台管理员', PROJECT_ADMIN: '项目管理员',
  BUSINESS_USER: '业务人员', VIEWER: '只读用户'
};

const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
};
</script>

<template>
  <div>
    <div class="page-header">
      <h2>角色权限管理</h2>
      <el-button type="primary" @click="openCreate">新建角色</el-button>
    </div>
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索角色名称/编码" clearable style="width:240px" @keyup.enter="fetchData" @clear="fetchData" />
      <el-button type="primary" @click="fetchData">查询</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="page-alert" />
    <div v-loading="loading" class="role-layout">
      <!-- 角色列表 -->
      <el-card class="role-list" shadow="never">
        <template #header><span>角色列表</span></template>
        <el-empty v-if="!roles.length" description="暂无角色" />
        <div v-for="role in roles" :key="role.id"
             :class="['role-item', { active: selectedRole?.id === role.id }]"
             @click="selectRole(role)">
          <div class="role-row">
            <div>
              <div class="role-name">{{ roleMap[role.roleCode] || role.roleName }}</div>
              <div class="role-code">{{ role.roleCode }}</div>
            </div>
            <el-tag :type="role.status === 'ENABLED' ? 'success' : 'info'" size="small">{{ role.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
          </div>
        </div>
      </el-card>

      <!-- 权限配置 -->
      <el-card class="perm-panel" shadow="never">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>权限配置{{ selectedRole ? ` — ${roleMap[selectedRole.roleCode] || selectedRole.roleName}` : '' }}</span>
            <div v-if="selectedRole" class="panel-actions">
              <el-button size="small" :disabled="isBuiltInRole(selectedRole)" @click="openEdit(selectedRole)">编辑</el-button>
              <el-button size="small" :disabled="isBuiltInRole(selectedRole)" @click="toggleStatus(selectedRole)">
                {{ selectedRole.status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" :disabled="isBuiltInRole(selectedRole)" @click="deleteRole(selectedRole)">删除</el-button>
              <el-button type="primary" size="small" :loading="saving" :disabled="!selectedRole || isBuiltInRole(selectedRole)" @click="savePermissions">保存权限</el-button>
            </div>
          </div>
        </template>
        <el-alert v-if="isBuiltInRole(selectedRole)" title="内置角色不能修改、停用、删除或重新分配权限" type="info" show-icon :closable="false" class="page-alert" />
        <div v-if="!selectedRole" class="empty-tip">请选择角色</div>
        <div v-else>
          <div v-for="(perms, group) in groupedPermissions" :key="group" class="perm-group">
            <div class="group-title">{{ groupLabels[group] || group }}</div>
            <el-checkbox-group v-model="checkedPermissionIds" class="perm-checks">
              <el-checkbox v-for="p in perms" :key="p.id" :value="p.id" :label="p.id">
                {{ p.permissionName }}<span class="perm-code">{{ p.permissionCode }}</span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingRoleId == null ? '新建角色' : '编辑角色'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="editingRoleId != null" placeholder="例如 CUSTOM_AUDITOR" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="权限">
          <el-checkbox-group v-model="dialogPermissionIds" class="dialog-perm-checks">
            <el-checkbox v-for="p in permissions" :key="p.id" :value="p.id" :label="p.id">
              {{ p.permissionName }}<span class="perm-code">{{ p.permissionCode }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { display:flex;justify-content:space-between;align-items:center;margin-bottom:16px }
.page-header h2 { margin:0;font-size:18px }
.search-bar { display:flex;gap:10px;margin-bottom:16px }
.page-alert { margin-bottom:12px }
.role-layout { display:flex;gap:16px;align-items:flex-start }
.role-list { width:220px;flex-shrink:0 }
.perm-panel { flex:1 }
.role-item { padding:10px 12px;border-radius:8px;cursor:pointer;margin-bottom:4px;transition:background .15s }
.role-item:hover { background:#f5f7fa }
.role-item.active { background:#ecf5ff }
.role-row { display:flex;justify-content:space-between;align-items:flex-start;gap:8px }
.role-name { font-weight:600;font-size:14px }
.role-code { font-size:12px;color:#999;margin-top:2px }
.panel-actions { display:flex;gap:8px;align-items:center;flex-wrap:wrap }
.perm-group { margin-bottom:18px }
.group-title { font-weight:600;font-size:13px;color:#333;margin-bottom:8px;padding-bottom:4px;border-bottom:1px solid #eee }
.perm-checks { display:flex;flex-wrap:wrap;gap:8px }
.perm-checks :deep(.el-checkbox) { margin-right:0 }
.dialog-perm-checks { display:flex;max-height:260px;overflow:auto;flex-wrap:wrap;gap:8px;padding:8px;border:1px solid #e4e7ed;border-radius:6px }
.dialog-perm-checks :deep(.el-checkbox) { margin-right:0 }
.perm-code { margin-left:6px;font-size:11px;color:#aaa }
.empty-tip { color:#aaa;text-align:center;padding:40px }
</style>
