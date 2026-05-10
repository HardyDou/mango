<template>
  <div class="upload-container">
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
      :on-progress="handleProgress"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :file-list="internalFileList"
      drag
    >
      <el-icon class="el-icon--upload">
        <upload-filled />
      </el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
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
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadProps, UploadRequestOptions } from 'element-plus';
import { uploadFile } from '../../api/upload';
import {
  mergeUploadResult,
  normalizeUploadFiles,
  type MangoUploadFileMeta,
  type MangoUploadUserFile,
} from './types';

const DEFAULT_ACTION = '/api/file/files';

const props = withDefaults(
  defineProps<{
    modelValue?: MangoUploadUserFile[];
    action?: string;
    headers?: Record<string, string>;
    accept?: string;
    limit?: number;
    multiple?: boolean;
    disabled?: boolean;
    autoUpload?: boolean;
    tip?: string;
    maxSize?: number; // MB
    useFileApi?: boolean;
  }>(),
  {
    modelValue: () => [],
    action: DEFAULT_ACTION,
    headers: () => ({}),
    accept: '*',
    limit: 1,
    multiple: false,
    disabled: false,
    autoUpload: true,
    maxSize: 10,
    useFileApi: true,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: MangoUploadUserFile[]): void;
  (e: 'change', value: MangoUploadUserFile[]): void;
  (e: 'success', response: MangoUploadFileMeta, file: MangoUploadUserFile): void;
  (e: 'error', error: any, file: MangoUploadUserFile): void;
}>();

const uploadRef = ref();
const internalFileList = ref<MangoUploadUserFile[]>([...props.modelValue]);

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

// Watch for external value changes
watch(
  () => props.modelValue,
  (newValue) => {
    internalFileList.value = [...newValue];
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
  }, uploadedFile);
  updateModelValue();
};

// Handle error
const handleError: UploadProps['onError'] = (error, file) => {
  ElMessage.error('上传失败');
  emit('error', error, file);
};

// Handle progress
const handleProgress: UploadProps['onProgress'] = (event, file) => {
  // Can be used for progress display
};

// Handle change
const handleChange: UploadProps['onChange'] = (file, files) => {
  internalFileList.value = normalizeUploadFiles(files as MangoUploadUserFile[]);
  updateModelValue();
  emit('change', files);
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = (file, files) => {
  internalFileList.value = normalizeUploadFiles(files as MangoUploadUserFile[]);
  updateModelValue();
  emit('change', files);
};

// Update model value
const updateModelValue = () => {
  emit('update:modelValue', internalFileList.value);
};

// Expose methods
defineExpose({
  upload: () => uploadRef.value?.submit(),
  abort: () => uploadRef.value?.abort(),
  clearFiles: () => uploadRef.value?.clearFiles(),
});
</script>

<style scoped lang="scss">
.upload-container {
  width: 100%;
}
</style>
