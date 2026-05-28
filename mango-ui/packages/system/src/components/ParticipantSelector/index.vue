<template>
  <div class="participant-selector">
    <div class="participant-selected-wrap">
      <button type="button" class="participant-add" title="选择对象" @click="open">
        <el-icon><Plus /></el-icon>
      </button>
      <div v-for="group in selectedGroups" :key="group.type" class="participant-selected-group">
        <span class="participant-selected-label">{{ group.label }}：</span>
        <div class="participant-selected-values">
          <el-tag v-for="item in group.items" :key="item.key" effect="plain" size="small">
            {{ item.label }}
          </el-tag>
        </div>
      </div>
      <span v-if="!selectedGroups.length" class="participant-empty-text">{{ placeholder }}</span>
    </div>

    <el-dialog v-model="visible" append-to-body destroy-on-close width="920px" class="participant-dialog">
      <template #header>
        <div class="participant-dialog-title">
          <strong>选择对象</strong>
          <span>支持用户、部门范围、岗位、角色组合选择</span>
        </div>
      </template>

      <div class="participant-dialog-body">
        <section class="participant-candidates">
          <el-tabs v-model="activeType" class="participant-dialog-tabs" @tab-change="handleTypeChange">
            <el-tab-pane label="用户" name="USER" />
            <el-tab-pane label="部门范围" name="ORG" />
            <el-tab-pane label="角色" name="ROLE" />
            <el-tab-pane label="岗位" name="POST" />
          </el-tabs>

          <el-input v-model="keyword" clearable class="participant-search" :placeholder="searchPlaceholder">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div v-if="activeType === 'ORG'" v-loading="currentLoading" class="participant-tree-wrap">
            <el-tree
              ref="orgTreeRef"
              :data="filteredOrgTreeOptions"
              :props="{ label: 'label', children: 'children' }"
              node-key="value"
              show-checkbox
              check-strictly
              default-expand-all
              check-on-click-node
              :default-checked-keys="tempValue.orgIds || []"
              @check="handleOrgCheck"
            />
            <el-empty v-if="!currentLoading && !filteredOrgTreeOptions.length" :image-size="64" description="暂无匹配数据" />
          </div>

          <div v-else v-loading="currentLoading" class="participant-list">
            <button
              v-for="item in currentOptions"
              :key="`${activeType}:${item.value}`"
              type="button"
              class="participant-item"
              :class="{ active: selectedSet.has(itemKey(activeType, item.value)) }"
              @click="toggleItem(activeType, item)"
            >
              <el-checkbox :model-value="selectedSet.has(itemKey(activeType, item.value))" @click.stop @change="() => toggleItem(activeType, item)" />
              <span class="participant-icon">
                <el-icon><component :is="typeIcon(activeType)" /></el-icon>
              </span>
              <span class="participant-name">
                <strong>{{ item.label }}</strong>
                <small>{{ typeLabel(activeType) }}</small>
              </span>
            </button>
            <el-empty v-if="!currentLoading && !currentOptions.length" :image-size="64" description="暂无匹配数据" />
          </div>
        </section>

        <aside class="participant-picked">
          <div class="picked-head">
            <div>
              <span>已选对象</span>
              <small>共 {{ pickedItems.length }} 项</small>
            </div>
            <el-button v-if="pickedItems.length" class="picked-clear" text type="danger" :icon="Delete" @click="clearPicked">清空</el-button>
          </div>
          <div class="picked-list">
            <div v-for="item in pickedItems" :key="item.key" class="picked-item">
              <span class="participant-icon">
                <el-icon><component :is="typeIcon(item.type)" /></el-icon>
              </span>
              <span class="participant-name">
                <strong>{{ item.label }}</strong>
                <small>{{ typeLabel(item.type) }}</small>
              </span>
              <el-button :icon="CircleClose" circle text @click="removePicked(item)" />
            </div>
            <el-empty v-if="!pickedItems.length" :image-size="64" description="未选择对象" />
          </div>
        </aside>
      </div>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ElTree } from 'element-plus';
