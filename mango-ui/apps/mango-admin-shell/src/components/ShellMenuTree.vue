<template>
  <template
    v-for="item in items"
    :key="item.path"
  >
    <el-sub-menu
      v-if="item.children && item.children.length > 0"
      :index="item.path"
    >
      <template #title>
        <el-icon
          v-if="item.icon"
          class="menu-icon"
        >
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.menuName }}</span>
      </template>
      <ShellMenuTree :items="item.children" />
    </el-sub-menu>
    <el-menu-item
      v-else
      :index="item.path"
    >
      <el-icon
        v-if="item.icon"
        class="menu-icon"
      >
        <component :is="item.icon" />
      </el-icon>
      <template #title>
        <span>{{ item.menuName }}</span>
      </template>
    </el-menu-item>
  </template>
</template>

<script setup lang="ts">
import type { ShellMenu } from '../runtime/menuHost';

defineProps<{
  items: ShellMenu[];
}>();
</script>

<style scoped>
.menu-icon {
  margin-right: 8px;
  font-size: 18px;
}
</style>
