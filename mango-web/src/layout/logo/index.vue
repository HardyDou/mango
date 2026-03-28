<template>
  <div class="layout-logo" v-if="!themeConfig.isCollapse" @click="onLogoClick">
    <span class="logo-text" :style="{ color: setFontColor }">{{ themeConfig.globalTitle }}</span>
  </div>
  <div class="layout-logo-collapsed" v-else @click="onLogoClick">
    <span class="logo-icon" :style="{ color: setFontColor }">M</span>
  </div>
</template>

<script setup lang="ts" name="layoutLogo">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const setFontColor = computed(() => {
  const { layout } = themeConfig.value;
  return layout === 'classic' || layout === 'transverse'
    ? 'var(--mango-color-top-bar)'
    : 'var(--mango-color-primary)';
});

const onLogoClick = () => {
  if (themeConfig.value.layout === 'transverse') return false;
  themeConfig.value.isCollapse = !themeConfig.value.isCollapse;
};
</script>

<style scoped lang="scss">
.layout-logo {
  width: auto;
  min-width: 160px;
  max-width: 220px;
  height: 40px;
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
  width: 40px;
  height: 40px;
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
