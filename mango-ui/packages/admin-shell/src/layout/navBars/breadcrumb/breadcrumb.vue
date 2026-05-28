<template>
  <div
    v-if="layoutStore.isBreadcrumb"
    class="layout-breadcrumb"
  >
    <el-breadcrumb separator="/">
      <el-breadcrumb-item
        v-for="item in breadcrumbs"
        :key="item.path"
        :to="item.path"
      >
        {{ item.meta?.title || item.name }}
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup lang="ts" name="breadcrumb">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useLayoutStore } from '../../../stores/layout';

const route = useRoute();
const layoutStore = useLayoutStore();

const breadcrumbs = computed(() => {
  return route.matched.filter((item) => item.meta?.title);
});
</script>

<style scoped lang="scss">
.layout-breadcrumb {
  display: flex;
  align-items: center;
  min-height: 28px;
  overflow: hidden;
  background: transparent;
  color: var(--el-text-color-regular);

  :deep(.el-breadcrumb) {
    display: flex;
    align-items: center;
    min-width: 0;
    white-space: nowrap;
  }

  :deep(.el-breadcrumb__item) {
    flex-shrink: 0;

    .el-breadcrumb__inner {
      color: var(--el-text-color-regular);
      &.is-link:hover {
        color: var(--mango-color-primary);
      }
    }
    .el-breadcrumb__separator {
      color: var(--el-text-color-placeholder);
    }
  }
}
</style>
