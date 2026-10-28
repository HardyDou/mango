<template>
  <span class="dict-display" :data-mode="mode">
    <template v-if="items.length > 0 && mode === 'tag'">
      <el-tag
        v-for="item in items"
        :key="item.value"
        class="dict-display__tag"
        :type="tagType || undefined"
        :effect="tagEffect"
        :size="tagSize"
        disable-transitions
      >
        {{ item.text }}
      </el-tag>
    </template>
    <template v-else-if="items.length > 0">
      <template v-for="(item, index) in items" :key="item.value">
        <span v-if="index > 0" class="dict-display__separator">{{ separator }}</span>
        <span class="dict-display__item" :data-known="item.known">{{ item.text }}</span>
      </template>
    </template>
    <span v-else class="dict-display__empty">{{ emptyText }}</span>
  </span>
</template>

<script setup lang="ts" name="DictDisplay">
import { computed, ref, watch } from 'vue';
import { listDictOptions, listDictTree, type DictTreeOption } from '../../api/dict';
import { createDictValueFilter, indexDictTree } from '../../utils/dictTree';
import type {
  DictDisplayFormat,
  DictDisplayItem,
  DictDisplayMode,
  DictDisplayShape,
  DictDisplayValue,
} from './types';

const props = withDefaults(defineProps<{
  dictType: string;
  shape?: DictDisplayShape;
  value?: DictDisplayValue;
  mode?: DictDisplayMode;
  separator?: string;
  displayFormat?: DictDisplayFormat;
  displaySeparator?: string;
  emptyText?: string;
  tagType?: '' | 'primary' | 'success' | 'info' | 'warning' | 'danger';
  tagEffect?: 'light' | 'dark' | 'plain';
  tagSize?: 'large' | 'default' | 'small';
  group?: string | string[];
  includeValues?: Array<string | number>;
  excludeValues?: Array<string | number>;
}>(), {
  shape: 'FLAT',
  value: undefined,
  mode: 'text',
  separator: '、',
  displayFormat: 'label',
  displaySeparator: ' / ',
  emptyText: '-',
  tagType: '',
  tagEffect: 'light',
  tagSize: 'default',
  group: undefined,
  includeValues: undefined,
  excludeValues: undefined,
});

const emit = defineEmits<{
  (event: 'error', error: unknown): void;
  (event: 'loaded', items: DictDisplayItem[]): void;
}>();

const flatOptions = ref<Map<string, { label: string; groups: string[] }>>(new Map());
const treeIndex = ref<Map<string, { node: DictTreeOption; path: DictTreeOption[] }>>(new Map());

const valueFilter = computed(() =>
  createDictValueFilter({
    group: props.group,
    includeValues: props.includeValues,
    excludeValues: props.excludeValues,
  })
);

const normalizedValues = computed(() => {
  const { value } = props;
  if (value === undefined || value === null || value === '') return [];
  const list = Array.isArray(value) ? value : [value];
  return list
    .filter((item) => item !== undefined && item !== null && item !== '')
    .map((item) => String(item));
});

const items = computed<DictDisplayItem[]>(() =>
  normalizedValues.value
    .filter((value) => {
      if (props.shape === 'TREE') {
        const entry = treeIndex.value.get(value);
        return valueFilter.value({ value, groups: entry?.node.groups });
      }
      const option = flatOptions.value.get(value);
      // 未命中字典时仍按 include/exclude 与分组判断；无分组配置时保持原回退行为
      return valueFilter.value({ value, groups: option?.groups });
    })
    .map((value) => {
      if (props.shape === 'TREE') {
        const entry = treeIndex.value.get(value);
        const path = entry ? entry.path.map((node) => node.label) : [];
        const label = entry?.node.label ?? value;
        const text = props.displayFormat === 'path' && path.length > 0
          ? path.join(props.displaySeparator)
          : label;
        return { value, label, path, text, known: Boolean(entry) };
      }
      const option = flatOptions.value.get(value);
      return {
        value,
        label: option?.label ?? value,
        path: [],
        text: option?.label ?? value,
        known: option !== undefined,
      };
    })
);

async function reload(force = false): Promise<void> {
  if (!props.dictType) {
    flatOptions.value = new Map();
    treeIndex.value = new Map();
    return;
  }

  try {
    if (props.shape === 'TREE') {
      const data = await listDictTree(props.dictType, force);
      treeIndex.value = indexDictTree(data);
    } else {
      const data = await listDictOptions(props.dictType, force);
      flatOptions.value = new Map(
        data.map((item) => [String(item.value), { label: item.label, groups: item.groups ?? [] }])
      );
    }
    emit('loaded', items.value);
  } catch (error) {
    flatOptions.value = new Map();
    treeIndex.value = new Map();
    emit('error', error);
  }
}

function getItems() {
  return items.value;
}

watch(
  () => [props.dictType, props.shape],
  () => {
    void reload();
  },
  { immediate: true }
);

defineExpose({ reload, getItems });
</script>

<style scoped>
.dict-display {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.dict-display__tag {
  margin-right: 2px;
}

.dict-display__separator {
  color: var(--el-text-color-secondary);
}

.dict-display__empty {
  color: var(--el-text-color-placeholder);
}
</style>
