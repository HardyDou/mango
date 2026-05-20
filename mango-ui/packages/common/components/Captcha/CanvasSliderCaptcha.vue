<template>
  <div class="captcha-card canvas-slider-captcha" :class="`is-${mode}`">
    <template v-if="mode !== 'trigger'">
      <div class="captcha-header">
        <span>拖动滑块完成验证</span>
        <el-button link type="primary" @click="refresh">刷新</el-button>
      </div>
      <SliderContent />
    </template>

    <template v-else-if="mode === 'trigger'">
      <el-popover
        v-model:visible="panelVisible"
        trigger="click"
        placement="bottom-start"
        :width="320"
        :teleported="false"
        :disabled="verified"
        @show="refresh"
      >
        <template #reference>
          <button class="verify-bar" :class="{ 'is-success': verified }" type="button" :disabled="verified">
            <span class="verify-handle">
              <el-icon v-if="verified"><Check /></el-icon>
              <el-icon v-else><Right /></el-icon>
            </span>
            <span>{{ verified ? '验证通过' : '向右拖动滑块完成验证' }}</span>
          </button>
        </template>
        <div class="captcha-header">
          <span>拖动滑块完成验证</span>
          <el-button link type="primary" @click="refresh">刷新</el-button>
        </div>
        <SliderContent />
      </el-popover>
    </template>

  </div>
</template>

<script setup lang="ts">
import { Check, DArrowRight, Right } from '@element-plus/icons-vue';
import { computed, defineComponent, h, onBeforeUnmount, onMounted, ref } from 'vue';

type CaptchaDisplayMode = 'embedded' | 'trigger' | 'popup';

const props = withDefaults(defineProps<{
  mode?: CaptchaDisplayMode;
}>(), {
  mode: 'embedded',
});

const emit = defineEmits<{
  success: [key: string];
  refresh: [];
}>();

const mode = computed(() => props.mode);
const sliderLeft = ref(0);
const targetLeft = ref(120);
const errorMessage = ref('');
const key = ref('');
const verified = ref(false);
const panelVisible = ref(false);

let dragStartX = 0;
let dragging = false;
let verifyResolver: ((passed: boolean) => void) | null = null;
let verifyPromise: Promise<boolean> | null = null;

function buildKey() {
  return `canvas_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

function refresh() {
  key.value = buildKey();
  sliderLeft.value = 0;
  targetLeft.value = Math.floor(80 + Math.random() * 120);
  errorMessage.value = '';
  verified.value = false;
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
    verified.value = true;
    panelVisible.value = false;
    emit('success', key.value);
    resolveVerify(true);
    return;
  }
  errorMessage.value = '位置不正确，请重试';
  refresh();
}

async function verify() {
  if (verified.value) return true;
  if (mode.value !== 'popup') return false;
  if (verifyPromise) return verifyPromise;
  verifyPromise = new Promise<boolean>((resolve) => {
    verifyResolver = resolve;
  });
  return verifyPromise;
}

function resolveVerify(passed: boolean) {
  verifyResolver?.(passed);
  verifyResolver = null;
  verifyPromise = null;
}

function handlePopupClosed() {
  if (!verified.value) {
    resolveVerify(false);
  }
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
  resolveVerify(false);
});

const SliderContent = defineComponent({
  name: 'CanvasSliderContent',
  setup() {
    return () => [
      h('div', { class: 'track' }, [
        h('div', { class: 'target', style: { left: `${targetLeft.value}px` } }),
        h('div', {
          class: 'slider',
          style: { left: `${sliderLeft.value}px` },
          onMousedown: startDrag,
          onTouchstart: (event: TouchEvent) => {
            event.preventDefault();
            startTouchDrag(event);
          },
        }, [
          h(DArrowRight),
        ]),
      ]),
      errorMessage.value ? h('div', { class: 'error-msg' }, errorMessage.value) : null,
    ];
  },
});

defineExpose({ refresh, verify });
</script>

<style scoped lang="scss">
.canvas-slider-captcha {
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  :deep(.track) {
    position: relative;
    height: 38px;
    border-radius: 19px;
    background: linear-gradient(90deg, #eef5ff 0%, #f6faff 100%);
    overflow: hidden;
  }

  :deep(.target) {
    position: absolute;
    top: 4px;
    width: 30px;
    height: 30px;
    border-radius: 15px;
    background: rgba(103, 194, 58, 0.15);
    border: 1px dashed #67c23a;
  }

  :deep(.slider) {
    position: absolute;
    top: 4px;
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 15px;
    background: #409eff;
    color: #fff;
    cursor: grab;
    font-size: 15px;
    box-shadow: 0 2px 6px rgb(64 158 255 / 20%);
  }

  :deep(.slider svg) {
    width: 15px;
    height: 15px;
  }

  :deep(.slider:active) {
    cursor: grabbing;
  }

  :deep(.error-msg) {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }

  .verify-bar {
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
    color: var(--el-text-color-primary);
    cursor: pointer;
    font-size: 16px;

    &:hover {
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }

    &:disabled {
      cursor: default;
    }

    &.is-success {
      border-color: var(--el-color-success-light-5);
      background: var(--el-color-success-light-9);
      color: var(--el-color-success);
      cursor: default;
    }
  }

  .verify-handle {
    display: inline-flex;
    width: 40px;
    height: 40px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-bg-color);
    box-shadow: 0 2px 8px rgb(31 45 61 / 12%);
    color: var(--el-text-color-regular);
    font-size: 22px;
  }
}

.is-trigger,
.is-popup {
  width: 100%;
  max-width: 420px;
}
</style>
