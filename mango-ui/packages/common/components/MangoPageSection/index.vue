<template>
  <section class="mango-page-section">
    <header v-if="title || $slots.extra" class="mango-page-section__header">
      <h3 v-if="title" class="mango-page-section__title">{{ title }}</h3>
      <div v-if="$slots.extra" class="mango-page-section__extra">
        <slot name="extra" />
      </div>
    </header>
    <div class="mango-page-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts" name="MangoPageSection">
defineProps<{
  title?: string;
}>();
</script>

<style scoped>
.mango-page-section {
  width: 100%;
  min-width: 0;
  padding: 16px;
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-light);
  border-radius: 6px;
  box-shadow: var(--mango-shadow-light);
}

.mango-page-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.mango-page-section__title {
  margin: 0;
  color: var(--mango-text-color);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.mango-page-section__extra {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.mango-page-section__body {
  min-width: 0;
}
</style>
