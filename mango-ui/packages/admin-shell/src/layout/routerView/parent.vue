<template>
  <div
    class="router-view-parent"
    :class="{ 'is-root': isRootParent }"
  >
    <router-view v-slot="{ Component, route }">
      <keep-alive :include="keepAliveNames">
        <component
          :is="Component"
          :key="route.path"
        />
      </keep-alive>
    </router-view>
  </div>
</template>

<script setup lang="ts" name="RouterViewParent">
import { computed, inject, provide } from 'vue';
import { storeToRefs } from 'pinia';
import { useKeepAliveNames } from '../../stores/keepAliveNames';

const storesKeepAliveNames = useKeepAliveNames();
const { keepAliveNames } = storeToRefs(storesKeepAliveNames);

const parentDepth = inject('routerViewParentDepth', 0);
const isRootParent = computed(() => parentDepth === 0);

provide('routerViewParentDepth', parentDepth + 1);
</script>

<style scoped lang="scss">
.router-view-parent {
  width: 100%;
  min-height: 100%;
  background-color: var(--mango-bg-main);
}
</style>
