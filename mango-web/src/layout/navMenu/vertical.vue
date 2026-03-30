<template>
  <el-menu
    router
    :default-active="state.defaultActive"
    background-color="transparent"
    :collapse="props.disableCollapse ? false : layoutStore.isCollapse"
    :unique-opened="layoutStore.isUniqueOpened"
    :collapse-transition="false"
    class="nav-menu-vertical"
  >
    <template v-for="val in menuList" :key="val.path">
      <el-sub-menu
        :index="val.path"
        v-if="val.children && val.children.length > 0"
      >
        <template #title>
          <el-icon v-if="val.meta?.icon" class="menu-icon">
            <HomeFilled v-if="val.meta.icon === 'HomeFilled'" />
            <User v-else-if="val.meta.icon === 'User'" />
            <Lock v-else-if="val.meta.icon === 'Lock'" />
            <Search v-else-if="val.meta.icon === 'Search'" />
            <Setting v-else-if="val.meta.icon === 'Setting'" />
            <Fold v-else-if="val.meta.icon === 'Fold'" />
            <Expand v-else-if="val.meta.icon === 'Expand'" />
            <Close v-else-if="val.meta.icon === 'Close'" />
          </el-icon>
          <span>{{ val.meta?.title || val.name }}</span>
        </template>
        <SubItem :chil="val.children" />
      </el-sub-menu>
      <el-menu-item :index="val.path" v-else>
        <el-icon v-if="val.meta?.icon" class="menu-icon">
          <HomeFilled v-if="val.meta.icon === 'HomeFilled'" />
          <User v-else-if="val.meta.icon === 'User'" />
          <Lock v-else-if="val.meta.icon === 'Lock'" />
          <Search v-else-if="val.meta.icon === 'Search'" />
          <Setting v-else-if="val.meta.icon === 'Setting'" />
          <Fold v-else-if="val.meta.icon === 'Fold'" />
          <Expand v-else-if="val.meta.icon === 'Expand'" />
          <Close v-else-if="val.meta.icon === 'Close'" />
        </el-icon>
        <span>{{ val.meta?.title || val.name }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts" name="navMenuVertical">
import { defineAsyncComponent, reactive, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useLayoutStore } from '@/stores/layout';
import {
  HomeFilled,
  User,
  Lock,
  Search,
  Setting,
  Close,
  Fold,
  Expand
} from '@element-plus/icons-vue';

const SubItem = defineAsyncComponent(() => import('./subItem.vue'));

interface MenuItem {
  path: string;
  name?: string;
  meta?: { title?: string; icon?: string; isLink?: string };
  children?: MenuItem[];
}

const props = defineProps<{
  menuList: MenuItem[];
  disableCollapse?: boolean;
}>();

const route = useRoute();
const layoutStore = useLayoutStore();

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

    .menu-icon {
      margin-right: 8px;
      font-size: 18px;
    }

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

  // 收起状态时图标居中
  &.is-collapse {
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      padding-left: 0 !important;
      padding-right: 0 !important;
      justify-content: center;

      .menu-icon {
        margin-right: 0;
      }
    }

    :deep(.el-sub-menu) {
      .el-menu {
        .el-menu-item {
          padding-left: 0 !important;
          justify-content: center;
        }
      }
    }
  }
}
</style>
