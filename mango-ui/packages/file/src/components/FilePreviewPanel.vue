<template>
  <div class="file-preview-panel">
    <el-skeleton v-if="loading" :rows="4" animated />
    <el-empty v-else-if="!preview" description="暂无文件" />
    <template v-else>
      <div v-if="showActions" class="preview-actions">
        <el-tag size="small" :type="previewModeTag">
          {{ previewModeLabel }}
        </el-tag>
        <el-button v-auth="downloadPermission" link type="primary" @click="openDownload">
          下载
        </el-button>
        <el-button v-if="canOpenInNewWindow" link type="primary" @click="openPreviewInNewWindow">
          新窗口预览
        </el-button>
      </div>

      <div class="preview-stage">
        <el-image
          v-if="isImage && inlinePreviewUrl"
          :src="inlinePreviewUrl"
          fit="contain"
          class="preview-image"
          :preview-src-list="[inlinePreviewUrl]"
        />
        <iframe
          v-else-if="isPdf && inlinePreviewUrl"
          class="preview-frame"
          :src="inlinePreviewUrl"
          title="文件预览"
        />
        <video
          v-else-if="isVideo && inlinePreviewUrl"
          class="preview-media"
          :src="inlinePreviewUrl"
          controls
        />
        <audio
          v-else-if="isAudio && inlinePreviewUrl"
          class="preview-audio"
          :src="inlinePreviewUrl"
          controls
        />
        <iframe
          v-else-if="documentPreviewUrl"
          class="preview-frame"
          :src="documentPreviewUrl"
          title="文件预览"
        />
        <div v-else class="preview-placeholder">
          <el-icon><Document /></el-icon>
          <div class="preview-name">{{ preview.fileName }}</div>
          <div class="preview-tip">当前类型需要接入文档预览服务，可下载后查看</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { Document } from '@element-plus/icons-vue';
import { downloadFileRecord, fileApi, normalizeFileId, type FilePreview, type FileReference } from '../api/file';
import { isPreviewDisplayUrl } from '../utils/previewUrl';
import type { ApiId } from '@mango/api-schema';

type PreviewActionsState = {
  canDownload: boolean;
  canOpenInNewWindow: boolean;
};

const props = withDefaults(defineProps<{
  fileId?: ApiId | `mango-file:${string}` | null;
  file?: FileReference;
  preview?: FilePreview | null;
  previewProviderUrl?: string;
  previewExternalExtensions?: string[];
  downloadPermission?: string;
  showActions?: boolean;
}>(), {
  showActions: true,
});

const emit = defineEmits<{
  (event: 'actions-change', value: PreviewActionsState): void;
}>();

const loading = ref(false);
const loadedPreview = ref<FilePreview | null>(null);
const inlinePreviewUrl = ref('');
const externalPreviewUrl = ref('');

const preview = computed(() => props.preview || loadedPreview.value);
const resolvedFileId = computed(() => normalizeFileId(props.file || props.fileId));

const isImage = computed(() => {
  const item = preview.value;
  const ext = extension.value;
  return Boolean(item?.contentType?.startsWith('image/'))
    || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'ico'].includes(ext);
});

const isPdf = computed(() => {
  const item = preview.value;
  return item?.contentType === 'application/pdf' || item?.fileExt?.toLowerCase() === 'pdf';
});

const isVideo = computed(() => {
  const ext = extension.value;
  return Boolean(preview.value?.contentType?.startsWith('video/'))
    || ['mp4', 'webm', 'ogg', 'mov', 'm4v'].includes(ext);
});
const isAudio = computed(() => {
  const ext = extension.value;
  return Boolean(preview.value?.contentType?.startsWith('audio/'))
    || ['mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac'].includes(ext);
});

const extension = computed(() => preview.value?.fileExt?.toLowerCase() || fileExtension(preview.value?.fileName));
const documentPreviewUrl = computed(() => {
  const item = preview.value;
  if (!item || isImage.value || isPdf.value || isVideo.value || isAudio.value) return '';
  if (externalPreviewUrl.value) return externalPreviewUrl.value;
  if (isDefaultPreviewProviderUrl(item.previewUrl)) return '';
  if (isPreviewDisplayUrl(item.previewUrl)) return item.previewUrl;
  const providerUrl = props.previewProviderUrl || import.meta.env.VITE_FILE_PREVIEW_PROVIDER_URL;
  if (!providerUrl) return '';
  const url = new URL(providerUrl, window.location.origin);
  url.searchParams.set('fileId', String(item.id));
  url.searchParams.set('fileName', item.fileName);
  return url.toString();
});

