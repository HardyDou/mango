<template>
  <div class="captcha-card behavior-captcha">
    <button
      class="verify-bar"
      :class="statusClass"
      type="button"
      :disabled="initializing || loading || verifyResult?.passed"
      @click="handleVerifyClick"
    >
      <span v-if="loading" class="verify-ripple" aria-hidden="true" />
      <span class="verify-icon" aria-hidden="true">
        <el-icon v-if="verifyResult?.passed"><Check /></el-icon>
        <svg
          v-else
          class="shield-icon"
          viewBox="0 0 24 24"
          fill="none"
          focusable="false"
        >
          <path
            d="M12 3.2 5.6 5.4v5.2c0 4.1 2.6 7.7 6.4 9.1 3.8-1.4 6.4-5 6.4-9.1V5.4L12 3.2Z"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linejoin="round"
          />
          <path
            d="M9 12.1 11.1 14 15.4 9.5"
            stroke="currentColor"
            stroke-width="2.2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
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
import { Check } from '@element-plus/icons-vue';
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
const initializing = ref(false);
const mouseTrack: TrackPoint[] = [];
const clickList: TrackPoint[] = [];
const keyList: KeyPoint[] = [];
let startTime = Date.now();
let failureResetTimer: ReturnType<typeof window.setTimeout> | null = null;

const statusClass = computed(() => {
  if (verifyResult.value?.passed) return 'is-success';
  if (verifyResult.value && !verifyResult.value.passed) return 'is-warning';
  return 'is-ready';
});

const statusText = computed(() => {
  if (initializing.value) return '初始化中...';
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
  clearFailureResetTimer();
  errorMessage.value = '';
  verifyResult.value = null;
  initializing.value = true;
  mouseTrack.splice(0);
  clickList.splice(0);
  keyList.splice(0);
  startTime = Date.now();
  try {
    captchaData.value = await generateBehavior();
    emit('refresh');
  } catch {
    captchaData.value = null;
    errorMessage.value = '行为验证初始化失败，请稍后重试';
  } finally {
    initializing.value = false;
  }
}

async function verify() {
  if (!captchaData.value?.key) {
    await refresh();
    if (!captchaData.value?.key) {
      errorMessage.value = '验证会话初始化失败，请重试';
      return false;
    }
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
    if (['CHALLENGE_NOT_FOUND', 'CHALLENGE_EXPIRED'].includes(verifyResult.value.reason)) {
      errorMessage.value = '验证会话已刷新，请重新验证';
      await refresh();
      return false;
    }
    errorMessage.value = `评分不足：${verifyResult.value.score.toFixed(2)}`;
    resetAfterFailure();
    return false;
  } catch {
    errorMessage.value = '行为验证未通过';
    resetAfterFailure();
    return false;
  } finally {
    loading.value = false;
  }
}

function resetAfterFailure() {
  clearFailureResetTimer();
  failureResetTimer = window.setTimeout(() => {
    void refresh();
  }, 900);
}

function clearFailureResetTimer() {
  if (failureResetTimer) {
    window.clearTimeout(failureResetTimer);
    failureResetTimer = null;
  }
}

async function handleVerifyClick() {
  if (initializing.value) return;
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
  clearFailureResetTimer();
  window.removeEventListener('mousemove', handleMouseMove);
  window.removeEventListener('click', handleClick);
  window.removeEventListener('keydown', handleKeydown);
});

defineExpose({ refresh, verify });
</script>

<style scoped lang="scss">
.behavior-captcha {
  .verify-bar {
    position: relative;
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
    overflow: hidden;
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease,
      color 0.2s ease,
      box-shadow 0.2s ease;

    &:hover:not(:disabled) {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);

      .verify-icon {
        color: var(--el-color-primary);
        transform: translateY(-1px);
      }
    }

    &:focus-visible {
      outline: 2px solid var(--el-color-primary-light-5);
      outline-offset: 2px;
    }

    &:disabled {
      cursor: default;
    }
  }

  .verify-ripple {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 38px;
    height: 38px;
    border-radius: 999px;
    background: color-mix(in srgb, var(--el-color-primary) 18%, transparent);
    opacity: 0;
    pointer-events: none;
    transform: translate(-50%, -50%) scaleX(1);
    animation: behavior-ripple 0.78s cubic-bezier(0.22, 1, 0.36, 1) infinite;
  }

  .verify-ripple::before,
  .verify-ripple::after {
    position: absolute;
    top: 0;
    width: 38px;
    height: 38px;
    border-radius: 999px;
    background: color-mix(in srgb, var(--el-color-primary) 16%, transparent);
    content: '';
  }

  .verify-ripple::before {
    right: 100%;
  }

  .verify-ripple::after {
    left: 100%;
  }

  .verify-icon {
    position: relative;
    z-index: 1;
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
    transition:
      color 0.2s ease,
      transform 0.2s ease,
      box-shadow 0.2s ease,
      background-color 0.2s ease;
  }

  .shield-icon {
    width: 23px;
    height: 23px;
  }

  .verify-text {
    position: relative;
    z-index: 1;
    font-size: 18px;
    font-weight: 500;
    line-height: 1.3;
    transition: color 0.2s ease;
  }

  .is-success {
    border-color: var(--el-color-primary);
    background:
      linear-gradient(
        90deg,
        color-mix(in srgb, var(--el-color-primary) 16%, transparent),
        color-mix(in srgb, var(--el-color-primary) 10%, transparent)
      ),
      var(--el-color-success-light-9);
    color: var(--el-color-primary);
    box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 16%, transparent);

    .verify-icon {
      background: rgb(255 255 255 / 86%);
      color: var(--el-color-primary);
      box-shadow: 0 0 0 5px color-mix(in srgb, var(--el-color-primary) 10%, transparent);
      animation: behavior-success-pop 0.32s cubic-bezier(0.22, 1, 0.36, 1);
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

@keyframes behavior-ripple {
  0% {
    opacity: 0.72;
    transform: translate(-50%, -50%) scaleX(0.45);
  }

  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scaleX(9);
  }
}

@keyframes behavior-success-pop {
  0% {
    transform: scale(0.86);
  }

  100% {
    transform: scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .behavior-captcha {
    .verify-ripple,
    .is-success .verify-icon {
      animation: none;
    }
  }
}
</style>
