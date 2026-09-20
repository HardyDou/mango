<template>
  <div class="mango-file-list" data-surface="detail.file-list" :data-state="normalizedRows.length ? 'ready' : 'empty'">
    <el-table
      :data="normalizedRows"
      :row-key="rowKey"
      :span-method="adjacentSpanMethod"
      :header-cell-style="resolvedHeaderCellStyle"
      :cell-style="resolvedBodyCellStyle"
      :style="resolvedTableStyle"
      border
      class="mango-file-list__table"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="selectable" type="selection" width="48" :selectable="canSelectRow" />
      <el-table-column
        v-for="column in normalizedColumns"
        :key="column.key"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :align="column.align"
        :fixed="column.fixed"
        :show-overflow-tooltip="column.showOverflowTooltip"
      >
        <template #default="{ row, $index }">
          <slot
            v-if="column.slot"
            :name="cellSlotName(column)"
            :row="row"
            :row-index="$index"
            :column="column"
            :value="cellValue(row, column)"
            :display-value="displayCellValue(row, column)"
          >
            <div class="mango-file-list__text-cell">{{ displayCellValue(row, column) }}</div>
          </slot>
          <div v-else-if="column.type === 'files'" class="mango-file-list__files-cell">
            <div
              v-for="(file, fileIndex) in displayFiles(row, column)"
              :key="fileKey(row, column, file, fileIndex)"
              class="mango-file-list__file"
            >
              <div class="mango-file-list__file-copy">
                <span :class="{ 'is-empty': !hasFile(file) }">{{ fileDisplayName(file, column) }}</span>
                <small v-if="fileMetaText(file, column)">{{ fileMetaText(file, column) }}</small>
              </div>
              <div class="mango-file-list__file-actions">
                <el-button
                  v-if="column.showPreview"
                  link
                  type="primary"
                  data-action="file-list.preview"
                  :disabled="!hasFile(file)"
                  @click.stop="previewFile(file)"
                  >预览</el-button
                >
                <el-button
                  v-if="column.showDownload"
                  link
                  type="primary"
                  data-action="file-list.download"
                  :disabled="!hasFile(file)"
                  @click.stop="downloadFile(file)"
                  >下载</el-button
                >
                <slot
                  v-if="column.fileActionsSlot"
                  :name="fileActionsSlotName(column)"
                  :row="row"
                  :row-index="$index"
                  :column="column"
                  :file="file"
                  :file-index="fileIndex"
                  :has-file="hasFile(file)"
                />
              </div>
            </div>
          </div>
          <div v-else class="mango-file-list__text-cell">{{ displayCellValue(row, column) }}</div>
        </template>
      </el-table-column>
      <template #empty><el-empty data-state="empty" :description="emptyText" :image-size="72" /></template>
    </el-table>
    <MangoFilePreviewDialog
      v-for="previewWindow in previewWindows"
      :key="previewWindow.key"
      :model-value="previewWindow.visible"
      :title="previewWindow.title"
      :file-id="previewWindow.fileId || undefined"
      :file="previewWindow.file"
      :preview="previewWindow.preview"
      @update:model-value="(value) => updatePreviewWindowVisible(previewWindow.key, value)"
      @closed="removePreviewWindow(previewWindow.key)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage, type TableColumnCtx } from 'element-plus';
import { downloadFileRecord, type FilePreview, type FileRecord } from '../api/file';
import MangoFilePreviewDialog from './MangoFilePreviewDialog.vue';
import type {
  MangoFileListCellSlotProps,
  MangoFileListColumn,
  MangoFileListColumnType,
  MangoFileListFile,
  MangoFileListFileActionsSlotProps,
  MangoFileListFileMeta,
  MangoFileListProps,
  MangoFileListRow,
} from './MangoFileList.types';

const SLOT_SEGMENT_PATTERN = /^[A-Za-z][A-Za-z0-9-]*$/;

interface NormalizedColumn extends MangoFileListColumn {
  prop: string;
  type: MangoFileListColumnType;
  align: 'left' | 'center' | 'right';
  emptyText: string;
  mergeBy: string[];
  fileMeta: MangoFileListFileMeta[];
  showPreview: boolean;
  showDownload: boolean;
}

interface FilePreviewWindow {
  key: string;
  identity: string;
  visible: boolean;
  title: string;
  fileId: string;
  file: FileRecord | null;
  preview: FilePreview | null;
}

defineOptions({ name: 'MangoFileList' });
defineSlots<{
  [name: `cell-${string}`]: (props: MangoFileListCellSlotProps) => unknown;
  [name: `file-actions-${string}`]: (props: MangoFileListFileActionsSlotProps) => unknown;
}>();

