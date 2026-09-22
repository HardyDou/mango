<template>
  <div
    v-bind="attrs"
    class="mango-attachment-upload-grid"
    :class="`is-${layout}`"
    :data-layout="layout"
    data-surface="mango.file.attachment-upload-grid"
  >
    <article
      v-for="category in normalizedCategories"
      :key="category.key"
      class="mango-attachment-upload-card"
      :class="{
        'is-dragging': draggingCategoryKey === category.key,
        'is-uploading': uploadingCategoryKeys.has(category.key),
        'is-multiple': category.maxFileCount > 1,
        'is-disabled': disabled,
      }"
      :data-record-key="category.key"
      :data-state="cardState(category)"
      @dragenter.prevent="handleDragEnter(category)"
      @dragover.prevent="handleDragOver(category)"
      @dragleave.prevent="handleDragLeave(category)"
      @drop.prevent="handleDrop(category, $event)"
    >
      <div class="mango-attachment-upload-card__upload">
        <div
          v-if="draggingCategoryKey === category.key"
          class="mango-attachment-upload-card__drag-mask"
          aria-live="polite"
        >
          <el-icon><UploadFilled /></el-icon>
          <span>松开以上传文件</span>
        </div>

        <div v-if="uploadingCategoryKeys.has(category.key)" class="mango-attachment-upload-card__empty">
          <el-icon class="mango-attachment-upload-card__upload-icon is-loading"><Loading /></el-icon>
          <strong>上传中...</strong>
          <span>请稍候</span>
        </div>

        <div
          v-else-if="!categoryFiles(category.key).length"
          class="mango-attachment-upload-card__empty"
          :role="disabled ? undefined : 'button'"
          :tabindex="disabled ? -1 : 0"
          @click.stop="openFilePicker(category)"
          @keydown.enter.prevent="openFilePicker(category)"
          @keydown.space.prevent="openFilePicker(category)"
        >
          <el-icon class="mango-attachment-upload-card__upload-icon"><UploadFilled /></el-icon>
          <strong>{{ disabled ? '暂无文件' : '拖拽或点击上传' }}</strong>
        </div>

        <div v-else-if="category.maxFileCount === 1" class="mango-attachment-upload-card__single-file">
          <div class="mango-attachment-upload-card__single-file-heading">
            <slot name="file-status" :file="categoryFiles(category.key)[0]" :category="category" />
            <strong :title="categoryFiles(category.key)[0].fileName">
              {{ categoryFiles(category.key)[0].fileName }}
            </strong>
          </div>
          <div v-if="$slots['file-note']" class="mango-attachment-upload-card__file-note">
            <slot name="file-note" :file="categoryFiles(category.key)[0]" :category="category" />
          </div>
          <div class="mango-attachment-upload-card__file-actions">
            <el-button link type="primary" @click.stop="openPreview(categoryFiles(category.key)[0])">预览</el-button>
            <el-button v-if="!disabled" link type="danger" @click.stop="removeFile(categoryFiles(category.key)[0])"
              >删除</el-button
            >
          </div>
        </div>

        <div v-else class="mango-attachment-upload-card__file-list">
          <div class="mango-attachment-upload-card__file-list-toolbar">
            <span class="mango-attachment-upload-card__file-list-title">已上传文件</span>
            <el-button
              v-if="!disabled && categoryFiles(category.key).length < category.maxFileCount"
              type="primary"
              plain
              size="small"
              :icon="Upload"
              data-action="mango.file.attachment-upload-grid.continue-upload"
              @click.stop="openFilePicker(category)"
            >
              继续上传
            </el-button>
          </div>
          <div
            v-for="file in categoryFiles(category.key)"
            :key="attachmentFileKey(file)"
            class="mango-attachment-upload-card__file-row"
          >
            <div class="mango-attachment-upload-card__file-content">
              <div class="mango-attachment-upload-card__file-heading">
                <slot name="file-status" :file="file" :category="category" />
                <span class="mango-attachment-upload-card__file-name" :title="file.fileName">{{ file.fileName }}</span>
              </div>
              <div v-if="$slots['file-note']" class="mango-attachment-upload-card__file-note">
                <slot name="file-note" :file="file" :category="category" />
              </div>
            </div>
            <el-button link type="primary" @click.stop="openPreview(file)">预览</el-button>
            <el-button v-if="!disabled" link type="danger" @click.stop="removeFile(file)">删除</el-button>
          </div>
        </div>
      </div>

      <footer class="mango-attachment-upload-card__footer">
        <div class="mango-attachment-upload-card__title-row">
          <strong class="mango-attachment-upload-card__title" :title="category.name">{{ category.name }}</strong>
          <el-tag size="small" effect="light" :type="category.required ? 'danger' : 'info'">
            {{ category.requirementLabel || (category.required ? '必需' : '可选') }}
          </el-tag>
        </div>
        <div class="mango-attachment-upload-card__rule">
          {{ categoryRuleText(category) }}
        </div>
        <div class="mango-attachment-upload-card__rule">说明：{{ categoryDescription(category) }}</div>
      </footer>
    </article>

    <input
      ref="fileInputRef"
      class="mango-attachment-upload-grid__input"
      type="file"
      :accept="activeAccept"
      :multiple="Boolean(activeCategory && activeCategory.maxFileCount > 1)"
      :disabled="disabled"
      @change="handleFileInputChange"
    />
  </div>

  <MangoFilePreviewDialog
    v-model="previewVisible"
    :title="previewTitle"
    :file-id="previewFile?.fileId"
    :file="previewRecord"
    :show-actions="showPreviewActions"
  />
