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
      <div class="target" :style="{ left: `${targetLeft}px` }" />
      <div
        class="slider"
        :style="{ left: `${sliderLeft}px` }"
        @mousedown="startDrag"
        @touchstart.prevent="startTouchDrag"
      />
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
const targetLeft = ref(0);
const errorMessage = ref('');

let dragStartX = 0;
let dragging = false;

async function refresh() {
  captchaData.value = await generateBlockPuzzle();
  sliderLeft.value = 0;
  targetLeft.value = Number(captchaData.value.x ?? 0);
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
  const maxLeft = Math.max((trackRef.value?.offsetWidth ?? 280) - 40, 0);
  sliderLeft.value = Math.min(Math.max(clientX - dragStartX, 0), maxLeft);
}

async function verifyPosition() {
  if (!captchaData.value?.key) return;
  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BLOCK_PUZZLE,
      pointJson: JSON.stringify({ x: sliderLeft.value, y: 0 }),
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

  .target {
    position: absolute;
    top: 55px;
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: rgb(255 255 255 / 30%);
    border: 2px dashed var(--el-color-primary);
    box-shadow: 0 0 0 999px rgb(0 0 0 / 8%);
  }

  .slider {
    position: absolute;
    top: 55px;
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: var(--el-color-primary);
    box-shadow: 0 4px 12px rgb(0 0 0 / 16%);
    cursor: grab;
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
