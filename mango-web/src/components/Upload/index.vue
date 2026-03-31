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
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import type { UploadProps, UploadUserFile } from 'element-plus';

const props = withDefaults(
  defineProps<{
    modelValue?: UploadUserFile[];
    action?: string;
    headers?: Record<string, string>;
    accept?: string;
    limit?: number;
    multiple?: boolean;
    disabled?: boolean;
    autoUpload?: boolean;
    tip?: string;
    maxSize?: number; // MB
  }>(),
  {
    modelValue: () => [],
    action: '/api/admin/upload/file',
    headers: () => ({}),
    accept: '*',
    limit: 1,
    multiple: false,
    disabled: false,
    autoUpload: true,
    maxSize: 10,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: UploadUserFile[]): void;
  (e: 'change', value: UploadUserFile[]): void;
  (e: 'success', response: any, file: UploadUserFile): void;
  (e: 'error', error: any, file: UploadUserFile): void;
}>();

const uploadRef = ref();
const internalFileList = ref<UploadUserFile[]>([...props.modelValue]);

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
const handleSuccess: UploadProps['onSuccess'] = (response, file) => {
  emit('success', response, file);
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
  internalFileList.value = files;
  updateModelValue();
  emit('change', files);
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = (file, files) => {
  internalFileList.value = files;
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
