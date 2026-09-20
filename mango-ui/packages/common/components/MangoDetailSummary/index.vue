<template>
  <section class="mango-detail-summary" :data-surface="dataSurface" aria-label="详情摘要">
    <div class="mango-detail-summary__main">
      <strong class="mango-detail-summary__title">{{ displayText(title) }}</strong>
      <div v-if="tags.length" class="mango-detail-summary__tags">
        <el-tag v-for="tag in tags" :key="tag.key" :type="tag.type" :effect="tag.effect || 'plain'" size="small">
          {{ displayText(tag.label) }}
        </el-tag>
      </div>
    </div>
    <div v-if="fields.length" class="mango-detail-summary__fields">
      <div v-for="field in fields" :key="field.key" class="mango-detail-summary__field">
        <span>{{ field.label }}</span>
        <strong>{{ displayText(field.value) }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { MangoDetailSummaryProps } from './types';

defineOptions({ name: 'MangoDetailSummary' });

withDefaults(defineProps<MangoDetailSummaryProps>(), {
  dataSurface: undefined,
  title: undefined,
  tags: () => [],
  fields: () => [],
});

function displayText(value: unknown) {
  return String(value ?? '').trim() || '-';
}
</script>

<style scoped>
.mango-detail-summary {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 5px 0;
}

.mango-detail-summary__main {
  min-width: 0;
  flex: 1 1 0;
}

.mango-detail-summary__title {
  display: block;
  color: var(--el-text-color-primary);
  font-size: var(--mango-detail-title-size, 18px);
  font-weight: 600;
  line-height: 26px;
  overflow-wrap: anywhere;
}

.mango-detail-summary__tags,
.mango-detail-summary__fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.mango-detail-summary__tags {
  margin-top: 10px;
}

.mango-detail-summary__fields {
  min-width: 0;
  flex: 0 0 auto;
  justify-content: flex-end;
  gap: 10px 24px;
}

.mango-detail-summary__field span,
.mango-detail-summary__field strong {
  display: block;
  text-align: right;
  overflow-wrap: anywhere;
}

.mango-detail-summary__field span {
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: var(--mango-detail-meta-size, 12px);
  line-height: 18px;
}

.mango-detail-summary__field strong {
  color: var(--el-text-color-primary);
  font-size: var(--mango-detail-body-size, 14px);
  font-weight: 600;
  line-height: 22px;
}

@media (width <= 768px) {
  .mango-detail-summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .mango-detail-summary__fields {
    width: 100%;
    justify-content: flex-start;
  }

  .mango-detail-summary__field span,
  .mango-detail-summary__field strong {
    text-align: left;
  }
}
</style>
