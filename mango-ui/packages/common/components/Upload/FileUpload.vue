<template>
  <div class="file-upload-container">
    <el-upload
      ref="uploadRef"
      :action="action"
      :headers="headers"
      :accept="accept"
      :limit="limit"
      :multiple="multiple"
      :disabled="disabled"
      :auto-upload="autoUpload"
      :http-request="fileApiRequest"
      :before-upload="handleBeforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :file-list="internalFileList"
    >
      <el-button
        type="primary"
        :disabled="disabled"
      >
        <el-icon class="el-icon--left">
          <upload />
        </el-icon>
        点击上传
      </el-button>
      <template #tip>
        <div
          v-if="tip"
          class="el-upload__tip"
        >
          {{ tip }}
        </div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import type { UploadProps, UploadRequestOptions } from 'element-plus';
import { getUploadedFileDetail, uploadFile } from '../../api/upload';
import {
  mergeUploadResult,
  modelValueToUploadFiles,
  normalizeUploadFiles,
  normalizeUploadResult,
  type MangoUploadFileMeta,
  type MangoUploadModelValue,
  type MangoUploadUserFile,
  uploadFilesToModelValue,
} from './types';

const DEFAULT_ACTION = '/api/file/files';

const props = withDefaults(
  defineProps<{
    modelValue?: MangoUploadModelValue;
    action?: string;
    headers?: Record<string, string>;
    accept?: string;
    limit?: number;
    multiple?: boolean;
    disabled?: boolean;
    autoUpload?: boolean;
    tip?: string;
    maxSize?: number;
    useFileApi?: boolean;
    valueType?: 'token' | 'record';
  }>(),
  {
    modelValue: '',
    action: DEFAULT_ACTION,
    headers: () => ({}),
    accept: '*',
    limit: 10,
    multiple: true,
    disabled: false,
    autoUpload: true,
    maxSize: 50,
    useFileApi: true,
    valueType: 'token',
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[] | MangoUploadFileMeta | MangoUploadFileMeta[] | null): void;
  (e: 'change', value: string | string[] | MangoUploadFileMeta | MangoUploadFileMeta[] | null): void;
  (e: 'success', value: MangoUploadFileMeta): void;
}>();

const uploadRef = ref();

const internalFileList = ref<MangoUploadUserFile[]>([]);

const handleFileApiRequest = (options: UploadRequestOptions) => {
  return uploadFile(options.file as File)
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

const initFileList = () => {
  internalFileList.value = modelValueToUploadFiles(props.modelValue);
  void hydrateFileList();
};

initFileList();

// Watch for external value changes
watch(
  () => props.modelValue,
  () => {
    initFileList();
  }
);

// Handle before upload
const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  const fileSizeMB = file.size / 1024 / 1024;
  if (fileSizeMB > props.maxSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`);
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
  emit('success', {
    id: uploadedFile.id,
    uid: uploadedFile.uid,
    name: uploadedFile.fileName || uploadedFile.name,
    url: uploadedFile.url || '',
    fileName: uploadedFile.fileName || uploadedFile.name,
    fileSize: Number(uploadedFile.fileSize ?? uploadedFile.size ?? 0),
    contentType: uploadedFile.contentType,
    objectName: uploadedFile.objectName,
  });
  updateModelValue();
};

// Handle error
const handleError: UploadProps['onError'] = () => {
  ElMessage.error('上传失败');
};

// Handle change
const handleChange: UploadProps['onChange'] = (_file, files) => {
  internalFileList.value = normalizeUploadFiles(files as MangoUploadUserFile[]);
  if (internalFileList.value.some(file => file.url)) {
    updateModelValue();
  }
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = (_file, files) => {
  internalFileList.value = normalizeUploadFiles(files as MangoUploadUserFile[]);
  updateModelValue();
};

// Update model value
const updateModelValue = () => {
  const value = uploadFilesToModelValue(internalFileList.value, props.multiple, props.valueType);
  emit('update:modelValue', value);
  emit('change', value);
};

async function hydrateFileList() {
  const files = internalFileList.value.filter(file => (file.id || file.fileId) && !file.fileName);
  if (!files.length) return;
  const records = await Promise.all(files.map(file => getUploadedFileDetail(file.id || file.fileId || '').catch(() => null)));
  const recordMap = new Map(
    records
      .filter((item): item is MangoUploadFileMeta => Boolean(item?.id))
      .map(item => [String(item.id), normalizeUploadResult(item)]),
  );
  if (!recordMap.size) return;
  internalFileList.value = internalFileList.value.map((file) => {
    const id = file.id || file.fileId;
    const record = id ? recordMap.get(String(id)) : undefined;
    return record
      ? {
          ...file,
          id: record.id,
          fileId: record.id,
          name: record.fileName,
          url: record.url,
          fileName: record.fileName,
          fileSize: record.fileSize,
          contentType: record.contentType,
          objectName: record.objectName,
        }
      : file;
  });
}

// Expose methods
defineExpose({
  upload: () => uploadRef.value?.submit(),
  abort: () => uploadRef.value?.abort(),
  clearFiles: () => uploadRef.value?.clearFiles(),
});
</script>

<style scoped lang="scss">
.file-upload-container {
  width: 100%;
}
</style>
