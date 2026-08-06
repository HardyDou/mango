<template>
  <div ref="wrapperRef" class="editor-wrapper" data-testid="mango-editor">
    <div v-if="!disabled && editorInstance" class="editor-toolbar-row">
      <Toolbar class="editor-toolbar" :editor="editorInstance" :default-config="toolbarConfig" :mode="mode" />
      <input
        ref="attachmentInputRef"
        class="editor-file-input"
        type="file"
        multiple
        :accept="attachmentAccept"
        data-testid="mango-editor-attachment-input"
        @change="handleAttachmentSelection"
      />
      <div v-if="$slots['toolbar-actions']" class="editor-toolbar-actions">
        <slot name="toolbar-actions" />
      </div>
    </div>
    <div
      v-if="uploadStatus !== 'idle'"
      class="editor-paste-status"
      data-testid="mango-editor-paste-status"
      role="status"
    >
      {{
        uploadStatus === 'processing'
          ? '正在上传文件…'
          : uploadStatus === 'failed'
            ? '部分文件上传失败'
            : '文件上传完成'
      }}
    </div>
    <div v-if="failedPreviewCount > 0" class="editor-preview-warning" data-testid="mango-editor-image-unavailable">
      {{ failedPreviewCount }} 个资源暂不可预览
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
      @custom-paste="handleCustomPaste"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { Boot, type IButtonMenu, type IDomEditor } from '@wangeditor/editor';
import '@wangeditor/editor/dist/css/style.css';
import {
  getUploadedFileDetail,
  importRemoteImage,
  uploadFile,
  uploadImage,
  type FileId,
  type UploadResult,
} from '../../api/upload';
import {
  collectManagedFileIds,
  managedAttachmentHtml,
  managedFileIdFromImage,
  managedImageHtml,
  managedImageToken,
  parseHtml,
  renderManagedHtml,
  serializeManagedHtml,
} from './managedImages';
import type { EditorAssetError, EditorAssetErrorSource, EditorImageError } from './types';

defineOptions({ name: 'MangoEditor' });

type ManagedDropHandler = (files: File[]) => boolean;
const uploadAttachmentMenuKey = 'uploadAttachment';
const attachmentMenuHandlers = new WeakMap<IDomEditor, () => void>();
const managedDropHandlers = new WeakMap<IDomEditor, ManagedDropHandler>();
const registeredAttachmentMenuBoots = ((
  globalThis as typeof globalThis & {
    __mangoRegisteredAttachmentMenuBoots?: WeakSet<typeof Boot>;
  }
).__mangoRegisteredAttachmentMenuBoots ??= new WeakSet<typeof Boot>());
let managedDropPluginRegistered = false;

function registerAttachmentMenu() {
  if (registeredAttachmentMenuBoots.has(Boot)) return;
  Boot.registerMenu({
    key: uploadAttachmentMenuKey,
    factory: (): IButtonMenu => ({
      title: '上传附件',
      tag: 'button',
      iconSvg:
        '<svg viewBox="0 0 24 24" fill="none"><path d="M21.44 11.05 12.25 20.24a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
      getValue: () => '',
      isActive: () => false,
      isDisabled: (editor) => !attachmentMenuHandlers.has(editor),
      exec: (editor) => attachmentMenuHandlers.get(editor)?.(),
    }),
  });
  registeredAttachmentMenuBoots.add(Boot);
}

function registerManagedDropPlugin() {
  if (managedDropPluginRegistered) return;
  Boot.registerPlugin((editor) => {
    const originalInsertData = editor.insertData.bind(editor);
    editor.insertData = (data: DataTransfer) => {
      const files = Array.from(data.files || []);
      const handler = managedDropHandlers.get(editor);
      if (files.length > 0 && handler?.(files)) return;
      originalInsertData(data);
    };
    return editor;
  });
  managedDropPluginRegistered = true;
}

registerAttachmentMenu();
registerManagedDropPlugin();

type EditorMode = 'default' | 'simple';
type EditorImageValueType = 'url' | 'id' | 'token';
type EditorPasteImageMode = 'default' | 'upload';
type EditorToolbarKey =
  | string
  | {
      key: string;
      title?: string;
      iconSvg?: string;
      menuKeys?: string[];
    };

const staleSelectionErrorMessage = 'Cannot resolve a Slate range from DOM range';
const maxPasteImages = 10;
const pasteConcurrency = 3;
const maxInlineImageBytes = 10 * 1024 * 1024;
let staleSelectionGuardRefs = 0;
let staleSelectionGuardCleanupTimer: ReturnType<typeof window.setTimeout> | undefined;

