<template>
  <component :is="layouts[themeConfig.layout]" />
</template>

<script setup lang="ts" name="layout">
import { onBeforeMount, onUnmounted, defineAsyncComponent } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';
import { Local } from '@/utils/storage';
import { mittBus } from '@/utils/mitt';

const layouts: Record<string, any> = {
  defaults: defineAsyncComponent(() => import('./main/defaults.vue')),
  classic: defineAsyncComponent(() => import('./main/classic.vue')),
  transverse: defineAsyncComponent(() => import('./main/transverse.vue')),
  columns: defineAsyncComponent(() => import('./main/columns.vue')),
};

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

// 窗口大小改变时(适配移动端) - 硬断点 1000px 保留
const onLayoutResize = () => {
  if (!Local.get('oldLayout')) Local.set('oldLayout', themeConfig.value.layout);
  const clientWidth = document.body.clientWidth;
  if (clientWidth < 1000) {
    themeConfig.value.isCollapse = false;
    mittBus.emit('layoutMobileResize', {
      isMobile: true,
      windowWidth: clientWidth,
    });
  } else {
    mittBus.emit('layoutMobileResize', {
      isMobile: false,
      windowWidth: clientWidth,
    });
  }
};

onBeforeMount(() => {
  onLayoutResize();
  window.addEventListener('resize', onLayoutResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', onLayoutResize);
});
</script>
