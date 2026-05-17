<template>
  <el-upload
    v-auth="uploadPermission"
    :show-file-list="showFileList"
    :multiple="multiple"
    :limit="limit"
    :accept="accept"
    :disabled="disabled"
    :http-request="handleUpload"
  >
    <el-button type="primary" :disabled="disabled">
      <el-icon class="el-icon--left"><Upload /></el-icon>
      {{ buttonText }}
    </el-button>
  </el-upload>
</template>

<script setup lang="ts">
import { ElMessage, type UploadRequestOptions } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import { fileApi, type FileRecord } from '../api/file';

const props = withDefaults(defineProps<{
  purpose?: string;
  accessLevel?: string;
  bizType?: string;
  bizId?: string;
  directoryId?: string | number;
  accept?: string;
  limit?: number;
  multiple?: boolean;
  disabled?: boolean;
  showFileList?: boolean;
  buttonText?: string;
  uploadPermission?: string;
}>(), {
  purpose: 'attachment',
  accessLevel: 'PRIVATE',
  accept: '*',
  limit: 10,
  multiple: false,
  disabled: false,
  showFileList: false,
  buttonText: '上传文件',
  uploadPermission: 'file:files:upload',
});

const emit = defineEmits<{
  (e: 'success', value: FileRecord): void;
  (e: 'error', value: Error): void;
}>();

async function handleUpload(options: UploadRequestOptions) {
  try {
    const record = await fileApi.upload(options.file as File, {
      purpose: props.purpose,
      accessLevel: props.accessLevel,
      bizType: props.bizType,
      bizId: props.bizId,
      directoryId: props.directoryId,
    });
    ElMessage.success('上传成功');
    emit('success', record);
    options.onSuccess?.(record);
  } catch (error) {
    emit('error', error as Error);
    options.onError?.(error as Error);
  }
}
</script>
