<template>
  <template
    v-for="val in chil"
    :key="val.path"
  >
    <el-menu-item
      v-if="!val.children || val.children.length === 0"
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
    <el-sub-menu
      v-else
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
  </template>
</template>

<script setup lang="ts" name="navMenuSubItem">
import { iconMap } from '@mango/common/utils/iconConfig';

defineProps<{
  chil: any[];
}>();
</script>

<style scoped>
.menu-icon {
  margin-right: 8px;
}
</style>
