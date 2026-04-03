<template>
  <div class="captcha-canvas-slider">
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
      ref="containerRef"
      class="puzzle-container"
      @mousedown="handleMouseDown"
      @touchstart="handleTouchStart"
    >
      <canvas
        ref="canvasRef"
        class="puzzle-canvas"
        width="320"
        height="160"
      />
      <div
        class="slider-handle"
        :style="{ left: sliderLeft + 'px' }"
      >
        <el-icon><DArrowRight /></el-icon>
      </div>
      <div
        v-if="showTip"
        class="tip-icon"
      >
        <el-icon><WarningFilled /></el-icon>
      </div>
    </div>
    <div
      v-if="errorMessage"
      class="error-msg"
    >
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts" name="CanvasSliderCaptcha">
import { ref, onMounted } from 'vue';
import { DArrowRight, WarningFilled } from '@element-plus/icons-vue';

const emit = defineEmits<{
  success: [key: string];
  refresh: [];
}>();

// 验证码 key
const key = ref('');
const containerRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const sliderLeft = ref(0);
const isDragging = ref(false);
const errorMessage = ref('');
const showTip = ref(false);

let startX = 0;
// 缺口位置（目标位置）
let targetX = 0;
// 允许的偏差范围
const TOLERANCE = 5;
// 滑块宽度
const SLIDER_WIDTH = 40;

onMounted(() => {
  refresh();
});

/**
 * 刷新验证码 - 重新生成缺口位置
 */
const refresh = async () => {
  errorMessage.value = '';
  sliderLeft.value = 0;
  showTip.value = false;
  key.value = generateKey();
  drawCanvas();
  emit('refresh');
};

/**
 * 生成唯一 key
 */
