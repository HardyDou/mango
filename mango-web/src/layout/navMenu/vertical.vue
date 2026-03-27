<template>
  <el-menu
    router
    :default-active="state.defaultActive"
    background-color="transparent"
    :collapse="themeConfig.isCollapse"
    :unique-opened="themeConfig.isUniqueOpened"
    :collapse-transition="false"
    class="nav-menu-vertical"
  >
    <template v-for="val in menuList" :key="val.path">
      <el-sub-menu
        :index="val.path"
        v-if="val.children && val.children.length > 0"
      >
        <template #title>
          <span>{{ val.meta?.title || val.name }}</span>
        </template>
        <SubItem :chil="val.children" />
      </el-sub-menu>
      <el-menu-item :index="val.path" v-else>
        <span>{{ val.meta?.title || val.name }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts" name="navMenuVertical">
import { defineAsyncComponent, onMounted, reactive, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const SubItem = defineAsyncComponent(() => import('./subItem.vue'));

interface MenuItem {
  path: string;
  name?: string;
  meta?: { title?: string; icon?: string; isLink?: string };
  children?: MenuItem[];
}

defineProps<{
  menuList: MenuItem[];
}>();

const route = useRoute();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const state = reactive({
  defaultActive: route.path,
  isCollapse: false,
});

watch(
  () => route.path,
  () => {
    state.defaultActive = route.path;
  }
);
</script>

<style scoped lang="scss">
.nav-menu-vertical {
  border-right: none;
  padding-top: 8px;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 44px;
    line-height: 44px;
    color: var(--mango-color-menu-bar);

    &:hover {
      background-color: var(--mango-color-menu-hover) !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    color: var(--mango-color-primary) !important;
    background-color: var(--mango-color-menu-active-bg) !important;
  }

  :deep(.el-sub-menu .el-menu-item) {
    padding-left: 44px !important;
  }
}
</style>
