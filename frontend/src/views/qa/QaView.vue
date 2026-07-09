<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import EmptyState from '../../components/common/EmptyState.vue';
import { createQaSession, fetchQaMessages, fetchQaSessions, sendQuestion, submitFeedback } from '../../api/qa';
import { useProjectStore } from '../../stores/project';
import type { ID, QaMessage, QaSession } from '../../api/types';

type QaMessageExtra = QaMessage & Record<string, unknown>;

const projectStore = useProjectStore();
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

const activeSession = computed(() => sessions.value.find((item) => String(item.sessionId) === String(activeSessionId.value)) || null);

function t(text: string) { return text; }
function localId() { return Number(`${Date.now()}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`); }

function createLocalUserMessage(sessionId: ID, projectId: ID, content: string): QaMessageExtra {
  const now = new Date().toISOString();
  const id = localId();
  return { id, messageId: id, sessionId, projectId, taskId: 0, fileId: 0, role: 'user', content, question: content, status: 'SUCCESS', createdAt: now, updatedAt: now };
}

function createErrorAssistantMessage(sessionId: ID, projectId: ID, content: string): QaMessageExtra {
  const now = new Date().toISOString();
  const id = localId();
  return { id, messageId: id, sessionId, projectId, taskId: 0, fileId: 0, role: 'assistant', content, answer: content, status: 'FAILED', createdAt: now, updatedAt: now, references: [] };
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
    sessions.value = await fetchQaSessions(projectId);
    const next = selectId || activeSessionId.value || sessions.value[0]?.sessionId || '';
    activeSessionId.value = next;
    if (next) await switchSession(next);
  } catch (err) {
    sessionError.value = err instanceof Error ? err.message : t('\u95ee\u7b54\u4f1a\u8bdd\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u95ee\u7b54\u63a5\u53e3\u3002');
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
    messageError.value = err instanceof Error ? err.message : t('\u4f1a\u8bdd\u6d88\u606f\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u95ee\u7b54\u63a5\u53e3\u3002');
  } finally {
    messageLoading.value = false;
  }
}

async function newSession() {
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning(t('\u8bf7\u5148\u9009\u62e9\u9879\u76ee'));
  sessionLoading.value = true;
  sessionError.value = '';
  try {
    const created = await createQaSession({ projectId, title: `${t('\u65b0\u5efa\u4f1a\u8bdd')} ${sessions.value.length + 1}` });
    sessions.value.unshift(created);
    activeSessionId.value = created.sessionId;
    messages.value = [];
  } catch (err) {
    sessionError.value = err instanceof Error ? err.message : t('\u95ee\u7b54\u4f1a\u8bdd\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u95ee\u7b54\u63a5\u53e3\u3002');
  } finally {
    sessionLoading.value = false;
  }
}

async function ask() {
  const content = question.value.trim();
  if (!content) return ElMessage.warning(t('\u8bf7\u8f93\u5165\u95ee\u9898'));
  const projectId = projectStore.currentProject?.projectId;
  if (!projectId) return ElMessage.warning(t('\u8bf7\u5148\u9009\u62e9\u9879\u76ee'));
  if (!activeSessionId.value) return ElMessage.warning(t('\u8bf7\u5148\u65b0\u5efa\u4f1a\u8bdd'));
  sending.value = true;
  messageError.value = '';
  const sessionId = activeSessionId.value;
  messages.value.push(createLocalUserMessage(sessionId, projectId, content));
  question.value = '';
  try {
    const answer = await sendQuestion(sessionId, { projectId, question: content, routeMode: 'MIXED' }) as QaMessageExtra;
    messages.value.push(answer);
  } catch {
    messages.value.push(createErrorAssistantMessage(sessionId, projectId, t('\u95ee\u9898\u53d1\u9001\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u95ee\u7b54\u63a5\u53e3\u3002')));
  } finally {
    sending.value = false;
  }
}

async function feedback(message: QaMessageExtra, useful: boolean) {
  try {
    await submitFeedback(message.messageId, useful);
    feedbackMap.value[String(message.messageId)] = useful;
    ElMessage.success(t('\u53cd\u9988\u5df2\u63d0\u4ea4'));
  } catch {
    ElMessage.error(t('\u53cd\u9988\u63d0\u4ea4\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u95ee\u7b54\u53cd\u9988\u63a5\u53e3\u3002'));
  }
}

