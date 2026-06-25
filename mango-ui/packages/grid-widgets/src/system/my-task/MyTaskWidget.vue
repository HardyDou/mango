<template>
  <section class="mango-grid-widget-my-task">
    <header class="mango-grid-widget-my-task__header">
      <span>我的任务</span>
      <button
        class="mango-grid-widget-my-task__all"
        type="button"
        @click="navigateToAll"
      >
        查看全部
      </button>
    </header>

    <div
      v-if="loadError"
      class="mango-grid-widget-my-task__error"
    >
      <span>任务加载失败</span>
      <button
        type="button"
        @click="loadSummary"
      >
        重试
      </button>
    </div>

    <div
      v-else
      class="mango-grid-widget-my-task__body"
      :class="{ 'is-loading': loading }"
    >
      <button
        class="mango-grid-widget-my-task__summary"
        type="button"
        @click="navigateToAll"
      >
        <span class="mango-grid-widget-my-task__summary-count">
          <strong>{{ summary.total }}</strong>
          <span>任务总数</span>
        </span>

        <span
          class="mango-grid-widget-my-task__bar"
          aria-hidden="true"
        >
          <span
            v-for="item in taskItems"
            :key="item.key"
            :class="`is-${item.key}`"
            :style="{ flexGrow: item.percent }"
          />
        </span>
      </button>

      <div class="mango-grid-widget-my-task__grid">
        <button
          v-for="item in taskItems"
          :key="item.key"
          class="mango-grid-widget-my-task__item"
          :class="`is-${item.key}`"
          type="button"
          @click="navigateToItem(item)"
        >
          <span class="mango-grid-widget-my-task__label">{{ item.label }}</span>
          <span
            class="mango-grid-widget-my-task__dot"
            aria-hidden="true"
          />
          <span class="mango-grid-widget-my-task__value">{{ item.value }}</span>
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { workflowApi, type WorkflowMyTaskSummary } from '@mango/workflow';
import type { MangoWidgetNavigateTarget, MyTaskWidgetProps } from '../../types';

defineOptions({
  name: 'MangoMyTaskWidget',
});

const props = withDefaults(defineProps<MyTaskWidgetProps>(), {
  taskPath: '/workflow/task/todo',
});

const loading = ref(false);
const loadError = ref(false);
const summary = ref<WorkflowMyTaskSummary>(createEmptySummary());

const taskItems = computed(() => {
  const total = Math.max(summary.value.total, 1);
  const hasTotal = summary.value.total > 0;
  return [
    {
      key: 'pending',
      label: '待完成',
      value: summary.value.pending,
      raw: { query: { todoType: 'CLAIMABLE' } },
    },
    {
      key: 'processing',
      label: '进行中',
      value: summary.value.processing,
      raw: { query: { todoType: 'ASSIGNED' } },
    },
    {
      key: 'completed',
      label: '已完成',
      value: summary.value.completed,
      path: '/workflow/task/done',
      raw: { query: {} },
    },
    {
      key: 'overdue',
      label: '已逾期',
      value: summary.value.overdue,
      raw: { query: { todoType: 'ALL', overdue: 'true' } },
    },
  ].map(item => ({
    ...item,
    percent: hasTotal ? Math.max(item.value, 0) / total : 1,
  }));
});

onMounted(() => {
  loadSummary();
});

async function loadSummary(): Promise<void> {
  loading.value = true;
  loadError.value = false;
  try {
    summary.value = await workflowApi.myTaskSummary();
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

async function navigateToAll(): Promise<void> {
  await navigate({ path: props.taskPath });
}

async function navigateToItem(item: { path?: string; raw: Record<string, unknown> }): Promise<void> {
  await navigate({
    path: item.path || props.taskPath,
    raw: item.raw,
  });
}

async function navigate(target: MangoWidgetNavigateTarget): Promise<void> {
  await props.runtime?.navigate?.(target);
}

function createEmptySummary(): WorkflowMyTaskSummary {
  return {
    total: 0,
    pending: 0,
    processing: 0,
    completed: 0,
    overdue: 0,
  };
}
</script>
