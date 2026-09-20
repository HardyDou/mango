<template>
  <RichTextViewer :content="content" @preview-request="openPreview" />
  <MangoFilePreviewDialog
    v-model="visible"
    :title="title"
    :file-id="fileId || undefined"
    :preview="preview"
    :text-content="textContent"
    :loading="loading"
    :empty-text="emptyText"
    @closed="resetPreview"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { RichTextViewer, type RichTextViewerPreviewRequest } from '@mango/common';
import { fileApi, MangoFilePreviewDialog, type FilePreview } from '@mango/file';
import type { MangoRichTextPreviewProps } from './MangoRichTextPreview.types';

defineOptions({ name: 'MangoRichTextPreview' });
withDefaults(defineProps<MangoRichTextPreviewProps>(), { content: '' });

const visible = ref(false);
const loading = ref(false);
const title = ref('文件预览');
const fileId = ref('');
const preview = ref<FilePreview | null>(null);
const textContent = ref('');
const emptyText = ref('暂无可预览内容');
let requestSequence = 0;

function openPreview(request: RichTextViewerPreviewRequest) {
  const sequence = ++requestSequence;
  visible.value = true;
  loading.value = true;
  title.value = request.fileName || '文件预览';
  fileId.value = '';
  preview.value = null;
  textContent.value = '';
  emptyText.value = '暂无可预览内容';
  void loadPreview(String(request.fileId), sequence);
}

async function loadPreview(id: string, sequence: number) {
  try {
    const result = await fileApi.preview(id);
    if (sequence !== requestSequence || !visible.value) return;
    title.value = result.fileName || title.value;
    if (isTextPreview(result)) {
      const response = (await fileApi.previewContent(id)) as {
        data: BlobPart;
        headers?: Record<string, string | undefined>;
      };
      const contentType = result.contentType || response.headers?.['content-type'] || 'text/plain';
      const blob = response.data instanceof Blob ? response.data : new Blob([response.data], { type: contentType });
      const text = await blob.text();
      if (sequence !== requestSequence || !visible.value) return;
      textContent.value = text;
      return;
    }
    preview.value = result;
    fileId.value = id;
  } catch {
    if (sequence !== requestSequence) return;
    emptyText.value = '文件预览加载失败';
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

function isTextPreview(value: FilePreview) {
  if (
    String(value.contentType || '')
      .split(';', 1)[0]
      .trim()
      .toLowerCase()
      .startsWith('text/')
  )
    return true;
  const extension = String(value.fileExt || value.fileName.split('.').pop() || '')
    .replace(/^\./, '')
    .toLowerCase();
  return ['csv', 'json', 'log', 'md', 'sql', 'txt', 'xml', 'yaml', 'yml'].includes(extension);
}

function resetPreview() {
  requestSequence += 1;
  fileId.value = '';
  preview.value = null;
  textContent.value = '';
  loading.value = false;
  emptyText.value = '暂无可预览内容';
}
</script>
