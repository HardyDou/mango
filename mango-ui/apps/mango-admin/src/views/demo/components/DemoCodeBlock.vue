<template>
  <div class="demo-code-editor">
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
import { computed } from 'vue';
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
</script>

<style scoped lang="scss">
.demo-code-editor {
  margin-top: 14px;
  border-top: 1px solid #ebeef5;
  background: #f8fafc;

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
