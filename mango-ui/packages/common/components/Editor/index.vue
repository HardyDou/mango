<template>
  <div ref="wrapperRef" class="editor-wrapper" data-testid="mango-editor">
    <div v-if="!disabled && editorInstance" class="editor-toolbar-row">
      <Toolbar
        class="editor-toolbar"
        :editor="editorInstance"
        :default-config="toolbarConfig"
        :mode="mode"
      />
      <div v-if="$slots['toolbar-actions']" class="editor-toolbar-actions">
        <slot name="toolbar-actions" />
      </div>
    </div>
    <div
      v-if="pasteStatus !== 'idle'"
      class="editor-paste-status"
      data-testid="mango-editor-paste-status"
      role="status"
    >
      {{ pasteStatus === 'processing' ? '正在托管粘贴图片…' : pasteStatus === 'failed' ? '部分图片处理失败' : '图片已托管' }}
    </div>
    <div
      v-if="failedPreviewCount > 0"
      class="editor-preview-warning"
      data-testid="mango-editor-image-unavailable"
    >
      {{ failedPreviewCount }} 张图片暂不可预览
    </div>
    <Editor
      ref="editorComponentRef"
      v-model="valueHtml"
      class="editor-content"
      :default-config="editorConfig"
      :mode="mode"
      :disabled="disabled"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import '@wangeditor/editor/dist/css/style.css';
import {
  getUploadedFileDetail,
  importRemoteImage,
  uploadImage,
  type FileId,
  type UploadResult,
} from '../../api/upload';
import {
  collectManagedFileIds,
  managedFileIdFromImage,
  managedImageHtml,
  managedImageToken,
  parseHtml,
  renderManagedHtml,
  serializeManagedHtml,
} from './managedImages';

defineOptions({ name: 'MangoEditor' });

type EditorMode = 'default' | 'simple';
type EditorImageValueType = 'url' | 'id' | 'token';
type EditorPasteImageMode = 'default' | 'upload';
type EditorImageErrorSource = 'upload' | 'paste' | 'preview' | 'serialize';
type EditorToolbarKey = string | {
  key: string;
  title?: string;
  iconSvg?: string;
  menuKeys?: string[];
};

export interface EditorImageError {
  code: string;
  message: string;
  source: EditorImageErrorSource;
  fileId?: FileId;
}

const staleSelectionErrorMessage = 'Cannot resolve a Slate range from DOM range';
const maxPasteImages = 10;
const pasteConcurrency = 3;
const maxInlineImageBytes = 10 * 1024 * 1024;
let staleSelectionGuardRefs = 0;
let staleSelectionGuardCleanupTimer: ReturnType<typeof window.setTimeout> | undefined;

function isWangEditorStaleSelectionError(error: unknown, message: string) {
  if (!message.includes(staleSelectionErrorMessage)) return false;
  const stack = error instanceof Error ? error.stack || '' : '';
  return stack.includes('@wangeditor') || stack.includes('wangeditor') || stack.includes('toSlateRange');
}

function handleStaleSelectionError(event: ErrorEvent) {
  if (isWangEditorStaleSelectionError(event.error, event.message)) {
    event.preventDefault();
    event.stopImmediatePropagation();
  }
}

function retainStaleSelectionGuard() {
  if (typeof window === 'undefined') return;
  if (staleSelectionGuardCleanupTimer) {
    window.clearTimeout(staleSelectionGuardCleanupTimer);
    staleSelectionGuardCleanupTimer = undefined;
  }
  if (staleSelectionGuardRefs === 0) window.addEventListener('error', handleStaleSelectionError, true);
  staleSelectionGuardRefs += 1;
}

function releaseStaleSelectionGuard() {
  if (typeof window === 'undefined' || staleSelectionGuardRefs === 0) return;
  staleSelectionGuardRefs -= 1;
  if (staleSelectionGuardRefs === 0) {
    staleSelectionGuardCleanupTimer = window.setTimeout(() => {
      window.removeEventListener('error', handleStaleSelectionError, true);
      staleSelectionGuardCleanupTimer = undefined;
    }, 500);
  }
}

