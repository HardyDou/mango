<template>
  <el-dialog
    ref="dialogRef"
    v-bind="$attrs"
    v-model="visible"
    :width="width"
    :show-close="false"
    :destroy-on-close="destroyOnClose"
    align-center
    class="mango-dialog"
    @open="emit('open')"
    @opened="emit('opened')"
    @close="emit('close')"
    @closed="emit('closed')"
  >
    <template #header>
      <div
        v-if="showHeader"
        class="mango-dialog__header"
      >
        <div class="mango-dialog__title">
          <slot name="title">
            {{ title }}
          </slot>
        </div>
        <div class="mango-dialog__header-actions">
          <slot name="headerExtra" />
          <button
            v-if="showClose"
            class="mango-dialog__close"
            type="button"
            aria-label="close"
            @click="handleClose"
          >
            <el-icon>
              <Close />
            </el-icon>
          </button>
        </div>
      </div>

      <div
        v-else
        class="mango-dialog__header mango-dialog__header--close-only"
      >
        <button
          v-if="showClose"
          class="mango-dialog__close"
          type="button"
          aria-label="close"
          @click="handleClose"
        >
          <el-icon>
            <Close />
          </el-icon>
        </button>
      </div>
    </template>

    <div class="mango-dialog__body">
      <slot />
    </div>

    <template
      v-if="$slots.footer"
      #footer
    >
      <div
        class="mango-dialog__footer"
        :class="`mango-dialog__footer--${footerAlign}`"
      >
        <slot name="footer" />
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Close } from '@element-plus/icons-vue';
import type { MangoDialogEmits, MangoDialogProps } from './types';

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
});

const emit = defineEmits<MangoDialogEmits>();

const dialogRef = ref<DialogExpose>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    emit('update:modelValue', value);
  },
});

function handleClose() {
  // Use Element Plus close flow so attrs such as before-close still take effect.
  dialogRef.value?.handleClose();
}
</script>

<style scoped lang="scss">
:global(.mango-dialog.el-dialog) {
  --mango-dialog-max-height: 90vh;
  --mango-dialog-header-height: 56px;
  --mango-dialog-close-row-height: 44px;
  --mango-dialog-footer-min-height: 56px;
  --mango-dialog-body-padding: 20px 24px;
  --mango-dialog-header-padding: 0 20px 0 24px;
  --mango-dialog-footer-padding: 16px 24px 18px;
  --mango-dialog-header-shadow: 0 6px 14px rgba(0, 0, 0, 0.06);

  display: flex;
  flex-direction: column;
  max-height: var(--mango-dialog-max-height);
  margin: 0;
  padding: 0;
  overflow: hidden;
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
    var(--mango-dialog-max-height) -
    var(--mango-dialog-header-height) -
    var(--mango-dialog-footer-min-height)
  );
  padding: var(--mango-dialog-body-padding);
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--el-bg-color);
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
