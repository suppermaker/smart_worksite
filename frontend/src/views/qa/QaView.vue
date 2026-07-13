<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchDataSources } from '../../api/datasource';
import { fetchKnowledgeBases } from '../../api/knowledge';
import { createQaSession, fetchQaMessages, fetchQaSessions, sendQuestion, submitFeedback } from '../../api/qa';
import { useProjectStore } from '../../stores/project';
import { useUserStore } from '../../stores/user';
import type { DataSourceItem, ID, KnowledgeBase, QaMessage, QaSession } from '../../api/types';
import { hasSuspiciousText } from '../../utils/textQuality';

type QaMessageExtra = QaMessage & Record<string, unknown>;

const projectStore = useProjectStore();
const userStore = useUserStore();
const sessionLoading = ref(false);
const messageLoading = ref(false);
const sending = ref(false);
const sessionError = ref('');
const messageError = ref('');
const question = ref('');
const sessions = ref<QaSession[]>([]);
const activeSessionId = ref<ID>('');
const messages = ref<QaMessageExtra[]>([]);
const feedbackMap = ref<Record<string, boolean>>({});
const routeMode = ref<'MODEL' | 'KNOWLEDGE' | 'DATABASE' | 'MIXED'>('MODEL');
const knowledgeBases = ref<KnowledgeBase[]>([]);
const dataSources = ref<DataSourceItem[]>([]);
const selectedKnowledgeBaseIds = ref<ID[]>([]);
const selectedDataSourceId = ref<ID | ''>('');
const resourceLoading = ref(false);
const resourceError = ref('');

const activeSession = computed(() => sessions.value.find((item) => String(item.sessionId) === String(activeSessionId.value)) || null);
const enabledKnowledgeBases = computed(() => knowledgeBases.value.filter((item) => ['ENABLED', 'ACTIVE'].includes(String(item.status).toUpperCase())));
const enabledDataSources = computed(() => dataSources.value.filter((item) => ['ENABLED', 'ACTIVE'].includes(String(item.status).toUpperCase())));
const canManageQa = computed(() => userStore.hasPermission('qa:manage'));
const qaManageTip = '当前账号没有问答操作权限';