import { Briefcase, CircleClose, Delete, OfficeBuilding, Plus, Search, Share, User } from '@element-plus/icons-vue';
import { get } from '@mango/common';
import type {
  ParticipantOrgTreeOption,
  ParticipantSelectorLoading,
  ParticipantSelectorValue,
  ParticipantTargetOption,
  ParticipantType,
} from './types';

type ParticipantOption = ParticipantTargetOption & { type?: ParticipantType };
type PickedItem = { key: string; type: ParticipantType; value: string; label: string };

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
}

interface IdentityUserRecord {
  userId?: string | number;
  id?: string | number;
  memberId?: string | number;
  username?: string;
  nickname?: string;
  memberName?: string;
}

const props = withDefaults(defineProps<{
  modelValue: ParticipantSelectorValue;
  userOptions?: ParticipantTargetOption[];
  roleOptions?: ParticipantTargetOption[];
  postOptions?: ParticipantTargetOption[];
  orgTreeOptions?: ParticipantOrgTreeOption[];
  targetLoading?: ParticipantSelectorLoading;
  placeholder?: string;
  searchPlaceholder?: string;
}>(), {
  userOptions: () => [],
  roleOptions: () => [],
  postOptions: () => [],
  orgTreeOptions: () => [],
  targetLoading: () => ({}),
  placeholder: '请选择用户、部门、角色或岗位',
  searchPlaceholder: '搜索名称/编码',
});

const emit = defineEmits<{
  'update:modelValue': [value: ParticipantSelectorValue];
  'ensure-users': [];
  'ensure-roles': [];
  'ensure-posts': [];
  'ensure-orgs': [];
}>();

const visible = ref(false);
const activeType = ref<ParticipantType>('USER');
const keyword = ref('');
const userLoading = ref(false);
const userOptions = ref<ParticipantOption[]>([]);
const tempValue = ref<ParticipantSelectorValue>(emptyValue());
const orgTreeRef = ref<InstanceType<typeof ElTree>>();

const selectedGroups = computed(() => [
  selectedGroupOf('USER', props.modelValue.userIds || [], mergedUserOptions.value),
  selectedGroupOf('ORG', props.modelValue.orgIds || [], flattenOrgOptions(props.orgTreeOptions)),
  selectedGroupOf('POST', props.modelValue.postIds || [], props.postOptions),
  selectedGroupOf('ROLE', props.modelValue.roleIds || [], props.roleOptions),
].filter(group => group.items.length > 0));

const selectedSet = computed(() => new Set(pickedItems.value.map(item => item.key)));

const pickedItems = computed<PickedItem[]>(() => [
  ...pickedOf('USER', tempValue.value.userIds || [], mergedUserOptions.value),
  ...pickedOf('ORG', tempValue.value.orgIds || [], flattenOrgOptions(props.orgTreeOptions)),
  ...pickedOf('POST', tempValue.value.postIds || [], props.postOptions),
  ...pickedOf('ROLE', tempValue.value.roleIds || [], props.roleOptions),
]);

const mergedUserOptions = computed(() => mergeOptions(props.userOptions, userOptions.value));

const currentLoading = computed(() => {
  if (activeType.value === 'USER') return userLoading.value || Boolean(props.targetLoading.users);
  if (activeType.value === 'ORG') return Boolean(props.targetLoading.orgs);
  if (activeType.value === 'ROLE') return Boolean(props.targetLoading.roles);
  return Boolean(props.targetLoading.posts);
});

const currentOptions = computed(() => {
  const source = activeType.value === 'USER'
    ? mergedUserOptions.value
    : activeType.value === 'ORG'
      ? flattenOrgOptions(props.orgTreeOptions)
      : activeType.value === 'ROLE'
        ? props.roleOptions
        : props.postOptions;
  const normalized = keyword.value.trim().toLowerCase();
  if (!normalized) return source;
  return source.filter(item => [item.label, item.value].some(value => String(value || '').toLowerCase().includes(normalized)));
});

const filteredOrgTreeOptions = computed(() => {
  const normalized = keyword.value.trim().toLowerCase();
  if (!normalized) return props.orgTreeOptions;
  return filterOrgTree(props.orgTreeOptions, normalized);
});

