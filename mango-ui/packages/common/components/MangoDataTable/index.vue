<template>
  <MangoListPanel class="mango-data-table" :class="{ 'mango-data-table--plain': !card }">
    <template v-if="hasActionsSlot" #actions>
      <slot name="actions" />
    </template>

    <template v-if="hasViewActions" #view-actions>
      <slot name="view-actions" />
      <el-segmented
        v-if="canSwitchMode"
        class="mango-data-table__mode-switch"
        :model-value="localMode"
        :options="modeOptions"
        size="small"
        aria-label="表格展示模式"
        @change="handleModeChange"
      >
        <template #default="{ item }">
          <el-tooltip :content="item.label" placement="top">
            <span class="mango-data-table__mode-option" :aria-label="item.label">
              <el-icon aria-hidden="true">
                <component :is="item.icon" />
              </el-icon>
            </span>
          </el-tooltip>
        </template>
      </el-segmented>
    </template>

    <div v-if="error && !loading" class="mango-data-table__state">
      <el-result icon="error" :title="errorTitle" :sub-title="error">
        <template v-if="retryable" #extra>
          <el-button type="primary" @click="emit('retry')">{{ retryText }}</el-button>
        </template>
      </el-result>
    </div>

    <el-table
      v-else
      ref="tableRef"
      v-loading="loading"
      v-bind="resolvedTableProps"
      :border="tableBorder"
      :data="rows"
      :row-key="rowKey"
      :header-cell-style="resolvedHeaderCellStyle"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="isExpandListMode" type="expand" width="48" fixed="left" align="center" :resizable="false">
        <template #default="scope">
          <dl class="mango-data-table__expand-list" :style="expandListStyle">
            <div v-for="column in expandableColumns" :key="column.field" class="mango-data-table__expand-item">
              <dt class="mango-data-table__expand-label">{{ column.label }}：</dt>
              <dd class="mango-data-table__expand-value">
                <MangoTableCell
                  :row="scope.row"
                  :row-index="scope.$index"
                  :column="column"
                  :empty-text="expand?.emptyText ?? '-'"
                  @action="handleColumnAction(column.field, scope.row, scope.$index)"
                  @change="emit('cell-change', $event)"
                >
                  <slot
                    :name="resolveSlotName(column)"
                    :column="column"
                    :field="column.field"
                    :row="scope.row"
                    :row-index="scope.$index"
                    :value="resolveFieldValue(scope.row, column.field)"
                  />
                </MangoTableCell>
              </dd>
            </div>
          </dl>
        </template>
      </el-table-column>

      <el-table-column
        v-if="showSelection"
        type="selection"
        width="48"
        align="center"
        :selectable="selectable"
        :resizable="tableResizable"
      />

      <el-table-column
        v-if="showIndex"
        type="index"
        label="序号"
        width="64"
        align="center"
        :index="resolveSerialNumber"
        :resizable="tableResizable"
      />

      <el-table-column
        v-for="column in mainColumns"
        :key="column.field"
        :prop="column.field"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :fixed="column.fixed"
        :align="column.align ?? 'left'"
        :sortable="column.sortable"
        :resizable="resolveColumnResizable(column)"
        :show-overflow-tooltip="column.showOverflowTooltip"
      >
        <template #default="scope">
          <MangoTableCell
            :row="scope.row"
            :row-index="scope.$index"
            :column="column"
            @action="handleColumnAction(column.field, scope.row, scope.$index)"
            @change="emit('cell-change', $event)"
          >
            <slot
              :name="resolveSlotName(column)"
              :column="column"
              :field="column.field"
              :row="scope.row"
              :row-index="scope.$index"
              :value="resolveFieldValue(scope.row, column.field)"
            />
          </MangoTableCell>
        </template>
      </el-table-column>

      <el-table-column
        v-if="showOperationColumn"
        :label="operation?.label ?? '操作'"
        :width="operation?.width"
        :min-width="operation?.minWidth"
        :fixed="operation?.fixed ?? 'right'"
        :align="operation?.align ?? 'left'"
        :resizable="operation?.resizable ?? false"
      >
        <template #default="scope">
          <div class="mango-data-table__operations">
            <el-button
              v-for="action in directActions(scope.row, scope.$index)"
              :key="action.key"
              v-bind="action.props"
              link
              :type="action.tone ?? 'primary'"
              :disabled="resolveActionCondition(action.disabled, scope.row, scope.$index)"
              :loading="resolveActionCondition(action.loading, scope.row, scope.$index)"
              @click.stop="handleAction(action.key, scope.row, scope.$index)"
            >
              {{ action.text }}
            </el-button>

            <el-dropdown
              v-if="overflowActions(scope.row, scope.$index).length"
              @command="handleDropdownCommand($event, scope.row, scope.$index)"
            >
              <el-button type="primary" link>更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="action in overflowActions(scope.row, scope.$index)"
                    :key="action.key"
                    :command="action.key"
                    :disabled="
                      resolveActionCondition(action.disabled, scope.row, scope.$index) ||
                      resolveActionCondition(action.loading, scope.row, scope.$index)
                    "
                  >
                    {{ action.text }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>

      <template #empty>
        <span class="mango-data-table__empty-text">{{ emptyText }}</span>
      </template>
    </el-table>

    <template v-if="showPagination" #pagination>
      <Pagination
        v-bind="pagination?.props"
        :page="pagination?.page ?? 1"
        :limit="pagination?.limit ?? 20"
        :total="pagination?.total ?? 0"
        :page-sizes="pagination?.pageSizes"
        :align="pagination?.align"
        :layout="pagination?.layout"
        @pagination="emit('page-change', $event)"
      />
    </template>
  </MangoListPanel>
</template>

<script setup lang="ts" generic="Row extends object">
defineOptions({ name: 'MangoDataTable' });

import { Connection, Grid } from '@element-plus/icons-vue';
import type { TableInstance } from 'element-plus';
import { computed, ref, useSlots, watch } from 'vue';
import type { Component } from 'vue';
import MangoListPanel from '../MangoListPanel/index.vue';
import Pagination from '../Pagination/index.vue';
import MangoTableCell from './MangoTableCell.vue';
import type {
  MangoDataTableEmits,
  MangoDataTableProps,
  MangoTableAction,
  MangoTableColumn,
  MangoTableCondition,
  MangoTableHeaderCellStyle,
  MangoTableMode,
  MangoTableOperation,
} from './types';

const props = withDefaults(defineProps<MangoDataTableProps<Row>>(), {
  loading: false,
  error: '',
  errorTitle: '列表加载失败',
  retryText: '重试',
  retryable: false,
  emptyText: '暂无数据',
  card: true,
  resizable: true,
  mode: 'flat',
  showModeSwitch: false,
  flatModeLabel: '平铺模式',
  expandModeLabel: '展开模式',
  showIndex: true,
  showSelection: false,
});
const emit = defineEmits<MangoDataTableEmits<Row>>();
const slots = useSlots();
const tableRef = ref<TableInstance>();
const localMode = ref<MangoTableMode>(props.mode);

const modeOptions = computed(
  () =>
    [
      { label: props.flatModeLabel, value: 'flat' as const, icon: Grid },
      { label: props.expandModeLabel, value: 'expand' as const, icon: Connection },
    ] satisfies Array<{ label: string; value: MangoTableMode; icon: Component }>,
);
const hasActionsSlot = computed(() => Boolean(slots.actions));
const visibleColumns = computed(() => props.columns.filter((column) => !column.hidden));
const tableResizable = computed(() => props.resizable);
const resolvedHeaderCellStyle = computed<MangoTableHeaderCellStyle>(() => ({
  backgroundColor: 'var(--mango-table-header-bg, #eef1f5)',
  color: 'var(--mango-table-header-text, #000000)',
  fontWeight: 600,
  padding: '16px 0',
  ...props.headerCellStyle,
}));
const resolvedTableProps = computed(() => {
  const tableProps = props.tableProps ?? {};
  if ('headerCellStyle' in tableProps || 'header-cell-style' in tableProps) {
    throw new Error('MangoDataTable: use the headerCellStyle prop instead of tableProps.');
  }
  return tableProps;
});
const tableBorder = computed(() => {
  const border = props.tableProps?.border;
  return typeof border === 'boolean' ? border : true;
});
const expandableColumns = computed(() => visibleColumns.value.filter((column) => column.expandable));
const expandType = computed(() => props.expand?.type ?? 'list');
const canSwitchMode = computed(
  () => props.showModeSwitch && expandType.value === 'list' && expandableColumns.value.length > 0,
);
const hasViewActions = computed(() => Boolean(slots['view-actions']) || canSwitchMode.value);
const isExpandListMode = computed(
  () => localMode.value === 'expand' && expandType.value === 'list' && expandableColumns.value.length > 0,
);
const mainColumns = computed(() =>
  isExpandListMode.value ? visibleColumns.value.filter((column) => !column.expandable) : visibleColumns.value,
);
const expandListStyle = computed(() => ({
  '--mango-table-expand-label-width': resolveCssLength(props.expand?.labelWidth ?? 120),
}));
const operation = computed<MangoTableOperation<Row> | undefined>(() => props.operation);
const showOperationColumn = computed(
  () => Boolean(operation.value) && operation.value?.visible !== false && Boolean(operation.value?.actions.length),
);
const showPagination = computed(() => Boolean(props.pagination && props.pagination.visible !== false && !props.error));

watch(
  () => props.mode,
  (mode) => {
    localMode.value = mode;
  },
);

function resolveSlotName(column: MangoTableColumn<Row>) {
  return column.slot ?? column.field;
}

function resolveCssLength(value: number | string) {
  return typeof value === 'number' ? `${value}px` : value;
}

function resolveFieldValue(row: Row, path: string): unknown {
  return path.split('.').reduce<unknown>((current, key) => {
    if (typeof current !== 'object' || current === null) return undefined;
    return (current as Record<string, unknown>)[key];
  }, row);
}

function resolveSerialNumber(index: number) {
  if (!props.pagination) return index + 1;
  return (props.pagination.page - 1) * props.pagination.limit + index + 1;
}

function resolveColumnResizable(column: MangoTableColumn<Row>) {
  return column.resizable ?? (tableResizable.value && !column.fixed);
}

function resolveActionCondition(
  condition: MangoTableCondition<Row> | undefined,
  row: Row,
  rowIndex: number,
  fallback = false,
) {
  return typeof condition === 'function' ? condition({ row, rowIndex }) : (condition ?? fallback);
}

function visibleActions(row: Row, rowIndex: number) {
  return operation.value?.actions.filter((action) => resolveActionCondition(action.visible, row, rowIndex, true)) ?? [];
}

function directActions(row: Row, rowIndex: number): MangoTableAction<Row>[] {
  return visibleActions(row, rowIndex).slice(0, operation.value?.moreCount ?? 3);
}

function overflowActions(row: Row, rowIndex: number): MangoTableAction<Row>[] {
  return visibleActions(row, rowIndex).slice(operation.value?.moreCount ?? 3);
}

function handleColumnAction(key: string, row: Row, rowIndex: number) {
  handleAction(key, row, rowIndex);
}

function handleAction(key: string, row: Row, rowIndex: number) {
  emit('action', { key, row, rowIndex, tableKey: props.tableKey });
}

function handleDropdownCommand(command: unknown, row: Row, rowIndex: number) {
  if (typeof command === 'string') handleAction(command, row, rowIndex);
}

function handleSelectionChange(rows: Row[]) {
  emit('selection-change', rows);
}

function handleModeChange(value: MangoTableMode) {
  if (value === localMode.value) return;
  localMode.value = value;
  if (value === 'flat') collapseAllRows();
  emit('update:mode', value);
  emit('mode-change', value);
}

function collapseAllRows() {
  props.rows.forEach((row) => tableRef.value?.toggleRowExpansion(row, false));
}

defineExpose({
  tableRef,
  clearSelection: () => tableRef.value?.clearSelection(),
  toggleRowSelection: (row: Row, selected?: boolean) => tableRef.value?.toggleRowSelection(row, selected),
  toggleRowExpansion: (row: Row, expanded?: boolean) => tableRef.value?.toggleRowExpansion(row, expanded),
});
</script>

<style scoped>
.mango-data-table.mango-data-table--plain {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.mango-data-table :deep(.el-table) {
  width: 100%;
}

.mango-data-table :deep(.el-table__cell) {
  border-right: 0;
}

.mango-data-table :deep(.el-table__body .el-table__cell) {
  padding: 16px 0;
  font-size: 14px;
  line-height: 20px;
}

.mango-data-table :deep(.el-table__empty-text) {
  width: 100%;
}

.mango-data-table__empty-text {
  color: var(--mango-text-color-placeholder);
  font-size: 14px;
  line-height: 20px;
}

.mango-data-table__state {
  min-height: 280px;
  display: grid;
  place-items: center;
}

.mango-data-table__mode-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  font-size: 16px;
  line-height: 1;
}

.mango-data-table__mode-switch {
  --el-border-radius-base: 6px;
}

.mango-data-table__expand-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  margin: 0 0 0 15px;
  padding: 16px 56px;
  background: var(--mango-table-expand-bg);
  border-radius: 6px;
}

.mango-data-table__expand-item {
  display: grid;
  grid-template-columns: var(--mango-table-expand-label-width) minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  min-width: 0;
  padding: 10px 0;
}

.mango-data-table__expand-label {
  margin: 0;
  color: var(--mango-table-header-text, #000);
  white-space: nowrap;
}

.mango-data-table__expand-value {
  margin: 0;
  min-width: 0;
  color: var(--mango-text-color);
  overflow-wrap: anywhere;
}

.mango-data-table__operations {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.mango-data-table :deep(.el-table__expanded-cell) {
  padding: 0;
}

.mango-data-table :deep(.el-table__expand-icon > .el-icon) {
  font-size: 16px;
}

@media (width <= 768px) {
  .mango-data-table__expand-list {
    padding: 16px 20px;
  }

  .mango-data-table__expand-item {
    grid-template-columns: minmax(88px, var(--mango-table-expand-label-width)) minmax(0, 1fr);
  }
}
</style>
