<template>
  <div class="router-view-parent" :class="{ 'is-root': isRootParent }">
    <router-view v-slot="{ Component, route }">
      <!-- Keep this instance mounted so switching to a non-cached page does not drop other tabs' state. -->
      <keep-alive>
        <component :is="Component" v-if="route.meta.keepAlive" :key="resolveTabKey(route)" />
      </keep-alive>
      <component :is="Component" v-if="!route.meta.keepAlive" :key="resolveTabKey(route)" />
    </router-view>
  </div>
</template>

<script setup lang="ts" name="RouterViewParent">
import { computed, inject, provide } from 'vue';
import type { RouteLocationNormalizedLoaded } from 'vue-router';
import { createTagSnapshot, getTagKey } from '@mango/common/utils/tagsView';

const parentDepth = inject('routerViewParentDepth', 0);
const isRootParent = computed(() => parentDepth === 0);

function resolveTabKey(route: RouteLocationNormalizedLoaded) {
  return getTagKey(
    createTagSnapshot({
      path: route.path,
      name: route.name,
      query: route.query,
      params: route.params,
      hash: route.hash,
      meta: { ...route.meta },
    }),
  );
}

provide('routerViewParentDepth', parentDepth + 1);
</script>

<style scoped lang="scss">
.router-view-parent {
  width: 100%;
  min-height: 100%;
  background-color: var(--mango-bg-main);
}
</style>
