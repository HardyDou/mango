<template>
  <div class="captcha-card behavior-captcha">
    <div class="captcha-header">
      <span>无感行为验证</span>
      <el-button link type="primary" @click="refresh">重新初始化</el-button>
    </div>
    <div class="behavior-status" :class="statusClass">
      <span class="status-dot" />
      <div>
        <strong>{{ statusText }}</strong>
        <p>{{ statusDesc }}</p>
      </div>
    </div>
    <div v-if="verifyResult" class="score-panel">
      <span>Score {{ verifyResult.score.toFixed(2) }}</span>
      <span>{{ verifyResult.riskLevel }} / {{ verifyResult.suggestAction }}</span>
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
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

const captchaData = ref<CaptchaResponse | null>(null);
const verifyResult = ref<BehaviorCaptchaVerifyResult | null>(null);
const errorMessage = ref('');
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
  if (verifyResult.value?.passed) return '验证已通过';
  if (verifyResult.value && !verifyResult.value.passed) return '需要二次验证';
  return '已开始静默采集';
});

const statusDesc = computed(() => {
  if (verifyResult.value?.passed) return '业务提交时可携带 captchaKey 和 score。';
  if (verifyResult.value && !verifyResult.value.passed) return '当前行为评分不足，业务可切换滑块或点选文字验证。';
  return '用户无需点击验证码，提交表单时自动完成行为评分。';
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
  }
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
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .behavior-status {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 12px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 4px;
    background: var(--el-fill-color-lighter);

    strong {
      display: block;
      margin-bottom: 4px;
      color: var(--el-text-color-primary);
      font-size: 14px;
      font-weight: 500;
    }

    p {
      margin: 0;
      color: var(--el-text-color-regular);
      font-size: 13px;
      line-height: 1.5;
    }
  }

  .status-dot {
    flex: 0 0 8px;
    width: 8px;
    height: 8px;
    margin-top: 6px;
    border-radius: 50%;
    background: var(--el-color-primary);
  }

  .is-success {
    border-color: var(--el-color-success-light-5);
    background: var(--el-color-success-light-9);

    .status-dot {
      background: var(--el-color-success);
    }
  }

  .is-warning {
    border-color: var(--el-color-warning-light-5);
    background: var(--el-color-warning-light-9);

    .status-dot {
      background: var(--el-color-warning);
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
</style>