function isWangEditorStaleSelectionError(error: unknown, message: string) {
  if (!String(message || '').includes(staleSelectionErrorMessage)) return false;
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

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    height?: number | string;
    disabled?: boolean;
    mode?: EditorMode;
    toolbarKeys?: EditorToolbarKey[];
    imageValueType?: EditorImageValueType;
    pasteImageMode?: EditorPasteImageMode;
    attachmentAccept?: string;
  }>(),
  {
    modelValue: '',
    placeholder: '请输入内容...',
    height: 300,
    disabled: false,
    mode: 'default',
    toolbarKeys: () => [],
    imageValueType: 'token',
    pasteImageMode: 'upload',
    attachmentAccept: '',
  },
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'change', value: string): void;
  (event: 'image-error', error: EditorImageError): void;
  (event: 'asset-error', error: EditorAssetError): void;
  (event: 'uploading-change', uploading: boolean): void;
}>();

const wrapperRef = ref<HTMLElement>();
const attachmentInputRef = ref<HTMLInputElement>();
const editorInstance = shallowRef<IDomEditor | null>(null);
const valueHtml = ref(props.modelValue);
const canonicalHtml = ref(props.modelValue);
const uploadStatus = ref<'idle' | 'processing' | 'done' | 'failed'>('idle');
const failedPreviewCount = ref(0);
const previewIds = new Map<string, FileId>();
let contentVersion = 0;
let applyingExternal = false;
let activeUploads = 0;
let destroyed = false;

const defaultToolbarKeys: EditorToolbarKey[] = [
  'headerSelect',
  '|',
  'bold',
  'underline',
  'italic',
  '|',
  'color',
  'bgColor',
  '|',
  'fontSize',
  'fontFamily',
  '|',
  'insertLink',
  'unLink',
  '|',
  'bulletedList',
  'numberedList',
  'indent',
  'delIndent',
  '|',
  'justifyLeft',
  'justifyRight',
  'justifyCenter',
  'justifyJustify',
  '|',
  'blockquote',
  '|',
  'uploadImage',
  uploadAttachmentMenuKey,
  '|',
  'insertVideo',
  '|',
  'codeBlock',
  '|',
  'undo',
  'redo',
  '|',
  'fullScreen',
];

const toolbarConfig = computed(() => {
  if (props.toolbarKeys?.length) return { toolbarKeys: withAttachmentMenu(props.toolbarKeys) };
  if (props.mode === 'simple') {
    const simpleToolbarKeys = Boot.simpleToolbarConfig.toolbarKeys;
    return simpleToolbarKeys?.length ? { toolbarKeys: withAttachmentMenu(simpleToolbarKeys) } : {};
  }
  return { toolbarKeys: defaultToolbarKeys };
});

function withAttachmentMenu(toolbarKeys: EditorToolbarKey[]) {
  if (toolbarKeys.some((item) => item === uploadAttachmentMenuKey)) return toolbarKeys;
  const nextToolbarKeys = [...toolbarKeys];
  const imageUploadIndex = nextToolbarKeys.findIndex((item) => item === 'uploadImage');
  nextToolbarKeys.splice(
    imageUploadIndex >= 0 ? imageUploadIndex + 1 : nextToolbarKeys.length,
    0,
    uploadAttachmentMenuKey,
  );
  return nextToolbarKeys;
}

