<template>
  <div class="workflow-advanced-config">
    <div class="advanced-toolbar">
      <span>保留给引擎或业务扩展的节点参数</span>
      <el-button :icon="Plus" type="primary" text @click="addRow">添加属性</el-button>
    </div>

    <div v-if="rows.length" class="advanced-rows">
      <div v-for="(row, index) in rows" :key="row.id" class="advanced-row">
        <el-button class="advanced-remove" link type="danger" @click="removeRow(index)">删除</el-button>
        <el-form label-position="top" class="advanced-card-form">
          <el-form-item label="属性名称">
            <el-input v-model="row.key" class="advanced-key" placeholder="如 priority" @blur="emitRows" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="row.type" class="advanced-type" @change="handleTypeChange(row)">
              <el-option label="文本" value="string" />
              <el-option label="数字" value="number" />
              <el-option label="布尔" value="boolean" />
              <el-option label="JSON" value="json" />
            </el-select>
          </el-form-item>
          <el-form-item label="属性值">
            <el-switch
              v-if="row.type === 'boolean'"
              v-model="row.booleanValue"
              class="advanced-value"
              @change="emitRows"
            />
            <el-input-number
              v-else-if="row.type === 'number'"
              v-model="row.numberValue"
              class="advanced-value"
              controls-position="right"
              @change="emitRows"
            />
            <el-input
              v-else
              v-model="row.value"
              class="advanced-value"
              :rows="row.type === 'json' ? 4 : 1"
              :type="row.type === 'json' ? 'textarea' : 'text'"
              placeholder="请输入属性值"
              @input="emitRows"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-empty v-else description="暂无扩展属性" :image-size="64" />

    <el-alert
      v-if="invalidMessage"
      :closable="false"
      class="advanced-error"
      :title="invalidMessage"
      type="error"
      show-icon
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { createNodeId } from '../../../../api/workflow';

type RowType = 'string' | 'number' | 'boolean' | 'json';

interface AdvancedRow {
  id: string;
  key: string;
  type: RowType;
  value: string;
  numberValue?: number;
  booleanValue?: boolean;
}

const props = withDefaults(defineProps<{
  modelValue?: Record<string, any>;
  reservedKeys?: string[];
}>(), {
  modelValue: () => ({}),
  reservedKeys: () => [],
});

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>];
}>();

const rows = ref<AdvancedRow[]>([]);
const invalidMessage = ref('');
const reservedSet = computed(() => new Set(props.reservedKeys));

watch(
  () => props.modelValue,
  value => {
    rows.value = Object.entries(value || {})
      .filter(([key]) => !reservedSet.value.has(key))
      .map(([key, item]) => toRow(key, item));
  },
  { immediate: true, deep: true },
);

function toRow(key: string, value: any): AdvancedRow {
  if (typeof value === 'boolean') {
    return { id: createNodeId('property'), key, type: 'boolean', value: '', booleanValue: value };
  }
  if (typeof value === 'number') {
    return { id: createNodeId('property'), key, type: 'number', value: String(value), numberValue: value };
  }
  if (value && typeof value === 'object') {
    return { id: createNodeId('property'), key, type: 'json', value: JSON.stringify(value, null, 2) };
  }
  return { id: createNodeId('property'), key, type: 'string', value: value === undefined || value === null ? '' : String(value) };
}

function addRow() {
  rows.value.push({
    id: createNodeId('property'),
    key: '',
    type: 'string',
    value: '',
  });
}

function removeRow(index: number) {
  rows.value.splice(index, 1);
  emitRows();
}

function handleTypeChange(row: AdvancedRow) {
  if (row.type === 'boolean') {
    row.booleanValue = row.value === 'true';
  }
  if (row.type === 'number') {
    row.numberValue = Number(row.value) || 0;
  }
  emitRows();
}

function emitRows() {
  const next: Record<string, any> = {};
  invalidMessage.value = '';
  for (const row of rows.value) {
    const key = row.key.trim();
    if (!key || reservedSet.value.has(key)) {
      continue;
    }
    try {
      next[key] = rowValue(row);
    } catch (error) {
      invalidMessage.value = `属性「${key}」不是合法 JSON`;
      return;
    }
  }
  emit('update:modelValue', next);
}

function rowValue(row: AdvancedRow) {
  if (row.type === 'boolean') {
    return Boolean(row.booleanValue);
  }
  if (row.type === 'number') {
    return Number(row.numberValue || 0);
  }
  if (row.type === 'json') {
    return row.value.trim() ? JSON.parse(row.value) : {};
  }
  return row.value;
}
</script>

<style scoped>
.workflow-advanced-config {
  display: grid;
  gap: 12px;
}

.advanced-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.advanced-rows {
  display: grid;
  gap: 10px;
}

.advanced-row {
  position: relative;
  padding: 14px 14px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.advanced-row:hover {
  border-color: var(--el-border-color);
}

.advanced-card-form {
  display: grid;
  gap: 10px;
}

.advanced-card-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.advanced-card-form :deep(.el-form-item__label) {
  margin-bottom: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
}

.advanced-key,
.advanced-type,
.advanced-value {
  width: 100%;
}

.advanced-remove {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  min-height: 24px;
  padding: 0 4px;
}

.advanced-error {
  margin-top: 4px;
}
</style>
