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
      <div
        class="trigger-shell"
        @mouseenter="showTriggerPanel"
        @mouseleave="hideTriggerPanel"
      >
        <div v-show="panelVisible || dragging" class="trigger-panel">
          <PuzzleContent />
        </div>
        <div
          ref="triggerTrackRef"
          class="trigger-track"
          :class="{ 'is-success': verified, 'is-failed': failed, 'is-dragging': dragging }"
        >
          <div class="trigger-fill" :style="triggerFillStyle" />
          <button
            class="trigger-handle"
            :style="triggerHandleStyle"
            type="button"
            :disabled="verified"
            @mousedown="startDrag"
            @touchstart.prevent="startTouchDrag"
          >
            <el-icon v-if="verified"><Check /></el-icon>
            <el-icon v-else><Right /></el-icon>
          </button>
          <div class="trigger-text">
            {{ verified ? '验证通过' : '向右拖动滑块填充拼图' }}
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <el-dialog
        v-model="panelVisible"
        title="请完成安全验证"
        width="360px"
        append-to-body
        @opened="handlePanelShow"
        @closed="handlePopupClosed"
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
const triggerTrackRef = ref<HTMLElement | null>(null);
const captchaData = ref<CaptchaResponse | null>(null);
const sliderLeft = ref(0);
const trackWidth = ref(280);
const targetLeft = ref(0);
const targetTop = ref(55);
const errorMessage = ref('');
const verified = ref(false);
const panelVisible = ref(false);
const failed = ref(false);
const backgroundWidth = ref(280);
const backgroundHeight = ref(160);
const sliderSourceSize = ref(50);

let dragStartX = 0;
const dragging = ref(false);
let resizeObserver: ResizeObserver | null = null;
let verifyResolver: ((passed: boolean) => void) | null = null;
let verifyPromise: Promise<boolean> | null = null;

const triggerTrackWidth = 318;
const triggerHandleSize = 38;
const controlHandleSize = 38;
const displayScale = computed(() => trackWidth.value / backgroundWidth.value);
const displaySliderSize = computed(() => sliderSourceSize.value * displayScale.value);
const displayTargetLeft = computed(() => targetLeft.value * displayScale.value);
const displayTargetTop = computed(() => targetTop.value * displayScale.value);
const trackAspectRatio = computed(() => `${backgroundWidth.value} / ${backgroundHeight.value}`);
const triggerMaxHandleLeft = computed(() => Math.max(trackWidth.value - triggerHandleSize, 0));
const triggerMaxPieceLeft = computed(() => Math.max(trackWidth.value - displaySliderSize.value, 0));
const controlMaxHandleLeft = computed(() => Math.max(trackWidth.value - controlHandleSize, 0));
const displaySliderLeft = computed(() => {
  const maxHandleLeft = mode.value === 'trigger' ? triggerMaxHandleLeft.value : controlMaxHandleLeft.value;
  if (maxHandleLeft <= 0) return 0;
  return (sliderLeft.value / maxHandleLeft) * triggerMaxPieceLeft.value;
});

const sliderStyle = computed(() => ({
  left: `${Math.round(displaySliderLeft.value)}px`,
  top: `${Math.round(displayTargetTop.value)}px`,
  width: `${Math.round(displaySliderSize.value)}px`,
  height: `${Math.round(displaySliderSize.value)}px`,
}));

const sliderFallbackStyle = computed(() => ({
  backgroundImage: captchaData.value?.backgroundImage ? `url(${captchaData.value.backgroundImage})` : undefined,
  backgroundSize: `${trackWidth.value}px ${backgroundHeight.value * displayScale.value}px`,
  backgroundPosition: `-${displayTargetLeft.value}px -${displayTargetTop.value}px`,
}));

const triggerHandleStyle = computed(() => ({
  left: `${sliderLeft.value}px`,
  width: `${triggerHandleSize}px`,
}));