const props = withDefaults(defineProps<{
  modelValue?: string;
  placeholder?: string;
  height?: number | string;
  disabled?: boolean;
  mode?: EditorMode;
  toolbarKeys?: EditorToolbarKey[];
  imageValueType?: EditorImageValueType;
  pasteImageMode?: EditorPasteImageMode;
}>(), {
  modelValue: '',
  placeholder: '请输入内容...',
  height: 300,
  disabled: false,
  mode: 'default',
  toolbarKeys: () => [],
  imageValueType: 'url',
  pasteImageMode: 'default',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'change', value: string): void;
  (event: 'image-error', error: EditorImageError): void;
  (event: 'uploading-change', uploading: boolean): void;
}>();

const wrapperRef = ref<HTMLElement>();
const editorInstance = shallowRef<any>(null);
const valueHtml = ref(props.modelValue);
const canonicalHtml = ref(props.modelValue);
const pasteStatus = ref<'idle' | 'processing' | 'done' | 'failed'>('idle');
const failedPreviewCount = ref(0);
const previewIds = new Map<string, FileId>();
let contentVersion = 0;
let applyingExternal = false;
let activeUploads = 0;
let destroyed = false;

const defaultToolbarKeys: EditorToolbarKey[] = [
  'headerSelect', '|', 'bold', 'underline', 'italic', '|', 'color', 'bgColor', '|',
  'fontSize', 'fontFamily', '|', 'insertLink', 'unLink', '|', 'bulletedList',
  'numberedList', 'indent', 'delIndent', '|', 'justifyLeft', 'justifyRight',
  'justifyCenter', 'justifyJustify', '|', 'blockquote', '|', 'insertImage', '|',
  'insertVideo', '|', 'codeBlock', '|', 'undo', 'redo', '|', 'fullScreen',
];

const toolbarConfig = computed(() => {
  if (props.toolbarKeys?.length) return { toolbarKeys: props.toolbarKeys };
  return props.mode === 'simple' ? {} : { toolbarKeys: defaultToolbarKeys };
});

const editorConfig = computed(() => ({
  placeholder: props.placeholder,
  customPaste: handleCustomPaste,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: maxInlineImageBytes,
      customUpload: async (file: File, insertFn: (url: string, alt: string, href: string) => void) => {
        try {
          const result = await uploadManagedFile(file);
          if (destroyed) return;
          if (props.imageValueType === 'token') {
            const id = requireManagedResultId(result, 'upload');
            const previewUrl = managedPreviewUrl(result);
            if (previewUrl) previewIds.set(previewUrl, id);
            insertFn(previewUrl || managedImageToken(id), result.fileName, '');
            return;
          }
          const imageValue = resolveLegacyImageInsertValue(result);
          insertFn(imageValue, result.fileName, imageValue);
        } catch {
          notifyImageError('EDITOR_IMAGE_UPLOAD_FAILED', '图片上传失败，请重试', 'upload');
        }
      },
    },
  },
}));

function strictManagedMode() {
  return props.imageValueType === 'token';
}

function strictPasteMode() {
  return strictManagedMode() && props.pasteImageMode === 'upload';
}

function resolveLegacyImageInsertValue(result: UploadResult) {
  if (props.imageValueType === 'id') return result.id ? String(result.id) : fallbackImageUrl(result);
  return result.url;
}

function fallbackImageUrl(result: UploadResult) {
  console.warn('[MangoEditor] imageValueType requires upload result id; fallback to url.');
  return result.url;
}

function requireManagedResultId(result: UploadResult, source: EditorImageErrorSource) {
  if (result.id) return String(result.id);
  notifyImageError('EDITOR_IMAGE_UPLOAD_FAILED', '文件服务未返回图片 ID', source);
  throw new Error('Managed image id is missing');
}

function managedPreviewUrl(result: UploadResult) {
  const value = result.previewUrl || result.url;
  return value && !value.startsWith('mango-file:') ? value : undefined;
}

function beginUpload() {
  activeUploads += 1;
  if (activeUploads === 1) emit('uploading-change', true);
}

