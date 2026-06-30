<template>
  <footer class="layout-footer">
    <span
      v-for="item in footerItems"
      :key="item"
      class="layout-footer-text"
    >{{ item }}</span>
  </footer>
</template>

<script setup lang="ts" name="layoutFooter">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { usePreferencesStore } from '../../stores/preferences';

const preferencesStore = usePreferencesStore();
const {
  footerAuthor,
  footerContact,
  footerCopyright,
  footerIcp,
  globalTitle,
} = storeToRefs(preferencesStore);

const footerItems = computed(() => {
  const copyright = footerCopyright.value?.trim();
  const icp = footerIcp.value?.trim();
  const contact = footerContact.value?.trim();
  const author = footerAuthor.value?.trim();
  const title = globalTitle.value?.trim();

  return [copyright || author || title || 'Mango', icp, contact].filter(Boolean);
});
</script>

<style scoped lang="scss">
.layout-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 12px;
  height: var(--mango-layout-footer-height);
  padding: 0 16px;
  border-top: 1px solid var(--mango-border-color);
  background: var(--mango-bg-color);
  color: var(--mango-text-color-regular);
  box-sizing: border-box;
}

.layout-footer-text {
  font-size: 13px;
  line-height: 1;
}
</style>
