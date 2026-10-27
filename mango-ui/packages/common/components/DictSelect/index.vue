<template>
  <div class="dict-select">
    <el-select
      v-if="shape === 'FLAT'"
      ref="flatSelectRef"
      class="dict-select__control"
      :model-value="modelValue"
      :placeholder="placeholder"
      :clearable="clearable"
      :disabled="disabled"
      :loading="loading"
      :multiple="multiple"
      :filterable="filterable"
      :collapse-tags="collapseTags"
      :collapse-tags-tooltip="collapseTagsTooltip"
      :multiple-limit="multipleLimit"
      :reserve-keyword="reserveKeyword"
      @update:model-value="handleUpdate"
      @change="handleChange"
      @clear="handleClear"
    >
      <el-option v-if="showAnyOption && !multiple" :label="anyOptionLabel" :value="anyOptionValue" />
      <el-option
        v-for="item in flatOptions"
        :key="item.value"
        :label="item.label"
        :value="numberValue ? Number(item.value) : item.value"
      />
    </el-select>

    <el-tree-select
      v-else
      ref="treeSelectRef"
      class="dict-select__control"
      :model-value="modelValue"
      :data="treeOptions"
      :props="treeProps"
      :multiple="multiple"
      :show-checkbox="multiple"
      :check-strictly="checkStrictly"
      :check-on-click-node="!multiple"
      :default-expand-all="defaultExpandAll"
      :placeholder="placeholder"
      :clearable="clearable"
      :disabled="disabled"
      :filterable="filterable"
      :collapse-tags="collapseTags"
      :collapse-tags-tooltip="collapseTagsTooltip"
      :loading="loading"
      :no-data-text="noDataText"
      :render-after-expand="false"
      @update:model-value="handleUpdate"
      @change="handleChange"
      @clear="handleClear"
      @check-change="handleCheckChange"
    >
      <template #label="{ label, value }">
        <slot
          name="selected"
          :value="value"
          :label="label"
          :text="formatTreeLabel(value, label)"
        >
          {{ formatTreeLabel(value, label) }}
        </slot>
      </template>
    </el-tree-select>
    <el-alert
      v-if="errorMessage"
      class="dict-select__error"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
    />
  </div>
</template>

<script setup lang="ts" name="DictSelect">
import { computed, ref, watch } from 'vue';
import { listDictOptions, listDictTree, type DictOption, type DictTreeOption } from '../../api/dict';

export type DictShape = 'FLAT' | 'TREE';
export type DictSelectValue = string | number | Array<string | number> | undefined;
export type DictDisplayFormat = 'label' | 'path';

interface DictSelectTreeOption extends DictTreeOption {
  disabled?: boolean;
  children?: DictSelectTreeOption[];
}

interface SelectInstance {
  focus: () => void;
  blur: () => void;
}

interface TreeSelectInstance extends SelectInstance {
  getCheckedNodes: (leafOnly?: boolean, includeHalfChecked?: boolean) => DictTreeOption[];
  getHalfCheckedNodes?: () => DictTreeOption[];
}

const props = withDefaults(defineProps<{
  modelValue?: DictSelectValue;
  dictType: string;
  shape?: DictShape;
  placeholder?: string;
  clearable?: boolean;
  disabled?: boolean;
  multiple?: boolean;
  filterable?: boolean;
  numberValue?: boolean;
  showAnyOption?: boolean;
  anyOptionLabel?: string;
  anyOptionValue?: string | number;
  collapseTags?: boolean;
  collapseTagsTooltip?: boolean;
  multipleLimit?: number;
  reserveKeyword?: boolean;
  leafOnly?: boolean;
  checkStrictly?: boolean;
  defaultExpandAll?: boolean;
  displayFormat?: DictDisplayFormat;
  displaySeparator?: string;
  noDataText?: string;
}>(), {
  modelValue: undefined,
  shape: 'FLAT',
  placeholder: '请选择',
  clearable: true,
  disabled: false,
  multiple: false,
  filterable: false,
  numberValue: false,
  showAnyOption: false,
  anyOptionLabel: '不限',
  anyOptionValue: '__ALL__',
  collapseTags: true,
  collapseTagsTooltip: true,
  multipleLimit: 0,
  reserveKeyword: false,
  leafOnly: true,
  checkStrictly: false,
  defaultExpandAll: true,
  displayFormat: 'label',
  displaySeparator: ' / ',
  noDataText: '暂无数据',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: DictSelectValue): void;
  (event: 'change', value: DictSelectValue, nodes: DictTreeOption[], paths: DictTreeOption[][]): void;
  (event: 'clear'): void;
  (event: 'check-change', node: DictTreeOption, checked: boolean, indeterminate: boolean): void;
  (event: 'error', error: unknown): void;
}>();

const flatOptions = ref<DictOption[]>([]);
const treeOptions = ref<DictSelectTreeOption[]>([]);
const loading = ref(false);
const errorMessage = ref('');
const flatSelectRef = ref<SelectInstance>();
const treeSelectRef = ref<TreeSelectInstance>();

const treeProps = computed(() => ({
  value: 'value',
  label: 'label',
  children: 'children',
  disabled: 'disabled',
}));

