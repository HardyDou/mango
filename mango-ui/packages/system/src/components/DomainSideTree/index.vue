<template>
  <aside class="domain-side-tree">
    <header class="domain-side-tree__head">
      <div>
        <h3>{{ title }}</h3>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <el-tooltip content="刷新" placement="top">
        <el-button :icon="Refresh" link type="primary" :loading="loading" @click="reload" />
      </el-tooltip>
    </header>

    <div v-if="searchable" class="domain-side-tree__search">
      <el-input v-model="keyword" :prefix-icon="Search" clearable :placeholder="searchPlaceholder" />
    </div>

    <div v-loading="loading" class="domain-side-tree__body">
      <button
        v-if="showAll"
        class="domain-side-tree__all"
        :class="{ active: !innerValue }"
        type="button"
        @click="selectAll"
      >
        <span class="domain-side-tree__label">
          <strong>{{ allLabel }}</strong>
          <em>{{ allCode }}</em>
        </span>
        <el-tag v-if="allCount !== undefined" effect="plain" size="small" type="info">{{ allCount }}</el-tag>
      </button>

      <el-tree
        v-if="filteredOptions.length > 0"
        class="domain-side-tree__tree"
        :data="filteredOptions"
        :expand-on-click-node="false"
        :props="treeProps"
        default-expand-all
        node-key="domainCode"
        @node-click="selectDomain"
      >
        <template #default="{ data }">
          <button
            class="domain-side-tree__node"
            :class="{ active: innerValue === data.domainCode }"
            type="button"
            @click.stop="selectDomain(data)"
          >
            <span class="domain-side-tree__label">
              <strong>{{ data.domainName }}</strong>
              <em>{{ data.domainCode }}</em>
            </span>
            <el-tag v-if="countMap[data.domainCode] !== undefined" effect="plain" size="small">
              {{ countMap[data.domainCode] }}
            </el-tag>
          </button>
        </template>
      </el-tree>

      <el-empty
        v-if="!loading && filteredOptions.length === 0"
        :image-size="72"
        :description="keyword ? '没有匹配的业务域' : '暂无可用业务域'"
      />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { Refresh, Search } from '@element-plus/icons-vue';
import { domainApi, type DomainItem } from '../../api/domain';

const props = withDefaults(defineProps<{
  modelValue?: string;
  title?: string;
  subtitle?: string;
  allLabel?: string;
  allCode?: string;
  allCount?: number;
  counts?: Record<string, number>;
  options?: DomainItem[];
  searchable?: boolean;
  searchPlaceholder?: string;
  showAll?: boolean;
}>(), {
  modelValue: '',
  title: '业务域',
  subtitle: '',
  allLabel: '全部',
  allCode: 'ALL',
  counts: () => ({}),
  options: undefined,
  searchable: true,
  searchPlaceholder: '请输入业务域名称',
  showAll: true,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'change', domain: DomainItem | undefined): void;
  (event: 'loaded', domains: DomainItem[]): void;
}>();

const loading = ref(false);
const keyword = ref('');
const innerValue = ref(props.modelValue || '');
const localOptions = ref<DomainItem[]>([]);

const treeProps = {
  label: 'domainName',
  children: 'children',
};

const sourceOptions = computed(() => props.options ?? localOptions.value);
const countMap = computed(() => props.counts || {});

const filteredOptions = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase();
  if (!normalizedKeyword) {
    return sourceOptions.value;
  }
  return filterDomains(sourceOptions.value, normalizedKeyword);
});

watch(
  () => props.modelValue,
  value => {
    innerValue.value = value || '';
  },
);

watch(
  () => props.options,
  value => {
    if (value) {
      emit('loaded', value);
    }
  },
  { immediate: true },
);

onMounted(() => {
  if (!props.options) {
    void reload();
  }
});

async function reload() {
  if (props.options) {
    emit('loaded', props.options);
    return;
  }
  loading.value = true;
  try {
    localOptions.value = await domainApi.enabledTree();
    emit('loaded', localOptions.value);
  } finally {
    loading.value = false;
  }
}

function selectAll() {
  innerValue.value = '';
  emit('update:modelValue', '');
  emit('change', undefined);
}

function selectDomain(domain: DomainItem) {
  innerValue.value = domain.domainCode;
  emit('update:modelValue', domain.domainCode);
  emit('change', domain);
}

function filterDomains(options: DomainItem[], normalizedKeyword: string): DomainItem[] {
  return options
    .map(item => {
      const children = filterDomains(item.children || [], normalizedKeyword);
      const matched = item.domainName.toLowerCase().includes(normalizedKeyword)
        || item.domainCode.toLowerCase().includes(normalizedKeyword)
        || item.domainShortCode.toLowerCase().includes(normalizedKeyword);
      return matched || children.length > 0 ? { ...item, children } : undefined;
    })
    .filter((item): item is DomainItem => Boolean(item));
}

defineExpose({
  reload,
});
</script>

<style scoped>
.domain-side-tree {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 420px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.domain-side-tree__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 14px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.domain-side-tree__head h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
}

.domain-side-tree__head p {
  margin: 3px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.3;
}

.domain-side-tree__search {
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.domain-side-tree__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 8px;
}

.domain-side-tree__all,
.domain-side-tree__node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 42px;
  padding: 7px 9px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.domain-side-tree__all {
  margin-bottom: 5px;
}

.domain-side-tree__all:hover,
.domain-side-tree__node:hover {
  background: var(--el-fill-color-light);
}

.domain-side-tree__all.active,
.domain-side-tree__node.active {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.domain-side-tree__label {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
}

.domain-side-tree__label strong,
.domain-side-tree__label em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.domain-side-tree__label strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-style: normal;
  font-weight: 600;
  line-height: 1.3;
}

.domain-side-tree__label em {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-style: normal;
  line-height: 1.25;
}

.domain-side-tree__tree {
  --el-tree-node-content-height: auto;
}

.domain-side-tree__tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 1px 0;
}

.domain-side-tree__tree :deep(.el-tree-node__expand-icon) {
  color: var(--el-text-color-secondary);
}

.domain-side-tree__tree :deep(.el-tree-node__label) {
  flex: 1;
  min-width: 0;
}
</style>
