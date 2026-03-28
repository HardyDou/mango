<template>
  <div class="layout-breadcrumb-settings" @click="openSettings">
    <el-icon :size="20">
      <component :is="SettingIcon" />
    </el-icon>
  </div>
</template>

<script setup lang="ts" name="breadcrumbSettings">
import { markRaw } from 'vue';
import { Setting } from '@element-plus/icons-vue';

const emit = defineEmits(['open']);

const openSettings = () => {
  emit('open');
};

// 使用 markRaw 包装图标组件
const SettingIcon = markRaw(Setting);
</script>

<style scoped lang="scss">
.layout-breadcrumb-settings {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  cursor: pointer;
  color: var(--mango-color-top-bar);
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}
</style>
