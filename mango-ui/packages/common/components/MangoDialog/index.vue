<template>
  <el-dialog
    ref="dialogRef"
    v-bind="forwardedAttrs"
    v-model="visible"
    :width="width"
    :show-close="false"
    :destroy-on-close="destroyOnClose"
    :modal="resolvedModal"
    :modal-penetrable="!resolvedModal"
    :close-on-click-modal="resolvedCloseOnClickModal"
    :lock-scroll="resolvedLockScroll"
    :z-index="dialogZIndex"
    :class="[
      $attrs.class,
      {
        'mango-dialog--draggable': draggable,
        'mango-dialog--resizable': resizable,
        'mango-dialog--free-layout': hasFreeLayout,
        'mango-dialog--interacting': isInteracting,
      },
    ]"
    :style="[$attrs.style, dialogStyle]"
    align-center
    class="mango-dialog"
    @open="handleOpen"
    @opened="emit('opened')"
    @close="handleClosing"
    @closed="handleClosed"
  >
    <template #header>
      <div v-if="showHeader" class="mango-dialog__header" @pointerdown="startDrag">
        <div class="mango-dialog__title">
          <slot name="title">
            {{ title }}
          </slot>
        </div>
        <div class="mango-dialog__header-actions">
          <slot name="headerExtra" />
          <button v-if="showClose" class="mango-dialog__close" type="button" aria-label="close" @click="handleClose">
            <el-icon>
              <Close />
            </el-icon>
          </button>
        </div>
      </div>

      <div v-else class="mango-dialog__header mango-dialog__header--close-only" @pointerdown="startDrag">
        <button v-if="showClose" class="mango-dialog__close" type="button" aria-label="close" @click="handleClose">
          <el-icon>
            <Close />
          </el-icon>
        </button>
      </div>

      <template v-if="resizable">
        <span
          v-for="corner in resizeCorners"
          :key="corner"
          class="mango-dialog__resize-handle"
          :class="`mango-dialog__resize-handle--${corner}`"
          aria-hidden="true"
          @pointerdown="startResize($event, corner)"
        />
      </template>
    </template>

    <div class="mango-dialog__body" @pointerdown.capture="bringToFront">
      <slot />
    </div>

    <template v-if="$slots.footer" #footer>
      <div
        class="mango-dialog__footer"
        :class="`mango-dialog__footer--${footerAlign}`"
        @pointerdown.capture="bringToFront"
      >
        <slot name="footer" />
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, useAttrs, watch } from 'vue';
import { Close } from '@element-plus/icons-vue';
import type { MangoDialogEmits, MangoDialogExpose, MangoDialogProps } from './types';
import { type DialogResizeCorner, useDialogWindow } from './useDialogWindow';

interface DialogExpose {
  handleClose: () => void;
}

defineOptions({
  name: 'MangoDialog',
  inheritAttrs: false,
});

const props = withDefaults(defineProps<MangoDialogProps>(), {
  title: '',
  width: '50%',
  showHeader: true,
  showClose: true,
  footerAlign: 'right',
  destroyOnClose: false,
  modal: undefined,
  closeOnClickModal: false,
  lockScroll: undefined,
  zIndex: undefined,
  draggable: false,
  resizable: false,
  minWidth: 320,
  minHeight: 240,
});

const emit = defineEmits<MangoDialogEmits>();
const attrs = useAttrs();
const dialogRef = ref<DialogExpose>();
const resizeCorners: DialogResizeCorner[] = ['north-west', 'north-east', 'south-west', 'south-east'];
let dialogUnavailable = false;
let dialogUnmounted = false;

const forwardedAttrs = computed(() => {
  const result = { ...attrs };
  delete result.class;
  delete result.style;
  return result;
});

const resolvedModal = computed(() => props.modal ?? !props.draggable);
const resolvedCloseOnClickModal = computed(() => (resolvedModal.value ? props.closeOnClickModal : false));
const resolvedLockScroll = computed(() => props.lockScroll ?? resolvedModal.value);
const {
  bringToFront: bringDialogToFront,
  dialogStyle,
  isInteracting,
  resetWindow,
  startDrag,
  startResize,
  zIndex: dialogZIndex,
} = useDialogWindow({
  draggable: computed(() => props.draggable),
  resizable: computed(() => props.resizable),
  minWidth: computed(() => props.minWidth),
  minHeight: computed(() => props.minHeight),
  zIndex: computed(() => props.zIndex),
});
const hasFreeLayout = computed(() => Object.keys(dialogStyle.value).length > 0);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    emit('update:modelValue', value);
  },
});

