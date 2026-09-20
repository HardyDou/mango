<template>
  <div
    class="rich-text-viewer"
    data-testid="mango-rich-text-viewer"
    @click="handlePreviewClick"
    @keydown="handlePreviewKeydown"
  >
    <div v-if="loading" class="rich-text-viewer__status" role="status">正在加载资源…</div>
    <!-- eslint-disable vue/no-v-html -- renderedHtml is filtered by sanitizeRichTextHtml before assignment. -->
    <div
      v-if="hasContent"
      ref="contentRef"
      class="rich-text-viewer__content"
      :class="{ 'is-loading': loading }"
      v-html="renderedHtml"
    />
    <!-- eslint-enable vue/no-v-html -->
    <span v-if="!hasContent && !loading" class="rich-text-viewer__empty">{{ emptyText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { downloadUploadedFile, getUploadedFileDetail, type FileId, type UploadResult } from '../../api/upload';
import { collectManagedFileIds, renderManagedHtml } from '../Editor/managedImages';
import { sanitizeRichTextHtml } from './sanitize';
import type {
  RichTextAssetContentResolver,
  RichTextAssetResolver,
  RichTextViewerPreviewRequest,
  RichTextViewerResolveError,
} from './types';

defineOptions({ name: 'MangoRichTextViewer' });

const props = withDefaults(
  defineProps<{
    content?: string;
    resolveFile?: RichTextAssetResolver;
    resolveFileContent?: RichTextAssetContentResolver;
    emptyText?: string;
  }>(),
  {
    content: '',
    resolveFile: undefined,
    resolveFileContent: undefined,
    emptyText: '-',
  },
);

const emit = defineEmits<{
  (event: 'resolve-error', payload: RichTextViewerResolveError): void;
  (event: 'preview-request', payload: RichTextViewerPreviewRequest): void;
}>();

const contentRef = ref<HTMLElement | null>(null);
const renderedHtml = ref('');
const loading = ref(false);
const resolvedAssets = new Map<FileId, UploadResult>();
const objectUrls = new Set<string>();
let renderVersion = 0;
const hasContent = computed(() => hasMeaningfulContent(renderedHtml.value));

watch(
  () => [props.content, props.resolveFile, props.resolveFileContent] as const,
  () => {
    void renderContent();
  },
  { immediate: true },
);

async function renderContent() {
  const version = ++renderVersion;
  releaseObjectUrls();
  resolvedAssets.clear();
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
        const asset = await resolver(id);
        resolvedAssets.set(id, asset);
        assets.set(id, await resolveRenderableAsset(id, asset, version));
      } catch (error) {
        assets.set(id, undefined);
        emit('resolve-error', { fileId: id, error });
      }
    }),
  );
  if (version !== renderVersion) {
    loading.value = false;
    return;
  }
  renderedHtml.value = decorateManagedNodes(sanitizeRichTextHtml(renderManagedHtml(sanitized, assets)));
  loading.value = false;
}

async function resolveRenderableAsset(id: FileId, asset: UploadResult, version: number) {
  const previewUrl = firstSafePreviewUrl(asset.directPreviewUrl, asset.previewUrl, asset.url);
  if (previewUrl && !isProtectedFileUrl(previewUrl)) {
    return { ...asset, url: previewUrl, previewUrl };
  }
  if (!isImageAsset(asset)) return asset;
  const resolver = props.resolveFileContent || defaultContentResolver;
  const blob = await resolver(id, asset);
  const objectUrl = URL.createObjectURL(blob);
  if (version !== renderVersion) {
    URL.revokeObjectURL(objectUrl);
    return asset;
  }
  objectUrls.add(objectUrl);
  return { ...asset, url: objectUrl, previewUrl: objectUrl };
}

async function defaultContentResolver(id: FileId) {
  const response = await downloadUploadedFile(id);
  return response.data;
}

function decorateManagedNodes(source: string) {
  const root = document.createElement('div');
  root.innerHTML = source;
  root.querySelectorAll<HTMLElement>('[data-file-id]').forEach((node) => {
    node.setAttribute('role', 'button');
    node.setAttribute('tabindex', '0');
  });
  return root.innerHTML;
}

function handlePreviewClick(event: MouseEvent) {
  const target = previewTarget(event.target);
  if (!target) return;
  event.preventDefault();
  emitPreviewRequest(target);
}

function handlePreviewKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' && event.key !== ' ') return;
  const target = previewTarget(event.target);
  if (!target) return;
  event.preventDefault();
  emitPreviewRequest(target);
}

function previewTarget(target: EventTarget | null) {
  if (!(target instanceof Element)) return null;
  const node = target.closest<HTMLElement>('[data-file-id]');
  return node && contentRef.value?.contains(node) ? node : null;
}

function emitPreviewRequest(target: HTMLElement) {
  const fileId = String(target.dataset.fileId || '').trim();
  if (!fileId) return;
  const asset = resolvedAssets.get(fileId);
  emit('preview-request', {
    fileId,
    kind: target.dataset.fileKind === 'image' || target.tagName === 'IMG' ? 'image' : 'attachment',
    fileName:
      asset?.fileName || target.dataset.fileName || target.getAttribute('alt') || target.textContent?.trim() || '文件',
    asset,
  });
}

function firstSafePreviewUrl(...values: Array<string | undefined>) {
  return values.find((value) => value && !/^\s*javascript:/i.test(value)) || '';
}

function isProtectedFileUrl(value: string) {
  let pathname = '';
  try {
    pathname = new URL(value, window.location.origin).pathname;
  } catch {
    [pathname] = value.split(/[?#]/);
  }
  const path = pathname.toLowerCase().replace(/\/+$/, '');
  return (
    path === '/api/file/files/download' ||
    path === '/file/files/download' ||
    path === '/api/file/files/preview-content' ||
    path === '/file/files/preview-content' ||
    path.startsWith('/api/file/local-objects/') ||
    path.startsWith('/file/local-objects/')
  );
}

function isImageAsset(asset: UploadResult) {
  if (asset.contentType?.startsWith('image/')) return true;
  return /\.(?:bmp|gif|ico|jpe?g|png|svg|webp)$/i.test(asset.fileName || '');
}

function hasMeaningfulContent(source: string) {
  if (!source.trim()) return false;
  const root = document.createElement('div');
  root.innerHTML = source;
  if ((root.textContent || '').replace(/\u00a0/g, ' ').trim()) return true;
  return Boolean(root.querySelector('img[data-file-id], a[data-file-id]'));
}

function releaseObjectUrls() {
  objectUrls.forEach((url) => URL.revokeObjectURL(url));
  objectUrls.clear();
}

onBeforeUnmount(() => {
  renderVersion += 1;
  releaseObjectUrls();
  resolvedAssets.clear();
});
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
      display: block;
      max-width: 100%;
      height: auto;
      object-fit: contain;
    }

    :deep([data-file-id]) {
      cursor: pointer;
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

  &__empty {
    color: var(--el-text-color-regular, #606266);
  }
}
</style>
