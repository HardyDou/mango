<template>
  <div class="user-selector">
    <template v-if="mode === 'select'">
      <el-select
        :model-value="modelValueForSelect"
        class="user-selector-input"
        clearable
        filterable
        remote
        reserve-keyword
        :collapse-tags="multiple"
        collapse-tags-tooltip
        :disabled="disabled"
        :loading="loading"
        :multiple="multiple"
        :placeholder="placeholder"
        :remote-method="handleRemoteSearch"
        @clear="clear"
        @focus="ensureLoaded"
        @visible-change="visible => visible && ensureLoaded()"
        @change="handleSelectChange"
      >
        <el-option
          v-for="item in filteredOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        >
          <div class="select-option">
            <span>{{ item.label }}</span>
            <small>{{ item.meta || item.username || '用户' }}</small>
          </div>
        </el-option>
      </el-select>
    </template>

    <template v-else>
      <el-input
        :model-value="summaryText"
        :placeholder="placeholder"
        :disabled="disabled"
        readonly
        @click="open"
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
          <aside class="selector-org">
            <div class="selector-column-title">组织架构</div>
            <button
              type="button"
              class="org-all"
              :class="{ active: selectedOrgId === undefined }"
              @click="selectedOrgId = undefined"
            >
              全部人员
            </button>
            <div v-loading="orgLoading" class="org-tree-wrap">
              <el-tree
                :data="orgOptions"
                :expand-on-click-node="false"
                default-expand-all
                node-key="id"
                :props="{ label: 'name', children: 'children' }"
                @node-click="handleOrgClick"
              />
            </div>
          </aside>

          <main class="selector-users">
            <div class="selector-search">
              <el-input v-model="keyword" clearable placeholder="搜索姓名、用户名">
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div v-loading="loading" class="selector-list">
              <button
                v-for="item in filteredOptions"
                :key="item.value"
                type="button"
                class="selector-item"
                :class="{ active: tempSelectedSet.has(item.value) }"
                @click="toggleTemp(item.value)"
              >
                <el-checkbox :model-value="tempSelectedSet.has(item.value)" @click.stop @change="() => toggleTemp(item.value)" />
                <el-avatar :size="32" :src="item.avatar">{{ item.label.slice(0, 1) }}</el-avatar>
                <span class="selector-item-meta">
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.meta || item.username || '用户' }}</small>
                </span>
              </button>
              <el-empty v-if="!loading && !filteredOptions.length" :image-size="64" description="暂无匹配人员" />
            </div>
          </main>

          <aside class="selector-picked">
            <div class="selected-header">
              <span>已选 {{ tempSelected.length }}</span>
              <el-button v-if="tempSelected.length" link type="danger" @click="clearTemp">清空</el-button>
            </div>
            <div class="selected-list">
              <div v-for="item in tempSelected" :key="item.value" class="selected-item">
                <el-avatar :size="30" :src="item.avatar">{{ item.label.slice(0, 1) }}</el-avatar>
                <span>{{ item.label }}</span>
                <el-button link type="danger" @click="removeTemp(item.value)">移除</el-button>
              </div>
              <el-empty v-if="!tempSelected.length" :image-size="64" description="未选择人员" />
            </div>
          </aside>
        </div>

        <template #footer>
          <el-button @click="close">取消</el-button>
          <el-button type="primary" @click="confirmSelection">确认</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getOrgTree } from '../../api/org';
import { get } from '../../utils/request';
import type { OrgNode } from '../OrgSelector/types';
import type { UserSelectorEmits, UserSelectorExpose, UserSelectorOption, UserSelectorProps } from './types';
import type { ApiId } from '@mango/api-schema';

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
}

type UserOption = UserSelectorOption & {
  orgId?: ApiId;
};

const props = withDefaults(defineProps<UserSelectorProps>(), {
  modelValue: () => [],
  mode: 'select',
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
const orgLoading = ref(false);
const keyword = ref('');
const options = ref<UserOption[]>([]);
const orgOptions = ref<OrgNode[]>([]);
const selectedOrgId = ref<ApiId | undefined>();
const tempSelectedValues = ref<string[]>([]);

const mode = computed(() => props.mode || 'select');

const modelValues = computed<string[]>(() => {
  if (Array.isArray(props.modelValue)) {
    return props.modelValue.map(String);
  }
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '') {
    return [];
  }
  return [String(props.modelValue)];
});

const modelValueForSelect = computed(() => (props.multiple ? modelValues.value : modelValues.value[0]));
const selectedList = computed(() => optionsByValues(modelValues.value));
const tempSelectedSet = computed(() => new Set(tempSelectedValues.value));
const tempSelected = computed(() => optionsByValues(tempSelectedValues.value));

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
  return options.value.filter(item => {
    const matchOrg = mode.value === 'select' || selectedOrgId.value === undefined || item.orgId === selectedOrgId.value;
    const matchKeyword = !normalized
      || [item.label, item.username, item.meta].some(value => String(value || '').toLowerCase().includes(normalized));
    return matchOrg && matchKeyword;
  });
});