</template>

<script setup lang="ts">
import { computed, ref, useAttrs } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Loading, Upload, UploadFilled } from '@element-plus/icons-vue';
import { fileApi, type FileRecord, type FileUploadParams } from '../api/file';
import MangoFilePreviewDialog from './MangoFilePreviewDialog.vue';
import type {
  MangoAttachmentUploadCategory,
  MangoAttachmentUploadFile,
  MangoAttachmentUploadFileSlotProps,
} from './MangoAttachmentUploadGrid.types';

defineOptions({ name: 'MangoAttachmentUploadGrid', inheritAttrs: false });

type NormalizedCategory = Omit<MangoAttachmentUploadCategory, 'maxFileCount' | 'formats' | 'accept'> & {
  minFileCount: number;
  maxFileCount: number;
  formats: string[];
  accept: string;
};

defineSlots<{
  'file-status'?: (props: MangoAttachmentUploadFileSlotProps) => unknown;
  'file-note'?: (props: MangoAttachmentUploadFileSlotProps) => unknown;
}>();

const props = withDefaults(
  defineProps<{
    modelValue?: MangoAttachmentUploadFile[];
    categories?: MangoAttachmentUploadCategory[];
    purpose?: string;
    accessLevel?: string;
    bizType?: string;
    bizId?: string | number;
    bizMeta?: Record<string, unknown> | string;
    directoryId?: string | number;
    showPreviewActions?: boolean;
    disabled?: boolean;
    layout?: 'grid' | 'stacked';
  }>(),
  {
    modelValue: () => [],
    categories: () => [],
    purpose: undefined,
    accessLevel: 'PRIVATE',
    bizType: undefined,
    bizId: undefined,
    bizMeta: undefined,
    directoryId: undefined,
    showPreviewActions: true,
    disabled: false,
    layout: 'grid',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: MangoAttachmentUploadFile[]];
  change: [value: MangoAttachmentUploadFile[]];
  'uploading-change': [uploading: boolean];
  success: [file: MangoAttachmentUploadFile, category: MangoAttachmentUploadCategory];
  error: [error: unknown, category: MangoAttachmentUploadCategory];
  remove: [file: MangoAttachmentUploadFile];
}>();

const attrs = useAttrs();
const fileInputRef = ref<HTMLInputElement | null>(null);
const activeCategoryKey = ref('');
const draggingCategoryKey = ref('');
const dragDepth = ref(0);
const uploadingCategoryKeys = ref(new Set<string>());
const previewVisible = ref(false);
const previewFile = ref<MangoAttachmentUploadFile | null>(null);

const normalizedCategories = computed<NormalizedCategory[]>(() =>
  props.categories
    .map((category) => {
      const minFileCount = normalizeMinFileCount(category.minFileCount);
      return {
        ...category,
        key: String(category.key || '').trim(),
        name: String(category.name || '').trim(),
        formats: normalizeFormats(category.formats),
        accept: String(category.accept || '').trim(),
        minFileCount,
        maxFileCount: normalizeMaxFileCount(category.maxFileCount, minFileCount),
      };
    })
    .filter((category) => category.key && category.name),
);

