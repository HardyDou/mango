<template>
  <div class="mango-ai-file-part" :class="`is-${part.type.toLowerCase()}`">
    <img v-if="part.type === 'IMAGE' && previewUrl" :src="previewUrl" :alt="fileName" />
    <video v-else-if="part.type === 'VIDEO' && previewUrl" :src="previewUrl" controls preload="metadata" />
    <audio v-else-if="part.type === 'AUDIO' && previewUrl" :src="previewUrl" controls preload="metadata" />
    <div v-else class="mango-ai-file-part__card">
      <el-icon><Document /></el-icon>
      <div>
        <strong>{{ fileName }}</strong
        ><small>{{ fileDescription }}</small>
      </div>
    </div>
    <div class="mango-ai-file-part__actions">
      <span v-if="loading">正在加载预览</span>
      <span v-else-if="previewError">预览不可用，可下载原文件</span>
      <el-button link type="primary" :disabled="downloading" @click="download">下载</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AiMessageContentPart } from '@mango/ai-api';
import { Document } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useAiConfigurationApi } from '../../composables/useAiConfigurationApi';

defineOptions({ name: 'AiChatFilePart' });
const props = defineProps<{ part: AiMessageContentPart }>();
const api = useAiConfigurationApi();
const previewUrl = ref('');
const loading = ref(false);
const previewError = ref(false);
const downloading = ref(false);
let objectUrl = '';

const fileName = computed(() => props.part.fileName || `文件 ${props.part.fileId || ''}`.trim());
const fileDescription = computed(() => {
  const size = props.part.fileSize ? formatBytes(props.part.fileSize) : '未知大小';
  return `${props.part.contentType || '文件'} · ${size}`;
});

onMounted(() => void loadPreview());
onBeforeUnmount(revokeObjectUrl);

async function loadPreview() {
  if (!props.part.fileId || !['IMAGE', 'VIDEO', 'AUDIO'].includes(props.part.type)) return;
  loading.value = true;
  try {
    const response = await api.previewChatFile(props.part.fileId);
    const blob =
      response instanceof Blob
        ? response
        : new Blob([response], { type: props.part.contentType || 'application/octet-stream' });
    objectUrl = URL.createObjectURL(blob);
    previewUrl.value = objectUrl;
  } catch {
    previewError.value = true;
  } finally {
    loading.value = false;
  }
}

async function download() {
  if (!props.part.fileId) return;
  downloading.value = true;
  try {
    const response = await api.downloadChatFile(props.part.fileId);
    const blob =
      response instanceof Blob
        ? response
        : new Blob([response], { type: props.part.contentType || 'application/octet-stream' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName.value;
    link.rel = 'noopener noreferrer';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 0);
  } catch {
    ElMessage.error('文件下载失败');
  } finally {
    downloading.value = false;
  }
}

function revokeObjectUrl() {
  if (objectUrl) URL.revokeObjectURL(objectUrl);
  objectUrl = '';
}

function formatBytes(value: number) {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}
</script>
