<template>
  <div class="h100" v-show="!isTagsViewCurrenFull">
    <el-aside
      class="layout-aside"
      :class="setCollapseStyle"
      v-if="setShowAside"
    >
      <Logo v-if="themeConfig.isShowLogo && (themeConfig.layout === 'defaults' || themeConfig.layout === 'columns')" />
      <el-scrollbar
        class="flex-auto"
        @mouseenter="onAsideEnterLeave(true)"
        @mouseleave="onAsideEnterLeave(false)"
      >
        <Vertical :menu-list="menuList" />
      </el-scrollbar>
    </el-aside>
  </div>
</template>

<script setup lang="ts" name="layoutAside">
import { computed, defineAsyncComponent, ref, watch } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoutesList } from '@/stores/routesList';
import { useThemeConfig } from '@/stores/themeConfig';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';

const Logo = defineAsyncComponent(() => import('../logo/index.vue'));
const Vertical = defineAsyncComponent(() => import('../navMenu/vertical.vue'));

const layoutAsideScrollbarRef = ref();
const storesRoutesList = useRoutesList();
const storesThemeConfig = useThemeConfig();
const storesTagsViewRoutes = useTagsViewRoutes();
const { routesList } = storeToRefs(storesRoutesList);
const { themeConfig } = storeToRefs(storesThemeConfig);
const { isTagsViewCurrenFull } = storeToRefs(storesTagsViewRoutes);

const menuList = ref<any[]>([]);

watch(
  () => routesList.value,
  (newVal) => {
    menuList.value = newVal;
  },
  { immediate: true }
);

const setShowAside = computed(() => {
  const { layout } = themeConfig.value;
  // 分栏布局不显示 aside
  return layout !== 'columns';
});

const setCollapseStyle = computed(() => {
  return themeConfig.value.isCollapse ? 'aside-collapse' : '';
});

const onAsideEnterLeave = (bool: boolean) => {
  if (themeConfig.value.layout === 'columns') {
    storesRoutesList.setColumnsMenuHover(bool);
  }
};
</script>

<style scoped lang="scss">
.layout-aside {
  background: var(--mango-bg-menu-bar);
  box-shadow: 2px 0 6px rgb(0 21 41 / 8%);
  height: inherit;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  overflow-x: hidden !important;
  transition: width var(--mango-transition-duration);

  &.aside-collapse {
    width: 64px !important;
  }

  :deep(.el-scrollbar__view) {
    overflow: hidden;
  }
}
</style>