const activeCategory = computed(() =>
  normalizedCategories.value.find((category) => category.key === activeCategoryKey.value),
);
const activeAccept = computed(
  () => activeCategory.value?.accept || activeCategory.value?.formats.map((format) => `.${format}`).join(',') || '',
);
const previewTitle = computed(() => `文件预览：${previewFile.value?.fileName || '附件'}`);
const previewRecord = computed<FileRecord | null>(() =>
  previewFile.value ? attachmentToFileRecord(previewFile.value) : null,
);
const uploadParams = computed<FileUploadParams>(() => ({
  purpose: props.purpose,
  accessLevel: props.accessLevel,
  bizType: props.bizType,
  bizId: props.bizId === undefined || props.bizId === null ? undefined : String(props.bizId),
  bizMeta: props.bizMeta,
  directoryId: props.directoryId === undefined || props.directoryId === null ? undefined : String(props.directoryId),
}));

function categoryFiles(categoryKey: string) {
  return props.modelValue.filter((file) => file.categoryKey === categoryKey);
}

function cardState(category: NormalizedCategory) {
  if (uploadingCategoryKeys.value.has(category.key)) return 'uploading';
  if (draggingCategoryKey.value === category.key) return 'dragging';
  return categoryFiles(category.key).length ? 'uploaded' : 'empty';
}

function openFilePicker(category: NormalizedCategory) {
  if (props.disabled) return;
  if (uploadingCategoryKeys.value.has(category.key)) return;
  if (category.maxFileCount > 1 && categoryFiles(category.key).length >= category.maxFileCount) {
    ElMessage.warning(`${category.name}最多可上传 ${category.maxFileCount} 个文件`);
    return;
  }
  activeCategoryKey.value = category.key;
  const input = fileInputRef.value;
  if (!input) return;
  // Vue updates bound attributes on the next render, but the native picker opens synchronously.
  input.accept = category.formats.map((format) => `.${format}`).join(',');
  if (category.accept) input.accept = category.accept;
  input.multiple = category.maxFileCount > 1;
  input.click();
}

function handleFileInputChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const category = activeCategory.value;
  const files = Array.from(input.files || []);
  input.value = '';
  if (!props.disabled && category) void acceptFiles(category, files);
}

function handleDragEnter(category: NormalizedCategory) {
  if (!canAcceptDrop(category)) return;
  if (draggingCategoryKey.value !== category.key) dragDepth.value = 0;
  dragDepth.value += 1;
  draggingCategoryKey.value = category.key;
}

function handleDragOver(category: NormalizedCategory) {
  if (!canAcceptDrop(category)) return;
  draggingCategoryKey.value = category.key;
}

function handleDragLeave(category: NormalizedCategory) {
  if (draggingCategoryKey.value !== category.key) return;
  dragDepth.value = Math.max(0, dragDepth.value - 1);
  if (!dragDepth.value) draggingCategoryKey.value = '';
}

function handleDrop(category: NormalizedCategory, event: DragEvent) {
  clearDragging();
  if (!canAcceptDrop(category)) return;
  void acceptFiles(category, Array.from(event.dataTransfer?.files || []));
}

function canAcceptDrop(category: NormalizedCategory) {
  if (props.disabled) return false;
  if (uploadingCategoryKeys.value.has(category.key)) return false;
  if (category.maxFileCount === 1 && category.allowSingleFileReplacement !== false) return true;
  return categoryFiles(category.key).length < category.maxFileCount;
}

