<template>
  <el-input
    :model-value="displayValue"
    :type="inputType"
    :disabled="disabled"
    :readonly="referenced"
    autocomplete="new-password"
    :placeholder="placeholder"
    data-field="notice.channel.secret"
    @focus="prepareReplacement"
    @blur="finishReplacement"
    @input="handleInput"
  >
    <template #suffix>
      <el-tooltip v-if="referenced" content="由引用管理，不能在页面查看明文" placement="top">
        <span class="secret-action-disabled">
          <el-button link disabled :icon="View" aria-label="Secret 由引用管理" />
        </span>
      </el-tooltip>
      <el-button
        v-else
        link
        :loading="loading"
        :icon="isVisible ? Hide : View"
        :aria-label="isVisible ? '隐藏 Secret' : '查看 Secret'"
        data-action="notice.channel.secret.reveal"
        @mousedown.prevent
        @click="toggleVisibility"
      />
    </template>
  </el-input>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch, type PropType } from 'vue';
import { ElMessage } from 'element-plus';
import { Hide, View } from '@element-plus/icons-vue';
import { getChannelSecret } from '../../../api/notice';

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean] as PropType<string | number | boolean | undefined>,
    default: '',
  },
  channelConfigId: {
    type: [String, Number] as PropType<string | number | undefined>,
    default: undefined,
  },
  secretKey: { type: String, required: true },
  configured: { type: Boolean, default: false },
  referenced: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  resetToken: { type: Number, default: 0 },
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const loading = ref(false);
const revealedValue = ref('');
const revealed = ref(false);
const localVisible = ref(false);
const replacing = ref(false);
let clearTimer: ReturnType<typeof setTimeout> | undefined;
let revealRequestId = 0;
let revealController: AbortController | undefined;

const localValue = computed(() => (props.modelValue == null ? '' : String(props.modelValue)));
const persistedUnchanged = computed(() => props.configured && !localValue.value && !replacing.value);
const displayValue = computed(() => {
  if (revealed.value) return revealedValue.value;
  if (persistedUnchanged.value) return '****';
  return localValue.value;
});
const isVisible = computed(() => revealed.value || localVisible.value);
const inputType = computed(() => (isVisible.value ? 'text' : 'password'));

function prepareReplacement() {
  if (props.referenced) return;
  if (persistedUnchanged.value) replacing.value = true;
}

function finishReplacement() {
  if (!localValue.value && !revealed.value) replacing.value = false;
}

function handleInput(value: string) {
  clearPlaintext();
  replacing.value = true;
  emit('update:modelValue', value);
}

async function toggleVisibility() {
  if (props.referenced || props.disabled) return;
  if (revealed.value) {
    clearPlaintext();
    replacing.value = false;
    return;
  }
  if (localValue.value || !props.configured) {
    localVisible.value = !localVisible.value;
    return;
  }
  if (!props.channelConfigId) return;
  const requestId = ++revealRequestId;
  revealController?.abort();
  const controller = new AbortController();
  revealController = controller;
  loading.value = true;
  try {
    const result = await getChannelSecret(String(props.channelConfigId), props.secretKey, controller.signal);
    if (requestId !== revealRequestId) return;
    revealedValue.value = result.value;
    revealed.value = true;
    replacing.value = false;
    scheduleClear();
  } catch {
    if (requestId !== revealRequestId) return;
    clearPlaintext();
    ElMessage.error('Secret 查看失败，已保持隐藏');
  } finally {
    if (requestId === revealRequestId) {
      loading.value = false;
      if (revealController === controller) revealController = undefined;
    }
  }
}

function scheduleClear() {
  if (clearTimer) clearTimeout(clearTimer);
  clearTimer = setTimeout(clearPlaintext, 60_000);
}

function clearPlaintext() {
  revealRequestId += 1;
  revealController?.abort();
  revealController = undefined;
  if (clearTimer) clearTimeout(clearTimer);
  clearTimer = undefined;
  loading.value = false;
  revealedValue.value = '';
  revealed.value = false;
  localVisible.value = false;
  if (!localValue.value) replacing.value = false;
}

watch(
  () => [props.channelConfigId, props.secretKey, props.resetToken],
  () => {
    clearPlaintext();
    replacing.value = false;
  },
);

onBeforeUnmount(clearPlaintext);
</script>

<style scoped>
.secret-action-disabled {
  display: inline-flex;
}
</style>
