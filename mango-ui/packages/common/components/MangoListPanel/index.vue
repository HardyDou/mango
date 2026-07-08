<template>
  <section class="mango-list-panel" data-surface="list">
    <div v-if="hasToolbar" class="mango-list-panel__toolbar">
      <div class="mango-list-panel__actions">
        <slot name="actions" />
      </div>
      <div class="mango-list-panel__view-actions">
        <slot name="view-actions" />
      </div>
    </div>
    <div class="mango-list-panel__table">
      <slot />
    </div>
    <div v-if="hasPagination" class="mango-list-panel__pagination">
      <slot name="pagination" />
    </div>
  </section>
</template>

<script setup lang="ts" name="MangoListPanel">
import { computed, useSlots } from 'vue';

const slots = useSlots();
const hasToolbar = computed(() => Boolean(slots.actions || slots['view-actions']));
const hasPagination = computed(() => Boolean(slots.pagination));
</script>

<style scoped>
.mango-list-panel {
  width: 100%;
  min-width: 0;
  padding: 16px;
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-light);
  border-radius: 6px;
  box-shadow: var(--mango-shadow-light);
}

.mango-list-panel__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.mango-list-panel__actions,
.mango-list-panel__view-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.mango-list-panel__view-actions {
  justify-content: flex-end;
}

.mango-list-panel__table {
  min-width: 0;
}

.mango-list-panel__table :deep(.el-table) {
  --el-table-border-color: var(--mango-border-light);
  --el-table-header-bg-color: var(--mango-bg-main);
  --el-table-row-hover-bg-color: var(--mango-color-primary-lighter);
}

.mango-list-panel__pagination {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  min-width: 0;
}

.mango-list-panel__pagination :deep(.el-pagination) {
  flex-wrap: wrap;
  gap: 8px;
  padding: 0;
}

@media (max-width: 720px) {
  .mango-list-panel {
    padding: 12px;
  }

  .mango-list-panel__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .mango-list-panel__view-actions {
    justify-content: flex-start;
  }

  .mango-list-panel__pagination {
    justify-content: flex-start;
  }
}
</style>
