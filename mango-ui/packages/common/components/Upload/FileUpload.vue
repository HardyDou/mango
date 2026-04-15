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
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import type { UploadProps, UploadUserFile } from 'element-plus';

const props = withDefaults(
  defineProps<{
    modelValue?: string | string[];
    action?: string;
    headers?: Record<string, string>;
    accept?: string;
    limit?: number;
    multiple?: boolean;
    disabled?: boolean;
    autoUpload?: boolean;
    tip?: string;
    maxSize?: number;
  }>(),
  {
    modelValue: '',
    action: '/api/admin/upload/file',
    headers: () => ({}),
    accept: '*',
    limit: 10,
    multiple: true,
    disabled: false,
    autoUpload: true,
    maxSize: 50,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void;
  (e: 'change', value: string | string[]): void;
}>();

const uploadRef = ref();

const internalFileList = ref<UploadUserFile[]>([]);

// Initialize file list from model value
const initFileList = () => {
  if (!props.modelValue) {
    internalFileList.value = [];
    return;
  }

  if (Array.isArray(props.modelValue)) {
    internalFileList.value = props.modelValue.map((url, index) => ({
      name: `file-${index}`,
      url,
    }));
  } else {
    internalFileList.value = [{ name: 'file-0', url: props.modelValue }];
  }
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
const handleSuccess: UploadProps['onSuccess'] = (response, file) => {
  file.url = response.url;
  updateModelValue();
};

// Handle error
const handleError: UploadProps['onError'] = () => {
  ElMessage.error('上传失败');
};

// Handle change
const handleChange: UploadProps['onChange'] = () => {
  updateModelValue();
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = () => {
  updateModelValue();
};

// Update model value
const updateModelValue = () => {
  const urls = internalFileList.value
    .filter((f) => f.url)
    .map((f) => f.url as string);

  const value = props.multiple ? urls : urls[0] || '';
  emit('update:modelValue', value);
  emit('change', value);
};

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
