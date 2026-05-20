<template>
  <div class="mango-file-upload" :class="[`is-${normalizedDisplay}`]">
    <div class="upload-control" :class="{ 'is-inline-manual': showInlineManualAction }">
      <el-upload
        ref="uploadRef"
        class="mango-upload-inner"
        :class="{ 'is-trigger-hidden': !showUploadTrigger }"
        v-model:file-list="internalFiles"
        :accept="accept"
        :auto-upload="auto"
        :disabled="readonly"
        :drag="normalizedDisplay === 'drag'"
        :limit="count || undefined"
        :list-type="elementListType"
        :multiple="multiple"
        :show-file-list="normalizedDisplay !== 'table'"
        :http-request="handleUpload"
        :before-upload="handleBeforeUpload"
        :on-change="handleChange"
        :on-remove="handleRemove"
        :on-success="handleSuccess"
        :on-error="handleError"
        :on-exceed="handleExceed"
        :on-preview="handlePreview"
      >
        <template v-if="normalizedDisplay === 'thumbnail'">
          <slot name="thumbnail-trigger">
            <slot name="trigger">
              <el-icon><Plus /></el-icon>
            </slot>
          </slot>
        </template>
        <template v-else-if="normalizedDisplay === 'drag'">
          <slot name="drag-trigger">
            <slot name="trigger">
              <el-icon class="upload-drag-icon"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖入文件或点击上传</div>
            </slot>
          </slot>
        </template>
        <template v-else>
          <slot name="trigger">
            <el-button type="primary" :disabled="readonly">
              <el-icon class="el-icon--left"><UploadIcon /></el-icon>
              {{ buttonText }}
            </el-button>
          </slot>
        </template>
      </el-upload>

      <el-button
        v-if="showManualAction"
        class="manual-submit"
        type="success"
        :disabled="!pendingFiles.length"
        @click="submitUpload"
      >
        上传到服务器
      </el-button>
    </div>

    <div v-if="normalizedDisplay === 'list' && !internalFiles.length" class="upload-empty">暂无文件</div>

    <el-table
      v-if="normalizedDisplay === 'table'"
      :data="tableFiles"
      size="small"
      class="upload-table"
      empty-text="暂无文件"
    >
      <el-table-column
        v-for="column in resolvedColumns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :min-width="column.minWidth"
        :width="column.width"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="column.key === 'fileSize'">{{ formatBytes(row.fileSize || row.size) }}</span>
          <el-progress
            v-else-if="column.key === 'uploadProgress'"
            :percentage="Number(row.percentage || (row.status === 'success' ? 100 : 0))"
            :status="row.status === 'fail' ? 'exception' : undefined"
          />
          <span v-else-if="column.key === 'createdTime'">{{ row.createdTime || '-' }}</span>
          <span v-else>{{ cellValue(row, column.key) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!readonly" label="操作" width="76">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="removeAt($index)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage, type UploadProps, type UploadRequestOptions, type UploadUserFile } from 'element-plus';
import type { AxiosProgressEvent } from 'axios';
import { Plus, Upload as UploadIcon, UploadFilled } from '@element-plus/icons-vue';
import {
  DEFAULT_MULTIPART_THRESHOLD,
  fileApi,
  fileToken,
  isFileAccessUrl,
  normalizeFileId,
  type FileRecord,
  type FileBizMeta,
  type FileId,
} from '../api/file';
import { formatBytes } from '../api/fileSettings';

defineOptions({
  name: 'MUpload',
});

export type UploadDisplay = 'list' | 'thumbnail' | 'table' | 'drag';
export type UploadValueType = 'id' | 'token' | 'record';
export type UploadColumnKey = keyof FileRecord | `meta.${string}` | string;

export interface UploadSizeRules {
  image?: string | number;
  video?: string | number;
  audio?: string | number;
  document?: string | number;
  archive?: string | number;
  other?: string | number;
  [key: string]: string | number | undefined;
}

export interface UploadColumn {
  key: UploadColumnKey;
  label: string;
  width?: number | string;
  minWidth?: number | string;
}

type UploadModelValue = string | string[] | FileRecord | FileRecord[] | null | undefined;
type InternalUploadFile = UploadUserFile & Partial<FileRecord>;

const props = withDefaults(defineProps<{
  modelValue?: UploadModelValue;
  fmt?: string | string[];
  count?: number;
  size?: string | number;
  sizes?: UploadSizeRules;
  display?: UploadDisplay;
  columns?: Array<UploadColumn | UploadColumnKey>;
  valueType?: UploadValueType;
  auto?: boolean;
  readonly?: boolean;
  purpose?: string;
  accessLevel?: string;
  bizType?: string;
  bizId?: FileId;
  bizMeta?: FileBizMeta;
  directoryId?: FileId;
  buttonText?: string;
}>(), {
  count: 1,
  display: 'list',
  valueType: 'token',
  auto: true,
  readonly: false,
  purpose: 'attachment',
  accessLevel: 'PRIVATE',
  buttonText: '上传文件',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: UploadModelValue): void;
  (event: 'change', value: UploadModelValue, records: FileRecord[]): void;
  (event: 'success', record: FileRecord): void;
  (event: 'error', error: unknown): void;
}>();

const internalFiles = ref<InternalUploadFile[]>([]);
const syncing = ref(false);
const uploadRef = ref();

const multiple = computed(() => props.count > 1);
const accept = computed(() => normalizeFormats(props.fmt).map(item => `.${item}`).join(','));
const normalizedDisplay = computed<UploadDisplay>(() => {
  if (props.display === 'thumbnail') return 'thumbnail';
  if (props.display === 'table') return 'table';
  if (props.display === 'drag') return 'drag';
  return 'list';
});
const elementListType = computed(() => normalizedDisplay.value === 'thumbnail' ? 'picture-card' : 'text');
const tableFiles = computed(() => internalFiles.value);
const pendingFiles = computed(() => internalFiles.value.filter(item => item.status === 'ready' && !item.id));
const resolvedColumns = computed(() => normalizeColumns(props.columns));
const showUploadTrigger = computed(() => !props.readonly && (!props.count || internalFiles.value.length < props.count));
const showManualAction = computed(() => !props.auto && !props.readonly);
const showInlineManualAction = computed(() => showManualAction.value && normalizedDisplay.value !== 'drag');

watch(() => props.modelValue, (value) => {
  if (isSameRecordValue(value)) return;
  syncing.value = true;
  internalFiles.value = modelValueToFiles(value);
  syncing.value = false;
  internalFiles.value
    .map(fileToRecord)
    .filter((item): item is FileRecord => Boolean(item?.id))
    .forEach(record => void hydratePreviewUrl(record, true));
}, { immediate: true, deep: true });

async function handleUpload(options: UploadRequestOptions) {
  try {
    const meta = normalizeBizMeta(props.bizMeta);
    const record = await fileApi.upload(options.file as File, {
      purpose: props.purpose,
      accessLevel: props.accessLevel,
      bizType: props.bizType,
      bizId: props.bizId ? String(props.bizId) : undefined,
      bizMeta: meta,
      directoryId: props.directoryId,
    }, {
      onUploadProgress: options.onProgress
        ? event => options.onProgress?.({
            ...event,
            percent: progressPercent(event.loaded, event.total || (options.file as File).size || 0),
          } as any)
        : undefined,
    });
    options.onSuccess?.(record);
    emit('success', record);
  } catch (error) {
    options.onError?.(error as Error);
    emit('error', error);
  }
}

const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (props.readonly) return false;
  if (!formatAllowed(file.name)) {
    ElMessage.error('该文件类型不允许上传');
    return false;
  }
  const maxSize = resolveMaxSize(file);
  if (maxSize > 0 && file.size > maxSize) {
    ElMessage.error(`文件大小不能超过 ${formatBytes(maxSize)}`);
    return false;
  }
  return true;
};

