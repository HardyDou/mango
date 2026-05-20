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
      :http-request="fileApiRequest"
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
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { UploadProps, UploadRequestOptions } from 'element-plus';
import { createUploadedFileObjectUrl, getUploadedFileDetail, uploadImage } from '../../api/upload';
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
    limit?: number;
    multiple?: boolean;
    disabled?: boolean;
    autoUpload?: boolean;
    maxSize?: number;
    useFileApi?: boolean;
    valueType?: 'token' | 'record';
  }>(),
  {
    modelValue: '',
    action: DEFAULT_ACTION,
    headers: () => ({}),
    limit: 5,
    multiple: true,
    disabled: false,
    autoUpload: true,
    maxSize: 5,
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
const previewVisible = ref(false);
const previewUrl = ref('');

const accept = 'image/*';

const internalFileList = ref<MangoUploadUserFile[]>([]);

const handleFileApiRequest = (options: UploadRequestOptions) => {
  return uploadImage(options.file as File)
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
const handleError: UploadProps['onError'] = (error, file) => {
  ElMessage.error('上传失败');
};

// Handle preview
const handlePreview: UploadProps['onPreview'] = async (file) => {
  const url = file.url || '';
  if (url.startsWith('mango-file:')) {
    previewUrl.value = await createUploadedFileObjectUrl(url.replace('mango-file:', ''));
  } else {
    previewUrl.value = url;
  }
  previewVisible.value = true;
};

// Handle remove
const handleRemove: UploadProps['onRemove'] = (_file, files) => {
  internalFileList.value = normalizeUploadFiles(files as MangoUploadUserFile[]);
  updateModelValue();
};

// Handle change
const handleChange: UploadProps['onChange'] = (_file, files) => {
  internalFileList.value = normalizeUploadFiles(files as MangoUploadUserFile[]);
  if (internalFileList.value.some(file => file.response || (file.url && !file.url.startsWith('blob:')))) {
    updateModelValue();
  }
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
