<template>
  <div
    class="layout-breadcrumb-search"
    @click="openSearch"
  >
    <el-icon :size="20">
      <Search />
    </el-icon>
  </div>
</template>

<script setup lang="ts" name="breadcrumbSearch">
import { Search } from '@element-plus/icons-vue';

const emit = defineEmits(['open']);

const openSearch = () => {
  emit('open');
};
</script>

<style>
.layout-breadcrumb-search {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  cursor: pointer;
  color: #ffffff;
  transition: opacity 0.2s;
}
.layout-breadcrumb-search:hover {
  opacity: 0.8;
}
</style>