function finishUpload() {
  activeUploads = Math.max(0, activeUploads - 1);
  if (activeUploads === 0) emit('uploading-change', false);
}

async function uploadManagedFile(file: File) {
  beginUpload();
  try {
    return await uploadImage(file);
  } finally {
    finishUpload();
  }
}

async function importManagedRemote(sourceUrl: string) {
  beginUpload();
  try {
    return await importRemoteImage({ sourceUrl });
  } finally {
    finishUpload();
  }
}

function notifyImageError(code: string, message: string, source: EditorImageErrorSource, fileId?: FileId) {
  emit('image-error', { code, message, source, fileId });
}

function handleCreated(editor: any) {
  editorInstance.value = editor;
  void syncExternalValue(props.modelValue);
}

function handleChange(editor: any) {
  const rawHtml = editor.getHtml();
  valueHtml.value = rawHtml;
  if (applyingExternal) return;
  if (!strictManagedMode()) {
    canonicalHtml.value = rawHtml;
    emitValue(rawHtml);
    return;
  }
  const normalized = serializeManagedHtml(rawHtml, previewIds);
  canonicalHtml.value = normalized.html;
  if (normalized.invalidImageCount > 0) {
    notifyImageError('EDITOR_UNMANAGED_IMAGE_BLOCKED', '未托管图片已从保存内容中移除', 'serialize');
  }
  emitValue(normalized.html);
}

function emitValue(value: string) {
  emit('update:modelValue', value);
  emit('change', value);
}

async function syncExternalValue(content: string) {
  const version = ++contentVersion;
  previewIds.clear();
  if (!strictManagedMode()) {
    canonicalHtml.value = content;
    failedPreviewCount.value = 0;
    setEditorHtml(content);
    return;
  }
  const normalized = serializeManagedHtml(content);
  canonicalHtml.value = normalized.html;
  if (normalized.invalidImageCount > 0) {
    notifyImageError('EDITOR_UNMANAGED_IMAGE_BLOCKED', '未托管图片已从保存内容中移除', 'serialize');
  }
  const ids = collectManagedFileIds(normalized.html);
  setEditorHtml(normalized.html);
  if (ids.length === 0) {
    failedPreviewCount.value = 0;
    return;
  }
  const previews = new Map<FileId, string | undefined>();
  await Promise.all(ids.map(async (id) => {
    try {
      const detail = await getUploadedFileDetail(id);
      previews.set(id, detail.previewUrl);
    } catch {
      previews.set(id, undefined);
      notifyImageError('EDITOR_IMAGE_PREVIEW_UNAVAILABLE', '图片暂不可预览', 'preview', id);
    }
  }));
  if (destroyed || version !== contentVersion) return;
  failedPreviewCount.value = ids.filter(id => !previews.get(id)).length;
  setEditorHtml(renderManagedHtml(normalized.html, previews, previewIds));
}

function setEditorHtml(content: string) {
  valueHtml.value = content;
  const editor = editorInstance.value;
  if (!editor || editor.getHtml?.() === content) return;
  applyingExternal = true;
  editor.setHtml?.(content);
  void nextTick(() => {
    applyingExternal = false;
  });
}

function updateContent(content: string) {
  void syncExternalValue(content);
  const normalized = strictManagedMode() ? serializeManagedHtml(content).html : content;
  canonicalHtml.value = normalized;
  emitValue(normalized);
}

function currentCanonicalHtml() {
  if (!strictManagedMode()) return editorInstance.value?.getHtml?.() || valueHtml.value || '';
  return serializeManagedHtml(editorInstance.value?.getHtml?.() || valueHtml.value || '', previewIds).html;
}

