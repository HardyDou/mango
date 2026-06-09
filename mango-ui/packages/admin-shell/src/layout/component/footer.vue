<template>
  <footer class="layout-footer">
    <span class="layout-footer-text">{{ footerText }}</span>
  </footer>
</template>

<script setup lang="ts" name="layoutFooter">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { usePreferencesStore } from '../../stores/preferences';

const preferencesStore = usePreferencesStore();
const { footerAuthor, globalTitle } = storeToRefs(preferencesStore);

const footerText = computed(() => {
  const author = footerAuthor.value?.trim();
  const title = globalTitle.value?.trim();

  if (author && title && author !== title) {
    return `${title} · ${author}`;
  }

  return author || title || 'Mango';
});
</script>

<style scoped lang="scss">
.layout-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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
