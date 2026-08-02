<template>
  <el-avatar v-bind="$attrs" :size="size" :shape="shape" :fit="fit" :src="resolvedSource" @error="handleError">
    <slot />
  </el-avatar>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue';
import { downloadUploadedFile } from '../../api/upload';

defineOptions({
  name: 'MangoAvatar',
  inheritAttrs: false,
});

const props = withDefaults(
  defineProps<{
    source?: string;
    size?: number | string;
    shape?: 'circle' | 'square';
    fit?: 'fill' | 'contain' | 'cover' | 'none' | 'scale-down';
  }>(),
  {
    source: '',
    size: undefined,
    shape: 'circle',
    fit: 'cover',
  },
);

const emit = defineEmits<{
  (event: 'error', error: unknown): void;
}>();

const resolvedSource = ref('');
let objectUrl = '';
let resolveSequence = 0;

watch(
  () => props.source,
  (source) => {
    void resolveSource(source);
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  resolveSequence += 1;
  revokeObjectUrl();
});

async function resolveSource(source?: string) {
  const sequence = ++resolveSequence;
  revokeObjectUrl();
  resolvedSource.value = '';
  const normalized = String(source || '').trim();
  if (isDisplayUrl(normalized)) {
    resolvedSource.value = normalized;
    return;
  }
  const fileId = fileTokenId(normalized);
  if (!fileId) return;
  try {
    const response = await downloadUploadedFile(fileId);
    const blob =
      response.data instanceof Blob
        ? response.data
        : new Blob([response.data], { type: response.headers?.['content-type'] || 'application/octet-stream' });
    const nextObjectUrl = URL.createObjectURL(blob);
    if (sequence !== resolveSequence) {
      URL.revokeObjectURL(nextObjectUrl);
      return;
    }
    objectUrl = nextObjectUrl;
    resolvedSource.value = nextObjectUrl;
  } catch (error) {
    if (sequence === resolveSequence) emit('error', error);
  }
}

function fileTokenId(value: string) {
  return value.match(/^mango-file:([1-9]\d*)$/u)?.[1] || '';
}

function isDisplayUrl(value: string) {
  return /^(https?:|data:|blob:|\/)/iu.test(value);
}

function revokeObjectUrl() {
  if (!objectUrl) return;
  URL.revokeObjectURL(objectUrl);
  objectUrl = '';
}

function handleError(error: Event) {
  emit('error', error);
}
</script>
