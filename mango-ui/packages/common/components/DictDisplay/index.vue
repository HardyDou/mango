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
import { indexDictTree } from '../../utils/dictTree';
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
});

const emit = defineEmits<{
  (event: 'error', error: unknown): void;
  (event: 'loaded', items: DictDisplayItem[]): void;
}>();

const flatOptions = ref<Map<string, string>>(new Map());
const treeIndex = ref<Map<string, { node: DictTreeOption; path: DictTreeOption[] }>>(new Map());

const normalizedValues = computed(() => {
  const { value } = props;
  if (value === undefined || value === null || value === '') return [];
  const list = Array.isArray(value) ? value : [value];
  return list
    .filter((item) => item !== undefined && item !== null && item !== '')
    .map((item) => String(item));
});

const items = computed<DictDisplayItem[]>(() =>
  normalizedValues.value.map((value) => {
    if (props.shape === 'TREE') {
      const entry = treeIndex.value.get(value);
      const path = entry ? entry.path.map((node) => node.label) : [];
      const label = entry?.node.label ?? value;
      const text = props.displayFormat === 'path' && path.length > 0
        ? path.join(props.displaySeparator)
        : label;
      return { value, label, path, text, known: Boolean(entry) };
    }
    const label = flatOptions.value.get(value);
    return {
      value,
      label: label ?? value,
      path: [],
      text: label ?? value,
      known: label !== undefined,
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
      flatOptions.value = new Map(data.map((item) => [String(item.value), item.label]));
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
