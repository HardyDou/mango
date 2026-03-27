<template>
  <template v-for="val in chil" :key="val.path">
    <el-menu-item :index="val.path" v-if="!val.children || val.children.length === 0">
      <span>{{ val.meta?.title || val.name }}</span>
    </el-menu-item>
    <el-sub-menu :index="val.path" v-else>
      <template #title>
        <span>{{ val.meta?.title || val.name }}</span>
      </template>
      <SubItem :chil="val.children" />
    </el-sub-menu>
  </template>
</template>

<script setup lang="ts" name="navMenuSubItem">
defineProps<{
  chil: any[];
}>();
</script>
