<template>
  <div class="rich-text-viewer" data-testid="mango-rich-text-viewer">
    <div v-if="loading" class="rich-text-viewer__status" role="status">正在加载资源…</div>
    <!-- eslint-disable vue/no-v-html -- renderedHtml is filtered by sanitizeRichTextHtml before assignment. -->
    <div class="rich-text-viewer__content" :class="{ 'is-loading': loading }" v-html="renderedHtml" />
    <!-- eslint-enable vue/no-v-html -->
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { getUploadedFileDetail, type FileId, type UploadResult } from '../../api/upload';
import { collectManagedFileIds, renderManagedHtml } from '../Editor/managedImages';
import { sanitizeRichTextHtml } from './sanitize';
import type { RichTextAssetResolver, RichTextViewerResolveError } from './types';

defineOptions({ name: 'MangoRichTextViewer' });

const props = withDefaults(
  defineProps<{
    content?: string;
    resolveFile?: RichTextAssetResolver;
  }>(),
  {
    content: '',
    resolveFile: undefined,
  },
);

const emit = defineEmits<{
  (event: 'resolve-error', payload: RichTextViewerResolveError): void;
}>();

const renderedHtml = ref('');
const loading = ref(false);
let renderVersion = 0;

watch(
  () => [props.content, props.resolveFile] as const,
  () => {
    void renderContent();
  },
  { immediate: true },
);

async function renderContent() {
  const version = ++renderVersion;
  const sanitized = sanitizeRichTextHtml(props.content);
  const ids = collectManagedFileIds(sanitized);
  if (ids.length === 0) {
    loading.value = false;
    renderedHtml.value = sanitized;
    return;
  }

  loading.value = true;
  renderedHtml.value = sanitized;
  const resolver = props.resolveFile || getUploadedFileDetail;
  const assets = new Map<FileId, UploadResult | undefined>();
  await Promise.all(
    ids.map(async (id) => {
      try {
        assets.set(id, await resolver(id));
      } catch (error) {
        assets.set(id, undefined);
        emit('resolve-error', { fileId: id, error });
      }
    }),
  );
  if (version !== renderVersion) return;
  renderedHtml.value = sanitizeRichTextHtml(renderManagedHtml(sanitized, assets));
  loading.value = false;
}
</script>

<style scoped lang="scss">
.rich-text-viewer {
  min-width: 0;
  color: var(--el-text-color-primary, #303133);

  &__status {
    margin-bottom: 6px;
    color: var(--el-text-color-secondary, #909399);
    font-size: 12px;
  }

  &__content {
    overflow-wrap: anywhere;

    &.is-loading {
      opacity: 0.72;
    }

    :deep(img) {
      max-width: 100%;
      height: auto;
    }

    :deep(a[data-file-kind='attachment']) {
      display: inline-flex;
      max-width: 100%;
      overflow-wrap: anywhere;
    }

    :deep([data-managed-state='failed']) {
      color: var(--el-text-color-secondary, #909399);
      text-decoration-style: dashed;
    }
  }
}
</style>
