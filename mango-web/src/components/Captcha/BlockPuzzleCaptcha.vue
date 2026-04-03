<template>
  <div class="captcha-block-puzzle">
    <div class="captcha-header">
      <span class="captcha-title">拖动滑块完成拼图</span>
      <el-button
        link
        type="primary"
        @click="refresh"
      >
        刷新
      </el-button>
    </div>
    <div
      ref="puzzleRef"
      class="puzzle-container"
      @mousedown="handleMouseDown"
      @touchstart="handleTouchStart"
    >
      <img
        v-if="captchaData?.backgroundImage"
        :src="captchaData.backgroundImage"
        class="background-image"
        alt="背景图"
      >
      <img
        v-if="captchaData?.sliderImage"
        :src="captchaData.sliderImage"
        class="slider-image"
        :style="{ left: sliderLeft + 'px' }"
        alt="滑块"
      >
      <div
        class="target-block"
        :style="{ left: targetLeft + 'px' }"
      />
      <div
        v-if="isDragging"
        class="slider-bar"
        :style="{ width: sliderLeft + 10 + 'px' }"
      />
    </div>
    <div
      v-if="errorMessage"
      class="error-msg"
    >
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts" name="BlockPuzzleCaptcha">
import { ref, onMounted, computed } from 'vue';
import { generateBlockPuzzle, verifyCaptcha, type CaptchaResponse, CaptchaType } from '@/api/admin/captcha';

const emit = defineEmits<{
  success: [key: string];
  refresh: [];
}>();

const captchaData = ref<CaptchaResponse | null>(null);
const sliderLeft = ref(0);
const targetLeft = ref(0);
const isDragging = ref(false);
const errorMessage = ref('');
const puzzleRef = ref<HTMLElement | null>(null);

let startX = 0;
let maxLeft = 0;

const refresh = async () => {
  errorMessage.value = '';
  sliderLeft.value = 0;
  captchaData.value = await generateBlockPuzzle();
  emit('refresh');
};

const handleMouseDown = (e: MouseEvent) => {
  if (!captchaData.value?.key) return;
  isDragging.value = true;
  startX = e.clientX;
  maxLeft = puzzleRef.value ? puzzleRef.value.offsetWidth - 50 : 200;

  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
};

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return;
  const diff = e.clientX - startX;
  sliderLeft.value = Math.min(Math.max(0, diff), maxLeft);
};

const handleMouseUp = async () => {
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
  isDragging.value = false;

  if (!captchaData.value?.key) return;

  const pointJson = JSON.stringify({
    x: sliderLeft.value,
    y: 5,
  });

  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BLOCK_PUZZLE,
      pointJson,
    });

    if (result) {
      emit('success', captchaData.value.key);
    } else {
      errorMessage.value = '校验失败，请重试';
      await refresh();
    }
  } catch {
    errorMessage.value = '校验失败，请重试';
    await refresh();
  }
};

const handleTouchStart = (e: TouchEvent) => {
  if (!captchaData.value?.key) return;
  isDragging.value = true;
  startX = e.touches[0].clientX;
  maxLeft = puzzleRef.value ? puzzleRef.value.offsetWidth - 50 : 200;

  document.addEventListener('touchmove', handleTouchMove);
  document.addEventListener('touchend', handleTouchEnd);
};

const handleTouchMove = (e: TouchEvent) => {
  if (!isDragging.value) return;
  const diff = e.touches[0].clientX - startX;
  sliderLeft.value = Math.min(Math.max(0, diff), maxLeft);
};

const handleTouchEnd = async () => {
  document.removeEventListener('touchmove', handleTouchMove);
  document.removeEventListener('touchend', handleTouchEnd);
  isDragging.value = false;

  if (!captchaData.value?.key) return;

  const pointJson = JSON.stringify({
    x: sliderLeft.value,
    y: 5,
  });

  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BLOCK_PUZZLE,
      pointJson,
    });

    if (result) {
      emit('success', captchaData.value.key);
    } else {
      errorMessage.value = '校验失败，请重试';
      await refresh();
    }
  } catch {
    errorMessage.value = '校验失败，请重试';
    await refresh();
  }
};

onMounted(() => {
  refresh();
});

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.captcha-block-puzzle {
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .captcha-title {
      font-size: 14px;
      color: #666;
    }
  }

  .puzzle-container {
    position: relative;
    width: 100%;
    height: 150px;
    background: #f5f5f5;
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;
    user-select: none;

    .background-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .slider-image {
      position: absolute;
      top: 0;
      width: 50px;
      height: 100%;
      object-fit: cover;
    }

    .target-block {
      position: absolute;
      top: 0;
      width: 50px;
      height: 100%;
      background: rgba(0, 0, 0, 0.3);
      border-radius: 4px;
    }

    .slider-bar {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 4px;
      background: #409eff;
      border-radius: 2px;
      transition: none;
    }
  }

  .error-msg {
    margin-top: 8px;
    font-size: 12px;
    color: #f56c6c;
  }
}
</style>