async function open() {
  tempValue.value = normalizeValue(props.modelValue);
  visible.value = true;
  keyword.value = '';
  await ensureTypeLoaded(activeType.value);
}

async function handleTypeChange(value: string | number) {
  keyword.value = '';
  await ensureTypeLoaded(value as ParticipantType);
}

async function ensureTypeLoaded(type: ParticipantType) {
  if (type === 'USER') {
    await ensureUsersLoaded();
  } else if (type === 'ORG') {
    emit('ensure-orgs');
  } else if (type === 'ROLE') {
    emit('ensure-roles');
  } else if (type === 'POST') {
    emit('ensure-posts');
  }
}

async function ensureUsersLoaded() {
  if (props.userOptions.length || userOptions.value.length) return;
  emit('ensure-users');
  userLoading.value = true;
  try {
    const data = await get<BackendPageResult<IdentityUserRecord>>('/identity/users/page', { params: { page: 1, size: 200 } });
    userOptions.value = (data?.records || data?.list || [])
      .map(toUserOption)
      .filter((item): item is ParticipantOption => Boolean(item));
  } finally {
    userLoading.value = false;
  }
}

function toUserOption(item: IdentityUserRecord): ParticipantOption | undefined {
  const id = item.userId ?? item.id ?? item.memberId;
  const name = item.nickname || item.memberName || item.username || id;
  const username = item.username && item.username !== name ? ` / ${item.username}` : '';
  return id === undefined ? undefined : { value: String(id), label: `${name}${username}` };
}

function selectedGroupOf(type: ParticipantType, values: string[], options: ParticipantTargetOption[]) {
  return {
    type,
    label: typeLabel(type),
    items: pickedOf(type, values, options),
  };
}

function mergeOptions(...groups: ParticipantTargetOption[][]) {
  const map = new Map<string, ParticipantTargetOption>();
  for (const group of groups) {
    for (const item of group || []) {
      if (!map.has(item.value)) {
        map.set(item.value, item);
      }
    }
  }
  return Array.from(map.values());
}

function toggleItem(type: ParticipantType, item: ParticipantTargetOption) {
  const key = valueKey(type);
  const values = new Set(tempValue.value[key] || []);
  if (values.has(item.value)) {
    values.delete(item.value);
  } else {
    values.add(item.value);
  }
  tempValue.value = {
    ...tempValue.value,
    [key]: Array.from(values),
  };
}

function removePicked(item: PickedItem) {
  const key = valueKey(item.type);
  tempValue.value = {
    ...tempValue.value,
    [key]: (tempValue.value[key] || []).filter(value => value !== item.value),
  };
  if (item.type === 'ORG') {
    orgTreeRef.value?.setCheckedKeys(tempValue.value.orgIds || []);
  }
}

function clearPicked() {
  tempValue.value = emptyValue();
  orgTreeRef.value?.setCheckedKeys([]);
}

function confirm() {
  emit('update:modelValue', normalizeValue(tempValue.value));
  visible.value = false;
}

function pickedOf(type: ParticipantType, values: string[], options: ParticipantTargetOption[]) {
  return values.map(value => {
    const option = options.find(item => item.value === value);
    return {
      key: itemKey(type, value),
      type,
      value,
      label: option?.label || value,
    };
  });
}

function flattenOrgOptions(items: ParticipantOrgTreeOption[]): ParticipantTargetOption[] {
  const result: ParticipantTargetOption[] = [];
  const visit = (nodes: ParticipantOrgTreeOption[]) => {
    for (const node of nodes || []) {
      result.push({ value: node.value, label: node.label });
      if (node.children?.length) visit(node.children);
    }
  };
  visit(items);
  return result;
}

function filterOrgTree(items: ParticipantOrgTreeOption[], normalized: string): ParticipantOrgTreeOption[] {
  return (items || []).reduce<ParticipantOrgTreeOption[]>((result, item) => {
    const children = item.children?.length ? filterOrgTree(item.children, normalized) : [];
    const matched = [item.label, item.value].some(value => String(value || '').toLowerCase().includes(normalized));
    if (matched || children.length) {
      result.push({
        ...item,
        children,
      });
    }
    return result;
  }, []);
}

