<template>
  <div class="layout-breadcrumb" v-if="themeConfig.isBreadcrumb">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item
        v-for="item in breadcrumbs"
        :key="item.path"
        :to="item.path"
      >
        {{ item.meta?.title || item.name }}
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup lang="ts" name="breadcrumb">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const route = useRoute();
const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const breadcrumbs = computed(() => {
  return route.matched.filter((item) => item.meta?.title);
});
</script>

<style scoped lang="scss">
.layout-breadcrumb {
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 40px;
  background: var(--mango-bg-color);
  border-bottom: 1px solid var(--mango-border-color);
}
</style>
