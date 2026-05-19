<template>
  <div class="captcha-card">
    <div class="captcha-header">
      <span>请依次点击：{{ promptText }}</span>
      <el-button link type="primary" @click="refresh">刷新</el-button>
    </div>
    <div ref="imageWrapRef" class="click-word-image" @click="handleImageClick">
      <img
        v-if="captchaData?.image"
        :src="captchaData.image"
        alt="点选文字验证码"
      >
      <div v-else class="captcha-placeholder">加载中...</div>
      <span
        v-for="(point, index) in clickPoints"
        :key="`${point.x}-${point.y}-${index}`"
        class="click-marker"
        :style="{ left: `${point.displayX}px`, top: `${point.displayY}px` }"
      >
        {{ index + 1 }}
      </span>
    </div>
    <div class="captcha-footer">
      <span>{{ clickPoints.length }}/{{ pointCount }} 已点击</span>
      <el-button link type="primary" :disabled="!clickPoints.length" @click="clearClicks">重选</el-button>
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { CaptchaType, generateClickWord, verifyCaptcha, type CaptchaResponse } from '../../api/captcha';

interface PublicExtra {
  width?: number;
  height?: number;
  pointCount?: number;
}

interface ClickPoint {
  x: number;
  y: number;
  displayX: number;
  displayY: number;
}

const emit = defineEmits<{
  success: [key: string];
  refresh: [];
}>();

const imageWrapRef = ref<HTMLElement | null>(null);
const captchaData = ref<CaptchaResponse | null>(null);
const clickPoints = ref<ClickPoint[]>([]);
const errorMessage = ref('');
const baseWidth = ref(320);
const baseHeight = ref(180);
const pointCount = ref(3);

const promptText = computed(() => captchaData.value?.target || '图片中的文字');

async function refresh() {
  captchaData.value = await generateClickWord();
  const extra = parseExtra(captchaData.value.extra);
  baseWidth.value = Number(extra.width || 320);
  baseHeight.value = Number(extra.height || 180);
  pointCount.value = Number(extra.pointCount || countTargetWords(captchaData.value.target) || 3);
  clickPoints.value = [];
  errorMessage.value = '';
  emit('refresh');
}

function parseExtra(extra?: string): PublicExtra {
  if (!extra) return {};
  try {
    return JSON.parse(extra) as PublicExtra;
  } catch {
    return {};
  }
}

function countTargetWords(target?: string) {
  return target?.split(',').filter(Boolean).length ?? 0;
}

function handleImageClick(event: MouseEvent) {
  if (!captchaData.value?.key || !imageWrapRef.value || clickPoints.value.length >= pointCount.value) {
    return;
  }
  const rect = imageWrapRef.value.getBoundingClientRect();
  const displayX = event.clientX - rect.left;
  const displayY = event.clientY - rect.top;
  const x = Math.round(displayX / rect.width * baseWidth.value);
  const y = Math.round(displayY / rect.height * baseHeight.value);
  clickPoints.value.push({ x, y, displayX, displayY });
  errorMessage.value = '';
  if (clickPoints.value.length === pointCount.value) {
    void verifyPoints();
  }
}

function clearClicks() {
  clickPoints.value = [];
  errorMessage.value = '';
}

async function verifyPoints() {
  if (!captchaData.value?.key) return false;
  if (clickPoints.value.length < pointCount.value) {
    errorMessage.value = '请先按提示完成点选';
    return false;
  }
  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.CLICK_WORD,
      pointJson: JSON.stringify({
        points: clickPoints.value.map(({ x, y }) => ({ x, y })),
      }),
    });
    if (result) {
      emit('success', captchaData.value.key);
      return true;
    }
  } catch {
    // ignored
  }
  errorMessage.value = '点击位置不正确，请重试';
  await refresh();
  return false;
}

onMounted(() => {
  void refresh();
});

defineExpose({ refresh, verify: verifyPoints });
</script>

<style scoped lang="scss">
.captcha-card {
  .captcha-header,
  .captcha-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
    color: var(--el-text-color-primary);
    font-size: 14px;
    line-height: 1.5;
  }

  .click-word-image {
    position: relative;
    width: 320px;
    max-width: 100%;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-light);
    cursor: crosshair;
    user-select: none;
  }

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .captcha-placeholder {
    display: flex;
    height: 100%;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-secondary);
  }

  .click-marker {
    position: absolute;
    z-index: 2;
    width: 24px;
    height: 24px;
    transform: translate(-50%, -50%);
    border: 2px solid #fff;
    border-radius: 50%;
    background: var(--el-color-primary);
    box-shadow: 0 4px 10px rgb(0 0 0 / 22%);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    line-height: 20px;
    text-align: center;
    pointer-events: none;
  }

  .captcha-footer {
    margin-top: 8px;
    margin-bottom: 0;
    color: var(--el-text-color-secondary);
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
