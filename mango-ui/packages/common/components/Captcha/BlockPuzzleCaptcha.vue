<template>
  <div class="captcha-card block-puzzle-captcha" :class="`is-${mode}`">
    <template v-if="mode === 'embedded'">
      <div class="captcha-header">
        <span>拖动滑块完成拼图</span>
        <el-button link type="primary" @click="refresh">刷新</el-button>
      </div>
      <div class="puzzle-panel">
        <PuzzleContent />
      </div>
    </template>

    <template v-else-if="mode === 'trigger'">
      <el-popover
        v-model:visible="panelVisible"
        trigger="click"
        placement="bottom-start"
        :width="panelWidth"
        :teleported="false"
        :disabled="verified"
        popper-class="captcha-popper"
        @show="handlePanelShow"
      >
        <template #reference>
          <button class="verify-bar" :class="{ 'is-success': verified }" type="button" :disabled="verified">
            <span class="verify-handle">
              <el-icon v-if="verified"><Check /></el-icon>
              <el-icon v-else><Right /></el-icon>
            </span>
            <span>{{ verified ? '验证通过' : '向右拖动滑块填充拼图' }}</span>
          </button>
        </template>
        <div class="puzzle-panel">
          <div class="captcha-header">
            <span>拖动滑块完成拼图</span>
            <el-button link type="primary" @click="refresh">刷新</el-button>
          </div>
          <PuzzleContent />
        </div>
      </el-popover>
    </template>

    <template v-else>
      <button class="verify-bar" :class="{ 'is-success': verified }" type="button" @click="openPopup">
        <span class="verify-handle">
          <el-icon v-if="verified"><Check /></el-icon>
          <el-icon v-else><Right /></el-icon>
        </span>
        <span>{{ verified ? '验证通过' : '点击完成滑块验证' }}</span>
      </button>
      <el-dialog
        v-model="panelVisible"
        title="安全验证"
        width="360px"
        append-to-body
        @opened="handlePanelShow"
      >
        <div class="puzzle-panel">
          <PuzzleContent />
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { Check, Right } from '@element-plus/icons-vue';
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { CaptchaType, generateBlockPuzzle, verifyCaptcha, type CaptchaResponse } from '../../api/captcha';

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
const trackRef = ref<HTMLElement | null>(null);
const captchaData = ref<CaptchaResponse | null>(null);
const sliderLeft = ref(0);
const trackWidth = ref(280);
const targetLeft = ref(0);
const targetTop = ref(55);
const errorMessage = ref('');
const verified = ref(false);
const panelVisible = ref(false);

let dragStartX = 0;
let dragging = false;
let resizeObserver: ResizeObserver | null = null;

const baseWidth = 280;
const baseHeight = 160;
const sliderSize = 50;
const panelWidth = 320;

const displayScale = computed(() => trackWidth.value / baseWidth);
const displaySliderSize = computed(() => sliderSize * displayScale.value);
const displayTargetLeft = computed(() => targetLeft.value * displayScale.value);
const displayTargetTop = computed(() => targetTop.value * displayScale.value);

const targetStyle = computed(() => ({
  left: `${displayTargetLeft.value}px`,
  top: `${displayTargetTop.value}px`,
  width: `${displaySliderSize.value}px`,
  height: `${displaySliderSize.value}px`,
}));

const sliderStyle = computed(() => ({
  left: `${sliderLeft.value}px`,
  top: `${displayTargetTop.value}px`,
  width: `${displaySliderSize.value}px`,
  height: `${displaySliderSize.value}px`,
}));

const sliderFallbackStyle = computed(() => ({
  backgroundImage: captchaData.value?.backgroundImage ? `url(${captchaData.value.backgroundImage})` : undefined,
  backgroundSize: `${trackWidth.value}px ${baseHeight * displayScale.value}px`,
  backgroundPosition: `-${displayTargetLeft.value}px -${displayTargetTop.value}px`,
}));

function setTrackRef(element: Element | null) {
  resizeObserver?.disconnect();
  trackRef.value = element as HTMLElement | null;
  if (!trackRef.value) return;
  trackWidth.value = trackRef.value.offsetWidth || baseWidth;
  resizeObserver = new ResizeObserver(([entry]) => {
    trackWidth.value = entry.contentRect.width || baseWidth;
  });
  resizeObserver.observe(trackRef.value);
}

