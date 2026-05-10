<template>
  <div class="excel-upload-container">
    <el-upload
      ref="uploadRef"
      :action="action"
      :headers="headers"
      :accept="accept"
      :limit="1"
      :multiple="false"
      :disabled="disabled"
      :auto-upload="autoUpload"
      :http-request="fileApiRequest"
      :before-upload="handleBeforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :file-list="internalFileList"
      drag
    >
      <el-icon class="el-icon--upload">
        <upload-filled />
      </el-icon>
      <div class="el-upload__text">
        将 Excel 文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          仅支持 .xls, .xlsx 格式，
          <template v-if="templateUrl">
            <el-link
              type="primary"
              :href="templateUrl"
              :underline="false"
            >
              下载模板
            </el-link>
          </template>
        </div>
      </template>
    </el-upload>

    <!-- Data Preview -->
    <div
      v-if="previewData.length > 0"
      class="excel-preview"
    >
      <div class="preview-header">
        <span>数据预览（前 {{ previewLimit }} 行）</span>
        <el-button
          type="text"
          @click="clearPreview"
        >
          清除
        </el-button>
      </div>
      <el-table
        :data="previewData"
        border
        size="small"
        max-height="300"
      >
        <el-table-column
          v-for="(header, index) in previewHeaders"
          :key="index"
          :prop="header"
          :label="header"
          min-width="120"
        />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadProps, UploadRequestOptions } from 'element-plus';
import * as XLSX from 'xlsx';
import { uploadExcel } from '../../api/upload';
import {
  mergeUploadResult,
  type MangoUploadFileMeta,
  type MangoUploadUserFile,
} from './types';

const DEFAULT_ACTION = '/api/file/files';

const props = withDefaults(
  defineProps<{
    modelValue?: any[];
    action?: string;
    headers?: Record<string, string>;
    disabled?: boolean;
    autoUpload?: boolean;
    templateUrl?: string;
    previewLimit?: number;
    useFileApi?: boolean;
  }>(),
  {
    modelValue: () => [],
    action: DEFAULT_ACTION,
    headers: () => ({}),
    disabled: false,
    autoUpload: true,
    previewLimit: 10,
    useFileApi: true,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: any[]): void;
  (e: 'change', value: any[]): void;
  (e: 'success', data: any[]): void;
  (e: 'upload-success', file: MangoUploadFileMeta): void;
}>();

const uploadRef = ref();
const accept = '.xls,.xlsx';
const internalFileList = ref<MangoUploadUserFile[]>([]);
const parsedData = ref<any[]>([]);

const handleFileApiRequest = (options: UploadRequestOptions) => {
  return uploadExcel(options.file as File)
    .then((result) => {
      options.onSuccess?.(result);
      return result;
    })
    .catch((error) => {
      options.onError?.(error);
      throw error;
    });
};

const fileApiRequest = computed(() => (
  props.useFileApi && props.action === DEFAULT_ACTION ? handleFileApiRequest : undefined
));

const previewHeaders = computed(() => {
  if (parsedData.value.length > 0) {
    return Object.keys(parsedData.value[0]);
  }
  return [];
});

const previewData = computed(() => {
  return parsedData.value.slice(0, props.previewLimit);
});

// Watch for external value changes
watch(
  () => props.modelValue,
  (newValue) => {
    parsedData.value = newValue || [];
  }
);

// Handle before upload
const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isExcel = file.name.endsWith('.xls') || file.name.endsWith('.xlsx');
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件');
    return false;
  }
  return true;
};

// Handle success
const handleSuccess: UploadProps['onSuccess'] = (response, file, files) => {
  const uploadedFile = mergeUploadResult(file as MangoUploadUserFile, response);
  internalFileList.value = files.map((item) => (
    item.uid === file.uid ? uploadedFile : item as MangoUploadUserFile
  ));
  const serverData = Array.isArray(response?.data) ? response.data : [];
  if (serverData.length > 0) {
    parsedData.value = serverData;
  }
  updateModelValue();
  emit('success', parsedData.value);
  emit('upload-success', {
    id: uploadedFile.id,
    uid: uploadedFile.uid,
    name: uploadedFile.fileName || uploadedFile.name,
    url: uploadedFile.url || '',
    fileName: uploadedFile.fileName || uploadedFile.name,
    fileSize: Number(uploadedFile.fileSize ?? uploadedFile.size ?? 0),
    contentType: uploadedFile.contentType,
    objectName: uploadedFile.objectName,
  });
};

// Handle error
const handleError: UploadProps['onError'] = () => {
  ElMessage.error('上传失败');
};

// Handle change
const handleChange: UploadProps['onChange'] = (file) => {
  if (file.status === 'ready') {
    parseExcel(file.raw);
  }
  if (file.status === 'removed') {
    clearPreview();
  }
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = () => {
  clearPreview();
};

// Parse Excel file locally
const parseExcel = (file: File) => {
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const data = e.target?.result;
      const workbook = XLSX.read(data, { type: 'binary' });
      const firstSheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[firstSheetName];
      const jsonData = XLSX.utils.sheet_to_json(worksheet);

      parsedData.value = jsonData as any[];
      updateModelValue();
    } catch (error) {
      ElMessage.error('Excel 解析失败');
    }
  };
  reader.readAsBinaryString(file);
};

// Clear preview
const clearPreview = () => {
  parsedData.value = [];
  internalFileList.value = [];
  updateModelValue();
};

// Update model value
const updateModelValue = () => {
  emit('update:modelValue', parsedData.value);
  emit('change', parsedData.value);
};

// Expose methods
defineExpose({
  upload: () => uploadRef.value?.submit(),
  abort: () => uploadRef.value?.abort(),
  clearFiles: () => uploadRef.value?.clearFiles(),
  getData: () => parsedData.value,
});
</script>

<style scoped lang="scss">
.excel-upload-container {
  width: 100%;

  .excel-preview {
    margin-top: 16px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 12px;

    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      font-size: 14px;
      color: #606266;
    }
  }
}
</style>