function handleOrgCheck(_node: ParticipantOrgTreeOption, checked: { checkedKeys: Array<string | number> }) {
  tempValue.value = {
    ...tempValue.value,
    orgIds: checked.checkedKeys.map(value => String(value)),
  };
}

function normalizeValue(value: ParticipantSelectorValue): ParticipantSelectorValue {
  return {
    userIds: normalizeList(value.userIds),
    orgIds: normalizeList(value.orgIds),
    roleIds: normalizeList(value.roleIds),
    postIds: normalizeList(value.postIds),
  };
}

function normalizeList(value: unknown) {
  if (Array.isArray(value)) return value.map(item => String(item).trim()).filter(Boolean);
  if (value === undefined || value === null || value === '') return [];
  return [String(value).trim()].filter(Boolean);
}

function emptyValue(): ParticipantSelectorValue {
  return { userIds: [], orgIds: [], roleIds: [], postIds: [] };
}

function itemKey(type: ParticipantType, value: string) {
  return `${type}:${value}`;
}

function valueKey(type: ParticipantType): keyof ParticipantSelectorValue {
  if (type === 'USER') return 'userIds';
  if (type === 'ORG') return 'orgIds';
  if (type === 'ROLE') return 'roleIds';
  return 'postIds';
}

function typeLabel(type: ParticipantType) {
  if (type === 'USER') return '用户';
  if (type === 'ORG') return '部门范围';
  if (type === 'ROLE') return '角色';
  return '岗位';
}

function typeIcon(type: ParticipantType) {
  if (type === 'USER') return User;
  if (type === 'ORG') return OfficeBuilding;
  if (type === 'ROLE') return Share;
  return Briefcase;
}
</script>

<style scoped>
.participant-selector {
  width: 100%;
}

.participant-selected-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  min-height: 40px;
  padding: 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.participant-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 18px;
  transition: border-color .16s ease, background-color .16s ease, box-shadow .16s ease;
}

.participant-add:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.participant-add:focus-visible {
  outline: 0;
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

.participant-selected-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-height: 32px;
}

.participant-selected-label {
  flex: 0 0 auto;
  color: var(--el-text-color-regular);
  font-size: 12px;
  font-weight: 700;
  line-height: 24px;
}

.participant-selected-values {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.participant-empty-text {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  line-height: 32px;
}

.participant-dialog-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  min-height: 500px;
}

.participant-candidates {
  min-width: 0;
  padding: 2px;
}

.participant-dialog-title {
  display: grid;
  gap: 3px;
}

.participant-dialog-title strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.participant-dialog-title span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.participant-dialog-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.participant-dialog-tabs :deep(.el-tabs__item) {
  height: 36px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 700;
}

.participant-search {
  margin-bottom: 12px;
}

.participant-list,
.participant-tree-wrap,
.picked-list {
  display: grid;
  align-content: start;
  gap: 6px;
  max-height: 430px;
  overflow: auto;
  padding-right: 3px;
}

.participant-tree-wrap {
  padding: 6px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.participant-tree-wrap :deep(.el-tree-node__content) {
  min-height: 32px;
  border-radius: 6px;
  font-size: 13px;
}

.participant-tree-wrap :deep(.el-tree-node__content:hover) {
  background: var(--el-fill-color-light);
}

.participant-item,
.picked-item {
  display: grid;
  grid-template-columns: 22px 34px minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  min-height: 48px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  text-align: left;
  cursor: pointer;
  transition: border-color .16s ease, background-color .16s ease;
}

.participant-item:hover {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-fill-color-extra-light);
}

.participant-item.active {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.participant-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 16px;
}

.participant-name {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.participant-name strong,
.participant-name small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.participant-name strong {
  font-size: 13px;
}

.participant-name small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.participant-picked {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-extra-light);
}

.picked-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.picked-head > div {
  display: grid;
  gap: 2px;
}

.picked-head small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 500;
}

.picked-clear {
  margin-left: auto;
}

.picked-item {
  grid-template-columns: 34px minmax(0, 1fr) 28px;
  cursor: default;
  background: var(--el-bg-color);
}
</style>
