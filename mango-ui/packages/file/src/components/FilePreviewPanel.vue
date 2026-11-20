<template>
  <div v-loading="loading" class="file-preview-panel" :class="{ 'file-preview-panel--fit-container': fitContainer }">
    <div v-if="loading" class="preview-stage preview-loading-stage" data-preview-loading />
    <template v-else>
      <div v-if="showActions" class="preview-actions">
        <el-tag size="small" :type="renderModeTag">
          {{ renderModeLabel }}
        </el-tag>
        <el-button link type="primary" :disabled="!canDownload" @click="openDownload"> 下载 </el-button>
        <el-button link type="primary" :disabled="!canOpenInNewWindow" @click="openPreviewInNewWindow">
          新窗口预览
        </el-button>
      </div>

      <div class="preview-stage" :data-render-mode="target">
        <div v-if="previewError" class="preview-error-state">
          <el-icon><Document /></el-icon>
          <div>{{ previewError }}</div>
        </div>
        <!-- 渲染分派唯一入口：按后端 renderMode 选择渲染器，前端不做格式判断 -->
        <pre v-else-if="target === 'TEXT'" class="preview-text">{{ textContent }}</pre>
        <iframe
          v-else-if="target === 'PDF' || target === 'PAGE'"
          class="preview-frame"
          :src="documentUrl"
          title="文件预览"
        />
        <div v-else-if="target === 'CONVERTING'" class="preview-document">
          <iframe class="preview-frame" :src="documentUrl" title="文件转换中" />
          <div class="preview-converting-hint" data-converting>
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>文件正在转换，完成后自动显示预览</span>
          </div>
        </div>
        <div v-else-if="target === 'IMAGE'" class="preview-image-viewer">
          <el-image-viewer
            v-if="mediaUrl"
            :url-list="[mediaUrl]"
            :infinite="false"
            :teleported="false"
            :close-on-press-escape="false"
          />
        </div>
        <video v-else-if="target === 'VIDEO'" class="preview-media" :src="mediaUrl" controls />
        <audio v-else-if="target === 'AUDIO'" class="preview-audio" :src="mediaUrl" controls />
        <iframe v-else-if="target === 'LEGACY'" class="preview-frame" :src="legacyUrl" title="文件预览" />
        <div v-else-if="target" class="preview-placeholder" :data-render-hint="target">
          <el-icon><Document /></el-icon>
          <div class="preview-name">{{ displayName }}</div>
          <div class="preview-tip">{{ placeholderTip }}</div>
          <el-button link type="primary" :disabled="!canDownload" @click="openDownload">下载文件</el-button>
        </div>
        <el-empty v-else class="preview-empty" description="暂无文件" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { Document, Loading } from '@element-plus/icons-vue';
import {
  downloadFileRecord,
  normalizeFileId,
  type FilePreviewLink,
  type FilePreviewRenderMode,
  type FileRecord,
} from '../api/file';
import { useFileApi } from '../composables/useFileApi';
import { cachedPreviewLink } from '../utils/previewLinkCache';
import type { FilePreviewPanelProps } from './FilePreviewPanel.types';

type PreviewActionsState = {
  canDownload: boolean;
  canOpenInNewWindow: boolean;
};

/** 内部渲染目标：后端 renderMode 之外，补充未入库文本与旧地址兜底。 */
type PreviewTarget = FilePreviewRenderMode | 'TEXT' | 'LEGACY';

const MEDIA_RENDER_MODES: FilePreviewRenderMode[] = ['IMAGE', 'VIDEO', 'AUDIO'];

const LABELS: Partial<Record<PreviewTarget, string>> = {
  TEXT: '文本预览',
  PDF: 'PDF 预览',
  IMAGE: '图片预览',
  VIDEO: '视频预览',
  AUDIO: '音频预览',
  CONVERTING: '转换中',
  NOT_SUPPORTED: '不支持预览',
  PAGE: '页面预览',
  LEGACY: '兼容预览',
};

const fileApi = useFileApi();

const props = withDefaults(defineProps<FilePreviewPanelProps>(), {
  fileId: null,
  textContent: '',
  file: null,
  title: '',
  showActions: true,
  fitContainer: false,
  preview: null,
  previewUrl: '',
  documentPreviewUrl: '',
  directPreviewUrl: '',
  previewProviderUrl: '',
  previewExternalExtensions: () => [],
});

const emit = defineEmits<{
  (event: 'actions-change', value: PreviewActionsState): void;
}>();

const previewLink = ref<FilePreviewLink | null>(null);
const mediaUrl = ref('');
const mediaObjectUrl = ref('');
const linkLoading = ref(false);
const mediaLoading = ref(false);
const previewError = ref('');
let linkSequence = 0;
let mediaSequence = 0;

