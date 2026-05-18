<template>
  <div class="sign-container">
    <div
      class="sign-canvas-wrapper"
      :class="{ 'is-disabled': disabled, 'is-empty': isEmpty }"
    >
      <canvas
        ref="canvasRef"
        :width="width"
        :height="height"
        @mousedown="handleStart"
        @mousemove="handleMove"
        @mouseup="handleEnd"
        @mouseleave="handleEnd"
        @touchstart.prevent="handleTouchStart"
        @touchmove.prevent="handleTouchMove"
        @touchend="handleEnd"
      />
      <div
        v-if="isEmpty && !disabled"
        class="sign-placeholder"
      >
        {{ placeholder }}
      </div>
      <div
        v-if="isEmpty && disabled"
        class="sign-placeholder"
      >
        {{ placeholder }}
      </div>
    </div>

    <div class="sign-toolbar">
      <div class="sign-colors">
        <span class="sign-color-label">{{ t('sign.color') }}:</span>
        <span
          v-for="color in colors"
          :key="color.value"
          class="sign-color-dot"
          :class="{ 'is-active': strokeColor === color.value }"
          :style="{ backgroundColor: color.value }"
          @click="selectColor(color.value)"
        />
      </div>
      <div class="sign-actions">
        <el-button
          size="small"
          :disabled="isEmpty || disabled"
          @click="handleClear"
        >
          {{ t('sign.clear') }}
        </el-button>
      </div>
    </div>

    <div
      v-if="hasError"
      class="sign-error"
    >
      <el-icon class="el-icon--left">
        <WarnTriangleFilled />
      </el-icon>
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { WarnTriangleFilled } from '@element-plus/icons-vue';
import type { SignProps, SignEmits, SignExpose } from './types';

const props = withDefaults(
  defineProps<SignProps>(),
  {
    modelValue: '',
    width: 400,
    height: 200,
    strokeColor: '#000000',
    lineWidth: 2,
    disabled: false,
    placeholder: 'sign.placeholder',
  }
);

const emit = defineEmits<SignEmits>();

const { t } = useI18n();

const canvasRef = ref<HTMLCanvasElement | null>(null);
const hasDrawn = ref(false);
const hasError = ref(false);
const errorMessage = ref('');
let ctx: CanvasRenderingContext2D | null = null;
let isDrawing = false;
let lastX = 0;
let lastY = 0;
let imageLoadTimestamp = 0; // Track image load order to ignore stale callbacks
const currentStrokeColor = ref(props.strokeColor);

const colors = [
  { label: 'Black', value: '#000000' },
  { label: 'Red', value: '#FF0000' },
  { label: 'Blue', value: '#0000FF' },
  { label: 'Green', value: '#00FF00' },
];

const isEmpty = computed(() => !hasDrawn.value);

function initCanvas() {
  const canvas = canvasRef.value;
  if (!canvas) return;

  ctx = canvas.getContext('2d');
  if (!ctx) return;

  ctx.strokeStyle = currentStrokeColor.value;
  ctx.lineWidth = props.lineWidth;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

  // Load existing signature if provided
  if (props.modelValue) {
    const img = new Image();
    img.onload = () => {
      ctx?.drawImage(img, 0, 0);
      hasDrawn.value = true;
    };
    img.src = props.modelValue;
  }
}

function getPosition(e: MouseEvent | TouchEvent): { x: number; y: number } {
  const canvas = canvasRef.value;
  if (!canvas) return { x: 0, y: 0 };

  const rect = canvas.getBoundingClientRect();
  const scaleX = canvas.width / rect.width;
  const scaleY = canvas.height / rect.height;

  if ('touches' in e && e.touches.length > 0) {
    return {
      x: (e.touches[0].clientX - rect.left) * scaleX,
      y: (e.touches[0].clientY - rect.top) * scaleY,
    };
  }
  return {
    x: (e.clientX - rect.left) * scaleX,
    y: (e.clientY - rect.top) * scaleY,
  };
}

