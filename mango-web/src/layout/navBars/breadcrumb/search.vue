<template>
  <div class="layout-breadcrumb-search" @click="openSearch">
    <el-tooltip content="菜单搜索" placement="bottom">
      <el-icon><Search /></el-icon>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts" name="breadcrumbSearch">
import { Search } from '@element-plus/icons-vue';

const emit = defineEmits(['open']);

const openSearch = () => {
  emit('open');
};
</script>

<style scoped lang="scss">
.layout-breadcrumb-search {
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