const props = withDefaults(defineProps<MangoFileListProps>(), {
  emptyText: '暂无文件',
  selectable: false,
  selectableWithoutFile: false,
  mergeAdjacentRows: true,
  tableStyle: undefined,
});
const emit = defineEmits<{ (event: 'selection-change', rows: MangoFileListRow[]): void }>();
const previewWindows = ref<FilePreviewWindow[]>([]);

const resolvedHeaderCellStyle = computed(() => ({
  height: '48px',
  color: 'var(--mango-table-header-text)',
  backgroundColor: 'var(--mango-table-header-bg)',
  fontWeight: 600,
  ...props.tableStyle?.headerCellStyle,
}));
const resolvedBodyCellStyle = computed(() => ({ padding: '0', ...props.tableStyle?.bodyCellStyle }));
const resolvedTableStyle = computed(() => ({
  '--mango-file-list-border-radius': cssSize(props.tableStyle?.borderRadius, '6px'),
  '--mango-file-list-content-min-height': cssSize(props.tableStyle?.contentMinHeight, '54px'),
  '--mango-file-list-content-padding': props.tableStyle?.contentPadding || '14px 16px',
  '--mango-file-list-content-line-height': cssSize(props.tableStyle?.contentLineHeight, '22px'),
}));

const normalizedRows = computed(() => {
  const keys = new Set<string>();
  return props.rows.map((row) => {
    const key = String(row.key ?? '').trim();
    if (!key) throw new Error('MangoFileList: 每一行都必须提供非空 key');
    if (keys.has(key)) throw new Error(`MangoFileList: 行 key ${key} 重复`);
    keys.add(key);
    return row;
  });
});

const normalizedColumns = computed<NormalizedColumn[]>(() => {
  if (!props.columns.length) throw new Error('MangoFileList: 至少需要配置一列');
  const keys = new Set<string>();
  const cellSlots = new Set<string>();
  const actionSlots = new Set<string>();
  return props.columns.map((column) => {
    const key = validateSlotSegment(column.key, '列 key');
    if (keys.has(key)) throw new Error(`MangoFileList: 列 key ${key} 重复`);
    keys.add(key);
    const type = column.type || 'text';
    const prop = String(column.prop || '').trim();
    const slot = column.slot ? validateSlotSegment(column.slot, `列 ${key} 的 slot`) : undefined;
    const fileActionsSlot = column.fileActionsSlot
      ? validateSlotSegment(column.fileActionsSlot, `列 ${key} 的 fileActionsSlot`)
      : undefined;
    if (type === 'custom' && !slot) throw new Error(`MangoFileList: 自定义列 ${key} 必须配置 slot`);
    if (fileActionsSlot && type !== 'files')
      throw new Error(`MangoFileList: 列 ${key} 只有 files 类型可以配置 fileActionsSlot`);
    if (slot && fileActionsSlot) throw new Error(`MangoFileList: 列 ${key} 的 slot 与 fileActionsSlot 不能同时配置`);
    if (type !== 'custom' && !prop) throw new Error(`MangoFileList: 列 ${key} 必须配置 prop`);
    if (column.mergeAdjacent && !prop && !column.mergeBy?.length)
      throw new Error(`MangoFileList: 合并列 ${key} 必须配置 prop 或 mergeBy`);
    if (slot && cellSlots.has(slot)) throw new Error(`MangoFileList: 单元格 Slot ${slot} 重复`);
    if (fileActionsSlot && actionSlots.has(fileActionsSlot))
      throw new Error(`MangoFileList: 文件操作 Slot ${fileActionsSlot} 重复`);
    if (slot) cellSlots.add(slot);
    if (fileActionsSlot) actionSlots.add(fileActionsSlot);
    return {
      ...column,
      key,
      prop,
      type,
      align: column.align || 'left',
      emptyText: column.emptyText ?? (type === 'files' ? '暂未上传' : '-'),
      mergeBy: (column.mergeBy?.length ? column.mergeBy : [prop]).filter(Boolean),
      slot,
      fileActionsSlot,
      fileMeta: column.fileMeta ?? ['version', 'uploadedAt'],
      showPreview: column.showPreview !== false,
      showDownload: column.showDownload !== false,
    };
  });
});

