<template>
  <slot
    v-if="column.type === 'custom'"
    :column="column"
    :field="column.field"
    :row="row"
    :row-index="rowIndex"
    :value="rawValue"
  />

  <MangoStatusText v-else-if="column.type === 'status'" :tone="matchedOption?.tone">
    {{ displayText }}
  </MangoStatusText>

  <el-tag v-else-if="column.type === 'tag'" v-bind="column.props" :type="tagTone">
    {{ displayText }}
  </el-tag>

  <el-button
    v-else-if="column.type === 'button'"
    v-bind="column.props"
    type="primary"
    link
    :disabled="isDisabled"
    :loading="column.loading"
    @click="emit('action')"
  >
    {{ displayText === emptyText ? column.label : displayText }}
  </el-button>

  <el-input
    v-else-if="column.type === 'input'"
    v-bind="column.props"
    :disabled="isDisabled"
    :model-value="inputValue"
    @focus="beginEditing"
    @update:model-value="handleValueUpdate"
    @change="commitChange"
    @blur="handleBlur"
  />

  <el-select
    v-else-if="column.type === 'select'"
    v-bind="column.props"
    :disabled="isDisabled"
    :loading="column.loading"
    :model-value="rawValue"
    @visible-change="handleSelectVisibleChange"
    @update:model-value="handleValueUpdate"
    @change="commitChange"
  >
    <el-option
      v-for="option in column.options ?? []"
      :key="String(option.value)"
      :label="option.label"
      :value="option.value"
      :disabled="option.disabled"
    />
  </el-select>

  <el-radio-group
    v-else-if="column.type === 'radio'"
    v-bind="column.props"
    :disabled="isDisabled"
    :model-value="rawValue"
    @update:model-value="handleValueUpdate"
    @change="commitChange"
  >
    <el-radio
      v-for="option in column.options ?? []"
      :key="String(option.value)"
      :value="option.value"
      :disabled="option.disabled"
    >
      {{ option.label }}
    </el-radio>
  </el-radio-group>

  <template v-else>{{ displayText }}</template>
</template>

<script setup lang="ts" generic="Row extends object">
import { computed, ref } from 'vue';
import MangoStatusText from '../MangoStatusText/index.vue';
import type { MangoTableCellChangeContext, MangoTableCellContext, MangoTableColumn } from './types';

const props = withDefaults(
  defineProps<{
    row: Row;
    rowIndex: number;
    column: MangoTableColumn<Row>;
    emptyText?: string;
  }>(),
  {
    emptyText: '-',
  },
);

const emit = defineEmits<{
  (event: 'change', context: MangoTableCellChangeContext<Row>): void;
  (event: 'action'): void;
}>();

const editing = ref(false);
const dirty = ref(false);
const previousValue = ref<unknown>();

const rawValue = computed(() => getByPath(props.row, props.column.field));
const cellContext = computed<MangoTableCellContext<Row>>(() => ({
  row: props.row,
  rowIndex: props.rowIndex,
  column: props.column,
  field: props.column.field,
  value: rawValue.value,
}));
const matchedOption = computed(() => props.column.options?.find((option) => option.value === rawValue.value));
const tagTone = computed(() => {
  const tone = matchedOption.value?.tone;
  return tone === 'neutral' ? undefined : tone;
});
const formattedValue = computed(() =>
  props.column.formatter ? props.column.formatter(cellContext.value) : (matchedOption.value?.label ?? rawValue.value),
);
const displayText = computed(() =>
  isEmptyValue(formattedValue.value) ? (props.column.emptyText ?? props.emptyText) : String(formattedValue.value),
);
const inputValue = computed(() => {
  const value = rawValue.value;
  return typeof value === 'string' || typeof value === 'number' ? value : '';
});
const isDisabled = computed(() => resolveCellCondition(props.column.disabled, cellContext.value));

function getByPath(target: object, path: string): unknown {
  return path.split('.').reduce<unknown>((current, key) => {
    if (!isRecord(current)) return undefined;
    return current[key];
  }, target);
}

function setByPath(target: object, path: string, value: unknown) {
  const keys = path.split('.');
  const lastKey = keys.pop();
  if (!lastKey) return;

  let current = target as Record<string, unknown>;
  keys.forEach((key) => {
    if (!isRecord(current[key])) current[key] = {};
    current = current[key] as Record<string, unknown>;
  });
  current[lastKey] = value;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isEmptyValue(value: unknown) {
  return value === undefined || value === null || value === '';
}

function resolveCellCondition(condition: MangoTableColumn<Row>['disabled'], context: MangoTableCellContext<Row>) {
  return typeof condition === 'function' ? condition(context) : (condition ?? false);
}

function beginEditing() {
  if (editing.value) return;
  previousValue.value = rawValue.value;
  editing.value = true;
}

function handleValueUpdate(value: unknown) {
  beginEditing();
  dirty.value = true;
  setByPath(props.row, props.column.field, value);
}

function commitChange() {
  if (!dirty.value) return;
  emit('change', {
    ...cellContext.value,
    value: rawValue.value,
    previousValue: previousValue.value,
  });
  dirty.value = false;
  editing.value = false;
}

function handleBlur() {
  commitChange();
  editing.value = false;
}

function handleSelectVisibleChange(visible: boolean) {
  if (visible) beginEditing();
  else editing.value = false;
}
</script>
