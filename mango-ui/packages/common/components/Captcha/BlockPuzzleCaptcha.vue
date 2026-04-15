<template>
  <div class="captcha-card">
    <div class="captcha-header">
      <span>拖动滑块完成拼图</span>
      <el-button link type="primary" @click="refresh">刷新</el-button>
    </div>
    <div ref="trackRef" class="track">
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
  targetLeft.value = Math.floor(60 + Math.random() * 140);
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
  const maxLeft = Math.max((trackRef.value?.offsetWidth ?? 260) - 40, 0);
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
    height: 48px;
    border-radius: 24px;
    background: #f5f7fa;
    overflow: hidden;
  }

  .target {
    position: absolute;
    top: 4px;
    width: 40px;
    height: 40px;
    border-radius: 20px;
    background: rgba(64, 158, 255, 0.15);
    border: 1px dashed #409eff;
  }

  .slider {
    position: absolute;
    top: 4px;
    width: 40px;
    height: 40px;
    border-radius: 20px;
    background: #409eff;
    cursor: grab;
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