async function acceptFiles(category: NormalizedCategory, files: File[]) {
  if (props.disabled || !files.length || uploadingCategoryKeys.value.has(category.key)) return;
  const currentFiles = categoryFiles(category.key);

  if (category.maxFileCount === 1) {
    if (files.length > 1) {
      ElMessage.warning(`${category.name}最多可上传 1 个文件`);
      return;
    }
    const validationMessage = validateFiles(category, files);
    if (validationMessage) {
      ElMessage.warning(validationMessage);
      return;
    }
    if (currentFiles.length && category.allowSingleFileReplacement === false) {
      ElMessage.warning(`${category.name}请先删除原文件，再新增文件`);
      return;
    }
    if (currentFiles.length && !(await confirmReplacement(category))) return;
    await uploadFiles(category, files, true);
    return;
  }

  const remaining = category.maxFileCount - currentFiles.length;
  if (files.length > remaining) {
    ElMessage.warning(`${category.name}最多可上传 ${category.maxFileCount} 个文件，当前还可上传 ${remaining} 个`);
    return;
  }
  const validationMessage = validateFiles(category, files);
  if (validationMessage) {
    ElMessage.warning(validationMessage);
    return;
  }
  await uploadFiles(category, files, false);
}

async function confirmReplacement(category: NormalizedCategory) {
  try {
    await ElMessageBox.confirm(`${category.name}已上传文件，是否使用新文件替换？`, '替换附件', {
      type: 'warning',
      confirmButtonText: '替换',
      cancelButtonText: '取消',
    });
    return true;
  } catch {
    return false;
  }
}

async function uploadFiles(category: NormalizedCategory, files: File[], replace: boolean) {
  setUploading(category.key, true);
  let nextValue = replace
    ? props.modelValue.filter((item) => item.categoryKey !== category.key)
    : [...props.modelValue];
  try {
    for (const file of files) {
      try {
        const uploaded = await fileApi.upload(file, uploadParams.value);
        const attachment = fileRecordToAttachment(category.key, uploaded, file);
        nextValue = [...nextValue, attachment];
        updateValue(nextValue);
        emit('success', attachment, category);
      } catch (error) {
        ElMessage.error(`${file.name} 上传失败：${error instanceof Error ? error.message : '未知错误'}`);
        emit('error', error, category);
      }
    }
  } finally {
    setUploading(category.key, false);
  }
}

function validateFiles(category: NormalizedCategory, files: File[]) {
  for (const file of files) {
    const ext = fileExtension(file.name);
    if (category.formats.length && !category.formats.includes(ext)) {
      return `${category.name}仅支持${formatDisplayText(category.formats)}`;
    }
    if (!category.formats.length && category.accept && !fileMatchesAccept(file, category.accept)) {
      return `${category.name}仅支持${acceptDisplayText(category.accept) || '指定格式文件'}`;
    }
    if (category.maxFileSizeMb && file.size > category.maxFileSizeMb * 1024 * 1024) {
      return `${file.name}超过 ${formatMegabytes(category.maxFileSizeMb)} 大小限制`;
    }
  }
  return '';
}

function removeFile(file: MangoAttachmentUploadFile) {
  if (props.disabled) return;
  const next = props.modelValue.filter((item) => attachmentFileKey(item) !== attachmentFileKey(file));
  updateValue(next);
  emit('remove', file);
}

function openPreview(file: MangoAttachmentUploadFile) {
  if (!file.fileId && !file.previewUrl && !file.downloadUrl) {
    ElMessage.warning('暂无可预览文件');
    return;
  }
  previewFile.value = file;
  previewVisible.value = true;
}

function updateValue(value: MangoAttachmentUploadFile[]) {
  emit('update:modelValue', value);
  emit('change', value);
}

function validate() {
  const missing = normalizedCategories.value
    .filter((category) => category.required && categoryFiles(category.key).length < category.minFileCount)
    .map((category) => category.name);
  if (!missing.length) return true;
  ElMessage.warning(`请上传：${missing.join('、')}`);
  return false;
}

function categoryRuleText(category: NormalizedCategory) {
  const formatText = formatDisplayText(category.formats) || acceptDisplayText(category.accept);
  const formatRule = formatText ? `可上传${formatText}类型文件` : '可上传文件';
  return `${formatRule}，最多上传${category.maxFileCount}个`;
}

function categoryDescription(category: NormalizedCategory) {
  return String(category.description || '').trim() || '-';
}

