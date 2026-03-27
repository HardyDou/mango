<template>
  <div class="layout-breadcrumb-settings" @click="openSettings">
    <el-tooltip content="主题配置" placement="bottom">
      <el-icon><Setting /></el-icon>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts" name="breadcrumbSettings">
import { Setting } from '@element-plus/icons-vue';

const emit = defineEmits(['open']);

const openSettings = () => {
  emit('open');
};
</script>

<style scoped lang="scss">
.layout-breadcrumb-settings {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  cursor: pointer;
  color: var(--mango-text-color-regular);
  transition: color 0.2s;

  &:hover {
    color: var(--mango-color-primary);
  }
}
</style>