function cssSize(value: string | number | undefined, fallback: string) {
  return typeof value === 'number' ? `${value}px` : value || fallback;
}
function rowKey(row: MangoFileListRow) {
  return String(row.key);
}
function cellValue(row: MangoFileListRow, column: MangoFileListColumn) {
  return column.prop ? row[column.prop] : undefined;
}
function displayValue(value: unknown) {
  if (value === null || value === undefined) return '';
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (typeof value === 'object') return '';
  return String(value).trim();
}
function displayCellValue(row: MangoFileListRow, column: NormalizedColumn) {
  const value = cellValue(row, column);
  return Array.isArray(value)
    ? value.map(displayValue).filter(Boolean).join('、') || column.emptyText
    : displayValue(value) || column.emptyText;
}
function columnFiles(row: MangoFileListRow, column: MangoFileListColumn) {
  const value = cellValue(row, column);
  return Array.isArray(value)
    ? value.filter(
        (file): file is MangoFileListFile => Boolean(file) && typeof file === 'object' && !Array.isArray(file),
      )
    : [];
}
function displayFiles(row: MangoFileListRow, column: MangoFileListColumn) {
  const files = columnFiles(row, column);
  return files.length ? files : [{}];
}
function fileNameText(file: MangoFileListFile) {
  return String(file.fileName || '').trim();
}
function hasFile(file: MangoFileListFile) {
  return Boolean(String(file.fileId || '').trim() || safeDirectUrl(file.fileUrl));
}
function fileDisplayName(file: MangoFileListFile, column: NormalizedColumn) {
  return hasFile(file) ? fileNameText(file) || '未命名文件' : column.emptyText;
}
function fileKey(row: MangoFileListRow, column: MangoFileListColumn, file: MangoFileListFile, index: number) {
  return `${rowKey(row)}:${column.key}:${String(file.key || file.fileId || file.fileUrl || index)}`;
}
function fileMetaText(file: MangoFileListFile, column: NormalizedColumn) {
  return column.fileMeta
    .flatMap((field) => {
      if (field === 'fileSize' && Number(file.fileSize || 0) > 0)
        return [`大小 ${formatFileSize(Number(file.fileSize))}`];
      if (field === 'version' && String(file.version ?? '').trim()) return [`版本 ${file.version}`];
      if (field === 'uploadedAt' && String(file.uploadedAt || '').trim()) return [`上传时间 ${file.uploadedAt}`];
      return [];
    })
    .join(' · ');
}
function formatFileSize(value: number) {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}
function cellSlotName(column: NormalizedColumn): `cell-${string}` {
  return `cell-${column.slot}`;
}
function fileActionsSlotName(column: NormalizedColumn): `file-actions-${string}` {
  return `file-actions-${column.fileActionsSlot}`;
}
function canSelectRow(row: MangoFileListRow) {
  return (
    props.selectableWithoutFile ||
    normalizedColumns.value
      .filter((column) => column.type === 'files')
      .some((column) => columnFiles(row, column).some(hasFile))
  );
}
function handleSelectionChange(rows: MangoFileListRow[]) {
  emit('selection-change', rows.filter(canSelectRow));
}

function adjacentSpanMethod({
  rowIndex,
  columnIndex,
}: {
  row: MangoFileListRow;
  column: TableColumnCtx<MangoFileListRow>;
  rowIndex: number;
  columnIndex: number;
}) {
  if (!props.mergeAdjacentRows) return [1, 1];
  const column = normalizedColumns.value[columnIndex - (props.selectable ? 1 : 0)];
  if (!column?.mergeAdjacent) return [1, 1];
  const currentKey = adjacentMergeKey(normalizedRows.value[rowIndex], column);
  if (!currentKey) return [1, 1];
  const previousKey = rowIndex > 0 ? adjacentMergeKey(normalizedRows.value[rowIndex - 1], column) : '';
  if (previousKey === currentKey) return [0, 0];
  let rowspan = 1;
  for (let index = rowIndex + 1; index < normalizedRows.value.length; index += 1) {
    if (adjacentMergeKey(normalizedRows.value[index], column) !== currentKey) break;
    rowspan += 1;
  }
  return [rowspan, 1];
}
function adjacentMergeKey(row: MangoFileListRow, column: NormalizedColumn) {
  const values = column.mergeBy.map((field) => mergeValue(row[field]));
  return values.some((value) => value === '') ? '' : JSON.stringify(values);
}
function mergeValue(value: unknown) {
  return value === null || value === undefined || typeof value === 'object' ? '' : String(value).trim();
}

