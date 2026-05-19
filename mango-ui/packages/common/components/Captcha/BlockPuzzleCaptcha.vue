<template>
  <div class="captcha-card">
    <div class="captcha-header">
      <span>拖动滑块完成拼图</span>
      <el-button link type="primary" @click="refresh">刷新</el-button>
    </div>
    <div ref="trackRef" class="track">
      <img
        v-if="captchaData?.backgroundImage"
        class="captcha-image"
        :src="captchaData.backgroundImage"
        alt="滑块验证码背景"
      >
      <div v-else class="captcha-placeholder">加载中...</div>
      <div
        class="slider"
        :style="{ left: `${sliderLeft}px`, top: `${targetTop}px` }"
        @mousedown="startDrag"
        @touchstart.prevent="startTouchDrag"
      >
        <img
          v-if="captchaData?.sliderImage"
          class="slider-image"
          :src="captchaData.sliderImage"
          alt="滑块拼图片"
        >
        <span v-else class="slider-fallback" />
      </div>
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { CaptchaType, generateBlockPuzzle, verifyCaptcha, type CaptchaResponse } from '../../api/captcha';

const emit = defineEmits<{
  success: [key: string];
  refresh: [];
}>();

const trackRef = ref<HTMLElement | null>(null);
const captchaData = ref<CaptchaResponse | null>(null);
const sliderLeft = ref(0);
const targetTop = ref(55);
const errorMessage = ref('');

let dragStartX = 0;
let dragging = false;

async function refresh() {
  captchaData.value = await generateBlockPuzzle();
  sliderLeft.value = 0;
  targetTop.value = Number(captchaData.value.y ?? 55);
  errorMessage.value = '';
  emit('refresh');
}

function startDrag(event: MouseEvent) {
  dragStartX = event.clientX - sliderLeft.value;
  dragging = true;
  document.addEventListener('mousemove', handleDrag);
  document.addEventListener('mouseup', finishDrag);
}

function startTouchDrag(event: TouchEvent) {
  dragStartX = event.touches[0].clientX - sliderLeft.value;
  dragging = true;
  document.addEventListener('touchmove', handleTouchDrag);
  document.addEventListener('touchend', finishTouchDrag);
}

function handleDrag(event: MouseEvent) {
  if (!dragging) return;
  updateSlider(event.clientX);
}

function handleTouchDrag(event: TouchEvent) {
  if (!dragging) return;
  updateSlider(event.touches[0].clientX);
}

function updateSlider(clientX: number) {
  const maxLeft = Math.max((trackRef.value?.offsetWidth ?? 280) - 50, 0);
  sliderLeft.value = Math.min(Math.max(clientX - dragStartX, 0), maxLeft);
}

async function verifyPosition() {
  if (!captchaData.value?.key) return;
  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BLOCK_PUZZLE,
      pointJson: JSON.stringify({ x: Math.round(sliderLeft.value), y: Math.round(targetTop.value) }),
    });
    if (result) {
      emit('success', captchaData.value.key);
      return;
    }
  } catch {
    // ignored
  }

  errorMessage.value = '校验失败，请重试';
  await refresh();
}

function cleanupEvents() {
  document.removeEventListener('mousemove', handleDrag);
  document.removeEventListener('mouseup', finishDrag);
  document.removeEventListener('touchmove', handleTouchDrag);
  document.removeEventListener('touchend', finishTouchDrag);
}

function finishDrag() {
  dragging = false;
  cleanupEvents();
  void verifyPosition();
}

function finishTouchDrag() {
  dragging = false;
  cleanupEvents();
  void verifyPosition();
}

onMounted(() => {
  void refresh();
});

onBeforeUnmount(() => {
  cleanupEvents();
});

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.captcha-card {
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .track {
    position: relative;
    width: 280px;
    max-width: 100%;
    height: 160px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-light);
    overflow: hidden;
  }

  .captcha-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .captcha-placeholder {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-secondary);
  }

  .slider {
    position: absolute;
    width: 50px;
    height: 50px;
    border-radius: 4px;
    background: rgb(255 255 255 / 82%);
    box-shadow: 0 6px 16px rgb(0 0 0 / 22%);
    cursor: grab;
    overflow: hidden;
    touch-action: none;
  }

  .slider:active {
    cursor: grabbing;
  }

  .slider-image {
    display: block;
    width: 100%;
    height: 100%;
  }

  .slider-fallback {
    display: block;
    width: 100%;
    height: 100%;
    background: var(--el-color-primary);
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
