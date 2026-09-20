<template>
  <div class="mango-side-drawer-shell">
    <slot name="main" />
    <el-button
      v-if="showTrigger && !visible"
      class="mango-side-drawer-shell__trigger"
      type="primary"
      :icon="Share"
      :data-action="dataAction"
      :aria-label="title"
      :title="title"
      @click="open"
    />
    <el-drawer
      v-model="visible"
      class="mango-side-drawer-shell__drawer"
      :title="title"
      direction="rtl"
      :size="drawerSize"
      append-to-body
      :data-surface="dataSurface"
      @open="emit('open')"
      @close="emit('close')"
    >
      <div class="mango-side-drawer-shell__content">
        <slot />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Share } from '@element-plus/icons-vue';
import type { MangoSideDrawerShellEmits, MangoSideDrawerShellExpose, MangoSideDrawerShellProps } from './types';

defineOptions({ name: 'MangoSideDrawerShell' });

const props = withDefaults(defineProps<MangoSideDrawerShellProps>(), {
  modelValue: false,
  title: '节点过程',
  showTrigger: true,
  drawerSize: 'min(420px, 100vw)',
  dataSurface: 'detail.side-drawer',
  dataAction: 'detail.side-drawer.open',
});
const emit = defineEmits<MangoSideDrawerShellEmits>();

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
});

function open() {
  visible.value = true;
}

function close() {
  visible.value = false;
}

function toggle() {
  visible.value = !visible.value;
}

defineExpose<MangoSideDrawerShellExpose>({ open, close, toggle });
</script>

<style scoped>
.mango-side-drawer-shell,
.mango-side-drawer-shell__main {
  width: 100%;
  min-width: 0;
}

.mango-side-drawer-shell__trigger {
  position: fixed;
  right: 32px;
  bottom: 124px;
  z-index: 100;
  width: 36px;
  min-width: 36px;
  height: 36px;
  padding: 0;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgb(49 92 246 / 24%);
}

.mango-side-drawer-shell__content {
  min-height: 100%;
}

:global(.mango-side-drawer-shell__drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

:global(.mango-side-drawer-shell__drawer .el-drawer__title) {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

:global(.mango-side-drawer-shell__drawer .el-drawer__body) {
  padding: 20px;
  overflow-y: auto;
}

@media (width <= 760px) {
  .mango-side-drawer-shell__trigger {
    right: 16px;
    bottom: 144px;
  }
}
</style>