const handleChange: UploadProps['onChange'] = (_file, files) => {
  if (syncing.value) return;
  internalFiles.value = files.map(normalizeUploadFile);
};

const handleRemove: UploadProps['onRemove'] = (_file, files) => {
  internalFiles.value = files.map(normalizeUploadFile);
  syncValue();
};

const handleSuccess: UploadProps['onSuccess'] = (response, file, files) => {
  const record = normalizeUploadResponse(response);
  internalFiles.value = files.map((item) => {
    if (item.uid === file.uid && record?.id) {
      return recordToFile(record, item.uid);
    }
    return normalizeUploadFile(item);
  });
  syncValue();
};

const handleError: UploadProps['onError'] = (_error, file, files) => {
  internalFiles.value = files.filter(item => item.uid !== file.uid).map(normalizeUploadFile);
  syncValue();
};

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`最多上传 ${props.count} 个文件`);
};

const handlePreview: UploadProps['onPreview'] = async (file) => {
  const record = fileToRecord(file as InternalUploadFile);
  const url = record ? previewUrl(record) : (file as InternalUploadFile).url;
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
};

async function submitUpload() {
  if (!pendingFiles.value.length) return;
  const pending = pendingFiles.value;
  try {
    pending.forEach((file) => {
      file.status = 'uploading';
      file.percentage = 0;
    });
    const files = pending.map(item => item.raw as File);
    const records = files.length === 1 || hasMultipartCandidate(files)
      ? [await fileApi.upload(files[0], uploadParams(), {
          onUploadProgress: event => updateSingleProgress(pending[0], event),
        }), ...(await uploadRemainingFiles(files.slice(1), pending.slice(1)))]
      : await fileApi.uploadBatch(files, uploadParams(), {
          onUploadProgress: event => updateBatchProgress(pending, event),
        });
    pending.forEach((file) => {
      file.percentage = 100;
    });
    const uploaded = records.map((record, index) => recordToFile(record, pending[index]?.uid));
    const uploadedUidSet = new Set(pending.map(item => item.uid));
    internalFiles.value = [
      ...internalFiles.value.filter(item => !uploadedUidSet.has(item.uid)),
      ...uploaded,
    ];
    syncValue();
    records.forEach(record => emit('success', record));
  } catch (error) {
    pending.forEach((file) => {
      file.status = 'fail';
    });
    emit('error', error);
  }
}