watch(
  () => props.modelValue,
  (value) => {
    if (value) dialogUnavailable = false;
  },
);

function handleClose() {
  // Use Element Plus close flow so attrs such as before-close still take effect.
  dialogRef.value?.handleClose();
}

function handleOpen() {
  dialogUnavailable = false;
  bringDialogToFront();
  emit('open');
}

function handleClosing() {
  dialogUnavailable = true;
  emit('close');
}

function handleClosed() {
  dialogUnavailable = true;
  resetWindow();
  emit('closed');
}

function bringToFront() {
  if (!props.modelValue || dialogUnavailable || dialogUnmounted) return;
  bringDialogToFront();
}

onBeforeUnmount(() => {
  dialogUnmounted = true;
});

defineExpose<MangoDialogExpose>({
  bringToFront,
});
</script>

<style scoped lang="scss">
:global(.mango-dialog.el-dialog) {
  --mango-dialog-max-width: calc(100vw - 24px);
  --mango-dialog-max-height: 90vh;
  --mango-dialog-header-height: 56px;
  --mango-dialog-close-row-height: 44px;
  --mango-dialog-footer-min-height: 56px;
  --mango-dialog-body-padding: 20px 24px;
  --mango-dialog-header-padding: 0 20px 0 24px;
  --mango-dialog-footer-padding: 16px 24px 18px;
  --mango-dialog-header-shadow: 0 6px 14px rgb(0 0 0 / 6%);

  display: flex;
  position: relative;
  flex-direction: column;
  max-width: var(--mango-dialog-max-width);
  max-height: var(--mango-dialog-max-height);
  margin: 0;
  padding: 0;
  overflow: hidden;
}

:global(.mango-dialog--free-layout.el-dialog) {
  max-width: none;
  max-height: none;
}

:global(.mango-dialog .el-dialog__header) {
  flex: none;
  padding: 0;
  margin-right: 0;
}

:global(.mango-dialog .el-dialog__body) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

:global(.mango-dialog .el-dialog__footer) {
  flex: none;
  padding: 0;
}

.mango-dialog__header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: var(--mango-dialog-header-height);
  padding: var(--mango-dialog-header-padding);
  background: var(--el-bg-color);
  box-shadow: var(--mango-dialog-header-shadow);
}

:global(.mango-dialog--draggable .mango-dialog__header) {
  cursor: move;
  user-select: none;
  touch-action: none;
}

.mango-dialog__header--close-only {
  justify-content: flex-end;
  min-height: var(--mango-dialog-close-row-height);
  border-bottom: 0;
  box-shadow: none;
}

.mango-dialog__title {
  min-width: 0;
  overflow: hidden;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mango-dialog__header-actions {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.mango-dialog__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--el-border-radius-base);
  font-size: 18px;
  transition:
    color var(--el-transition-duration),
    background-color var(--el-transition-duration);
}

.mango-dialog__close:hover {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}

.mango-dialog__body {
  flex: 1 1 auto;
  min-height: 0;
  max-height: calc(
    var(--mango-dialog-max-height) - var(--mango-dialog-header-height) - var(--mango-dialog-footer-min-height)
  );
  padding: var(--mango-dialog-body-padding);
  overflow: hidden auto;
  background: var(--el-bg-color);
}

:global(.mango-dialog--free-layout .mango-dialog__body) {
  max-height: none;
}

.mango-dialog__resize-handle {
  position: absolute;
  z-index: 2;
  width: 18px;
  height: 18px;
  touch-action: none;
}

.mango-dialog__resize-handle--north-west {
  top: 0;
  left: 0;
  cursor: nwse-resize;
}

.mango-dialog__resize-handle--north-east {
  top: 0;
  right: 0;
  cursor: nesw-resize;
}

.mango-dialog__resize-handle--south-west {
  bottom: 0;
  left: 0;
  cursor: nesw-resize;
}

.mango-dialog__resize-handle--south-east {
  right: 0;
  bottom: 0;
  cursor: nwse-resize;
}

:global(.mango-dialog--interacting.el-dialog),
:global(.mango-dialog--interacting.el-dialog *) {
  user-select: none;
}

.mango-dialog__footer {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: var(--mango-dialog-footer-min-height);
  padding: var(--mango-dialog-footer-padding);
  background: var(--el-bg-color);
}

.mango-dialog__footer--left {
  justify-content: flex-start;
}

.mango-dialog__footer--center {
  justify-content: center;
}

.mango-dialog__footer--right {
  justify-content: flex-end;
}
</style>