const triggerFillStyle = computed(() => ({
  width: `${sliderLeft.value + triggerHandleSize}px`,
}));

const controlHandleStyle = computed(() => ({
  left: `${sliderLeft.value}px`,
  width: `${controlHandleSize}px`,
}));

const controlFillStyle = computed(() => ({
  width: `${sliderLeft.value + controlHandleSize}px`,
}));

function setTrackRef(element: Element | null) {
  resizeObserver?.disconnect();
  trackRef.value = element as HTMLElement | null;
  if (!trackRef.value) return;
  trackWidth.value = trackRef.value.offsetWidth || backgroundWidth.value;
  resizeObserver = new ResizeObserver(([entry]) => {
    trackWidth.value = entry.contentRect.width || backgroundWidth.value;
  });
  resizeObserver.observe(trackRef.value);
}

async function refresh(options: { errorMessage?: string; failed?: boolean } = {}) {
  captchaData.value = await generateBlockPuzzle();
  sliderLeft.value = 0;
  backgroundWidth.value = Number(captchaData.value.backgroundWidth ?? 280);
  backgroundHeight.value = Number(captchaData.value.backgroundHeight ?? 160);
  sliderSourceSize.value = Number(captchaData.value.sliderSize ?? 50);
  targetLeft.value = Number(captchaData.value.x ?? 0);
  targetTop.value = Number(captchaData.value.y ?? 55);
  errorMessage.value = options.errorMessage ?? '';
  verified.value = false;
  failed.value = options.failed ?? false;
  emit('refresh');
}

async function handlePanelShow() {
  await nextTick();
  syncTriggerTrackWidth();
  if (trackRef.value) {
    trackWidth.value = trackRef.value.offsetWidth || trackWidth.value || backgroundWidth.value;
  }
}

async function showTriggerPanel() {
  if (verified.value) return;
  panelVisible.value = true;
  await handlePanelShow();
}

function hideTriggerPanel() {
  if (!dragging.value) {
    panelVisible.value = false;
  }
}

function syncTriggerTrackWidth() {
  if (mode.value === 'trigger' && triggerTrackRef.value) {
    trackWidth.value = triggerTrackRef.value.offsetWidth || triggerTrackWidth;
  }
}

async function verify() {
  if (verified.value) return true;
  if (mode.value !== 'popup') return false;
  if (verifyPromise) return verifyPromise;
  panelVisible.value = true;
  await handlePanelShow();
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

function startDrag(event: MouseEvent) {
  if (mode.value === 'trigger') {
    void showTriggerPanel();
  }
  failed.value = false;
  errorMessage.value = '';
  syncTriggerTrackWidth();
  dragStartX = event.clientX - sliderLeft.value;
  dragging.value = true;
  document.addEventListener('mousemove', handleDrag);
  document.addEventListener('mouseup', finishDrag);
}

function startTouchDrag(event: TouchEvent) {
  if (mode.value === 'trigger') {
    void showTriggerPanel();
  }
  failed.value = false;
  errorMessage.value = '';
  syncTriggerTrackWidth();
  dragStartX = event.touches[0].clientX - sliderLeft.value;
  dragging.value = true;
  document.addEventListener('touchmove', handleTouchDrag);
  document.addEventListener('touchend', finishTouchDrag);
}

function handleDrag(event: MouseEvent) {
  if (!dragging.value) return;
  updateSlider(event.clientX);
}

function handleTouchDrag(event: TouchEvent) {
  if (!dragging.value) return;
  updateSlider(event.touches[0].clientX);
}

function updateSlider(clientX: number) {
  const maxLeft = mode.value === 'trigger'
    ? triggerMaxHandleLeft.value
    : controlMaxHandleLeft.value;
  sliderLeft.value = Math.min(Math.max(clientX - dragStartX, 0), maxLeft);
}

async function verifyPosition() {
  if (!captchaData.value?.key) return;
  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.BLOCK_PUZZLE,
      pointJson: JSON.stringify({
        x: Math.round(displaySliderLeft.value / displayScale.value),
        y: Math.round(targetTop.value),
      }),
    });
    if (result) {
      verified.value = true;
      panelVisible.value = false;
      emit('success', captchaData.value.key);
      resolveVerify(true);
      return;
    }
  } catch {
    // ignored
  }

  await refresh({ errorMessage: '校验失败，请重试', failed: true });
  await handlePanelShow();
  if (mode.value === 'trigger') {
    panelVisible.value = true;
    window.setTimeout(() => {
      if (!dragging.value && !verified.value) {
        failed.value = false;
        errorMessage.value = '';
        panelVisible.value = false;
      }
    }, 800);
  }
}

