<template>
  <div class="china-area">
    <el-cascader
      v-model="selectedValue"
      :options="areaOptions"
      :props="cascaderProps"
      :placeholder="placeholderText"
      :disabled="disabled"
      :clearable="clearable"
      :filterable="filterable"
      :collapse-tags="collapseTags"
      :show-all-levels="showAllLevels"
      :separator="separator"
      :loading="loading"
      :no-match-text="t('chinaArea.noMatch')"
      :no-data-text="t('chinaArea.noData')"
      size="default"
      @change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { getAreaTree } from '../../api/area';
import type { AreaNode, ChinaAreaProps, ChinaAreaEmits, ChinaAreaExpose } from './types';

const props = withDefaults(
  defineProps<ChinaAreaProps>(),
  {
    modelValue: () => [],
    placeholder: 'chinaArea.placeholder',
    level: 3,
    showAllLevels: true,
    showHot: true,
    disabled: false,
    clearable: true,
    filterable: true,
    collapseTags: false,
    separator: '/',
  }
);

const emit = defineEmits<ChinaAreaEmits>();

const { t } = useI18n();

const areaOptions = ref<AreaNode[]>([]);
const selectedValue = ref<number[]>([...(props.modelValue || [])]);
const loading = ref(false);

// 缓存已加载的节点，避免重复请求
const loadedNodesCache = new Map<number, AreaNode[]>();

// 请求配置
const REQUEST_TIMEOUT = 10000; // 10秒超时
const MAX_RETRIES = 2;

const placeholderText = computed(() => {
  return props.placeholder?.startsWith('chinaArea.')
    ? t(props.placeholder)
    : props.placeholder;
});

function markLeaf(nodes: AreaNode[], fallbackLevel: number): AreaNode[] {
  return nodes.map((item) => {
    const level = item.level || fallbackLevel;
    return {
      ...item,
      leaf: level >= props.level,
      children: item.children?.length ? markLeaf(item.children, level + 1) : item.children,
    };
  });
}

/**
 * 带超时和重试的请求
 */
async function fetchWithRetry<T>(
  fetchFn: () => Promise<T>,
  retries = MAX_RETRIES
): Promise<T> {
  let lastError: Error;

  for (let i = 0; i <= retries; i++) {
    try {
      return await Promise.race([
        fetchFn(),
        new Promise<never>((_, reject) =>
          setTimeout(() => reject(new Error('请求超时，请重试')), REQUEST_TIMEOUT)
        ),
      ]);
    } catch (err) {
      lastError = err as Error;
      if (i < retries) {
        // 重试前等待 1 秒
        await new Promise((r) => setTimeout(r, 1000));
      }
    }
  }

  throw lastError!;
}

/**
 * 懒加载子节点
 */
async function lazyLoad(
  node: { value: number; data: AreaNode; children: AreaNode[]; loading: boolean; loaded: boolean },
  resolve: (data: AreaNode[]) => void,
  reject: (err: Error) => void
) {
  const parentId = node.level === 0 ? 0 : node.value;

  if (node.level >= props.level) {
    resolve([]);
    return;
  }

  // 检查缓存
  if (loadedNodesCache.has(parentId)) {
    resolve(loadedNodesCache.get(parentId)!);
    return;
  }

  // 标记该节点正在加载（可选，用于显示 loading 状态）
  node.loading = true;

  try {
    const data = await fetchWithRetry(() =>
      getAreaTree({ type: node.level + 1, parentId })
    );

    const result = markLeaf(data || [], node.level + 1);

    // 缓存结果
    loadedNodesCache.set(parentId, result);

    // 如果没有子数据，给节点一个空数组让它显示叶子节点效果
    resolve(result);
  } catch (err) {
    console.error('[ChinaArea] 加载失败:', err);
    // 返回空数组，让用户可以重试
    resolve([]);
  } finally {
    node.loading = false;
  }
}

const cascaderProps = computed(() => ({
  value: 'id',
  label: 'name',
  children: 'children',
  leaf: (_data: AreaNode, node: { level: number }) => node.level >= props.level,
  expandTrigger: 'hover' as const,
  checkStrictly: false,
  emitPath: true,
  lazy: true,
  lazyLoad: (node: any, resolve: any, reject: any) => lazyLoad(node, resolve, reject),
}));

function handleChange(value: number[]) {
  emit('update:modelValue', value || []);
  emit('change', value || []);
}

function getValue(): number[] {
  return selectedValue.value;
}

function clear() {
  selectedValue.value = [];
  // 清除缓存
  loadedNodesCache.clear();
}

/**
 * 清除单个节点的缓存，强制重新加载
 */
function clearNodeCache(parentId: number) {
  loadedNodesCache.delete(parentId);
}

// Watch for external modelValue changes
watch(
  () => props.modelValue,
  (newVal) => {
    selectedValue.value = Array.isArray(newVal) ? [...newVal] : [];
  }
);

// Expose methods
defineExpose<ChinaAreaExpose>({
  getValue,
  clear,
  clearNodeCache,
});
</script>

<style scoped lang="scss">
.china-area {
  width: 100%;
}
</style>