function handleStart(e: MouseEvent | TouchEvent) {
  if (props.disabled) return;
  isDrawing = true;
  const pos = getPosition(e);
  lastX = pos.x;
  lastY = pos.y;
}

function handleMove(e: MouseEvent | TouchEvent) {
  if (!isDrawing || props.disabled || !ctx) return;

  const pos = getPosition(e);

  ctx.beginPath();
  ctx.moveTo(lastX, lastY);
  ctx.lineTo(pos.x, pos.y);
  ctx.stroke();

  lastX = pos.x;
  lastY = pos.y;
  hasDrawn.value = true;
}

function handleEnd() {
  if (!isDrawing) return;
  isDrawing = false;

  if (hasDrawn.value) {
    generateSignature();
  }
}

function handleTouchStart(e: TouchEvent) {
  handleStart(e);
}

function handleTouchMove(e: TouchEvent) {
  handleMove(e);
}

function handleClear() {
  const canvas = canvasRef.value;
  if (!canvas || !ctx) return;

  ctx.clearRect(0, 0, canvas.width, canvas.height);
  hasDrawn.value = false;
  hasError.value = false;
  errorMessage.value = '';
  emit('update:modelValue', '');
  emit('change', '');
}

function selectColor(color: string) {
  if (props.disabled) return;
  currentStrokeColor.value = color;
  if (ctx) {
    ctx.strokeStyle = color;
  }
}

function generateSignature() {
  const canvas = canvasRef.value;
  if (!canvas) return;

  try {
    const dataUrl = canvas.toDataURL('image/png');
    hasError.value = false;
    errorMessage.value = '';
    emit('update:modelValue', dataUrl);
    emit('change', dataUrl);
  } catch (error) {
    hasError.value = true;
    errorMessage.value = t('sign.error');
    console.error('Failed to generate signature:', error);
  }
}

function clear() {
  handleClear();
}

function getSignature(): string {
  const canvas = canvasRef.value;
  if (!canvas) return '';
  return canvas.toDataURL('image/png');
}

function isEmptyCanvas(): boolean {
  return isEmpty.value;
}

// Watch for external modelValue changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue && newValue !== getSignature()) {
      const canvas = canvasRef.value;
      if (!canvas || !ctx) return;

      const currentTimestamp = Date.now();
      imageLoadTimestamp = currentTimestamp;
      const img = new Image();
      img.onload = () => {
        // Ignore stale callbacks from previous loads
        if (imageLoadTimestamp !== currentTimestamp) return;
        ctx?.clearRect(0, 0, canvas.width, canvas.height);
        ctx?.drawImage(img, 0, 0);
        hasDrawn.value = true;
      };
      img.src = newValue;
    }
  }
);

// Watch for disabled state changes
watch(
  () => props.disabled,
  (disabled) => {
    if (disabled && ctx) {
      isDrawing = false;
    }
  }
);

onMounted(() => {
  initCanvas();
});

// Expose methods
defineExpose<SignExpose>({
  clear,
  getSignature,
  isEmpty: isEmptyCanvas,
});
</script>

<style scoped lang="scss">
.sign-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sign-canvas-wrapper {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  overflow: hidden;

  &.is-disabled {
    background-color: #f5f7fa;
    cursor: not-allowed;
  }

  &.is-empty {
    border-style: dashed;
  }

  canvas {
    display: block;
    cursor: crosshair;

    .is-disabled & {
      cursor: not-allowed;
    }
  }
}

.sign-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 14px;
  pointer-events: none;
  user-select: none;
}

.sign-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.sign-colors {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sign-color-label {
  font-size: 12px;
  color: #606266;
}

.sign-color-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    transform: scale(1.1);
  }

  &.is-active {
    border-color: #409eff;
    box-shadow: 0 0 4px rgba(64, 158, 255, 0.5);
  }
}

.sign-actions {
  display: flex;
  gap: 8px;
}

.sign-error {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f56c6c;
  font-size: 12px;
}
</style>