function t(text: string) { return text; }
function localId() { return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`); }

function createLocalUserMessage(sessionId: ID, projectId: ID, content: string): QaMessageExtra {
  const now = new Date().toISOString();
  const id = localId();
  return { messageId: id, sessionId, projectId, role: 'user', content, question: content, status: 'SUCCESS', createdAt: now, updatedAt: now };
}

function createErrorAssistantMessage(sessionId: ID, projectId: ID, content: string): QaMessageExtra {
  const now = new Date().toISOString();
  const id = localId();
  return { messageId: id, sessionId, projectId, role: 'assistant', content, answer: content, status: 'FAILED', createdAt: now, updatedAt: now, references: [] };
}

async function loadResources(projectId: ID) {
  resourceLoading.value = true;
  resourceError.value = '';
  try {
    const [bases, sources] = await Promise.all([
      fetchKnowledgeBases(projectId),
      fetchDataSources({ projectId, pageNo: 1, pageSize: 100 })
    ]);
    knowledgeBases.value = bases;
    dataSources.value = sources.records;
    selectedKnowledgeBaseIds.value = selectedKnowledgeBaseIds.value.filter((id) => enabledKnowledgeBases.value.some((item) => String(item.knowledgeBaseId) === String(id)));
    if (selectedDataSourceId.value && !enabledDataSources.value.some((item) => String(item.dataSourceId) === String(selectedDataSourceId.value))) {
      selectedDataSourceId.value = '';
    }
  } catch (err) {
    knowledgeBases.value = [];
    dataSources.value = [];
    selectedKnowledgeBaseIds.value = [];
    selectedDataSourceId.value = '';
    resourceError.value = err instanceof Error ? err.message : t('问答资源加载失败，请检查知识库和数据源接口。');
  } finally {
    resourceLoading.value = false;
  }
}

async function loadSessions(selectId?: ID) {
  sessionLoading.value = true;
  sessionError.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const projectId = projectStore.currentProject?.projectId;
    if (!projectId) {
      sessions.value = [];
      activeSessionId.value = '';
      messages.value = [];
      return;
    }
    await loadResources(projectId);
    sessions.value = await fetchQaSessions(projectId);
    const next = selectId || activeSessionId.value || sessions.value[0]?.sessionId || '';
    activeSessionId.value = next;
    if (next) await switchSession(next);
  } catch (err) {
    sessionError.value = err instanceof Error ? err.message : t('问答会话加载失败，请检查后端问答接口。');
    sessions.value = [];
  } finally {
    sessionLoading.value = false;
  }
}

async function switchSession(sessionId: ID) {
  activeSessionId.value = sessionId;
  messageLoading.value = true;
  messageError.value = '';
  messages.value = [];
  try {
    messages.value = await fetchQaMessages(sessionId) as QaMessageExtra[];
  } catch (err) {
    messageError.value = err instanceof Error ? err.message : t('会话消息加载失败，请检查后端问答接口。');
  } finally {
    messageLoading.value = false;
  }
}

async function newSession() {
  if (!canManageQa.value) return ElMessage.warning(qaManageTip);
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning(t('请先选择项目'));
  sessionLoading.value = true;
  sessionError.value = '';
  try {
    const created = await createQaSession({ projectId, title: `${t('新建会话')} ${sessions.value.length + 1}` });
    sessions.value.unshift(created);
    activeSessionId.value = created.sessionId;
    messages.value = [];
  } catch (err) {
    sessionError.value = err instanceof Error ? err.message : t('问答会话加载失败，请检查后端问答接口。');
  } finally {
    sessionLoading.value = false;
  }
}

async function ask() {
  if (!canManageQa.value) return ElMessage.warning(qaManageTip);
  const content = question.value.trim();
  if (!content) return ElMessage.warning(t('请输入问题'));
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning(t('请先选择项目'));
  if (!activeSessionId.value) return ElMessage.warning(t('请先新建会话'));
  if (routeMode.value === 'DATABASE' && !selectedDataSourceId.value) return ElMessage.warning(t('数据库问答必须选择一个已启用的数据源'));
  if (routeMode.value === 'MIXED' && selectedDataSourceId.value && !enabledDataSources.value.some((item) => String(item.dataSourceId) === String(selectedDataSourceId.value))) return ElMessage.warning(t('请选择已启用的数据源'));
  if (routeMode.value === 'KNOWLEDGE' && !selectedKnowledgeBaseIds.value.length) return ElMessage.warning(t('知识库问答必须选择至少一个已启用知识库'));
  sending.value = true;
  messageError.value = '';
  const sessionId = activeSessionId.value;
  messages.value.push(createLocalUserMessage(sessionId, projectId, content));
  question.value = '';
  try {
    const payload = {
      question: content,
      routeMode: routeMode.value,
      knowledgeBaseIds: ['KNOWLEDGE', 'MIXED'].includes(routeMode.value) ? selectedKnowledgeBaseIds.value : [],
      dataSourceIds: ['DATABASE', 'MIXED'].includes(routeMode.value) && selectedDataSourceId.value ? [selectedDataSourceId.value] : []
    };
    const answer = await sendQuestion(sessionId, payload, projectId) as QaMessageExtra;
    messages.value.push(answer);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    messages.value.push(createErrorAssistantMessage(sessionId, projectId, `${t('问题发送失败，请检查后端问答接口。')}${detail}`));
  } finally {
    sending.value = false;
  }
}

async function feedback(message: QaMessageExtra, useful: boolean) {
  if (!canManageQa.value) return ElMessage.warning(qaManageTip);
  try {
    await submitFeedback(message.messageId, useful);
    feedbackMap.value[String(message.messageId)] = useful;
    ElMessage.success(t('反馈已提交'));
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`${t('反馈提交失败，请检查后端问答反馈接口。')}${detail}`);
  }
}

onMounted(() => loadSessions());
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ t('知识问答') }}</h2>
        <p class="page-desc">{{ t('支持连续提问、来源引用和答案反馈。') }}</p>
      </div>
      <el-button v-if="canManageQa" type="primary" :loading="sessionLoading" @click="newSession">{{ t('新建会话') }}</el-button>
    </div>

    <div class="three-col">
      <el-card class="work-card" v-loading="sessionLoading">
        <h3 class="panel-title">{{ t('会话列表') }}</h3>
        <el-alert v-if="sessionError" :title="sessionError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
        <EmptyState v-if="!sessions.length" :description="t('暂无会话，请新建会话。')" />
        <div v-else class="session-list">
          <button v-for="item in sessions" :key="item.sessionId" type="button" class="session-item" :class="{ active: String(activeSessionId) === String(item.sessionId) }" @click="switchSession(item.sessionId)">
            <strong>{{ item.title }} <el-tag v-if="hasSuspiciousText(item.title)" type="warning" size="small">疑似历史乱码数据</el-tag></strong>
            <span>{{ item.updatedAt || item.createdAt }}</span>
          </button>
        </div>
      </el-card>

      <el-card class="work-card qa-main" v-loading="messageLoading">
        <h3 class="panel-title">{{ t('对话区') }}{{ activeSession ? ` / ${activeSession.title}` : '' }}</h3>
        <el-alert v-if="messageError" :title="messageError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
        <el-alert v-if="resourceError" :title="resourceError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
        <div class="qa-options" v-loading="resourceLoading">
          <el-select v-model="routeMode" style="width: 160px">
            <el-option label="模型问答" value="MODEL" />
            <el-option label="知识库问答" value="KNOWLEDGE" />
            <el-option label="数据库问答" value="DATABASE" />
            <el-option label="混合问答" value="MIXED" />
          </el-select>
          <el-select v-if="['KNOWLEDGE', 'MIXED'].includes(routeMode)" v-model="selectedKnowledgeBaseIds" multiple collapse-tags collapse-tags-tooltip style="min-width: 240px" placeholder="选择已启用知识库">
            <el-option v-for="item in enabledKnowledgeBases" :key="item.knowledgeBaseId" :label="item.name" :value="item.knowledgeBaseId" />
          </el-select>
          <el-select v-if="['DATABASE', 'MIXED'].includes(routeMode)" v-model="selectedDataSourceId" clearable style="min-width: 220px" placeholder="选择一个已启用数据源">
            <el-option v-for="item in enabledDataSources" :key="item.dataSourceId" :label="item.name" :value="item.dataSourceId" />
          </el-select>
        </div>
        <EmptyState v-if="!messages.length" :description="t('暂无消息，请输入问题开始问答。')" />
        <div v-for="msg in messages" :key="msg.messageId" class="chat" :class="String(msg.role || 'assistant')">
          <b>{{ msg.role === 'user' ? t('我') : t('AI助手') }}</b>
          <el-tag v-if="hasSuspiciousText(msg.content || msg.answer)" type="warning" size="small" style="margin-left: 6px">疑似历史乱码数据</el-tag>
          <p>{{ msg.content || msg.answer }}</p>
          <template v-if="msg.role === 'assistant'">
            <div class="reference-block">
              <strong>{{ t('来源引用') }}</strong>
              <EmptyState v-if="!msg.references?.length" :description="t('暂无来源引用。')" />
              <div v-for="source in msg.references" :key="`${msg.messageId}-${source.title}-${source.page}`" class="source">
                <b>{{ source.title }}</b>
                <p class="muted">{{ t('来源：') }}{{ source.sourceType }} / {{ t('位置：') }}{{ source.page || '-' }} / {{ t('分数：') }}{{ source.score }}</p>
              </div>
            </div>
            <div class="feedback">
              <span v-if="feedbackMap[String(msg.messageId)] !== undefined">{{ t('已反馈：') }}{{ feedbackMap[String(msg.messageId)] ? t('有用') : t('无用') }}</span>
              <template v-else>
                <el-button v-if="canManageQa" size="small" @click="feedback(msg, true)">{{ t('有用') }}</el-button>
                <el-button v-if="canManageQa" size="small" @click="feedback(msg, false)">{{ t('无用') }}</el-button>
              </template>
            </div>
          </template>
        </div>
        <el-input v-model="question" type="textarea" :rows="3" :placeholder="canManageQa ? t('\u8bf7\u8f93\u5165\u95ee\u9898') : qaManageTip" :disabled="!canManageQa" @keyup.ctrl.enter="ask" />
        <el-button v-if="canManageQa" type="primary" style="margin-top: 10px" :loading="sending" :disabled="sending" @click="ask">{{ t('发送') }}</el-button>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.three-col { display: grid; grid-template-columns: 280px 1fr; gap: 16px; align-items: start; }
.session-list { display: grid; gap: 10px; }
.session-item { text-align: left; border: 1px solid var(--sw-border); border-radius: 10px; background: #fff; padding: 12px; cursor: pointer; display: grid; gap: 6px; }
.session-item.active { border-color: var(--sw-primary); box-shadow: 0 0 0 3px rgba(30, 94, 255, 0.12); }
.session-item span, .muted { color: var(--sw-muted); font-size: 12px; }
.chat { padding: 12px; border: 1px solid var(--sw-border); border-radius: 12px; margin-bottom: 12px; }
.chat.user { background: #f8fafc; }
.chat.assistant { background: #fff; }
.chat p { margin: 6px 0; }
.reference-block { margin-top: 10px; padding: 10px; background: #f8fafc; border-radius: 10px; }
.source { padding: 8px 0; border-bottom: 1px solid var(--sw-border); }
.feedback { margin-top: 8px; display: flex; gap: 8px; align-items: center; color: var(--sw-muted); }
.qa-options { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
@media (max-width: 960px) { .three-col { grid-template-columns: 1fr; } }
</style>