const generateKey = (): string => {
  return `canvas_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
};

/**
 * 绘制 Canvas 拼图
 */
const drawCanvas = () => {
  const canvas = canvasRef.value;
  if (!canvas) return;

  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  // 画布尺寸
  const width = canvas.width;
  const height = canvas.height;

  // 随机生成缺口位置 (30% - 70% 范围内)
  targetX = Math.floor(width * (0.3 + Math.random() * 0.4));

  // 清空画布
  ctx.clearRect(0, 0, width, height);

  // 绘制背景渐变
  const gradient = ctx.createLinearGradient(0, 0, width, height);
  gradient.addColorStop(0, '#667eea');
  gradient.addColorStop(1, '#764ba2');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);

  // 绘制装饰图案
  drawDecorations(ctx, width, height);

  // "挖"出缺口（用背景色绘制一个凹槽）
  ctx.save();
  ctx.globalCompositeOperation = 'destination-out';
  drawPuzzlePiece(ctx, targetX, 20, SLIDER_WIDTH, height - 40, true);
  ctx.restore();

  // 在缺口位置绘制提示（半透明）
  ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
  ctx.fillRect(targetX, 20, SLIDER_WIDTH, height - 40);
};

/**
 * 绘制装饰图案
 */
const drawDecorations = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
  ctx.fillStyle = 'rgba(255, 255, 255, 0.1)';

  // 圆圈
  for (let i = 0; i < 5; i++) {
    const x = Math.random() * width;
    const y = Math.random() * height;
    const r = 20 + Math.random() * 30;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();
  }

  // 方块
  for (let i = 0; i < 3; i++) {
    const x = Math.random() * width;
    const y = Math.random() * height;
    const size = 30 + Math.random() * 40;
    ctx.fillRect(x, y, size, size);
  }
};

/**
 * 绘制拼图块形状
 */
const drawPuzzlePiece = (
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  isHole: boolean
) => {
  const radius = 4;
  const tabWidth = w * 0.4;
  const tabHeight = h * 0.2;

  ctx.beginPath();
  ctx.moveTo(x + radius, y);

  if (isHole) {
    // 上边 - 向下凸起
    ctx.lineTo(x + w * 0.3, y);
    ctx.arcTo(x + w * 0.4, y, x + w * 0.4, y + tabHeight, radius);
    ctx.arcTo(x + w * 0.4, y + tabHeight * 2, x + w * 0.5, y + tabHeight * 2, radius);
    ctx.lineTo(x + w - radius, y);
  } else {
    ctx.lineTo(x + w - radius, y);
  }

  ctx.arcTo(x + w, y, x + w, y + radius, radius);
  ctx.lineTo(x + w, y + h - radius);

  if (isHole) {
    // 右边 - 向左凹陷
    ctx.lineTo(x + w, y + h * 0.5);
    ctx.arcTo(x + w, y + h * 0.5 - tabHeight, x + w - tabWidth, y + h * 0.5 - tabHeight, radius);
    ctx.arcTo(x + w - tabWidth * 2, y + h * 0.5, x + w - tabWidth, y + h * 0.5 + tabHeight, radius);
    ctx.lineTo(x + w, y + h - radius);
  } else {
    ctx.arcTo(x + w, y + h, x + w - radius, y + h, radius);
    ctx.lineTo(x + radius, y + h);
  }

  ctx.arcTo(x, y + h, x, y + h - radius, radius);

  if (isHole) {
    // 下边 - 向上凹陷
    ctx.lineTo(x, y + h * 0.5);
    ctx.arcTo(x, y + h * 0.5 + tabHeight, x + tabWidth, y + h * 0.5 + tabHeight, radius);
    ctx.arcTo(x + tabWidth * 2, y + h * 0.5, x + tabWidth, y + h * 0.5 - tabHeight, radius);
    ctx.lineTo(x, y + radius);
  } else {
    ctx.arcTo(x, y, x + radius, y, radius);
  }

  ctx.closePath();
  ctx.fill();
};

/**
 * 鼠标按下开始拖动
 */
const handleMouseDown = (e: MouseEvent) => {
  if (!key.value) return;
  isDragging.value = true;
  startX = e.clientX;
  showTip.value = false;

  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
};

/**
 * 鼠标移动
 */
const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return;
  const diff = e.clientX - startX;
  const maxLeft = (containerRef.value?.offsetWidth || 320) - SLIDER_WIDTH - 10;
  sliderLeft.value = Math.min(Math.max(0, diff), maxLeft);
};

/**
 * 鼠标释放 - 完成验证
 */
const handleMouseUp = async () => {
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
  isDragging.value = false;

  if (!key.value) return;
  await verify();
};

/**
 * 触摸开始
 */
const handleTouchStart = (e: TouchEvent) => {
  if (!key.value) return;
  isDragging.value = true;
  startX = e.touches[0].clientX;
  showTip.value = false;

  document.addEventListener('touchmove', handleTouchMove);
  document.addEventListener('touchend', handleTouchEnd);
};

/**
 * 触摸移动
 */
const handleTouchMove = (e: TouchEvent) => {
  if (!isDragging.value) return;
  const diff = e.touches[0].clientX - startX;
  const maxLeft = (containerRef.value?.offsetWidth || 320) - SLIDER_WIDTH - 10;
  sliderLeft.value = Math.min(Math.max(0, diff), maxLeft);
};

/**
 * 触摸结束
 */
const handleTouchEnd = async () => {
  document.removeEventListener('touchmove', handleTouchMove);
  document.removeEventListener('touchend', handleTouchEnd);
  isDragging.value = false;

  if (!key.value) return;
  await verify();
};

/**
 * 验证滑块位置
 */
const verify = async () => {
  // 计算偏差
  const diff = Math.abs(sliderLeft.value - targetX);

  if (diff <= TOLERANCE) {
    // 验证成功
    emit('success', key.value);
  } else if (diff <= TOLERANCE * 3) {
    // 偏差稍大，提示用户
    errorMessage.value = '位置偏差较大，请重试';
    showTip.value = true;
    await refresh();
  } else {
    // 偏差太大
    errorMessage.value = '验证失败，请重试';
    showTip.value = true;
    await refresh();
  }
};

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.captcha-canvas-slider {
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
    width: 320px;
    height: 160px;
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;
    user-select: none;

    .puzzle-canvas {
      display: block;
    }

    .slider-handle {
      position: absolute;
      top: 0;
      width: 40px;
      height: 100%;
      background: rgba(255, 255, 255, 0.9);
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: grab;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
      transition: box-shadow 0.2s;

      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      }

      &:active {
        cursor: grabbing;
      }

      .el-icon {
        font-size: 20px;
        color: #667eea;
      }
    }

    .tip-icon {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      font-size: 40px;
      color: rgba(255, 255, 255, 0.8);
      animation: shake 0.5s ease-in-out;
    }
  }

  .error-msg {
    margin-top: 8px;
    font-size: 12px;
    color: #f56c6c;
  }
}

@keyframes shake {
  0%, 100% { transform: translate(-50%, -50%) rotate(0deg); }
  25% { transform: translate(-50%, -50%) rotate(-10deg); }
  75% { transform: translate(-50%, -50%) rotate(10deg); }
}
</style>