function handleCustomPaste(editor: any, event: ClipboardEvent, callback: (allowDefault: boolean) => void) {
  if (props.disabled || !strictPasteMode() || !event.clipboardData) {
    callback(true);
    return;
  }
  const html = event.clipboardData.getData('text/html');
  const imageFiles = [...event.clipboardData.files].filter(file => file.type.startsWith('image/'));
  const imageCount = parseHtml(html).querySelectorAll('img').length;
  if (imageFiles.length === 0 && imageCount === 0) {
    callback(true);
    return;
  }
  event.preventDefault();
  callback(false);
  pasteStatus.value = 'processing';
  const version = contentVersion;
  void preparePastedHtml(html, imageFiles)
    .then((prepared) => {
      if (destroyed || version !== contentVersion) return;
      editor.dangerouslyInsertHtml?.(prepared.html);
      handleChange(editor);
      pasteStatus.value = prepared.failed > 0 ? 'failed' : 'done';
      if (prepared.failed > 0) {
        notifyImageError('EDITOR_IMAGE_UPLOAD_FAILED', `${prepared.failed} 张图片处理失败`, 'paste');
      }
    })
    .catch(() => {
      pasteStatus.value = 'failed';
      notifyImageError('EDITOR_IMAGE_UPLOAD_FAILED', '粘贴图片处理失败', 'paste');
    });
}

async function preparePastedHtml(html: string, clipboardFiles: File[]) {
  const root = parseHtml(html);
  const images = [...root.querySelectorAll('img')] as HTMLImageElement[];
  const fileQueue = [...clipboardFiles];
  let failed = 0;
  if (images.length + fileQueue.length > maxPasteImages) {
    notifyImageError('EDITOR_PASTE_IMAGE_LIMIT_EXCEEDED', `单次最多处理 ${maxPasteImages} 张图片`, 'paste');
  }
  const acceptedImages = images.slice(0, maxPasteImages);
  images.slice(maxPasteImages).forEach(image => image.remove());
  failed += Math.max(0, images.length - acceptedImages.length);
  await mapWithConcurrency(acceptedImages, pasteConcurrency, async (image) => {
    const source = image.getAttribute('src') || '';
    const existingId = managedFileIdFromImage(image);
    if (existingId) {
      await renderExistingToken(image, existingId);
      return;
    }
    try {
      let result: UploadResult;
      if (source.startsWith('data:image/')) {
        result = await uploadManagedFile(dataUriToFile(source));
      } else if (source.startsWith('blob:')) {
        const file = fileQueue.shift();
        if (!file) throw new Error('Blob image has no clipboard file');
        result = await uploadManagedFile(file);
      } else if (/^https?:\/\//i.test(source)) {
        result = await importManagedRemote(source);
      } else {
        throw new Error('Unsupported image source');
      }
      applyManagedResult(image, result);
    } catch {
      failed += 1;
      image.remove();
    }
  });
  const remainingCapacity = Math.max(0, maxPasteImages - acceptedImages.length);
  const remainingFiles = fileQueue.slice(0, remainingCapacity);
  const appended: string[] = [];
  await mapWithConcurrency(remainingFiles, pasteConcurrency, async (file) => {
    try {
      const result = await uploadManagedFile(file);
      const id = requireManagedResultId(result, 'paste');
      const previewUrl = managedPreviewUrl(result);
      if (previewUrl) previewIds.set(previewUrl, id);
      appended.push(managedImageHtml(id, previewUrl, result.fileName));
    } catch {
      failed += 1;
    }
  });
  if (!html && appended.length > 0) return { html: appended.join(''), failed };
  root.insertAdjacentHTML('beforeend', appended.join(''));
  return { html: root.innerHTML, failed };
}

async function renderExistingToken(image: HTMLImageElement, id: FileId) {
  try {
    const detail = await getUploadedFileDetail(id);
    const previewUrl = detail.previewUrl;
    if (previewUrl) previewIds.set(previewUrl, id);
    image.outerHTML = managedImageHtml(id, previewUrl, image.getAttribute('alt') || '');
  } catch {
    image.outerHTML = managedImageHtml(id, undefined, image.getAttribute('alt') || '暂不可预览');
    notifyImageError('EDITOR_IMAGE_PREVIEW_UNAVAILABLE', '图片暂不可预览', 'preview', id);
  }
}

function applyManagedResult(image: HTMLImageElement, result: UploadResult) {
  const id = requireManagedResultId(result, 'paste');
  const previewUrl = managedPreviewUrl(result);
  if (previewUrl) previewIds.set(previewUrl, id);
  image.outerHTML = managedImageHtml(id, previewUrl, image.getAttribute('alt') || result.fileName);
}

