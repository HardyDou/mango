<template>
  <el-aside class="layout-columns-aside" v-show="!isTagsViewCurrenFull">
    <el-scrollbar class="h100">
      <Vertical :menu-list="menuList" />
    </el-scrollbar>
  </el-aside>
</template>

<script setup lang="ts" name="layoutColumnsAside">
import { defineAsyncComponent, onMounted, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '@/stores/routesList';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';

const Vertical = defineAsyncComponent(() => import('../navMenu/vertical.vue'));

const storesRoutesList = useRoutesList();
const storesTagsViewRoutes = useTagsViewRoutes();
const { routesList } = storeToRefs(storesRoutesList);
const { isTagsViewCurrenFull } = storeToRefs(storesTagsViewRoutes);

const menuList = ref<any[]>([]);

watch(
  () => routesList.value,
  (newVal) => {
    menuList.value = newVal as any[];
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.layout-columns-aside {
  width: 64px;
  background: var(--mango-bg-columns-menu-bar, var(--mango-bg-menu-bar));
  height: 100%;
  overflow: hidden;
}
</style>
