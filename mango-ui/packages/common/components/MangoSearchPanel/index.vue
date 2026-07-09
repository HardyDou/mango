<template>
  <section class="mango-search-panel" :class="{ 'mango-search-panel--more-bottom': morePlacement === 'bottom' }" data-surface="search">
    <el-form class="mango-search-panel__form" :model="model" :label-width="labelWidth" @submit.prevent>
      <div
        ref="fieldsRef"
        class="mango-search-panel__fields"
        :class="fieldsClasses"
        :style="fieldsStyle"
      >
        <slot />
      </div>
      <div class="mango-search-panel__actions">
        <el-form-item label-width="0">
          <slot name="actions">
            <el-button type="primary" :icon="Search" @click="emit('search')">
              {{ searchText }}
            </el-button>
            <el-button v-if="showReset" :icon="Refresh" @click="emit('reset')">
              {{ resetText }}
            </el-button>
            <el-button
              v-if="showActionMoreButton"
              link
              type="primary"
              :icon="expanded ? ArrowUp : ArrowDown"
              @click="toggleExpanded"
            >
              {{ expanded ? collapseText : expandText }}
            </el-button>
          </slot>
        </el-form-item>
      </div>
    </el-form>
    <div v-if="showBottomMoreButton" class="mango-search-panel__more">
      <el-button
        link
        type="primary"
        :icon="expanded ? ArrowUp : ArrowDown"
        @click="toggleExpanded"
      >
        {{ expanded ? collapseText : expandText }}
      </el-button>
    </div>
  </section>
</template>

<script setup lang="ts" name="MangoSearchPanel">
import { ArrowDown, ArrowUp, Refresh, Search } from '@element-plus/icons-vue';
import { computed, nextTick, onBeforeUnmount, onMounted, onUpdated, ref, watch } from 'vue';

type SearchPanelColumns = number | 'auto';
type SearchPanelMorePlacement = 'actions' | 'bottom';

const props = withDefaults(defineProps<{
  model?: Record<string, unknown>;
  labelWidth?: string | number;
  searchText?: string;
  resetText?: string;
  showReset?: boolean;
  collapsible?: boolean;
  defaultExpanded?: boolean;
  collapsedRows?: number;
  collapsedCount?: number;
  expandText?: string;
  collapseText?: string;
  columns?: SearchPanelColumns;
  morePlacement?: SearchPanelMorePlacement;
  fieldMinWidth?: string;
  fieldMaxWidth?: string;
}>(), {
  model: undefined,
  labelWidth: '96px',
  searchText: '查询',
  resetText: '重置',
  showReset: true,
  collapsible: false,
  defaultExpanded: false,
  collapsedRows: 1,
  collapsedCount: undefined,
  columns: 'auto',
  morePlacement: 'actions',
  fieldMinWidth: '280px',
  fieldMaxWidth: '320px',
  expandText: '展开',
  collapseText: '收起',
});

const emit = defineEmits<{
  search: [];
  reset: [];
  expandChange: [expanded: boolean];
}>();

const fieldsRef = ref<HTMLElement>();
const expanded = ref(props.defaultExpanded);
const hasOverflow = ref(false);
const fieldsWidth = ref(0);
let resizeObserver: ResizeObserver | undefined;

const configuredColumns = computed(() => (typeof props.columns === 'number' && props.columns > 0 ? Math.floor(props.columns) : undefined));
const fixedColumns = computed(() => configuredColumns.value !== undefined);

// 固定列模式会跟随容器宽度降列，保证 collapsedRows 对应真实可见行数。
const renderedColumns = computed(() => {
  const columns = configuredColumns.value;
  if (!columns) {
    return undefined;
  }
  if (fieldsWidth.value > 0 && fieldsWidth.value <= 640) {
    return 1;
  }
  if (fieldsWidth.value > 0 && fieldsWidth.value <= 960) {
    return Math.min(columns, 2);
  }
  return columns;
});

const fieldsClasses = computed(() => ({
  'mango-search-panel__fields--auto': !fixedColumns.value,
  'mango-search-panel__fields--fixed': fixedColumns.value,
}));

const fieldsStyle = computed(() => ({
  '--mango-search-columns': String(renderedColumns.value ?? 1),
  '--mango-search-field-min-width': props.fieldMinWidth,
  '--mango-search-field-max-width': props.fieldMaxWidth,
}));

const showActionMoreButton = computed(() => props.collapsible && hasOverflow.value && props.morePlacement === 'actions');
const showBottomMoreButton = computed(() => props.collapsible && hasOverflow.value && props.morePlacement === 'bottom');

const getFieldItems = () => {
  const fields = fieldsRef.value;
  if (!fields) {
    return [];
  }
  return Array.from(fields.children).filter((element): element is HTMLElement => element instanceof HTMLElement);
};

