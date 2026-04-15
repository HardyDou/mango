<template>
  <div class="image-upload-container">
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
      :on-preview="handlePreview"
      :on-remove="handleRemove"
      :on-change="handleChange"
      :file-list="internalFileList"
      list-type="picture-card"
    >
      <el-icon class="el-icon-plus">
        <plus />
      </el-icon>
    </el-upload>

    <!-- Image Preview Dialog -->
    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="60%"
    >
      <img
        :src="previewUrl"
        alt="Preview"
        style="width: 100%"
      >
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { UploadProps, UploadUserFile } from 'element-plus';

const props = withDefaults(
  defineProps<{
    modelValue?: string | string[];
    action?: string;
    headers?: Record<string, string>;
    limit?: number;
    multiple?: boolean;
    disabled?: boolean;
    autoUpload?: boolean;
    maxSize?: number;
  }>(),
  {
    modelValue: '',
    action: '/api/admin/upload/image',
    headers: () => ({}),
    limit: 5,
    multiple: true,
    disabled: false,
    autoUpload: true,
    maxSize: 5,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void;
  (e: 'change', value: string | string[]): void;
}>();

const uploadRef = ref();
const previewVisible = ref(false);
const previewUrl = ref('');

const accept = 'image/*';

const internalFileList = ref<UploadUserFile[]>([]);

// Initialize file list from model value
const initFileList = () => {
  if (!props.modelValue) {
    internalFileList.value = [];
    return;
  }

  if (Array.isArray(props.modelValue)) {
    internalFileList.value = props.modelValue.map((url, index) => ({
      name: `image-${index}`,
      url,
    }));
  } else {
    internalFileList.value = [{ name: 'image-0', url: props.modelValue }];
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
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    ElMessage.error('只能上传图片文件');
    return false;
  }

  const fileSizeMB = file.size / 1024 / 1024;
  if (fileSizeMB > props.maxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`);
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
const handleError: UploadProps['onError'] = (error, file) => {
  ElMessage.error('上传失败');
};

// Handle preview
const handlePreview: UploadProps['onPreview'] = (file) => {
  previewUrl.value = file.url || '';
  previewVisible.value = true;
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = () => {
  updateModelValue();
};

// Handle change
const handleChange: UploadProps['onChange'] = () => {
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
.image-upload-container {
  :deep(.el-upload-list--picture-card) {
    display: flex;
    flex-wrap: wrap;
  }

  :deep(.el-upload--picture-card) {
    width: 100px;
    height: 100px;
    line-height: 100px;
  }
}
</style>
