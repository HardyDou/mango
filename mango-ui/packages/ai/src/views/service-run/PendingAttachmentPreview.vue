<template>
  <div class="mango-ai-pending-preview" :class="`is-${type.toLowerCase()}`">
    <img v-if="type === 'IMAGE' && objectUrl" :src="objectUrl" :alt="file.name" />
    <video v-else-if="type === 'VIDEO' && objectUrl" :src="objectUrl" muted preload="metadata" />
    <el-icon v-else-if="type === 'AUDIO'"><Microphone /></el-icon>
    <el-icon v-else><Document /></el-icon>
  </div>
</template>

<script setup lang="ts">
import type { AiMessageContentPartCommand } from '@mango/ai-api';
import { Document, Microphone } from '@element-plus/icons-vue';
import { onBeforeUnmount, ref, watch } from 'vue';

defineOptions({ name: 'AiPendingAttachmentPreview' });
const props = defineProps<{
  file: File;
  type: Extract<AiMessageContentPartCommand['type'], 'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE'>;
}>();
const objectUrl = ref('');

watch(
  () => [props.file, props.type] as const,
  ([file, type]) => {
    revokeObjectUrl();
    if (type === 'IMAGE' || type === 'VIDEO') objectUrl.value = URL.createObjectURL(file);
  },
  { immediate: true },
);

function revokeObjectUrl() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value);
  objectUrl.value = '';
}

onBeforeUnmount(revokeObjectUrl);
</script>