function updateSingleProgress(file: InternalUploadFile | undefined, event: AxiosProgressEvent) {
  if (!file) return;
  file.percentage = progressPercent(event.loaded, event.total || file.raw?.size || file.size || 0);
}

function updateBatchProgress(files: InternalUploadFile[], event: AxiosProgressEvent) {
  const total = event.total || files.reduce((sum, file) => sum + Number(file.raw?.size || file.size || 0), 0);
  const loaded = Math.min(event.loaded || 0, total || 0);
  let offset = 0;

  files.forEach((file) => {
    const size = Number(file.raw?.size || file.size || 0);
    if (!size || !total) {
      file.percentage = progressPercent(loaded, total);
      return;
    }
    const fileLoaded = Math.min(Math.max(loaded - offset, 0), size);
    file.percentage = progressPercent(fileLoaded, size);
    offset += size;
  });
}

async function uploadRemainingFiles(files: File[], pending: InternalUploadFile[]) {
  const records: FileRecord[] = [];
  for (let index = 0; index < files.length; index++) {
    records.push(await fileApi.upload(files[index], uploadParams(), {
      onUploadProgress: event => updateSingleProgress(pending[index], event),
    }));
  }
  return records;
}

function hasMultipartCandidate(files: File[]) {
  return files.some(file => file.size >= DEFAULT_MULTIPART_THRESHOLD);
}

function progressPercent(loaded: number, total: number) {
  if (!total) return 0;
  return Math.max(0, Math.min(99, Math.round((loaded / total) * 100)));
}

function removeAt(index: number) {
  internalFiles.value.splice(index, 1);
  syncValue();
}

function syncValue() {
  const records = internalFiles.value
    .map(fileToRecord)
    .filter((item): item is FileRecord => Boolean(item));
  const value = modelValueFromRecords(records);
  emit('update:modelValue', value);
  emit('change', value, records);
}

function modelValueFromRecords(records: FileRecord[]): UploadModelValue {
  if (props.valueType === 'record') {
    return multiple.value ? records : records[0] || null;
  }
  if (props.valueType === 'id') {
    return multiple.value ? records.map(item => String(item.id || '')).filter(Boolean) : String(records[0]?.id || '');
  }
  return multiple.value ? records.map(item => fileToken(item.id)) : fileToken(records[0]?.id);
}

function uploadParams() {
  return {
    purpose: props.purpose,
    accessLevel: props.accessLevel,
    bizType: props.bizType,
    bizId: props.bizId ? String(props.bizId) : undefined,
    bizMeta: normalizeBizMeta(props.bizMeta),
    directoryId: props.directoryId,
  };
}

function isSameRecordValue(value?: UploadModelValue) {
  const incomingIds = modelValueIds(value);
  if (!incomingIds.length) return false;
  const currentIds = internalFiles.value
    .map(fileToRecord)
    .filter((item): item is FileRecord => Boolean(item?.id))
    .map(item => String(item.id));
  return currentIds.length === incomingIds.length
    && currentIds.every((id, index) => id === incomingIds[index]);
}

function modelValueIds(value?: UploadModelValue) {
  if (!value) return [];
  const values = Array.isArray(value) ? value : [value];
  return values
    .map(item => normalizeFileId(item as any))
    .filter(Boolean);
}

