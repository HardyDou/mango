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
    <template
      v-for="val in menuList"
      :key="val.path"
    >
      <el-sub-menu
        v-if="val.children && val.children.length > 0"
        :index="val.path"
      >
        <template #title>
          <el-icon
            v-if="val.meta?.icon"
            class="menu-icon"
          >
            <component :is="iconMap[val.meta.icon]" />
          </el-icon>
          <span>{{ val.meta?.title || val.name }}</span>
        </template>
        <SubItem :chil="val.children" />
      </el-sub-menu>
      <el-menu-item
        v-else
        :index="val.path"
      >
        <el-icon
          v-if="val.meta?.icon"
          class="menu-icon"
        >
          <component :is="iconMap[val.meta.icon]" />
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
import { iconMap } from '@/config/iconConfig';

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