const selectedNodes = computed(() => {
  if (props.shape === 'FLAT') {
    return flatOptions.value.filter((item) => selectedValues.value.includes(item.value));
  }
  return selectedPaths.value.map((path) => path.at(-1)).filter((node): node is DictTreeOption => Boolean(node));
});
const selectedPaths = computed(() => {
  if (props.shape === 'FLAT') return [];
  return selectedValues.value.map((value) => findNodePath(treeOptions.value, String(value)));
});
const selectedValues = computed(() => {
  if (props.modelValue === undefined || props.modelValue === '') return [];
  return Array.isArray(props.modelValue) ? props.modelValue.map(String) : [String(props.modelValue)];
});
async function reload(force = false) {
  if (!props.dictType) {
    flatOptions.value = [];
    treeOptions.value = [];
    return [];
  }

  loading.value = true;
  errorMessage.value = '';
  try {
    if (props.shape === 'TREE') {
      const data = await listDictTree(props.dictType, force);
      treeOptions.value = mapTreeOptions(data, props.leafOnly);
      return treeOptions.value;
    }
    flatOptions.value = await listDictOptions(props.dictType, force);
    return flatOptions.value;
  } catch (error) {
    flatOptions.value = [];
    treeOptions.value = [];
    errorMessage.value = error instanceof Error ? error.message : '字典加载失败，请检查字典编码和权限。';
    emit('error', error);
    return [];
  } finally {
    loading.value = false;
  }
}

function mapTreeOptions(nodes: DictTreeOption[], leafOnly: boolean): DictSelectTreeOption[] {
  return nodes.map((node) => ({
    ...node,
    disabled: leafOnly && Boolean(node.children?.length),
    children: node.children ? mapTreeOptions(node.children, leafOnly) : undefined,
  }));
}

function handleUpdate(value: DictSelectValue) {
  emit('update:modelValue', normalizeValue(value));
}

function handleChange(value: DictSelectValue) {
  const normalized = normalizeValue(value);
  const selection = resolveSelection(normalized);
  emit('change', normalized, selection.nodes, selection.paths);
}

function handleClear() {
  emit('clear');
}

function formatTreeLabel(value: string | number, fallbackLabel: string) {
  if (props.displayFormat !== 'path') return fallbackLabel;
  const path = findNodePath(treeOptions.value, String(value));
  return path.length > 0 ? path.map((node) => node.label).join(props.displaySeparator) : fallbackLabel;
}

function handleCheckChange(node: DictTreeOption, checked: boolean, indeterminate: boolean) {
  emit('check-change', node, checked, indeterminate);
  emitHalfCheckedChange();
}

/**
 * Tree 只在 checked 状态变化时触发 check-change，父节点进入/退出半选不会通知；
 * 这里对比半选节点快照，只补发发生变化的父节点，避免重复或回头事件。
 */
let halfCheckedValues = new Set<string>();

function emitHalfCheckedChange() {
  const current = new Map<string, DictTreeOption>();
  for (const node of treeSelectRef.value?.getHalfCheckedNodes?.() ?? []) {
    current.set(String(node.value), node);
  }
  for (const [value, node] of current) {
    if (!halfCheckedValues.has(value)) {
      emit('check-change', node, false, true);
    }
  }
  for (const value of halfCheckedValues) {
    if (!current.has(value)) {
      const previous = findNodePath(treeOptions.value, value).at(-1);
      if (previous) emit('check-change', previous, false, false);
    }
  }
  halfCheckedValues = new Set(current.keys());
}

function normalizeValue(value: DictSelectValue): DictSelectValue {
  if (props.showAnyOption && !props.multiple && value === props.anyOptionValue) return undefined;
  if (props.shape === 'FLAT' && props.numberValue && Array.isArray(value)) return value.map(Number);
  if (props.shape === 'FLAT' && props.numberValue && value !== undefined && value !== '') return Number(value);
  return value;
}

function resolveSelection(value: DictSelectValue) {
  if (props.shape === 'FLAT') {
    return {
      nodes: flatOptions.value.filter((item) => normalizeValueArray(value).includes(item.value)),
      paths: [] as DictTreeOption[][],
    };
  }
  const paths = normalizeValueArray(value)
    .map((item) => findNodePath(treeOptions.value, item))
    .filter((path) => path.length > 0);
  return {
    nodes: paths.map((path) => path.at(-1)).filter((node): node is DictTreeOption => Boolean(node)),
    paths,
  };
}

function normalizeValueArray(value: DictSelectValue) {
  if (value === undefined || value === '') return [];
  return (Array.isArray(value) ? value : [value]).map(String);
}

function getSelectedNodes() {
  return selectedNodes.value;
}

function getSelectedPaths() {
  return selectedPaths.value;
}

function getHalfCheckedNodes() {
  if (props.shape !== 'TREE') return [];
  return treeSelectRef.value?.getHalfCheckedNodes?.() ?? [];
}

function clear() {
  emit('update:modelValue', props.multiple ? [] : undefined);
  halfCheckedValues.clear();
  emit('clear');
}

function focus() {
  if (props.shape === 'TREE') treeSelectRef.value?.focus();
  else flatSelectRef.value?.focus();
}

function blur() {
  if (props.shape === 'TREE') treeSelectRef.value?.blur();
  else flatSelectRef.value?.blur();
}

function findNodePath(nodes: DictSelectTreeOption[], value: string, parents: DictTreeOption[] = []): DictTreeOption[] {
  for (const node of nodes) {
    const currentPath = [...parents, node];
    if (node.value === value) return currentPath;
    const childPath = findNodePath(node.children ?? [], value, currentPath);
    if (childPath.length > 0) return childPath;
  }
  return [];
}

watch(() => [props.dictType, props.shape, props.leafOnly], () => {
  void reload();
}, { immediate: true });

defineExpose({ reload, getSelectedNodes, getSelectedPaths, getHalfCheckedNodes, clear, focus, blur });
</script>

<style scoped>
.dict-select__control {
  min-width: 180px;
}

.dict-select__error {
  margin-top: 8px;
}
</style>
