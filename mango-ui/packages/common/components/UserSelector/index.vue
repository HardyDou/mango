<template>
  <div class="user-selector">
    <el-input
      :model-value="summaryText"
      :placeholder="placeholder"
      :disabled="disabled"
      readonly
      @click="openDialog"
    >
      <template #suffix>
        <el-icon class="selector-arrow"><ArrowDown /></el-icon>
      </template>
    </el-input>

    <div v-if="selectedList.length" class="selector-tags">
      <el-tag
        v-for="item in selectedList"
        :key="item.value"
        closable
        size="small"
        @close="removeSelected(item.value)"
      >
        {{ item.label }}
      </el-tag>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="title"
      :width="width"
      append-to-body
      destroy-on-close
    >
      <div class="selector-dialog">
        <div class="selector-panel">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索用户名/姓名"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div v-loading="loading" class="selector-list">
            <button
              v-for="item in filteredOptions"
              :key="item.value"
              type="button"
              class="selector-item"
              :class="{ active: tempSelectedSet.has(item.value) }"
              @click="toggleOption(item.value)"
            >
              <div class="selector-item-check">
                <el-checkbox
                  :model-value="tempSelectedSet.has(item.value)"
                  @click.stop
                  @change="() => toggleOption(item.value)"
                />
              </div>
              <el-avatar :size="34" :src="item.avatar">
                {{ item.label.slice(0, 1) }}
              </el-avatar>
              <div class="selector-item-meta">
                <div class="selector-item-title">{{ item.label }}</div>
                <div v-if="item.meta" class="selector-item-subtitle">{{ item.meta }}</div>
              </div>
            </button>

            <el-empty
              v-if="!loading && !filteredOptions.length"
              :image-size="64"
              description="暂无匹配人员"
            />
          </div>
        </div>

        <div class="selector-selected">
          <div class="selected-header">
            <span>已选 {{ tempSelected.length }}</span>
            <el-button link type="danger" @click="clearTemp">清空</el-button>
          </div>
          <div class="selected-list">
            <div
              v-for="item in tempSelected"
              :key="item.value"
              class="selected-item"
            >
              <div class="selected-item-main">
                <el-avatar :size="32" :src="item.avatar">
                  {{ item.label.slice(0, 1) }}
                </el-avatar>
                <div>
                  <div class="selected-item-title">{{ item.label }}</div>
                  <div v-if="item.meta" class="selected-item-subtitle">{{ item.meta }}</div>
                </div>
              </div>
              <el-button link @click="removeTemp(item.value)">移除</el-button>
            </div>
            <el-empty
              v-if="!tempSelected.length"
              :image-size="64"
              description="未选择人员"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="confirmSelection">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { get } from '../../utils/request';
import type { UserSelectorEmits, UserSelectorExpose, UserSelectorOption, UserSelectorProps } from './types';

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
}

const props = withDefaults(defineProps<UserSelectorProps>(), {
  modelValue: () => [],
  multiple: false,
  placeholder: '请选择人员',
  title: '选择人员',
  disabled: false,
  width: '920px',
  max: 0,
});

const emit = defineEmits<UserSelectorEmits>();

const dialogVisible = ref(false);
const loading = ref(false);
const keyword = ref('');
const options = ref<UserSelectorOption[]>([]);
const tempSelectedValues = ref<string[]>([]);

const modelValues = computed<string[]>(() => {
  if (Array.isArray(props.modelValue)) {
    return props.modelValue.map(String);
  }
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '') {
    return [];
  }
  return [String(props.modelValue)];
});

const selectedList = computed(() => optionsByValues(modelValues.value));
const summaryText = computed(() => {
  if (!selectedList.value.length) {
    return '';
  }
  if (!props.multiple) {
    return selectedList.value[0]?.label || '';
  }
  return `已选择 ${selectedList.value.length} 人`;
});

const filteredOptions = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  if (!normalized) {
    return options.value;
  }
  return options.value.filter(item =>
    [item.label, item.username, item.meta].some(value => String(value || '').toLowerCase().includes(normalized)));
});