const loading = computed(() => linkLoading.value || mediaLoading.value);
const resolvedFileId = computed(() => normalizeFileId(props.file || props.fileId));
const renderMode = computed<FilePreviewRenderMode | null>(() => previewLink.value?.renderMode ?? null);
const documentUrl = computed(() => previewLink.value?.previewUrl || '');
/** 旧地址兜底：仅在未传 fileId 时生效，下一个 major 移除。 */
const legacyUrl = computed(() =>
  absoluteUrl(
    props.previewUrl ||
      props.documentPreviewUrl ||
      props.directPreviewUrl ||
      props.preview?.documentPreviewUrl ||
      props.preview?.directPreviewUrl ||
      props.preview?.previewUrl,
  ),
);
const target = computed<PreviewTarget | ''>(() => {
  if (!resolvedFileId.value) {
    if (props.textContent) return 'TEXT';
    return legacyUrl.value ? 'LEGACY' : '';
  }
  return previewLink.value ? (renderMode.value ?? '') : '';
});
const displayName = computed(
  () => props.title || props.preview?.fileName || fileNameOf(props.file) || previewLink.value?.fileName || '',
);
const renderModeLabel = computed(() => (target.value ? LABELS[target.value as PreviewTarget] || '' : ''));
const renderModeTag = computed(() =>
  target.value === 'PDF' ||
  target.value === 'PAGE' ||
  target.value === 'IMAGE' ||
  target.value === 'VIDEO' ||
  target.value === 'AUDIO'
    ? 'success'
    : 'info',
);
const placeholderTip = computed(() => {
  if (target.value === 'NOT_SUPPORTED') return '该格式不支持在线预览，请下载原文件查看';
  return '';
});
const canDownload = computed(() => Boolean(resolvedFileId.value));
const canOpenInNewWindow = computed(() => Boolean(documentUrl.value || mediaUrl.value || legacyUrl.value));

async function loadPreviewLink() {
  const id = resolvedFileId.value;
  const sequence = ++linkSequence;
  previewLink.value = null;
  previewError.value = '';
  if (!id) {
    linkLoading.value = false;
    return;
  }
  linkLoading.value = true;
  try {
    const link = await cachedPreviewLink(fileApi, id);
    if (sequence === linkSequence) previewLink.value = link;
  } catch {
    if (sequence === linkSequence) previewError.value = '文件预览加载失败';
  } finally {
    if (sequence === linkSequence) linkLoading.value = false;
  }
}

async function loadMedia() {
  const id = resolvedFileId.value;
  const mode = target.value as FilePreviewRenderMode | '';
  const sequence = ++mediaSequence;
  revokeMediaObjectUrl();
  mediaUrl.value = '';
  if (!id || !MEDIA_RENDER_MODES.includes(mode as FilePreviewRenderMode)) {
    mediaLoading.value = false;
    return;
  }
  mediaLoading.value = true;
  try {
    const response = await fileApi.previewContent(id);
    const contentType = response.headers?.['content-type'] || 'application/octet-stream';
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data], { type: contentType });
    const objectUrl = URL.createObjectURL(blob);
    if (sequence !== mediaSequence) {
      URL.revokeObjectURL(objectUrl);
      return;
    }
    mediaObjectUrl.value = objectUrl;
    mediaUrl.value = objectUrl;
  } catch {
    if (sequence === mediaSequence) previewError.value = '文件预览加载失败';
  } finally {
    if (sequence === mediaSequence) mediaLoading.value = false;
  }
}

async function openDownload() {
  const id = resolvedFileId.value;
  if (!id) return;
  await downloadFileRecord(fileApi, { id, fileName: displayName.value || `file-${id}` });
}

function openPreviewInNewWindow() {
  const url = documentUrl.value || mediaUrl.value || legacyUrl.value;
  if (!url) return;
  window.open(url, '_blank', 'noopener,noreferrer');
}

function revokeMediaObjectUrl() {
  if (!mediaObjectUrl.value) return;
  URL.revokeObjectURL(mediaObjectUrl.value);
  mediaObjectUrl.value = '';
}

/** 旧地址入参已弃用：每次挂载只提示一次，下一个 major 移除。 */
function warnDeprecatedProps() {
  const used = [
    ['previewUrl', props.previewUrl],
    ['documentPreviewUrl', props.documentPreviewUrl],
    ['directPreviewUrl', props.directPreviewUrl],
    ['previewProviderUrl', props.previewProviderUrl],
    ['previewExternalExtensions', props.previewExternalExtensions.length ? 'set' : ''],
  ]
    .filter(([, value]) => Boolean(value))
    .map(([name]) => name);
  if (!used.length) return;
  console.warn(
    `[FilePreviewPanel] ${used.join('、')} 已弃用：渲染类型由后端 preview-link 的 renderMode 决定，给 fileId 即可，下一个 major 移除。`,
  );
}

