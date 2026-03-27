<template>
  <div class="router-view-parent">
    <router-view v-slot="{ Component, route }">
      <keep-alive :include="keepAliveNames">
        <component :is="Component" :key="route.path" />
      </keep-alive>
    </router-view>
  </div>
</template>

<script setup lang="ts" name="RouterViewParent">
import { storeToRefs } from 'pinia';
import { useKeepAliveNames } from '@/stores/keepAliveNames';

const storesKeepAliveNames = useKeepAliveNames();
const { keepAliveNames } = storeToRefs(storesKeepAliveNames);
</script>

<style scoped lang="scss">
.router-view-parent {
  width: 100%;
  height: 100%;
}
</style>
