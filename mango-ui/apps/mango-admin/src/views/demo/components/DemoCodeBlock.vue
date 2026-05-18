<template>
  <div class="demo-code-editor">
    <el-button class="copy-button" text size="small" @click="copyCode">
      {{ copied ? '已复制' : '复制' }}
    </el-button>
    <CodeEditor
      :model-value="code"
      :language="language"
      :height="editorHeight"
      readonly
      :line-numbers="true"
      :match-brackets="false"
      :auto-close-brackets="false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { CodeEditor } from '@mango/common';

const props = withDefaults(defineProps<{
  code: string;
  language?: string;
}>(), {
  language: 'html',
});

const editorHeight = computed(() => {
  const lineCount = Math.max(4, props.code.split('\n').length);
  return `${Math.min(420, Math.max(128, lineCount * 22 + 28))}px`;
});

const copied = ref(false);

async function copyCode() {
  try {
    await navigator.clipboard.writeText(props.code);
    copied.value = true;
    window.setTimeout(() => {
      copied.value = false;
    }, 1600);
  } catch {
    ElMessage.error('复制失败');
  }
}
</script>

<style scoped lang="scss">
.demo-code-editor {
  position: relative;
  margin-top: 14px;
  border-top: 1px solid #ebeef5;
  background: #f8fafc;

  .copy-button {
    position: absolute;
    top: 8px;
    right: 10px;
    z-index: 2;
    height: 24px;
    padding: 0 8px;
    color: #606266;
    background: rgba(248, 250, 252, 0.92);
  }

  :deep(.code-editor-container) {
    border: 0;
    border-radius: 0;
    background: transparent;
  }

  :deep(.CodeMirror) {
    background: #f8fafc;
    color: #1f2937;
    font-size: 13px;
  }

  :deep(.CodeMirror-gutters) {
    background: #f1f5f9;
    border-right-color: #e5e7eb;
  }

  :deep(.CodeMirror-linenumber) {
    color: #94a3b8;
  }
}
</style>
