<template>
  <div class="captcha-card behavior-captcha">
    <button
      class="verify-bar"
      :class="statusClass"
      type="button"
      :disabled="loading || verifyResult?.passed"
      @click="handleVerifyClick"
    >
      <span class="verify-icon" aria-hidden="true">
        <el-icon v-if="verifyResult?.passed"><Check /></el-icon>
        <el-icon v-else-if="loading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><Key /></el-icon>
      </span>
      <span class="verify-text">{{ statusText }}</span>
    </button>
    <div v-if="verifyResult && props.showScore" class="score-panel">
      <span>Score {{ verifyResult.score.toFixed(2) }}</span>
      <span>{{ verifyResult.riskLevel }} / {{ verifyResult.suggestAction }}</span>
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { Check, Key, Loading } from '@element-plus/icons-vue';
import {
  CaptchaType,
  generateBehavior,
  verifyBehaviorCaptcha,
  type BehaviorCaptchaVerifyResult,
  type CaptchaResponse,
} from '../../api/captcha';

interface TrackPoint {
  x: number;
  y: number;
  t: number;
}

interface KeyPoint {
  key: string;
  t: number;
}

const emit = defineEmits<{
  success: [key: string, code?: string];
  refresh: [];
}>();

const props = withDefaults(defineProps<{
  showScore?: boolean;
}>(), {
  showScore: false,
});

const captchaData = ref<CaptchaResponse | null>(null);
const verifyResult = ref<BehaviorCaptchaVerifyResult | null>(null);
const errorMessage = ref('');
const loading = ref(false);
const mouseTrack: TrackPoint[] = [];
const clickList: TrackPoint[] = [];
const keyList: KeyPoint[] = [];
let startTime = Date.now();

const statusClass = computed(() => {
  if (verifyResult.value?.passed) return 'is-success';
  if (verifyResult.value && !verifyResult.value.passed) return 'is-warning';
  return 'is-ready';
});

const statusText = computed(() => {
  if (loading.value) return '验证中...';
  if (verifyResult.value?.passed) return '验证成功';
  if (verifyResult.value && !verifyResult.value.passed) return '点击重新验证';
  return '点击完成验证';
});

function trimTrack(list: TrackPoint[], maxLength: number) {
  if (list.length > maxLength) {
    list.splice(0, list.length - maxLength);
  }
}

function handleMouseMove(event: MouseEvent) {
  mouseTrack.push({ x: event.clientX, y: event.clientY, t: Date.now() });
  trimTrack(mouseTrack, 300);
}

function handleClick(event: MouseEvent) {
  clickList.push({ x: event.clientX, y: event.clientY, t: Date.now() });
  trimTrack(clickList, 80);
}

function handleKeydown(event: KeyboardEvent) {
  keyList.push({ key: event.key, t: Date.now() });
  if (keyList.length > 120) {
    keyList.splice(0, keyList.length - 120);
  }
}

function getDeviceFinger() {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  if (!ctx) return '';
  ctx.textBaseline = 'top';
  ctx.font = '14px Arial';
  ctx.fillText('mango-behavior-captcha', 2, 2);
  return canvas.toDataURL();
}

function createPayload() {
  return {
    behavior: {
      mouseTrack,
      clickList,
      keyList,
      startTime,
    },
    device: {
      ua: navigator.userAgent,
      screen: `${screen.width}-${screen.height}`,
      pixelRatio: window.devicePixelRatio,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      language: navigator.language,
      hardwareConcurrency: navigator.hardwareConcurrency,
      finger: getDeviceFinger(),
    },
    ts: Date.now(),
  };
}

async function refresh() {
  errorMessage.value = '';
  verifyResult.value = null;
  loading.value = false;
  mouseTrack.splice(0);
  clickList.splice(0);
  keyList.splice(0);
  startTime = Date.now();
  captchaData.value = await generateBehavior();
  emit('refresh');
}

async function verify() {
  if (!captchaData.value?.key) {
    errorMessage.value = '验证会话已过期，请重新初始化';
    return false;
  }
  try {
    loading.value = true;
    errorMessage.value = '';
    verifyResult.value = await verifyBehaviorCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BEHAVIOR,
      pointJson: JSON.stringify(createPayload()),
    });
    if (verifyResult.value.passed) {
      emit('success', captchaData.value.key, JSON.stringify(verifyResult.value));
      return true;
    }
    errorMessage.value = `评分不足：${verifyResult.value.score.toFixed(2)}`;
    return false;
  } catch {
    errorMessage.value = '行为验证未通过';
    return false;
  } finally {
    loading.value = false;
  }
}

async function handleVerifyClick() {
  if (verifyResult.value && !verifyResult.value.passed) {
    await refresh();
  }
  await verify();
}

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove, { passive: true });
  window.addEventListener('click', handleClick, { passive: true });
  window.addEventListener('keydown', handleKeydown);
  void refresh();
});

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove);
  window.removeEventListener('click', handleClick);
  window.removeEventListener('keydown', handleKeydown);
});

defineExpose({ refresh, verify });
</script>

<style scoped lang="scss">
.behavior-captcha {
  .verify-bar {
    display: flex;
    width: 100%;
    min-height: 48px;
    align-items: center;
    justify-content: center;
    gap: 14px;
    padding: 0 16px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-fill-color-blank);
    color: #10183f;
    cursor: pointer;
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease,
      color 0.2s ease,
      box-shadow 0.2s ease;

    &:hover:not(:disabled) {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }

    &:focus-visible {
      outline: 2px solid var(--el-color-primary-light-5);
      outline-offset: 2px;
    }

    &:disabled {
      cursor: default;
    }
  }

  .verify-icon {
    display: inline-flex;
    width: 38px;
    height: 38px;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--el-fill-color-light);
    box-shadow: 0 4px 12px rgb(31 45 61 / 16%);
    color: #687083;
    font-size: 22px;
  }

  .verify-text {
    font-size: 18px;
    font-weight: 500;
    line-height: 1.3;
  }

  .is-loading {
    animation: behavior-rotate 0.9s linear infinite;
  }

  .is-success {
    border-color: #80c9bb;
    background: var(--el-color-success-light-9);
    color: #7abdaf;

    .verify-icon {
      background: rgb(255 255 255 / 86%);
      color: #7abdaf;
      box-shadow: none;
    }
  }

  .is-warning {
    border-color: var(--el-color-warning-light-5);
    background: var(--el-color-warning-light-9);

    .verify-icon {
      color: var(--el-color-warning);
    }
  }

  .score-panel {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-top: 10px;
    color: var(--el-text-color-regular);
    font-size: 13px;
  }

  .error-msg {
    margin-top: 8px;
    color: var(--el-color-danger);
    font-size: 12px;
  }
}

@keyframes behavior-rotate {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
