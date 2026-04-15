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
import { useLayoutStore } from '@/stores/layout';

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
  padding: 0 16px;
  height: 40px;
  background: transparent;
  color: var(--mango-color-top-bar);

  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      color: var(--mango-color-top-bar);
      &.is-link:hover {
        color: rgba(255, 255, 255, 0.8);
      }
    }
    .el-breadcrumb__separator {
      color: rgba(255, 255, 255, 0.6);
    }
  }
}
</style>
