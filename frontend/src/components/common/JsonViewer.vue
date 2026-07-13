<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage } from 'element-plus';
const props = defineProps<{ value: unknown; title?: string }>();
const jsonText = computed(() => JSON.stringify(props.value, null, 2));
async function copy() {
  try {
    await navigator.clipboard.writeText(jsonText.value);
    ElMessage.success('JSON已复制');
  } catch (error) {
    const detail = error instanceof Error && error.message ? `：${error.message}` : '';
    ElMessage.error(`JSON复制失败${detail}`);
  }
}
function download() {
  try {
    const blob = new Blob([jsonText.value], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'result.json';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (error) {
    const detail = error instanceof Error && error.message ? `：${error.message}` : '';
    ElMessage.error(`JSON下载失败${detail}`);
  }
}
</script>

<template>
  <div class="json-viewer">
    <div class="json-head"><strong>{{ title || 'JSON结果' }}</strong><span><el-button size="small" @click="copy">复制</el-button><el-button size="small" @click="download">下载</el-button></span></div>
    <pre>{{ jsonText }}</pre>
  </div>
</template>

<style scoped>.json-viewer{border:1px solid var(--sw-border);border-radius:12px;overflow:hidden;background:#0f172a}.json-head{display:flex;justify-content:space-between;align-items:center;padding:10px 12px;background:#fff}.json-viewer pre{margin:0;padding:14px;color:#dbeafe;overflow:auto;max-height:360px}</style>
