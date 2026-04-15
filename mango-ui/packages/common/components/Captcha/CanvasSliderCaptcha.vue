<template>
  <div class="captcha-card">
    <div class="captcha-header">
      <span>拖动滑块完成验证</span>
      <el-button link type="primary" @click="refresh">刷新</el-button>
    </div>
    <div class="track">
      <div class="target" :style="{ left: `${targetLeft}px` }" />
      <div
        class="slider"
        :style="{ left: `${sliderLeft}px` }"
        @mousedown="startDrag"
        @touchstart.prevent="startTouchDrag"
      >
        <el-icon><DArrowRight /></el-icon>
      </div>
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { DArrowRight } from '@element-plus/icons-vue';
import { onBeforeUnmount, onMounted, ref } from 'vue';

const emit = defineEmits<{
  success: [key: string];
  refresh: [];
}>();

const sliderLeft = ref(0);
const targetLeft = ref(120);
const errorMessage = ref('');
const key = ref('');

let dragStartX = 0;
let dragging = false;

function buildKey() {
  return `canvas_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function refresh() {
  key.value = buildKey();
  sliderLeft.value = 0;
  targetLeft.value = Math.floor(80 + Math.random() * 120);
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
  sliderLeft.value = Math.min(Math.max(event.clientX - dragStartX, 0), 220);
}

function handleTouchDrag(event: TouchEvent) {
  if (!dragging) return;
  sliderLeft.value = Math.min(Math.max(event.touches[0].clientX - dragStartX, 0), 220);
}

function verifyCurrent() {
  dragging = false;
  cleanupEvents();
  if (Math.abs(sliderLeft.value - targetLeft.value) <= 8) {
    emit('success', key.value);
    return;
  }
  errorMessage.value = '位置不正确，请重试';
  refresh();
}

function finishDrag() {
  verifyCurrent();
}

function finishTouchDrag() {
  verifyCurrent();
}

function cleanupEvents() {
  document.removeEventListener('mousemove', handleDrag);
  document.removeEventListener('mouseup', finishDrag);
  document.removeEventListener('touchmove', handleTouchDrag);
  document.removeEventListener('touchend', finishTouchDrag);
}

onMounted(() => {
  refresh();
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
    background: linear-gradient(90deg, #eef5ff 0%, #f6faff 100%);
    overflow: hidden;
  }

  .target {
    position: absolute;
    top: 4px;
    width: 40px;
    height: 40px;
    border-radius: 20px;
    background: rgba(103, 194, 58, 0.15);
    border: 1px dashed #67c23a;
  }

  .slider {
    position: absolute;
    top: 4px;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 20px;
    background: #409eff;
    color: #fff;
    cursor: grab;
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
