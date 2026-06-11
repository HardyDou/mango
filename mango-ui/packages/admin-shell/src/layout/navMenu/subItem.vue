<template>
  <template
    v-for="val in chil"
    :key="val.path"
  >
    <el-menu-item
      v-if="!val.children || val.children.length === 0"
      :index="val.path"
      :style="getMenuIndentStyle(level)"
    >
      <el-icon
        v-if="val.meta?.icon"
        class="menu-icon"
      >
        <component :is="iconMap[val.meta.icon]" />
      </el-icon>
      <span>{{ val.meta?.title || val.name }}</span>
    </el-menu-item>
    <el-sub-menu
      v-else
      :index="val.path"
      :style="getMenuIndentStyle(level)"
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
      <SubItem
        :chil="val.children"
        :level="level + 1"
      />
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts" name="navMenuSubItem">
import { iconMap } from '@mango/common/utils/iconConfig';

interface MenuItem {
  path: string;
  name?: string;
  meta?: { title?: string; icon?: string; isLink?: string };
  children?: MenuItem[];
}

withDefaults(
  defineProps<{
    chil: MenuItem[];
    level?: number;
  }>(),
  {
    level: 2,
  }
);

const ROOT_MENU_INDENT = 20;
const MENU_INDENT_STEP = 24;

const getMenuIndentStyle = (level: number): { '--mango-nav-menu-indent': string } => ({
  '--mango-nav-menu-indent': `${ROOT_MENU_INDENT + (level - 1) * MENU_INDENT_STEP}px`,
});
</script>

<style scoped>
.menu-icon {
  margin-right: 8px;
}
</style>