async function ensureLoaded() {
  if (options.value.length) {
    return;
  }
  loading.value = true;
  try {
    const data = await get<BackendPageResult<any>>('/identity/users/page', {
      params: { page: 1, size: 200 },
    });
    options.value = (data?.records || data?.list || [])
      .map(toUserOption)
      .filter(Boolean) as UserOption[];
  } finally {
    loading.value = false;
  }
}

async function ensureOrgLoaded() {
  if (orgOptions.value.length) {
    return;
  }
  orgLoading.value = true;
  try {
    orgOptions.value = await getOrgTree({ parentId: '0' });
  } finally {
    orgLoading.value = false;
  }
}

function toUserOption(item: any): UserOption | undefined {
  const id = item.userId ?? item.id ?? item.memberId;
  const value = item.username ?? id;
  const name = item.nickname || item.memberName || item.username || id;
  if (value === undefined || name === undefined) {
    return undefined;
  }
  const username = item.username && item.username !== name ? String(item.username) : undefined;
  return {
    value: String(value),
    label: String(name),
    username,
    avatar: item.avatar ? String(item.avatar) : undefined,
    meta: username ? `@${username}` : '用户',
    orgId: item.primaryOrgId === undefined || item.primaryOrgId === null ? undefined : String(item.primaryOrgId),
  };
}

function handleRemoteSearch(value: string) {
  keyword.value = value;
  void ensureLoaded();
}

function handleSelectChange(value: string | string[]) {
  const values = Array.isArray(value) ? value.map(String) : value ? [String(value)] : [];
  if (props.multiple && props.max > 0 && values.length > props.max) {
    ElMessage.warning(`最多选择 ${props.max} 人`);
    emitValue(modelValues.value);
    return;
  }
  emitValue(values);
}

async function open() {
  if (props.disabled) {
    return;
  }
  keyword.value = '';
  selectedOrgId.value = undefined;
  tempSelectedValues.value = [...modelValues.value];
  dialogVisible.value = true;
  await Promise.all([ensureLoaded(), ensureOrgLoaded()]);
}

function close() {
  dialogVisible.value = false;
}

function clear() {
  emitValue([]);
}

function clearTemp() {
  tempSelectedValues.value = [];
}

function handleOrgClick(node: OrgNode) {
  selectedOrgId.value = node.id;
}

function toggleTemp(value: string) {
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
  emitValue(modelValues.value.filter(item => item !== value));
}

function confirmSelection() {
  emitValue(tempSelectedValues.value);
  close();
}

function emitValue(values: string[]) {
  const nextValue = props.multiple ? values : values[0];
  emit('update:modelValue', nextValue);
  emit('change', nextValue);
}

function optionsByValues(values: string[]) {
  const matched = options.value.filter(item => values.includes(item.value));
  const missing = values
    .filter(value => !matched.some(item => item.value === value))
    .map(value => ({ value, label: value, meta: '用户' } satisfies UserOption));
  return [...matched, ...missing];
}

defineExpose<UserSelectorExpose>({
  open: () => {
    void open();
  },
  close,
  clear,
});
</script>

<style scoped>
.user-selector,
.user-selector-input {
  width: 100%;
}

.select-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.select-option small {
  color: var(--el-text-color-secondary);
}

.selector-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.selector-dialog {
  display: grid;
  grid-template-columns: 200px minmax(320px, 1fr) 220px;
  gap: 12px;
  min-height: 420px;
}

.selector-org,
.selector-users,
.selector-picked {
  min-width: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.selector-column-title,
.selected-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.selector-search {
  padding: 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.org-all {
  width: calc(100% - 16px);
  margin: 8px;
  padding: 7px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-regular);
  text-align: left;
  cursor: pointer;
}

.org-all.active,
.org-all:hover {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.org-tree-wrap {
  height: 364px;
  overflow: auto;
}

.selector-list,
.selected-list {
  height: 364px;
  overflow: auto;
}

.selector-users .selector-list {
  height: 312px;
}

.selector-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.selector-item:hover,
.selector-item.active {
  background: var(--el-color-primary-light-9);
}

.selector-item-meta {
  display: grid;
  min-width: 0;
}

.selector-item-meta strong,
.selected-item span {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selector-item-meta small {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-item {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

:deep(.el-tree-node__content) {
  height: 30px;
}
</style>
