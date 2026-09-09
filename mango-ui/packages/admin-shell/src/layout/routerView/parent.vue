<template>
  <div class="router-view-parent" :class="{ 'is-root': isRootParent }">
    <router-view v-slot="{ Component, route }">
      <keep-alive v-if="route.meta.keepAlive">
        <component
          :is="Component"
          :key="
            getTagKey(
              createTagSnapshot({
                path: route.path,
                name: route.name,
                query: route.query,
                params: route.params,
                hash: route.hash,
                meta: { ...route.meta },
              }),
            )
          "
        />
      </keep-alive>
      <component
        v-else
        :is="Component"
        :key="
          getTagKey(
            createTagSnapshot({
              path: route.path,
              name: route.name,
              query: route.query,
              params: route.params,
              hash: route.hash,
              meta: { ...route.meta },
            }),
          )
        "
      />
    </router-view>
  </div>
</template>

<script setup lang="ts" name="RouterViewParent">
import { computed, inject, provide } from 'vue';
import { createTagSnapshot, getTagKey } from '@mango/common/utils/tagsView';

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
