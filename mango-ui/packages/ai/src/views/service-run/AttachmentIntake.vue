<template>
  <div
    class="mango-ai-attachment-intake"
    :class="{ 'is-dragging': dragging }"
    @dragenter.prevent="handleDragEnter"
    @dragover.prevent="handleDragOver"
    @dragleave.prevent="handleDragLeave"
    @drop.prevent="handleDrop"
    @paste="handlePaste"
  >
    <slot />
    <div v-if="dragging" class="mango-ai-attachment-intake__overlay" data-state="ai.service-run.attachment-drop">
      <span>松开以添加到当前消息</span>
      <small>{{ hint }}</small>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue';

defineOptions({ name: 'AiAttachmentIntake' });
const props = defineProps<{ disabled: boolean; hint: string }>();
const emit = defineEmits<{ files: [files: File[]] }>();
const dragging = ref(false);
let dragDepth = 0;

function handleDragEnter(event: DragEvent) {
  if (props.disabled || !hasFiles(event.dataTransfer)) return;
  dragDepth += 1;
  dragging.value = true;
}

function handleDragOver(event: DragEvent) {
  if (!props.disabled && hasFiles(event.dataTransfer) && event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy';
  }
}

function handleDragLeave(event: DragEvent) {
  if (!hasFiles(event.dataTransfer)) return;
  dragDepth = Math.max(0, dragDepth - 1);
  if (dragDepth === 0) dragging.value = false;
}

function handleDrop(event: DragEvent) {
  resetDragState();
  if (props.disabled) return;
  emitFiles(event.dataTransfer?.files);
}

function handlePaste(event: ClipboardEvent) {
  if (props.disabled) return;
  const files = Array.from(event.clipboardData?.files || []);
  if (!files.length) return;
  event.preventDefault();
  emit('files', files);
}

function emitFiles(fileList?: FileList | null) {
  const files = Array.from(fileList || []);
  if (files.length) emit('files', files);
}

function hasFiles(transfer?: DataTransfer | null) {
  return Array.from(transfer?.types || []).includes('Files');
}

function resetDragState() {
  dragDepth = 0;
  dragging.value = false;
}

onBeforeUnmount(resetDragState);
</script>