const clearHiddenState = (items = getFieldItems()) => {
  items.forEach((item) => {
    item.removeAttribute('data-mango-search-hidden');
    item.removeAttribute('aria-hidden');
  });
};

const resolveCollapsedCount = () => {
  if (props.collapsedCount && props.collapsedCount > 0) {
    return props.collapsedCount;
  }

  if (renderedColumns.value) {
    return Math.max(1, renderedColumns.value * props.collapsedRows);
  }

  const fields = fieldsRef.value;
  if (!fields) {
    return Math.max(1, props.collapsedRows);
  }

  const templateColumns = window.getComputedStyle(fields).gridTemplateColumns;
  const columnCount = templateColumns && templateColumns !== 'none'
    ? templateColumns.split(' ').filter(Boolean).length
    : 1;

  return Math.max(1, columnCount * props.collapsedRows);
};

const applyCollapse = () => {
  const items = getFieldItems();
  if (!props.collapsible || items.length === 0) {
    hasOverflow.value = false;
    clearHiddenState(items);
    return;
  }

  const visibleCount = resolveCollapsedCount();
  hasOverflow.value = items.length > visibleCount;

  items.forEach((item, index) => {
    const shouldHide = hasOverflow.value && !expanded.value && index >= visibleCount;
    if (shouldHide) {
      item.setAttribute('data-mango-search-hidden', 'true');
      item.setAttribute('aria-hidden', 'true');
    } else {
      item.removeAttribute('data-mango-search-hidden');
      item.removeAttribute('aria-hidden');
    }
  });
};

const scheduleApplyCollapse = () => {
  void nextTick(applyCollapse);
};

const updateFieldsWidth = () => {
  fieldsWidth.value = fieldsRef.value?.clientWidth ?? 0;
};

const toggleExpanded = () => {
  expanded.value = !expanded.value;
  emit('expandChange', expanded.value);
};

watch(
  () => [
    props.collapsible,
    props.collapsedRows,
    props.collapsedCount,
    props.columns,
    props.morePlacement,
    fieldsWidth.value,
    expanded.value,
  ],
  scheduleApplyCollapse,
);

onMounted(() => {
  updateFieldsWidth();
  scheduleApplyCollapse();
  if (typeof ResizeObserver !== 'undefined' && fieldsRef.value) {
    resizeObserver = new ResizeObserver(() => {
      updateFieldsWidth();
      scheduleApplyCollapse();
    });
    resizeObserver.observe(fieldsRef.value);
  }
});

onUpdated(() => {
  updateFieldsWidth();
  scheduleApplyCollapse();
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
});
</script>

<style scoped>
.mango-search-panel {
  width: 100%;
  padding: 14px 16px 2px;
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-light);
  border-radius: 6px;
  box-shadow: var(--mango-shadow-light);
}

.mango-search-panel--more-bottom {
  padding-bottom: 10px;
}

.mango-search-panel__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px 16px;
  align-items: stretch;
}

.mango-search-panel__fields {
  display: grid;
  gap: 12px 16px;
  min-width: 0;
}

.mango-search-panel__fields--auto {
  grid-template-columns: repeat(auto-fill, minmax(var(--mango-search-field-min-width), var(--mango-search-field-max-width)));
  justify-content: start;
}

.mango-search-panel__fields--fixed {
  grid-template-columns: repeat(var(--mango-search-columns), minmax(0, 1fr));
}

.mango-search-panel__actions {
  display: flex;
  align-self: end;
  justify-content: flex-end;
  min-width: 148px;
}

.mango-search-panel :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 12px;
}

.mango-search-panel__fields :deep([data-mango-search-hidden='true']) {
  display: none !important;
}

.mango-search-panel__fields :deep(.el-form-item),
.mango-search-panel__fields :deep(.el-form-item__content) {
  min-width: 0;
}

.mango-search-panel__fields :deep(.el-form-item__content) {
  flex: 1;
}

.mango-search-panel__fields :deep(.el-input),
.mango-search-panel__fields :deep(.el-select),
.mango-search-panel__fields :deep(.el-date-editor),
.mango-search-panel__fields :deep(.el-cascader) {
  width: 100%;
  min-width: 0;
}

.mango-search-panel__actions :deep(.el-form-item__content) {
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.mango-search-panel__more {
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

@media (max-width: 960px) {
  .mango-search-panel__form {
    grid-template-columns: 1fr;
  }

  .mango-search-panel__actions {
    justify-content: flex-end;
    min-width: 0;
  }

  .mango-search-panel__actions :deep(.el-form-item__content) {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .mango-search-panel {
    padding: 12px 12px 0;
  }

  .mango-search-panel__fields {
    grid-template-columns: 1fr;
  }
}
</style>