function modelValueToFiles(value?: UploadModelValue): InternalUploadFile[] {
  if (!value) return [];
  const values = Array.isArray(value) ? value : [value];
  return values.flatMap((item, index) => {
    if (!item) return [];
    if (typeof item === 'string') {
      if (isFileAccessUrl(item)) {
        return [{
          uid: index,
          name: item.split('/').pop() || `file-${index}`,
          fileName: item.split('/').pop() || `file-${index}`,
          url: item,
        }];
      }
      const id = normalizeFileId(item);
      return [{
        uid: index,
        id,
        name: `file-${id || index}`,
        fileName: `file-${id || index}`,
        url: '',
      }];
    }
    return [recordToFile(item)];
  });
}

function normalizeUploadFile(file: UploadUserFile): InternalUploadFile {
  const response = normalizeUploadResponse((file as InternalUploadFile).response);
  if (response?.id) {
    return recordToFile(response, file.uid);
  }
  return file as InternalUploadFile;
}

function normalizeUploadResponse(response: unknown): FileRecord | undefined {
  const body = response as { data?: FileRecord; id?: FileId } | FileRecord | undefined;
  if (!body) return undefined;
  const record = ('data' in body && body.data ? body.data : body) as FileRecord;
  return record?.id ? record : undefined;
}

function recordToFile(record: FileRecord, uid?: number): InternalUploadFile {
  const url = displayUrl(record);
  return {
    ...record,
    uid: uid ?? Number(Date.now()),
    id: record.id,
    name: record.fileName,
    fileName: record.fileName,
    fileSize: record.fileSize,
    contentType: record.contentType,
    url,
    status: 'success',
    response: record,
  };
}

function previewUrl(record: Partial<FileRecord>) {
  return directDisplayUrl(record.directPreviewUrl)
    || directDisplayUrl(record.directDownloadUrl)
    || directDisplayUrl(record.url)
    || directDisplayUrl(record.previewUrl)
    || directDisplayUrl(record.downloadUrl)
    || (record.id ? fileApi.downloadUrl(record.id) : '');
}

function displayUrl(record: Partial<FileRecord>) {
  const directUrl = directDisplayUrl(record.directPreviewUrl)
    || directDisplayUrl(record.directDownloadUrl)
    || directDisplayUrl(record.url)
    || directDisplayUrl(record.previewUrl)
    || directDisplayUrl(record.downloadUrl)
    || '';
  if (normalizedDisplay.value === 'thumbnail') {
    return directUrl;
  }
  return directUrl || (record.id ? fileApi.downloadUrl(record.id) : '');
}

function directDisplayUrl(value?: string) {
  if (!value) return false;
  if (/^(blob|data):/i.test(value)) return true;
  return /^(https?:)?\/\//i.test(value) ? value : false;
}

async function hydratePreviewUrl(record: FileRecord, shouldSyncValue = false) {
  if (!record.id || record.directPreviewUrl || record.previewUrl || record.directDownloadUrl) return;
  try {
    let hydrated = false;
    const preview = await fileApi.preview(record.id);
    internalFiles.value = internalFiles.value.map((file) => {
      const current = fileToRecord(file);
      if (!current || String(current.id) !== String(record.id)) {
        return file;
      }
      hydrated = true;
      return recordToFile({
        ...current,
        previewUrl: preview.previewUrl,
        downloadUrl: preview.downloadUrl || current.downloadUrl,
        directPreviewUrl: preview.directPreviewUrl,
        directDownloadUrl: preview.directDownloadUrl,
        directPreviewExpireSeconds: preview.directPreviewExpireSeconds,
        directDownloadExpireSeconds: preview.directDownloadExpireSeconds,
      }, file.uid);
    });
    if (hydrated && shouldSyncValue) {
      syncValue();
    }
  } catch {
    // 预览地址补齐失败时保留上传返回地址，避免影响表单值同步。
  }
}

function fileToRecord(file: InternalUploadFile): FileRecord | null {
  const response = file.response as FileRecord | undefined;
  if (response?.id) return response;
  if (!file.id) return null;
  return {
    ...file,
    id: file.id,
    fileName: file.fileName || file.name,
    fileSize: Number(file.fileSize ?? file.size ?? 0),
    contentType: file.contentType,
  } as FileRecord;
}

function formatAllowed(fileName: string) {
  const formats = normalizeFormats(props.fmt);
  if (!formats.length) return true;
  return formats.includes(fileExt(fileName));
}

function resolveMaxSize(file: File) {
  const category = fileCategory(file);
  const categorySize = props.sizes?.[category] ?? props.sizes?.other;
  return parseSize(categorySize ?? props.size);
}