function fileNameOf(value: FilePreviewPanelProps['file']): string {
  if (!value || typeof value !== 'object') return '';
  return (value as FileRecord).fileName || '';
}

function absoluteUrl(value?: string) {
  if (!value) return '';
  try {
    return new URL(value, window.location.origin).toString();
  } catch {
    return value;
  }
}

watch(resolvedFileId, loadPreviewLink, { immediate: true });
watch(target, loadMedia, { immediate: true });
watch(
  () => [canDownload.value, canOpenInNewWindow.value],
  () => {
    emit('actions-change', {
      canDownload: canDownload.value,
      canOpenInNewWindow: canOpenInNewWindow.value,
    });
  },
  { immediate: true },
);
onMounted(warnDeprecatedProps);
onBeforeUnmount(() => {
  linkSequence += 1;
  mediaSequence += 1;
  linkLoading.value = false;
  mediaLoading.value = false;
  revokeMediaObjectUrl();
  mediaUrl.value = '';
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
  gap: 12px;
  height: var(--mango-file-preview-panel-height, auto);
  min-height: var(--mango-file-preview-panel-min-height, 0);
  overflow: hidden;
}

.preview-stage {
  position: relative;
  flex: 1 1 auto;
  min-height: var(--mango-file-preview-stage-min-height, 220px);
  height: var(--mango-file-preview-stage-height, auto);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  overflow: hidden;
  display: flex;
  align-items: stretch;
}

.preview-loading-stage {
  flex: 1 1 auto;
}

.preview-actions {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.preview-document {
  position: relative;
  flex: 1 1 auto;
  display: flex;
  align-items: stretch;
  min-height: var(--mango-file-preview-content-min-height, 420px);
}

.preview-converting-hint {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 13px;
  pointer-events: none;
}

.preview-text {
  flex: 1 1 auto;
  width: 100%;
  margin: 0;
  padding: 16px;
  overflow: auto;
  color: var(--el-text-color-primary);
  font-family: var(--el-font-family-mono, monospace);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

.preview-image-viewer {
  position: relative;
  width: 100%;
  height: var(--mango-file-preview-content-height, 360px);
  overflow: hidden;
  display: block;
}

/* ElImageViewer has no inline mode; retain its native controls while constraining the modal surface to this stage. */
.preview-image-viewer :deep(.el-image-viewer__wrapper) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.preview-image-viewer :deep(.el-image-viewer__mask),
.preview-image-viewer :deep(.el-image-viewer__close) {
  display: none;
}

.preview-media {
  width: 100%;
  height: var(--mango-file-preview-content-height, 420px);
  max-height: 100%;
  display: block;
  background: #000;
}

.preview-audio {
  width: calc(100% - 48px);
  margin: 88px 24px;
}

.preview-frame {
  width: 100%;
  height: var(--mango-file-preview-content-height, 100%);
  min-height: var(--mango-file-preview-content-min-height, 420px);
  flex: 1 1 auto;
  align-self: stretch;
  border: 0;
  display: block;
  background: #fff;
}

.preview-placeholder {
  flex: 1 1 auto;
  min-height: var(--mango-file-preview-stage-min-height, 220px);
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 24px;
}

.preview-error-state {
  flex: 1 1 auto;
  min-height: var(--mango-file-preview-stage-min-height, 220px);
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--el-color-danger);
  text-align: center;
  padding: 24px;
}

.preview-error-state .el-icon {
  font-size: 42px;
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

.file-preview-panel--fit-container {
  width: 100%;
  height: var(--mango-file-preview-panel-height, 100%);
  min-width: 0;
  min-height: var(--mango-file-preview-panel-min-height, 0);
}

.file-preview-panel--fit-container .preview-stage {
  width: 100%;
  height: var(--mango-file-preview-stage-height, auto);
  min-width: 0;
  min-height: var(--mango-file-preview-stage-min-height, 0);
}

.file-preview-panel--fit-container .preview-document,
.file-preview-panel--fit-container .preview-image-viewer,
.file-preview-panel--fit-container .preview-media,
.file-preview-panel--fit-container .preview-frame {
  flex: 1 1 auto;
  width: 100%;
  height: var(--mango-file-preview-content-height, 100%);
  min-width: 0;
  min-height: var(--mango-file-preview-content-min-height, 0);
  max-width: 100%;
  max-height: 100%;
}

.file-preview-panel--fit-container .preview-media {
  object-fit: contain;
}

.file-preview-panel--fit-container .preview-audio {
  flex: none;
  width: 100%;
  max-width: 100%;
  margin: auto 0;
}

.file-preview-panel--fit-container .preview-placeholder {
  min-width: 0;
  min-height: var(--mango-file-preview-stage-min-height, 0);
}

.file-preview-panel--fit-container > .preview-loading-stage {
  width: 100%;
  min-width: 0;
  min-height: 0;
}
</style>