onMounted(() => loadSessions());
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ t('\u77e5\u8bc6\u95ee\u7b54') }}</h2>
        <p class="page-desc">{{ t('\u652f\u6301\u8fde\u7eed\u63d0\u95ee\u3001\u6765\u6e90\u5f15\u7528\u548c\u7b54\u6848\u53cd\u9988\u3002') }}</p>
      </div>
      <el-button type="primary" :loading="sessionLoading" @click="newSession">{{ t('\u65b0\u5efa\u4f1a\u8bdd') }}</el-button>
    </div>

    <div class="three-col">
      <el-card class="work-card" v-loading="sessionLoading">
        <h3 class="panel-title">{{ t('\u4f1a\u8bdd\u5217\u8868') }}</h3>
        <el-alert v-if="sessionError" :title="sessionError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
        <EmptyState v-if="!sessions.length" :description="t('\u6682\u65e0\u4f1a\u8bdd\uff0c\u8bf7\u65b0\u5efa\u4f1a\u8bdd\u3002')" />
        <div v-else class="session-list">
          <button v-for="item in sessions" :key="item.sessionId" type="button" class="session-item" :class="{ active: String(activeSessionId) === String(item.sessionId) }" @click="switchSession(item.sessionId)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.updatedAt || item.createdAt }}</span>
          </button>
        </div>
      </el-card>

      <el-card class="work-card qa-main" v-loading="messageLoading">
        <h3 class="panel-title">{{ t('\u5bf9\u8bdd\u533a') }}{{ activeSession ? ` / ${activeSession.title}` : '' }}</h3>
        <el-alert v-if="messageError" :title="messageError" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
        <EmptyState v-if="!messages.length" :description="t('\u6682\u65e0\u6d88\u606f\uff0c\u8bf7\u8f93\u5165\u95ee\u9898\u5f00\u59cb\u95ee\u7b54\u3002')" />
        <div v-for="msg in messages" :key="msg.messageId" class="chat" :class="msg.role">
          <b>{{ msg.role === 'user' ? t('\u6211') : t('AI\u52a9\u624b') }}</b>
          <p>{{ msg.content || msg.answer }}</p>
          <template v-if="msg.role === 'assistant'">
            <div class="reference-block">
              <strong>{{ t('\u6765\u6e90\u5f15\u7528') }}</strong>
              <EmptyState v-if="!msg.references?.length" :description="t('\u6682\u65e0\u6765\u6e90\u5f15\u7528\u3002')" />
              <div v-for="source in msg.references" :key="`${msg.messageId}-${source.title}-${source.page}`" class="source">
                <b>{{ source.title }}</b>
                <p class="muted">{{ t('\u6765\u6e90\uff1a') }}{{ source.sourceType }} / {{ t('\u4f4d\u7f6e\uff1a') }}{{ source.page || '-' }} / {{ t('\u5206\u6570\uff1a') }}{{ source.score }}</p>
              </div>
            </div>
            <div class="feedback">
              <span v-if="feedbackMap[String(msg.messageId)] !== undefined">{{ t('\u5df2\u53cd\u9988\uff1a') }}{{ feedbackMap[String(msg.messageId)] ? t('\u6709\u7528') : t('\u65e0\u7528') }}</span>
              <template v-else>
                <el-button size="small" @click="feedback(msg, true)">{{ t('\u6709\u7528') }}</el-button>
                <el-button size="small" @click="feedback(msg, false)">{{ t('\u65e0\u7528') }}</el-button>
              </template>
            </div>
          </template>
        </div>
        <el-input v-model="question" type="textarea" :rows="3" :placeholder="t('\u8bf7\u8f93\u5165\u95ee\u9898')" @keyup.ctrl.enter="ask" />
        <el-button type="primary" style="margin-top: 10px" :loading="sending" :disabled="sending" @click="ask">{{ t('\u53d1\u9001') }}</el-button>
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
@media (max-width: 960px) { .three-col { grid-template-columns: 1fr; } }
</style>
