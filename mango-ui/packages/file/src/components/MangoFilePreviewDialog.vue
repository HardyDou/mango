<template>
  <MangoDialog
    v-model="visible"
    :title="title"
    class="mango-file-preview-dialog"
    draggable
    resizable
    append-to-body
    destroy-on-close
    :min-width="480"
    :min-height="320"
    @closed="emit('closed')"
  >
    <div v-loading="loading" class="mango-file-preview-dialog__content">
      <FilePreviewPanel
        v-if="visible && hasPreview"
        :file-id="normalizedFileId || undefined"
        :file="file || undefined"
        :preview="preview || undefined"
        :show-actions="showActions"
        fit-container
        class="mango-file-preview-dialog__panel"
      />
      <pre v-else-if="textContent" class="mango-file-preview-dialog__text">{{ textContent }}</pre>
      <el-empty v-else :description="emptyText" />
    </div>
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </MangoDialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { MangoDialog } from '@mango/common';
import FilePreviewPanel from './FilePreviewPanel.vue';
import type { MangoFilePreviewDialogEmits, MangoFilePreviewDialogProps } from './MangoFilePreviewDialog.types';

defineOptions({ name: 'MangoFilePreviewDialog' });

const props = withDefaults(defineProps<MangoFilePreviewDialogProps>(), {
  title: '文件预览',
  fileId: '',
  file: null,
  preview: null,
  textContent: '',
  showActions: true,
  loading: false,
  emptyText: '暂无可预览内容',
});
const emit = defineEmits<MangoFilePreviewDialogEmits>();
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
});
const normalizedFileId = computed(() => String(props.fileId || '').trim());
const hasPreview = computed(() => Boolean(normalizedFileId.value || props.file || props.preview));
</script>

<style>
.mango-file-preview-dialog__content {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
}

.mango-file-preview-dialog__panel {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}

.mango-file-preview-dialog__text {
  width: 100%;
  min-height: 0;
  padding: 16px;
  margin: 0;
  overflow: auto;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  font: inherit;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.mango-file-preview-dialog.mango-dialog.el-dialog {
  display: flex;
  width: min(60vw, calc(100vw - 48px));
  height: min(65vh, calc(100vh - 48px));
  max-width: none;
  max-height: none;
  flex-direction: column;
  margin: 24px auto 0;
  overflow: hidden;
}

.mango-file-preview-dialog .mango-dialog__header {
  flex: 0 0 auto;
  border-bottom: 1px solid var(--el-border-color-lighter);
  box-shadow: none;
}

.mango-file-preview-dialog .mango-dialog__body {
  display: flex;
  min-height: 0;
  max-height: none;
  flex: 1 1 auto;
  padding: 12px 16px 16px;
  overflow: hidden;
}

@media (width <= 768px) {
  .mango-file-preview-dialog.mango-dialog.el-dialog {
    width: calc(100vw - 24px);
    height: calc(100vh - 24px);
    margin-top: 12px;
  }
}
</style>
