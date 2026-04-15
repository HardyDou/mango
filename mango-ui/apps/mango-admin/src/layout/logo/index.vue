<template>
  <div
    v-if="!layoutStore.isCollapse"
    class="layout-logo"
    @click="onLogoClick"
  >
    <span
      class="logo-text"
      :style="{ color: setFontColor }"
    >{{ preferencesStore.globalTitle }}</span>
  </div>
  <div
    v-else
    class="layout-logo-collapsed"
    @click="onLogoClick"
  >
    <span
      class="logo-icon"
      :style="{ color: setFontColor }"
    >M</span>
  </div>
</template>

<script setup lang="ts" name="layoutLogo">
import { computed } from 'vue';
import { useLayoutStore } from '@/stores/layout';
import { usePreferencesStore } from '@/stores/preferences';

const layoutStore = useLayoutStore();
const preferencesStore = usePreferencesStore();

const setFontColor = computed(() => {
  return layoutStore.layout === 'classic' || layoutStore.layout === 'transverse'
    ? 'var(--mango-color-top-bar)'
    : 'var(--mango-color-primary)';
});

const onLogoClick = () => {
  if (layoutStore.layout === 'transverse') return false;
  layoutStore.toggleCollapse();
};
</script>

<style scoped lang="scss">
.layout-logo {
  width: auto;
  min-width: 160px;
  max-width: 220px;
  height: var(--mango-header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;

  .logo-text {
    font-size: 16px;
    font-weight: 700;
    white-space: nowrap;
  }
}

.layout-logo-collapsed {
  width: 64px;
  height: var(--mango-header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;

  .logo-icon {
    font-size: 20px;
    font-weight: 700;
  }
}
</style>
