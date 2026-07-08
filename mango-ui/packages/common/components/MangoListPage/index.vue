<template>
  <main class="mango-list-page" :class="{ 'mango-list-page--dense': dense }" :data-page="dataPage || undefined">
    <slot name="search" />
    <slot />
  </main>
</template>

<script setup lang="ts" name="MangoListPage">
withDefaults(defineProps<{
  dataPage?: string;
  dense?: boolean;
}>(), {
  dataPage: undefined,
  dense: false,
});
</script>

<style scoped>
.mango-list-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.mango-list-page--dense {
  gap: 8px;
}
</style>
