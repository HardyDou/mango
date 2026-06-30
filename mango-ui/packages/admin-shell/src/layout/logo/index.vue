<template>
  <div
    v-if="!layoutStore.isCollapse"
    class="layout-logo"
    @click="onLogoClick"
  >
    <img
      v-if="preferencesStore.logoUrl"
      class="logo-image"
      :src="preferencesStore.logoUrl"
      alt="logo"
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
    <img
      v-if="preferencesStore.logoUrl"
      class="logo-image"
      :src="preferencesStore.logoUrl"
      alt="logo"
    >
    <span
      v-else
      class="logo-icon"
      :style="{ color: setFontColor }"
    >{{ collapsedText }}</span>
  </div>
</template>

<script setup lang="ts" name="layoutLogo">
import { computed } from 'vue';
import { useLayoutStore } from '../../stores/layout';
import { usePreferencesStore } from '../../stores/preferences';

const layoutStore = useLayoutStore();
const preferencesStore = usePreferencesStore();

const setFontColor = computed(() => {
  return layoutStore.layout === 'classic' || layoutStore.layout === 'transverse'
    ? 'var(--mango-color-top-bar)'
    : 'var(--mango-color-primary)';
});

const collapsedText = computed(() => preferencesStore.shortTitle?.trim().charAt(0) || 'M');

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
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s;

  .logo-text {
    font-size: 16px;
    font-weight: 700;
    white-space: nowrap;
  }

  .logo-image {
    width: 28px;
    height: 28px;
    object-fit: contain;
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

  .logo-image {
    width: 30px;
    height: 30px;
    object-fit: contain;
  }
}
</style>