const tempSelectedSet = computed(() => new Set(tempSelectedValues.value));
const tempSelected = computed(() => optionsByValues(tempSelectedValues.value));

async function ensureLoaded() {
  if (options.value.length) {
    return;
  }
  loading.value = true;
  try {
    const data = await get<BackendPageResult<any>>('/identity/users/page', {
      params: {
        page: 1,
        size: 200,
      },
    });
    options.value = (data?.records || data?.list || [])
      .map((item: any) => {
        const id = item.userId ?? item.id ?? item.memberId;
        const value = item.username ?? id;
        const name = item.nickname || item.memberName || item.username || id;
        if (value === undefined || name === undefined) {
          return undefined;
        }
        return {
          value: String(value),
          label: String(name),
          username: item.username ? String(item.username) : undefined,
          avatar: item.avatar ? String(item.avatar) : undefined,
          meta: item.username && item.username !== name ? String(item.username) : '用户',
        } satisfies UserSelectorOption;
      })
      .filter(Boolean) as UserSelectorOption[];
  } finally {
    loading.value = false;
  }
}

function openDialog() {
  if (props.disabled) {
    return;
  }
  void ensureLoaded();
  keyword.value = '';
  tempSelectedValues.value = [...modelValues.value];
  dialogVisible.value = true;
}

function close() {
  dialogVisible.value = false;
}

function clear() {
  const value = props.multiple ? [] : undefined;
  emit('update:modelValue', value);
  emit('change', value);
}

function clearTemp() {
  tempSelectedValues.value = [];
}

function toggleOption(value: string) {
  const exists = tempSelectedSet.value.has(value);
  if (!props.multiple) {
    tempSelectedValues.value = exists ? [] : [value];
    return;
  }
  if (!exists && props.max > 0 && tempSelectedValues.value.length >= props.max) {
    ElMessage.warning(`最多选择 ${props.max} 人`);
    return;
  }
  tempSelectedValues.value = exists
    ? tempSelectedValues.value.filter(item => item !== value)
    : [...tempSelectedValues.value, value];
}

function removeTemp(value: string) {
  tempSelectedValues.value = tempSelectedValues.value.filter(item => item !== value);
}

function removeSelected(value: string) {
  const values = modelValues.value.filter(item => item !== value);
  const nextValue = props.multiple ? values : values[0];
  emit('update:modelValue', nextValue);
  emit('change', nextValue);
}

function confirmSelection() {
  const nextValue = props.multiple ? [...tempSelectedValues.value] : tempSelectedValues.value[0];
  emit('update:modelValue', nextValue);
  emit('change', nextValue);
  close();
}

function optionsByValues(values: string[]) {
  const valueSet = new Set(values);
  const matched = options.value.filter(item => valueSet.has(item.value));
  const missing = values
    .filter(value => !matched.some(item => item.value === value))
    .map(value => ({ value, label: value, meta: '用户' } satisfies UserSelectorOption));
  return [...matched, ...missing];
}

defineExpose<UserSelectorExpose>({
  open: openDialog,
  close,
  clear,
});
</script>

<style scoped>
.selector-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.selector-arrow {
  color: var(--el-text-color-secondary);
}

.selector-dialog {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 20px;
}

.selector-panel,
.selector-selected {
  min-width: 0;
}

.selector-list,
.selected-list {
  margin-top: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  max-height: 480px;
  overflow: auto;
  background: #fff;
}

.selector-item,
.selected-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;
  text-align: left;
}

.selector-item:last-child,
.selected-item:last-child {
  border-bottom: 0;
}

.selector-item {
  cursor: pointer;
}

.selector-item.active {
  background: color-mix(in srgb, var(--el-color-primary) 8%, #fff);
}

.selector-item-check {
  flex: 0 0 auto;
}

.selector-item-meta,
.selected-item-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.selected-item {
  justify-content: space-between;
}

.selector-item-title,
.selected-item-title {
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.selector-item-subtitle,
.selected-item-subtitle {
  color: #6b7280;
  font-size: 12px;
}

.selected-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #111827;
  font-weight: 600;
}
</style>