const previewModeLabel = computed(() => {
  if (isImage.value) return '图片预览';
  if (isPdf.value) return 'PDF 预览';
  if (isVideo.value) return '视频预览';
  if (isAudio.value) return '音频预览';
  if (documentPreviewUrl.value) return '文档预览服务';
  return '下载查看';
});

const previewModeTag = computed(() => documentPreviewUrl.value ? 'success' : 'info');
const downloadPermission = computed(() => props.downloadPermission || 'file:files:download');
const previewTargetUrl = computed(() => inlinePreviewUrl.value || documentPreviewUrl.value);
const canOpenInNewWindow = computed(() => Boolean(previewTargetUrl.value));
const canDownload = computed(() => Boolean(preview.value?.id));

async function loadPreview() {
  if (props.preview || !resolvedFileId.value) {
    loadedPreview.value = null;
    return;
  }
  loading.value = true;
  try {
    loadedPreview.value = await fileApi.preview(resolvedFileId.value);
  } finally {
    loading.value = false;
  }
}

async function loadInlinePreview() {
  inlinePreviewUrl.value = '';
  externalPreviewUrl.value = '';
  const item = preview.value;
  if (!item) {
    return;
  }
  if (!isImage.value && !isPdf.value && !isVideo.value && !isAudio.value) {
    externalPreviewUrl.value = await resolveExternalPreviewUrl(item);
    return;
  }
  inlinePreviewUrl.value = resolveInlinePreviewUrl(item);
}

async function openDownload() {
  const item = preview.value;
  if (item) await downloadFileRecord(item);
}

async function openPreviewInNewWindow() {
  const item = preview.value;
  if (!item || !previewTargetUrl.value) return;

  const target = window.open('about:blank', '_blank');
  const url = documentPreviewUrl.value && isDefaultPreviewProviderUrl(item.previewUrl)
    ? await resolveExternalPreviewUrl(item)
    : previewTargetUrl.value;
  if (!url) {
    target?.close();
    return;
  }
  if (target) {
    target.opener = null;
    target.location.href = url;
    return;
  }
  window.open(url, '_blank', 'noopener,noreferrer');
}

async function resolveExternalPreviewUrl(item: FilePreview) {
  if (isDefaultPreviewProviderUrl(item.previewUrl)) {
    const link = await fileApi.previewLink(item.id);
    return isPreviewDisplayUrl(link.previewUrl) ? link.previewUrl : '';
  }
  return isPreviewDisplayUrl(item.previewUrl) ? item.previewUrl : '';
}

function resolveInlinePreviewUrl(item: FilePreview) {
  if (isPreviewDisplayUrl(item.directPreviewUrl)) return item.directPreviewUrl;
  if (isPreviewDisplayUrl(item.previewUrl)) return item.previewUrl;
  return '';
}

function isDefaultPreviewProviderUrl(value?: string) {
  if (!value) return false;
  try {
    const url = new URL(value, window.location.origin);
    return url.pathname === '/api/file-preview/files/preview'
      || url.pathname === '/file-preview/files/preview'
      || url.pathname === '/api/file-preview/files/preview-entry'
      || url.pathname === '/file-preview/files/preview-entry';
  } catch {
    return value.startsWith('/api/file-preview/files/preview') || value.startsWith('/file-preview/files/preview');
  }
}

function fileExtension(fileName?: string): string {
  if (!fileName || !fileName.includes('.')) return '';
  return fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase();
}

watch(() => [resolvedFileId.value, props.preview], loadPreview);
watch(preview, loadInlinePreview, { immediate: true });
watch(() => [canDownload.value, canOpenInNewWindow.value], () => {
  emit('actions-change', {
    canDownload: canDownload.value,
    canOpenInNewWindow: canOpenInNewWindow.value,
  });
}, { immediate: true });
onMounted(loadPreview);
onBeforeUnmount(() => {
  inlinePreviewUrl.value = '';
});

defineExpose({
  openDownload,
  openPreviewInNewWindow,
});
</script>

<style scoped>
.file-preview-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-stage {
  min-height: 220px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  overflow: hidden;
}

.preview-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.preview-image {
  width: 100%;
  height: 360px;
  display: block;
}

.preview-media {
  width: 100%;
  max-height: 420px;
  display: block;
  background: #000;
}

.preview-audio {
  width: calc(100% - 48px);
  margin: 88px 24px;
}

.preview-frame {
  width: 100%;
  height: 420px;
  border: 0;
  display: block;
  background: #fff;
}

.preview-placeholder {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 24px;
}

.preview-placeholder .el-icon {
  font-size: 42px;
  color: var(--el-color-primary);
}

.preview-name {
  max-width: 100%;
  color: var(--el-text-color-primary);
  font-weight: 600;
  word-break: break-all;
}

.preview-tip {
  font-size: 13px;
}
</style>