function previewFile(value: MangoFileListFile) {
  const file = fileRecord(value);
  if (!file) return ElMessage.warning('暂无可预览文件');
  const fileId = String(value.fileId || '').trim();
  const url = safeDirectUrl(value.fileUrl);
  const identity = fileId ? `file:${fileId}` : `url:${url}`;
  if (previewWindows.value.some((item) => item.identity === identity)) return;
  previewWindows.value.push({
    key: identity,
    identity,
    visible: true,
    title: `文件预览：${file.fileName}`,
    fileId,
    file: fileId ? null : file,
    preview: fileId ? null : directFilePreview(file, url),
  });
}
function updatePreviewWindowVisible(key: string, visible: boolean) {
  const item = previewWindows.value.find((window) => window.key === key);
  if (item) item.visible = visible;
}
function removePreviewWindow(key: string) {
  previewWindows.value = previewWindows.value.filter((item) => item.key !== key);
}
async function downloadFile(file: MangoFileListFile) {
  const fileId = String(file.fileId || '').trim();
  if (fileId) {
    try {
      await downloadFileRecord({ id: fileId, fileName: fileNameText(file) || `file-${fileId}` });
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '文件下载失败');
    }
    return;
  }
  const url = safeDirectUrl(file.fileUrl);
  if (!url) return ElMessage.warning('暂无可下载文件');
  const link = document.createElement('a');
  link.href = url;
  link.download = fileNameText(file) || '文件';
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  document.body.appendChild(link);
  link.click();
  link.remove();
}
function fileRecord(file: MangoFileListFile): FileRecord | null {
  if (!hasFile(file)) return null;
  const url = safeDirectUrl(file.fileUrl);
  return {
    id: String(file.fileId || ''),
    fileName: fileNameText(file) || '未命名文件',
    fileSize: Number(file.fileSize || 0),
    previewUrl: url,
    downloadUrl: url,
  };
}
function safeDirectUrl(value: unknown) {
  const url = String(value || '').trim();
  return /^(https?:|data:|blob:)/i.test(url) || url.startsWith('/') ? url : '';
}
function directFilePreview(file: FileRecord, url: string): FilePreview {
  if (!url) throw new Error(`${file.fileName} 缺少预览地址`);
  return {
    id: file.id,
    fileName: file.fileName,
    fileExt: fileExtension(file.fileName),
    fileSize: file.fileSize,
    contentType: contentTypeFromName(file.fileName),
    previewable: true,
    previewUrl: url,
    downloadUrl: url,
    directAccess: true,
    directPreviewUrl: url,
    directDownloadUrl: url,
  };
}
function fileExtension(fileName: string) {
  return fileName.includes('.') ? fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase() : '';
}
function contentTypeFromName(fileName: string) {
  const mapping: Record<string, string> = {
    pdf: 'application/pdf',
    png: 'image/png',
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    gif: 'image/gif',
    webp: 'image/webp',
  };
  return mapping[fileExtension(fileName)] || 'application/octet-stream';
}
function validateSlotSegment(value: string, label: string) {
  if (!SLOT_SEGMENT_PATTERN.test(value))
    throw new Error(`MangoFileList: ${label} 必须以英文字母开头，且只能包含英文字母、数字和短横线`);
  return value;
}
</script>

<style scoped>
.mango-file-list {
  min-width: 0;
  overflow-x: auto;
}

.mango-file-list__table {
  width: 100%;
  overflow: hidden;
  border-radius: var(--mango-file-list-border-radius);
}

.mango-file-list__text-cell {
  min-height: var(--mango-file-list-content-min-height);
  padding: var(--mango-file-list-content-padding);
  box-sizing: border-box;
  color: var(--el-text-color-regular);
  line-height: var(--mango-file-list-content-line-height);
  overflow-wrap: anywhere;
}

.mango-file-list__files-cell {
  min-width: 0;
}

.mango-file-list__file {
  display: flex;
  min-height: var(--mango-file-list-content-min-height);
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: var(--mango-file-list-content-padding);
  box-sizing: border-box;
}

.mango-file-list__file + .mango-file-list__file {
  border-top: 1px solid var(--el-border-color-lighter);
}

.mango-file-list__file-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  overflow-wrap: anywhere;
}

.mango-file-list__file-copy > span {
  color: var(--el-text-color-primary);
  line-height: var(--mango-file-list-content-line-height);
}

.mango-file-list__file-copy > span.is-empty,
.mango-file-list__file-copy small {
  color: var(--el-text-color-placeholder);
}

.mango-file-list__file-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 10px;
}

.mango-file-list__file-actions .el-button + .el-button {
  margin-left: 0;
}

@media (width <= 768px) {
  .mango-file-list__table {
    min-width: 720px;
  }
}
</style>
