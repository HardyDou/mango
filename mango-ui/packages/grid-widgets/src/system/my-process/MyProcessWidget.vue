<template>
  <section class="mango-grid-widget-my-process">
    <header class="mango-grid-widget-my-process__header">
      <span>我的申请</span>
      <button
        class="mango-grid-widget-my-process__all"
        type="button"
        @click="navigateToAll"
      >
        查看全部
      </button>
    </header>

    <div
      v-if="loadError"
      class="mango-grid-widget-my-process__error"
    >
      <span>申请加载失败</span>
      <button
        type="button"
        @click="loadSummary"
      >
        重试
      </button>
    </div>

    <div
      v-else
      class="mango-grid-widget-my-process__grid"
      :class="{ 'is-loading': loading }"
    >
      <button
        v-for="item in processItems"
        :key="item.key"
        class="mango-grid-widget-my-process__item"
        type="button"
        @click="navigateToItem(item)"
      >
        <span class="mango-grid-widget-my-process__value">{{ item.value }}</span>
        <span class="mango-grid-widget-my-process__label">{{ item.label }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { workflowApi, type WorkflowBusinessApplySummary } from '@mango/workflow';
import type { MangoWidgetNavigateTarget, MyProcessWidgetProps } from '../../types';

defineOptions({
  name: 'MangoMyProcessWidget',
});

const props = withDefaults(defineProps<MyProcessWidgetProps>(), {
  processPath: '/workflow/task/initiated',
});

const loading = ref(false);
const loadError = ref(false);
const summary = ref<WorkflowBusinessApplySummary>(createEmptySummary());

const processItems = computed(() => [
  {
    key: 'inReview',
    label: '审核中',
    value: summary.value.inReview,
    raw: { query: { statuses: ['SUBMITTED', 'IN_APPROVAL'] } },
  },
  {
    key: 'completed',
    label: '已完成',
    value: summary.value.completed,
    raw: { query: { statuses: ['APPROVED'] } },
  },
  {
    key: 'rejected',
    label: '已驳回',
    value: summary.value.rejected,
    raw: { query: { statuses: ['REJECTED'] } },
  },
  {
    key: 'withdrawn',
    label: '已撤回',
    value: summary.value.withdrawn,
    raw: { query: { statuses: ['WITHDRAWN'] } },
  },
]);

onMounted(() => {
  loadSummary();
});

async function loadSummary(): Promise<void> {
  loading.value = true;
  loadError.value = false;
  try {
    summary.value = await workflowApi.businessApplyMySummary();
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

async function navigateToAll(): Promise<void> {
  await navigate({ path: props.processPath });
}

async function navigateToItem(item: { raw: Record<string, unknown> }): Promise<void> {
  await navigate({
    path: props.processPath,
    raw: item.raw,
  });
}

async function navigate(target: MangoWidgetNavigateTarget): Promise<void> {
  await props.runtime?.navigate?.(target);
}

function createEmptySummary(): WorkflowBusinessApplySummary {
  return {
    inReview: 0,
    completed: 0,
    rejected: 0,
    withdrawn: 0,
  };
}
</script>