function normalizeFormats(value?: string | string[]) {
  const values = Array.isArray(value) ? value : String(value || '').split(/[,，\s]+/);
  return values
    .flatMap(item => expandFormat(item.trim().replace(/^\./, '').toLowerCase()))
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index);
}

function expandFormat(value: string) {
  const groups: Record<string, string[]> = {
    image: ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'],
    video: ['mp4', 'mov', 'avi', 'mkv', 'webm'],
    audio: ['mp3', 'wav', 'aac', 'flac', 'ogg'],
    pdf: ['pdf'],
    word: ['doc', 'docx', 'wps'],
    excel: ['xls', 'xlsx', 'xlsm', 'csv', 'et'],
    ppt: ['ppt', 'pptx', 'dps'],
    archive: ['zip', 'rar', '7z', 'tar', 'gz'],
    text: ['txt', 'csv', 'md', 'json', 'xml'],
    office: ['doc', 'docx', 'xls', 'xlsx', 'xlsm', 'ppt', 'pptx', 'wps', 'et', 'dps'],
    odf: ['odt', 'ods', 'odp'],
  };
  return groups[value] || [value];
}

function parseSize(value?: string | number) {
  if (value === undefined || value === null || value === '') return 0;
  if (typeof value === 'number') return value * 1024 * 1024;
  const match = value.trim().match(/^(\d+(?:\.\d+)?)(b|kb|mb|gb)?$/i);
  if (!match) return 0;
  const amount = Number(match[1]);
  const unit = (match[2] || 'mb').toLowerCase();
  if (unit === 'gb') return amount * 1024 * 1024 * 1024;
  if (unit === 'mb') return amount * 1024 * 1024;
  if (unit === 'kb') return amount * 1024;
  return amount;
}

function fileCategory(file: File) {
  const type = file.type.toLowerCase();
  const ext = fileExt(file.name);
  if (type.startsWith('image/')) return 'image';
  if (type.startsWith('video/')) return 'video';
  if (type.startsWith('audio/')) return 'audio';
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'txt', 'csv', 'odt', 'ods'].includes(ext)) return 'document';
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return 'archive';
  return 'other';
}

function fileExt(fileName?: string) {
  if (!fileName || !fileName.includes('.')) return '';
  return fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase();
}

function normalizeBizMeta(value?: FileBizMeta) {
  if (!value) return undefined;
  return typeof value === 'string' ? value : JSON.stringify(value);
}

function normalizeColumns(columns?: Array<UploadColumn | UploadColumnKey>): UploadColumn[] {
  const defaultColumns: UploadColumnKey[] = ['fileName', 'fileSize', 'uploadProgress', 'createdTime'];
  return (columns?.length ? columns : defaultColumns).map((item) => {
    if (typeof item !== 'string') return item;
    return {
      key: item,
      label: columnLabel(item),
      minWidth: item === 'fileName' ? 180 : 120,
    };
  });
}

function columnLabel(key: string) {
  const labels: Record<string, string> = {
    fileName: '文件名',
    fileSize: '大小',
    uploadProgress: '进度',
    contentType: '类型',
    createdTime: '上传时间',
    createdBy: '上传账号',
    bizId: '业务ID',
    purpose: '用途',
  };
  return labels[key] || key.replace(/^meta\./, '');
}

function cellValue(row: FileRecord, key: string) {
  if (!key.startsWith('meta.')) return (row as Record<string, unknown>)[key] ?? '-';
  const meta = typeof row.bizMeta === 'string' ? safeJson(row.bizMeta) : row.bizMeta;
  return (meta as Record<string, unknown> | undefined)?.[key.replace('meta.', '')] ?? '-';
}

function safeJson(value: string) {
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}
</script>

<style scoped>
.mango-file-upload {
  display: inline-block;
  width: 100%;
}

.mango-file-upload.is-list,
.mango-file-upload.is-drag,
.mango-file-upload.is-table {
  max-width: 100%;
}

.upload-drag-icon {
  margin-top: 12px;
  font-size: 36px;
  color: var(--el-color-primary);
}

.upload-table {
  margin-top: 10px;
}

.upload-control {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.upload-control.is-inline-manual {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 12px;
}

.mango-upload-inner.is-trigger-hidden :deep(.el-upload) {
  display: none;
}

.manual-submit {
  flex: 0 0 auto;
}

.is-drag .manual-submit {
  justify-content: flex-start;
  margin-top: 12px;
}

.upload-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  margin-top: 10px;
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  background: var(--el-fill-color-blank);
}
</style>