function cleanupEvents() {
  document.removeEventListener('mousemove', handleDrag);
  document.removeEventListener('mouseup', finishDrag);
  document.removeEventListener('touchmove', handleTouchDrag);
  document.removeEventListener('touchend', finishTouchDrag);
}

function finishDrag() {
  dragging.value = false;
  cleanupEvents();
  void verifyPosition();
}

function finishTouchDrag() {
  dragging.value = false;
  cleanupEvents();
  void verifyPosition();
}

onMounted(() => {
  void refresh();
});

onBeforeUnmount(() => {
  cleanupEvents();
  resizeObserver?.disconnect();
  resolveVerify(false);
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
        h('div', { class: 'track-ratio', style: { aspectRatio: trackAspectRatio.value } }, [
          captchaData.value?.backgroundImage
            ? h('img', {
              class: 'captcha-image',
              src: captchaData.value.backgroundImage,
              alt: '滑块验证码背景',
            })
            : h('div', { class: 'captcha-placeholder' }, '加载中...'),
          h('div', {
            class: 'slider',
            style: sliderStyle.value,
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
      ]),
      mode.value !== 'trigger'
        ? h('div', {
          class: [
            'control-track',
            verified.value ? 'is-success' : '',
            failed.value ? 'is-failed' : '',
            dragging.value ? 'is-dragging' : '',
          ],
        }, [
          h('div', { class: 'control-fill', style: controlFillStyle.value }),
          h('button', {
            class: 'control-handle',
            style: controlHandleStyle.value,
            type: 'button',
            disabled: verified.value,
            onMousedown: startDrag,
            onTouchstart: (event: TouchEvent) => {
              event.preventDefault();
              startTouchDrag(event);
            },
          }, [
            h(Check, { class: verified.value ? '' : 'is-hidden' }),
            h(Right, { class: verified.value ? 'is-hidden' : '' }),
          ]),
          h('div', { class: 'control-text' }, verified.value ? '验证通过' : '向右拖动滑块填充拼图'),
        ])
        : null,
      errorMessage.value ? h('div', { class: 'error-msg' }, errorMessage.value) : null,
    ];
  },
});