function formatDisplayText(formats: string[]) {
  const displayValues = formats.map((format) => {
    if (format === 'pdf') return 'PDF';
    if (['doc', 'docx'].includes(format)) return 'WORD';
    if (['xls', 'xlsx'].includes(format)) return 'Excel';
    if (['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(format)) return '图片';
    if (['zip', 'rar', '7z'].includes(format)) return '压缩包';
    return format.toUpperCase();
  });
  return Array.from(new Set(displayValues)).join('、');
}

function acceptDisplayText(accept: string) {
  const displayValues = accept
    .split(',')
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean)
    .map((value) => {
      if (value === 'image/*') return '图片';
      if (value === 'audio/*') return '音频';
      if (value === 'video/*') return '视频';
      if (value === 'application/pdf') return 'PDF';
      if (value.startsWith('.')) return formatDisplayText([value.slice(1)]);
      return value.toUpperCase();
    });
  return Array.from(new Set(displayValues)).join('、');
}

function fileMatchesAccept(file: File, accept: string) {
  const fileType = String(file.type || '').toLowerCase();
  const ext = fileExtension(file.name);
  return accept
    .split(',')
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean)
    .some((value) => {
      if (value.startsWith('.')) return ext === value.slice(1);
      if (value.endsWith('/*')) return fileType.startsWith(value.slice(0, -1));
      return fileType === value;
    });
}

function formatMegabytes(value: number) {
  return `${Number.isInteger(value) ? value : Number(value.toFixed(1))}M`;
}

function normalizeFormats(formats: string[] | undefined) {
  return Array.from(
    new Set(
      (formats || [])
        .map((format) =>
          String(format || '')
            .trim()
            .replace(/^\./, '')
            .toLowerCase(),
        )
        .filter(Boolean),
    ),
  );
}

function normalizeMaxFileCount(value: number | undefined, minimum: number) {
  const count = Number(value);
  const normalized = Number.isFinite(count) && count > 0 ? Math.floor(count) : 1;
  return Math.max(normalized, minimum);
}

function normalizeMinFileCount(value: number | undefined) {
  const count = Number(value);
  return Number.isFinite(count) && count >= 0 ? Math.floor(count) : 1;
}

function fileRecordToAttachment(categoryKey: string, record: FileRecord, file: File): MangoAttachmentUploadFile {
  return {
    categoryKey,
    fileId: String(record.id || ''),
    fileName: record.fileName || file.name,
    fileExt: record.fileExt || fileExtension(file.name),
    fileSize: Number(record.fileSize || file.size || 0),
    previewUrl: record.previewUrl,
    downloadUrl: record.downloadUrl,
  };
}

function attachmentToFileRecord(file: MangoAttachmentUploadFile): FileRecord {
  return {
    id: file.fileId,
    fileName: file.fileName,
    fileExt: file.fileExt || fileExtension(file.fileName),
    fileSize: Number(file.fileSize || 0),
    previewUrl: file.previewUrl,
    downloadUrl: file.downloadUrl,
  };
}

function attachmentFileKey(file: MangoAttachmentUploadFile) {
  return `${file.categoryKey}:${file.fileId || file.fileName}`;
}

function fileExtension(fileName: string) {
  const index = fileName.lastIndexOf('.');
  return index >= 0 ? fileName.slice(index + 1).toLowerCase() : '';
}

function setUploading(categoryKey: string, uploading: boolean) {
  const next = new Set(uploadingCategoryKeys.value);
  if (uploading) next.add(categoryKey);
  else next.delete(categoryKey);
  uploadingCategoryKeys.value = next;
  emit('uploading-change', next.size > 0);
}

function clearDragging() {
  dragDepth.value = 0;
  draggingCategoryKey.value = '';
}

defineExpose({ validate });
</script>

<style scoped>
.mango-attachment-upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 250px), 1fr));
  gap: 20px 16px;
  width: 100%;
}