const editorConfig = computed(() => ({
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: maxInlineImageBytes,
      customUpload: async (file: File, insertFn: (url: string, alt: string, href: string) => void) => {
        try {
          const result = await uploadManagedImage(file);
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

function requireManagedResultId(result: UploadResult, source: EditorAssetErrorSource, file?: File) {
  if (result.id) return String(result.id);
  notifyAssetError('EDITOR_ASSET_ID_MISSING', '文件服务未返回文件 ID', source, undefined, file);
  throw new Error('Managed asset id is missing');
}

function managedPreviewUrl(result?: UploadResult) {
  const value = result?.previewUrl || result?.downloadUrl || result?.url;
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
    return await (isImageFile(file) ? uploadImage(file) : uploadFile(file));
  } finally {
    finishUpload();
  }
}

async function uploadManagedImage(file: File) {
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

function notifyAssetError(code: string, message: string, source: EditorAssetErrorSource, fileId?: FileId, file?: File) {
  const error: EditorAssetError = {
    code,
    message,
    source,
    fileId,
    fileName: file?.name,
    kind: file ? (isImageFile(file) ? 'image' : 'attachment') : undefined,
  };
  emit('asset-error', error);
  if (!file || isImageFile(file)) emit('image-error', error);
}

function notifyImageError(code: string, message: string, source: EditorAssetErrorSource, fileId?: FileId) {
  notifyAssetError(code, message, source, fileId);
}

function handleCreated(editor: IDomEditor) {
  attachmentMenuHandlers.set(editor, openAttachmentPicker);
  editorInstance.value = editor;
  managedDropHandlers.set(editor, handleManagedDrop);
  void syncExternalValue(props.modelValue);
}

function handleChange(editor: IDomEditor, force = false) {
  const rawHtml = editor.getHtml();
  valueHtml.value = rawHtml;
  if (applyingExternal && !force) return;
  if (!strictManagedMode()) {
    canonicalHtml.value = rawHtml;
    emitValue(rawHtml);
    return;
  }
  const normalized = serializeManagedHtml(rawHtml, previewIds);
  canonicalHtml.value = normalized.html;
  if (normalized.invalidAssetCount > 0) {
    notifyAssetError('EDITOR_INVALID_MANAGED_ASSET', '无效的托管资源已从保存内容中移除', 'serialize');
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
  if (normalized.invalidAssetCount > 0) {
    notifyAssetError('EDITOR_INVALID_MANAGED_ASSET', '无效的托管资源已从保存内容中移除', 'serialize');
  }
  const ids = collectManagedFileIds(normalized.html);
  setEditorHtml(normalized.html);
  if (ids.length === 0) {
    failedPreviewCount.value = 0;
    return;
  }
  const previews = new Map<FileId, UploadResult | undefined>();
  await Promise.all(
    ids.map(async (id) => {
      try {
        const detail = await getUploadedFileDetail(id);
        previews.set(id, detail);
      } catch {
        previews.set(id, undefined);
        notifyAssetError('EDITOR_ASSET_PREVIEW_UNAVAILABLE', '资源暂不可预览', 'preview', id);
      }
    }),
  );
  if (destroyed || version !== contentVersion) return;
  failedPreviewCount.value = ids.filter((id) => !managedPreviewUrl(previews.get(id))).length;
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

function handleCustomPaste(editor: IDomEditor, event: ClipboardEvent, callback: (allowDefault: boolean) => void) {
  if (props.disabled || !strictPasteMode() || !event.clipboardData) {
    callback(true);
    return;
  }
  const html = event.clipboardData.getData('text/html');
  const files = [...event.clipboardData.files];
  const imageCount = parseHtml(html).querySelectorAll('img').length;
  if (files.length === 0 && imageCount === 0) {
    callback(true);
    return;
  }
  event.preventDefault();
  callback(false);
  uploadStatus.value = 'processing';
  const version = contentVersion;
  void preparePastedHtml(html, files)
    .then((prepared) => {
      if (destroyed || version !== contentVersion) return;
      editor.dangerouslyInsertHtml?.(prepared.html);
      handleChange(editor, true);
      uploadStatus.value = prepared.failed > 0 ? 'failed' : 'done';
      if (prepared.failed > 0) {
        notifyAssetError('EDITOR_ASSET_UPLOAD_FAILED', `${prepared.failed} 个文件处理失败`, 'paste');
      }
    })
    .catch(() => {
      uploadStatus.value = 'failed';
      notifyAssetError('EDITOR_ASSET_UPLOAD_FAILED', '粘贴文件处理失败', 'paste');
    });
}

async function preparePastedHtml(html: string, clipboardFiles: File[]) {
  const root = parseHtml(html);
  const images = [...root.querySelectorAll('img')] as HTMLImageElement[];
  const fileQueue = [...clipboardFiles];
  let failed = 0;
  if (images.length + fileQueue.length > maxPasteImages) {
    notifyAssetError('EDITOR_PASTE_ASSET_LIMIT_EXCEEDED', `单次最多处理 ${maxPasteImages} 个文件`, 'paste');
  }
  const acceptedImages = images.slice(0, maxPasteImages);
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
        result = await uploadManagedFile(takeFirstImageFile(fileQueue) || dataUriToFile(source));
      } else if (source.startsWith('blob:')) {
        const file = takeFirstImageFile(fileQueue);
        if (!file) throw new Error('Blob image has no clipboard file');
        result = await uploadManagedFile(file);
      } else if (/^https?:\/\//i.test(source)) {
        result = await importManagedRemote(source);
      } else {
        return;
      }
      applyManagedResult(image, result);
    } catch {
      failed += 1;
    }
  });
  const remainingCapacity = Math.max(0, maxPasteImages - acceptedImages.length);
  const remainingFiles = fileQueue.slice(0, remainingCapacity);
  failed += Math.max(0, fileQueue.length - remainingFiles.length);
  const appended = new Array<string>(remainingFiles.length).fill('');
  await mapWithConcurrency(remainingFiles, pasteConcurrency, async (file, index) => {
    try {
      const result = await uploadManagedFile(file);
      appended[index] = uploadedAssetHtml(file, result, 'paste');
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
    const previewUrl = managedPreviewUrl(detail);
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

function uploadedAssetHtml(file: File, result: UploadResult, source: EditorAssetErrorSource) {
  const id = requireManagedResultId(result, source, file);
  const previewUrl = managedPreviewUrl(result);
  if (previewUrl) previewIds.set(previewUrl, id);
  return isImageFile(file)
    ? managedImageHtml(id, previewUrl, result.fileName || file.name)
    : managedAttachmentHtml(id, previewUrl, result.fileName || file.name);
}

function takeFirstImageFile(files: File[]) {
  const index = files.findIndex(isImageFile);
  if (index < 0) return undefined;
  return files.splice(index, 1)[0];
}

function isImageFile(file: File) {
  return file.type.startsWith('image/');
}

function openAttachmentPicker() {
  attachmentInputRef.value?.click();
}

function handleAttachmentSelection(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  input.value = '';
  if (files.length > 0) void insertUploadedFiles(files, 'upload');
}

function handleManagedDrop(files: File[]) {
  if (props.disabled || !strictManagedMode()) return false;
  void insertUploadedFiles(files, 'drop');
  return true;
}

async function insertUploadedFiles(files: File[], source: 'upload' | 'drop') {
  const editor = editorInstance.value;
  if (!editor || files.length === 0) return;
  const acceptedFiles = files.slice(0, maxPasteImages);
  const fragments = new Array<string>(acceptedFiles.length).fill('');
  let failed = Math.max(0, files.length - acceptedFiles.length);
  uploadStatus.value = 'processing';
  await mapWithConcurrency(acceptedFiles, pasteConcurrency, async (file, index) => {
    try {
      const result = await uploadManagedFile(file);
      fragments[index] = uploadedAssetHtml(file, result, source);
    } catch {
      failed += 1;
      notifyAssetError('EDITOR_ASSET_UPLOAD_FAILED', `${file.name} 上传失败`, source, undefined, file);
    }
  });
  if (destroyed) return;
  const html = fragments.join('');
  if (html) {
    editor.dangerouslyInsertHtml?.(html);
    handleChange(editor, true);
  }
  uploadStatus.value = failed > 0 ? 'failed' : 'done';
}

function dataUriToFile(source: string) {
  const match = /^data:(image\/[a-z0-9.+-]+);base64,([a-z0-9+/=\s]+)$/i.exec(source);
  if (!match) throw new Error('Unsupported data URI');
  const encoded = match[2].replace(/\s/g, '');
  if (encoded.length > Math.ceil((maxInlineImageBytes * 4) / 3) + 4) throw new Error('Inline image is too large');
  const binary = window.atob(encoded);
  if (binary.length > maxInlineImageBytes) throw new Error('Inline image is too large');
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  const extension =
    match[1]
      .split('/')[1]
      .replace('jpeg', 'jpg')
      .replace(/[^a-z0-9]/gi, '') || 'png';
  return new File([bytes], `pasted-image.${extension}`, { type: match[1].toLowerCase() });
}

async function mapWithConcurrency<T>(
  items: T[],
  concurrency: number,
  worker: (item: T, index: number) => Promise<void>,
) {
  let nextIndex = 0;
  const runners = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (nextIndex < items.length) {
      const index = nextIndex;
      nextIndex += 1;
      await worker(items[index], index);
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

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue !== canonicalHtml.value) void syncExternalValue(newValue);
  },
);

watch(
  () => props.imageValueType,
  () => {
    void syncExternalValue(props.modelValue);
  },
);

watch(
  () => props.disabled,
  (disabled) => {
    if (!editorInstance.value) return;
    if (disabled) editorInstance.value.disable();
    else editorInstance.value.enable();
  },
);

watch(
  () => props.mode,
  () => {
    if (!editorInstance.value) return;
    editorInstance.value.destroy();
    editorInstance.value = null;
  },
);

watch(() => props.height, applyHeight);

function applyHeight(height: number | string) {
  const heightValue = typeof height === 'number' ? `${height}px` : String(height);
  wrapperRef.value?.style.setProperty('--editor-height', heightValue);
}

onBeforeUnmount(() => {
  destroyed = true;
  contentVersion += 1;
  blurEditorSelection();
  if (editorInstance.value) {
    attachmentMenuHandlers.delete(editorInstance.value);
    managedDropHandlers.delete(editorInstance.value);
  }
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

  .editor-file-input {
    display: none;
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
    height: var(--editor-height) !important;
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

    :deep(a[data-file-kind='attachment']) {
      display: inline-flex;
      align-items: center;
      max-width: 100%;
      overflow-wrap: anywhere;
    }

    :deep(a[data-file-kind='attachment'][data-managed-state='failed']) {
      color: var(--el-text-color-secondary, #909399);
      text-decoration-style: dashed;
    }
  }
}

:global(.w-e-full-screen-container) {
  z-index: 3000 !important;
  background: #fff;
}
</style>
