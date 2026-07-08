<template>
  <main class="mango-detail-page" :data-page="dataPage || undefined">
    <header class="mango-detail-page__backbar">
      <el-button link :icon="Back" @click="emit('back')">{{ backText }}</el-button>
      <span class="mango-detail-page__title">{{ title }}</span>
    </header>
    <div class="mango-detail-page__content">
      <slot />
    </div>
    <footer v-if="$slots.actions" class="mango-detail-page__actions">
      <slot name="actions" />
    </footer>
  </main>
</template>

<script setup lang="ts" name="MangoDetailPage">
import { Back } from '@element-plus/icons-vue';

withDefaults(defineProps<{
  title: string;
  backText?: string;
  dataPage?: string;
}>(), {
  backText: '返回',
  dataPage: undefined,
});

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

.mango-detail-page__backbar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
}

.mango-detail-page__title {
  color: var(--mango-text-color);
  font-size: 18px;
  font-weight: 600;
  line-height: 26px;
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
