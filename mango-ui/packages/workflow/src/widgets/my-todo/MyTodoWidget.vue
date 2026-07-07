<template>
  <section class="mango-grid-widget-my-todo">
    <header class="mango-grid-widget-my-todo__header">
      <span>我的待办</span>
      <button
        v-if="!inlineState"
        class="mango-grid-widget-my-todo__all"
        type="button"
        @click="navigateToAll"
      >
        查看全部
      </button>
    </header>

    <div
      v-if="inlineState"
      class="mango-grid-widget-my-todo__state"
      :data-state="inlineState"
    >
      <strong>{{ inlineStateTitle }}</strong>
      <span>{{ inlineStateMessage }}</span>
    </div>

    <div
      v-else
      class="mango-grid-widget-my-todo__grid"
      :class="{ 'is-loading': loading }"
    >
      <button
        v-for="item in todoItems"
        :key="item.key"
        class="mango-grid-widget-my-todo__item"
        type="button"
        @click="navigateToItem(item)"
      >
        <span class="mango-grid-widget-my-todo__value">{{ item.value }}</span>
        <span class="mango-grid-widget-my-todo__label">{{ item.label }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { workflowApi, type WorkflowTaskSummary } from '../../api/workflow';
import { isAccessDeniedError } from '../access-error';
import type { MangoWidgetNavigateTarget, MyTodoWidgetProps } from '../types';

defineOptions({
  name: 'MangoMyTodoWidget',
});

const props = withDefaults(defineProps<MyTodoWidgetProps>(), {
  todoPath: '/workflow/task/todo',
});

const loading = ref(false);
const summary = ref<WorkflowTaskSummary>(createEmptySummary());
const inlineState = ref<'missing-permission' | 'load-failed' | ''>('');

const todoItems = computed(() => [
  {
    key: 'pendingApproval',
    label: '待审批',
    value: summary.value.pendingApproval,
    raw: { query: { todoType: 'ASSIGNED' } },
  },
  {
    key: 'pendingHandle',
    label: '待处理',
    value: summary.value.pendingHandle,
    raw: { query: { todoType: 'CLAIMABLE' } },
  },
  {
    key: 'pendingConfirm',
    label: '待确认',
    value: summary.value.pendingConfirm,
    path: '/workflow/task/copied',
    raw: { query: { unread: 'true' } },
  },
  {
    key: 'overdue',
    label: '已超时',
    value: summary.value.overdue,
    raw: { query: { overdue: 'true' } },
  },
]);
const inlineStateTitle = computed(() => inlineState.value === 'missing-permission' ? '缺少权限' : '加载失败');
const inlineStateMessage = computed(() => inlineState.value === 'missing-permission'
  ? '当前账号无权查看我的待办'
  : '待办统计加载失败，请稍后重试');

onMounted(() => {
  loadSummary();
});

async function loadSummary(): Promise<void> {
  loading.value = true;
  try {
    summary.value = await workflowApi.todoSummary();
    inlineState.value = '';
  } catch (error) {
    inlineState.value = isAccessDeniedError(error) ? 'missing-permission' : 'load-failed';
  } finally {
    loading.value = false;
  }
}

async function navigateToAll(): Promise<void> {
  await navigate({ path: props.todoPath });
}

async function navigateToItem(item: { path?: string; raw: Record<string, unknown> }): Promise<void> {
  await navigate({
    path: item.path || props.todoPath,
    raw: item.raw,
  });
}

async function navigate(target: MangoWidgetNavigateTarget): Promise<void> {
  await props.runtime?.navigate?.(target);
}

function createEmptySummary(): WorkflowTaskSummary {
  return {
    pendingApproval: 0,
    pendingHandle: 0,
    pendingConfirm: 0,
    overdue: 0,
  };
}
</script>