defineExpose({ refresh, verify });
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
    width: 280px;
    max-width: 100%;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-light);
    overflow: hidden;
  }

  :deep(.track-ratio) {
    position: relative;
    width: 100%;
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

  :deep(.slider) {
    position: absolute;
    z-index: 2;
    box-sizing: border-box;
    border-radius: 4px;
    background: transparent;
    filter: drop-shadow(0 6px 10px rgb(0 0 0 / 24%));
    overflow: hidden;
    pointer-events: none;
  }

  :deep(.slider-image) {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: fill;
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

  :deep(.control-track) {
    position: relative;
    width: 280px;
    max-width: 100%;
    height: 38px;
    margin-top: 12px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-fill-color-lighter);
    overflow: hidden;
    user-select: none;
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease;

    &.is-dragging {
      border-color: var(--el-color-primary);
    }

    &.is-success {
      border-color: var(--el-color-success);
      background: var(--el-color-success-light-9);

      .control-fill {
        background: var(--el-color-success-light-8);
      }

      .control-handle {
        border-color: var(--el-color-success);
        background: var(--el-color-success);
        color: #fff;
        cursor: default;
      }

      .control-text {
        color: var(--el-color-success);
      }
    }

    &.is-failed {
      border-color: var(--el-color-danger);

      .control-fill {
        background: var(--el-color-danger-light-9);
      }
    }
  }

  :deep(.control-fill) {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 0;
    background: var(--el-color-primary-light-8);
  }

  :deep(.control-handle) {
    position: absolute;
    top: -1px;
    bottom: -1px;
    z-index: 2;
    display: inline-flex;
    min-width: 38px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-bg-color);
    box-shadow: 0 2px 8px rgb(31 45 61 / 18%);
    color: var(--el-text-color-regular);
    cursor: grab;
    font-size: 22px;
    transition:
      background-color 0.2s ease,
      border-color 0.2s ease,
      color 0.2s ease;
    touch-action: none;

    &:active {
      cursor: grabbing;
    }

    &:disabled {
      cursor: default;
    }

    .is-hidden {
      display: none;
    }
  }

  :deep(.control-text) {
    position: absolute;
    inset: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding-left: 44px;
    color: var(--el-text-color-regular);
    font-size: 15px;
    pointer-events: none;
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

  .trigger-shell {
    position: relative;
    width: 318px;
    max-width: 100%;
  }

  .trigger-panel {
    position: absolute;
    right: 0;
    bottom: calc(100% + 16px);
    left: 0;
    z-index: 10;
    padding: 0;
    border-radius: 4px;
    background: var(--el-bg-color);
    box-shadow: 0 8px 24px rgb(31 45 61 / 18%);

    :deep(.track) {
      width: 100%;
      border-color: var(--el-border-color);
      box-shadow: none;
    }

    :deep(.error-msg) {
      position: absolute;
      right: 10px;
      bottom: 8px;
      z-index: 3;
      margin: 0;
      padding: 3px 8px;
      border-radius: 3px;
      background: rgb(0 0 0 / 55%);
      color: #fff;
      font-size: 12px;
    }
  }

  .trigger-track {
    position: relative;
    width: 318px;
    max-width: 100%;
    height: 38px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-fill-color-lighter);
    overflow: hidden;
    user-select: none;
    transition:
      border-color 0.2s ease,
      background-color 0.2s ease;

    &.is-dragging {
      border-color: var(--el-color-primary);
    }

    &.is-success {
      border-color: var(--el-color-success);
      background: var(--el-color-success-light-9);

      .trigger-fill {
        background: var(--el-color-success-light-8);
      }

      .trigger-handle {
        border-color: var(--el-color-success);
        background: var(--el-color-success);
        color: #fff;
        cursor: default;
      }

      .trigger-text {
        color: var(--el-color-success);
      }
    }

    &.is-failed {
      border-color: var(--el-color-danger);

      .trigger-fill {
        background: var(--el-color-danger-light-9);
      }
    }
  }

  .trigger-fill {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 0;
    background: var(--el-color-primary-light-8);
  }

  .trigger-handle {
    position: absolute;
    top: -1px;
    bottom: -1px;
    z-index: 2;
    display: inline-flex;
    min-width: 38px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-bg-color);
    box-shadow: 0 2px 8px rgb(31 45 61 / 18%);
    color: var(--el-text-color-regular);
    cursor: grab;
    font-size: 22px;
    transition:
      background-color 0.2s ease,
      border-color 0.2s ease,
      color 0.2s ease;
    touch-action: none;

    &:active {
      cursor: grabbing;
    }

    &:disabled {
      cursor: default;
    }
  }

  .trigger-text {
    position: absolute;
    inset: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding-left: 44px;
    color: var(--el-text-color-regular);
    font-size: 15px;
    pointer-events: none;
  }
}

.is-trigger,
.is-popup {
  width: 100%;
  max-width: 420px;
}
</style>
