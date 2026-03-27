<template>
  <div class="layout-logo" v-if="setShowLogo" @click="onLogoClick">
    <span :style="{ color: setFontColor }">{{ themeConfig.globalTitle }}</span>
  </div>
  <div class="layout-logo-size" v-else @click="onLogoClick">
    <span class="layout-logo-size-text">M</span>
  </div>
</template>

<script setup lang="ts" name="layoutLogo">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useThemeConfig } from '@/stores/themeConfig';

const storesThemeConfig = useThemeConfig();
const { themeConfig } = storeToRefs(storesThemeConfig);

const setShowLogo = computed(() => {
  const { isCollapse, layout } = themeConfig.value;
  return !isCollapse || layout === 'classic' || document.body.clientWidth < 1000;
});

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
  width: 220px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  white-space: nowrap;
  overflow: hidden;

  span {
    color: var(--mango-color-primary);
  }
}

.layout-logo-size {
  width: 100%;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &-text {
    font-size: 20px;
    font-weight: 700;
    color: var(--mango-color-primary);
  }
}
</style>
