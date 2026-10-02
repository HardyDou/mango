<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <main class="mango-detail-page" :data-page="dataPage || undefined">
    <MangoPageBackBar :title="title" :back-label="backText" :show-refresh="false" @back="emit('back')" />
    <div class="mango-detail-page__content">
      <slot />
    </div>
    <footer v-if="$slots.actions" class="mango-detail-page__actions">
      <slot name="actions" />
    </footer>
  </main>
</template>

<script setup lang="ts" name="MangoDetailPage">
import MangoPageBackBar from '../MangoPageBackBar/index.vue';

withDefaults(
  defineProps<{
    title: string;
    backText?: string;
    dataPage?: string;
  }>(),
  {
    backText: '返回',
    dataPage: undefined,
  },
);

const emit = defineEmits<{
  back: [];
}>();
</script>

<style scoped>
.mango-detail-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  min-width: 0;
  padding-bottom: 64px;
}

.mango-detail-page__content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.mango-detail-page__actions {
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-light);
  border-radius: 6px;
  box-shadow: var(--mango-shadow-light);
}
</style>
