<template>
  <div
    v-if="!layoutStore.isCollapse"
    class="layout-logo"
    @click="onLogoClick"
  >
    <img
      v-if="fullLogoUrl"
      class="logo-image logo-image-full"
      :src="fullLogoUrl"
      alt="logo"
    >
    <span
      v-else
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
      v-if="collapsedLogoUrl"
      class="logo-image logo-image-collapsed"
      :src="collapsedLogoUrl"
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

const fullLogoUrl = computed(() => preferencesStore.logoUrl);
const collapsedLogoUrl = computed(() => preferencesStore.logoIconUrl || preferencesStore.logoUrl);
const collapsedText = computed(() => preferencesStore.shortTitle?.trim().charAt(0) || 'M');

const onLogoClick = () => {
  if (layoutStore.layout === 'transverse') return false;
  layoutStore.toggleCollapse();
};
</script>

<style scoped lang="scss">
.layout-logo {
  width: 220px;
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

  .logo-image-full {
    width: auto;
    max-width: calc(100% - 24px);
    height: 32px;
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

  .logo-image-collapsed {
    width: 30px;
    height: 30px;
    object-fit: contain;
  }
}
</style>
