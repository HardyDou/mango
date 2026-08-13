<template>
  <el-menu mode="horizontal" router :default-active="activeMenuPath" class="nav-menu-horizontal">
    <template v-for="val in menuList" :key="val.path">
      <el-menu-item v-if="!val.children || val.children.length === 0" :index="val.path">
        <span>{{ val.meta?.title || val.name }}</span>
      </el-menu-item>
      <el-sub-menu v-else :index="val.path">
        <template #title>
          <span>{{ val.meta?.title || val.name }}</span>
        </template>
        <el-menu-item v-for="child in val.children" :key="child.path" :index="child.path">
          {{ child.meta?.title || child.name }}
        </el-menu-item>
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<script setup lang="ts" name="navMenuHorizontal">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { resolveActiveMenuPath } from '@mango/common/utils/menuTree';

const props = defineProps<{
  menuList: HorizontalMenuItem[];
}>();

interface HorizontalMenuItem {
  path: string;
  name?: string;
  meta?: { title?: string };
  children?: HorizontalMenuItem[];
}

const route = useRoute();
const activeMenuPath = computed(() => resolveActiveMenuPath(props.menuList, route.path) || route.path);
</script>

<style scoped lang="scss">
.nav-menu-horizontal {
  border-bottom: 1px solid var(--mango-border-color);
}
</style>