.mango-attachment-upload-grid.is-stacked {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mango-attachment-upload-grid.is-stacked .mango-attachment-upload-card {
  display: flex;
  overflow: hidden;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.mango-attachment-upload-grid.is-stacked .mango-attachment-upload-card__footer {
  order: -1;
  padding: 10px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.mango-attachment-upload-grid.is-stacked .mango-attachment-upload-card__upload {
  height: auto;
  min-height: 126px;
  margin: 14px;
  border-radius: 4px;
}

.mango-attachment-upload-grid.is-stacked .mango-attachment-upload-card__file-list {
  max-height: 280px;
  min-height: 126px;
}

.mango-attachment-upload-card {
  position: relative;
  min-width: 0;
  border-radius: 8px;
  outline: none;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    filter 0.18s ease;
}

.mango-attachment-upload-card.is-disabled {
  cursor: default;
}

.mango-attachment-upload-card__upload {
  position: relative;
  box-sizing: border-box;
  height: 228px;
  overflow: hidden;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
  outline: none;
  transition:
    border-color 0.18s ease,
    border-width 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.mango-attachment-upload-card:not(.is-disabled):hover .mango-attachment-upload-card__upload,
.mango-attachment-upload-card:not(.is-disabled):focus-visible .mango-attachment-upload-card__upload {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.mango-attachment-upload-card.is-dragging {
  z-index: 2;
  transform: scale(1.025);
  filter: saturate(1.08);
}

.mango-attachment-upload-card.is-dragging .mango-attachment-upload-card__upload {
  border: 3px solid var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow:
    0 0 0 5px var(--el-color-primary-light-7),
    0 14px 34px rgb(64 158 255 / 22%);
}

.mango-attachment-upload-card.is-uploading {
  cursor: wait;
}

.mango-attachment-upload-card__drag-mask {
  position: absolute;
  z-index: 3;
  inset: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 90%, transparent);
  color: var(--el-color-primary);
  font-size: 15px;
  font-weight: 700;
  pointer-events: none;
}

.mango-attachment-upload-card__drag-mask .el-icon {
  font-size: 44px;
}

.mango-attachment-upload-card__empty,
.mango-attachment-upload-card__single-file {
  display: flex;
  height: 100%;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  text-align: center;
}

.mango-attachment-upload-card__empty {
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.mango-attachment-upload-card__empty strong {
  color: var(--el-color-primary);
  font-size: 15px;
  line-height: 22px;
}

.mango-attachment-upload-card__empty span {
  font-size: 13px;
  line-height: 20px;
}

.mango-attachment-upload-card__upload-icon {
  margin-bottom: 4px;
  color: var(--el-color-primary);
  font-size: 48px;
}

.mango-attachment-upload-card__upload-icon.is-loading {
  animation: mango-attachment-upload-spin 1s linear infinite;
}

.mango-attachment-upload-card__single-file {
  gap: 12px;
}

.mango-attachment-upload-card__single-file-heading {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.mango-attachment-upload-card__single-file-heading :deep(.el-tag) {
  flex: 0 0 auto;
}

.mango-attachment-upload-card__single-file-heading > strong {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mango-attachment-upload-card__file-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.mango-attachment-upload-card__file-actions :deep(.el-button),
.mango-attachment-upload-card__file-row :deep(.el-button) {
  margin-left: 0;
}

.mango-attachment-upload-card__file-list {
  display: flex;
  box-sizing: border-box;
  height: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  padding: 14px;
}

.mango-attachment-upload-card__file-list-toolbar {
  position: sticky;
  z-index: 1;
  top: 0;
  display: flex;
  min-height: 24px;
  flex: 0 0 auto;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.mango-attachment-upload-card__file-list-title {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  font-weight: 600;
}

.mango-attachment-upload-card__file-row {
  display: flex;
  min-width: 0;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
  padding: 9px 10px;
}

.mango-attachment-upload-card__file-content {
  min-width: 0;
  flex: 1;
}

.mango-attachment-upload-card__file-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.mango-attachment-upload-card__file-heading :deep(.el-tag) {
  flex: 0 0 auto;
}

.mango-attachment-upload-card__file-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mango-attachment-upload-card__file-note {
  min-width: 0;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 18px;
  overflow-wrap: anywhere;
  text-align: left;
}

.mango-attachment-upload-card__file-note:not(:empty) {
  margin-top: 2px;
}

.mango-attachment-upload-card__footer {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 10px 2px 0;
}

.mango-attachment-upload-card__title-row {
  display: flex;
  min-width: 0;
  gap: 8px;
  align-items: center;
}

.mango-attachment-upload-card__title {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mango-attachment-upload-card__title-row :deep(.el-tag) {
  flex: 0 0 auto;
}

.mango-attachment-upload-card__rule {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  overflow-wrap: anywhere;
}

.mango-attachment-upload-grid__input {
  display: none;
}

@keyframes mango-attachment-upload-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .mango-attachment-upload-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .mango-attachment-upload-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .mango-attachment-upload-card__upload {
    height: 210px;
  }
}
</style>
