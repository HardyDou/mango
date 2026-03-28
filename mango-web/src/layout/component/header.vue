<template>
  <el-header class="layout-header" v-show="!isTagsViewCurrenFull">
    <NavBarsIndex />
  </el-header>
</template>

<script setup lang="ts" name="layoutHeader">
import { defineAsyncComponent } from 'vue';
import { storeToRefs } from 'pinia';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';

const NavBarsIndex = defineAsyncComponent(() => import('../navBars/index.vue'));

const storesTagsViewRoutes = useTagsViewRoutes();
const { isTagsViewCurrenFull } = storeToRefs(storesTagsViewRoutes);
</script>

<style scoped lang="scss">
.layout-header {
  flex-shrink: 0;
  width: 100%;
  height: var(--mango-header-height);
  background: var(--mango-bg-top-bar);
  color: var(--mango-color-top-bar);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
</style>
