<template>
  <div class="layout-navbars-container">
    <div class="layout-navbars-container-left">
      <BreadcrumbIndex />
    </div>
    <div class="layout-navbars-container-right">
      <Search />
      <CloseFull />
      <Settings />
      <User />
    </div>
    <TagsView v-if="setShowTagsView" />
  </div>
</template>

<script setup lang="ts" name="layoutNavBars">
import { defineAsyncComponent, computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const BreadcrumbIndex = defineAsyncComponent(() => import('./breadcrumb/breadcrumb.vue'));
const TagsView = defineAsyncComponent(() => import('./tagsView/tagsView.vue'));
const Search = defineAsyncComponent(() => import('./breadcrumb/search.vue'));
const CloseFull = defineAsyncComponent(() => import('./breadcrumb/closeFull.vue'));
const Settings = defineAsyncComponent(() => import('./breadcrumb/settings.vue'));
const User = defineAsyncComponent(() => import('./breadcrumb/user.vue'));

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const setShowTagsView = computed(() => {
  const { layout, isTagsview } = themeConfig.value;
  return layout !== 'classic' && isTagsview;
});
</script>

<style scoped lang="scss">
.layout-navbars-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;

  .layout-navbars-container-left {
    display: flex;
    align-items: center;
    height: 40px;
    padding-left: 16px;
  }

  .layout-navbars-container-right {
    position: absolute;
    right: 16px;
    top: 0;
    display: flex;
    align-items: center;
    height: 40px;
    gap: 4px;
  }
}
</style>