function dataUriToFile(source: string) {
  const match = /^data:(image\/[a-z0-9.+-]+);base64,([a-z0-9+/=\s]+)$/i.exec(source);
  if (!match) throw new Error('Unsupported data URI');
  const encoded = match[2].replace(/\s/g, '');
  if (encoded.length > Math.ceil(maxInlineImageBytes * 4 / 3) + 4) throw new Error('Inline image is too large');
  const binary = window.atob(encoded);
  if (binary.length > maxInlineImageBytes) throw new Error('Inline image is too large');
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  const extension = match[1].split('/')[1].replace('jpeg', 'jpg').replace(/[^a-z0-9]/gi, '') || 'png';
  return new File([bytes], `pasted-image.${extension}`, { type: match[1].toLowerCase() });
}

async function mapWithConcurrency<T>(items: T[], concurrency: number, worker: (item: T) => Promise<void>) {
  let nextIndex = 0;
  const runners = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (nextIndex < items.length) {
      const index = nextIndex;
      nextIndex += 1;
      await worker(items[index]);
    }
  });
  await Promise.all(runners);
}

function blurEditorSelection() {
  editorInstance.value?.blur?.();
  if (typeof window !== 'undefined') window.getSelection()?.removeAllRanges();
}

onMounted(() => {
  retainStaleSelectionGuard();
  applyHeight(props.height);
});

watch(() => props.modelValue, (newValue) => {
  if (newValue !== canonicalHtml.value) void syncExternalValue(newValue);
});

watch(() => props.imageValueType, () => {
  void syncExternalValue(props.modelValue);
});

watch(() => props.disabled, (disabled) => {
  if (!editorInstance.value) return;
  if (disabled) editorInstance.value.disable();
  else editorInstance.value.enable();
});

watch(() => props.mode, () => {
  if (!editorInstance.value) return;
  editorInstance.value.destroy();
  editorInstance.value = null;
});

watch(() => props.height, applyHeight);

function applyHeight(height: number | string) {
  const heightValue = typeof height === 'number' ? `${height}px` : String(height);
  wrapperRef.value?.style.setProperty('--editor-height', heightValue);
}

onBeforeUnmount(() => {
  destroyed = true;
  contentVersion += 1;
  blurEditorSelection();
  editorInstance.value?.destroy?.();
  if (activeUploads > 0) emit('uploading-change', false);
  releaseStaleSelectionGuard();
});

defineExpose({
  getEditor: () => editorInstance.value,
  getText: () => editorInstance.value?.getText() || '',
  getHtml: currentCanonicalHtml,
  setContent: updateContent,
  clear: () => updateContent(''),
  blur: blurEditorSelection,
});
</script>

<style scoped lang="scss">
.editor-wrapper {
  --editor-height: 300px;

  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;

  .editor-toolbar-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    border-bottom: 1px solid #dcdfe6;
  }

  .editor-toolbar {
    flex: 1 1 480px;
    min-width: 0;
    border-bottom: 0;
  }

  .editor-toolbar-actions {
    display: flex;
    flex: 0 1 auto;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 4px 8px;
  }

  .editor-paste-status,
  .editor-preview-warning {
    padding: 4px 10px;
    color: var(--el-text-color-secondary, #909399);
    font-size: 12px;
    background: var(--el-fill-color-light, #f5f7fa);
  }

  .editor-preview-warning {
    color: var(--el-color-warning, #e6a23c);
  }

  .editor-content {
    height: var(--editor-height);
    overflow-y: auto;

    :deep(.w-e-text-container) {
      height: var(--editor-height);
      min-height: var(--editor-height);
    }

    :deep(.w-e-scroll) {
      height: var(--editor-height);
    }

    :deep(img[data-managed-state='failed']) {
      display: inline-block;
      min-width: 120px;
      min-height: 48px;
      border: 1px dashed var(--el-border-color, #dcdfe6);
      background: var(--el-fill-color-light, #f5f7fa);
    }
  }
}

:global(.w-e-full-screen-container) {
  z-index: 3000 !important;
  background: #fff;
}
</style>