async function refresh() {
  captchaData.value = await generateBlockPuzzle();
  sliderLeft.value = 0;
  targetLeft.value = Number(captchaData.value.x ?? 0);
  targetTop.value = Number(captchaData.value.y ?? 55);
  errorMessage.value = '';
  verified.value = false;
  emit('refresh');
}

async function handlePanelShow() {
  await nextTick();
  if (trackRef.value) {
    trackWidth.value = trackRef.value.offsetWidth || baseWidth;
  }
}

function openPopup() {
  if (verified.value) return;
  panelVisible.value = true;
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
  const maxLeft = Math.max(trackWidth.value - displaySliderSize.value, 0);
  sliderLeft.value = Math.min(Math.max(clientX - dragStartX, 0), maxLeft);
}

async function verifyPosition() {
  if (!captchaData.value?.key) return;
  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BLOCK_PUZZLE,
      pointJson: JSON.stringify({
        x: Math.round(sliderLeft.value / displayScale.value),
        y: Math.round(targetTop.value),
      }),
    });
    if (result) {
      verified.value = true;
      panelVisible.value = false;
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
  resizeObserver?.disconnect();
});

watch(() => props.mode, () => {
  panelVisible.value = false;
  void nextTick(handlePanelShow);
});

const PuzzleContent = defineComponent({
  name: 'BlockPuzzleContent',
  setup() {
    return () => [
      h('div', { ref: setTrackRef, class: 'track' }, [
        captchaData.value?.backgroundImage
          ? h('img', {
            class: 'captcha-image',
            src: captchaData.value.backgroundImage,
            alt: '滑块验证码背景',
          })
          : h('div', { class: 'captcha-placeholder' }, '加载中...'),
        captchaData.value?.backgroundImage
          ? h('div', { class: 'target', style: targetStyle.value })
          : null,
        h('div', {
          class: 'slider',
          style: sliderStyle.value,
          onMousedown: startDrag,
          onTouchstart: (event: TouchEvent) => {
            event.preventDefault();
            startTouchDrag(event);
          },
        }, [
          captchaData.value?.sliderImage
            ? h('img', {
              class: 'slider-image',
              src: captchaData.value.sliderImage,
              alt: '滑块拼图片',
            })
            : captchaData.value?.backgroundImage
              ? h('span', { class: 'slider-fallback', style: sliderFallbackStyle.value })
              : h('span', { class: 'slider-empty' }),
        ]),
      ]),
      errorMessage.value ? h('div', { class: 'error-msg' }, errorMessage.value) : null,
    ];
  },
});

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.block-puzzle-captcha {
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .puzzle-panel {
    width: 100%;
  }

  :deep(.track) {
    position: relative;
    width: 280px;
    max-width: 100%;
    aspect-ratio: 7 / 4;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-light);
    overflow: hidden;
  }

  :deep(.captcha-image) {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  :deep(.captcha-placeholder) {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--el-text-color-secondary);
  }

  :deep(.target) {
    position: absolute;
    z-index: 1;
    border: 2px dashed rgb(255 255 255 / 95%);
    border-radius: 4px;
    background: rgb(0 0 0 / 22%);
    box-shadow:
      inset 0 0 0 1px rgb(0 0 0 / 35%),
      0 0 0 999px rgb(0 0 0 / 5%);
    pointer-events: none;
  }

  :deep(.slider) {
    position: absolute;
    z-index: 2;
    border-radius: 4px;
    background: transparent;
    filter: drop-shadow(0 6px 10px rgb(0 0 0 / 24%));
    cursor: grab;
    overflow: hidden;
    touch-action: none;
  }

  :deep(.slider:active) {
    cursor: grabbing;
  }

  :deep(.slider-image) {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  :deep(.slider-fallback) {
    display: block;
    width: 100%;
    height: 100%;
    background-repeat: no-repeat;
    background-color: var(--el-fill-color-light);
  }

  :deep(.slider-empty) {
    display: block;
    width: 100%;
    height: 100%;
    background: var(--el-fill-color);
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
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease,
      color 0.2s ease;

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
