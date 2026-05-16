<!--
  Condition branch editor.
  Local implementation for Mango Workflow.
  Condition grouping model references willianfu/jw-workflow-engine (Apache-2.0)
  and workflow_vue3 style condition groups; this file keeps a separate implementation.
-->
<template>
  <div class="workflow-condition-config">
    <el-radio-group :model-value="mode" class="condition-mode-switch" @change="value => $emit('update-mode', value as ConditionEditMode)">
      <el-radio-button label="BUILDER">配置方式</el-radio-button>
      <el-radio-button label="EXPRESSION">表达式方式</el-radio-button>
    </el-radio-group>

    <template v-if="mode === 'EXPRESSION'">
      <el-form-item label="条件表达式">
        <el-input
          v-model="node.conditionExpression"
          :rows="5"
          placeholder="${(amount > 100000 || applicant.orgId == '1') && tenantId == '1'}"
          type="textarea"
          @input="$emit('sync')"
          @change="value => $emit('parse-expression', String(value || ''))"
        />
      </el-form-item>
    </template>

    <template v-else>
      <div class="condition-config-title">
        <span>条件分组</span>
        <el-button link type="primary" :icon="Plus" @click="$emit('add-group')">添加分组</el-button>
      </div>

      <div class="condition-builder drawer-condition-builder">
        <div v-for="(group, groupIndex) in groups" :key="group.id" class="condition-group">
          <div class="condition-group-header">
            <div class="condition-group-meta">
              <span class="condition-group-index">分组 {{ groupIndex + 1 }}</span>
              <el-select
                v-if="groupIndex > 0"
                v-model="group.connector"
                class="condition-group-connector"
                placeholder="组间关系"
                @change="$emit('apply')"
              >
                <el-option label="并且 AND" value="AND" />
                <el-option label="或者 OR" value="OR" />
              </el-select>
              <span v-else class="condition-group-anchor">当</span>
            </div>
            <div class="condition-group-actions">
              <el-button link type="primary" @click="$emit('add-row', groupIndex)">添加条件</el-button>
              <el-button link type="danger" :disabled="groups.length === 1" @click="$emit('remove-group', groupIndex)">删除分组</el-button>
            </div>
          </div>

          <div class="condition-group-body">
            <div v-for="(row, rowIndex) in group.rows" :key="row.id" class="condition-row">
              <el-select
                v-if="rowIndex > 0"
                v-model="row.connector"
                class="condition-connector"
                placeholder="组内关系"
                @change="$emit('apply')"
              >
                <el-option label="并且 AND" value="AND" />
                <el-option label="或者 OR" value="OR" />
              </el-select>
              <span v-else class="condition-connector-placeholder">条件</span>
              <el-select
                v-model="row.variable"
                class="condition-variable"
                clearable
                filterable
                placeholder="选择字段"
                @change="onVariableChange(row)"
              >
                <el-option-group v-for="groupItem in variableGroups" :key="groupItem.label" :label="groupItem.label">
                  <el-option v-for="item in groupItem.options" :key="item.value" :label="`${item.label}（${item.value}）`" :value="item.value">
                    <span class="condition-option-label">{{ item.label }}</span>
                    <span class="condition-option-meta">{{ item.value }}{{ item.dataType ? ` / ${dataTypeLabel(item.dataType)}` : '' }}</span>
                  </el-option>
                </el-option-group>
              </el-select>
              <el-select v-model="row.operator" class="condition-operator" placeholder="关系" @change="onOperatorChange(row)">
                <el-option v-for="operator in operatorOptions(row.variable)" :key="operator.value" :label="operator.label" :value="operator.value" />
              </el-select>
              <el-tree-select
                v-if="variableDataType(row.variable) === 'ORG'"
                v-model="row.value"
                class="condition-value"
                :data="orgTreeOptions"
                :props="{ label: 'label', value: 'value', children: 'children' }"
                clearable
                filterable
                check-strictly
                placeholder="选择部门"
                @focus="$emit('ensure-orgs')"
                @visible-change="visible => visible && $emit('ensure-orgs')"
                @change="$emit('apply')"
              />
              <el-select
                v-else-if="variableDataType(row.variable) === 'USER'"
                v-model="row.value"
                class="condition-value"
                clearable
                filterable
                :loading="targetLoading.users"
                placeholder="选择人员"
                @focus="$emit('ensure-users')"
                @visible-change="visible => visible && $emit('ensure-users')"
                @change="$emit('apply')"
              >
                <el-option v-for="item in userOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-select
                v-else-if="variableDataType(row.variable) === 'POST'"
                v-model="row.value"
                class="condition-value"
                clearable
                filterable
                :loading="targetLoading.posts"
                placeholder="选择岗位"
                @focus="$emit('ensure-posts')"
                @visible-change="visible => visible && $emit('ensure-posts')"
                @change="$emit('apply')"
              >
                <el-option v-for="item in postOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-select
                v-else-if="variableDataType(row.variable) === 'ROLE'"
                v-model="row.value"
                class="condition-value"
                clearable
                filterable
                :loading="targetLoading.roles"
                placeholder="选择角色"
                @focus="$emit('ensure-roles')"
                @visible-change="visible => visible && $emit('ensure-roles')"
                @change="$emit('apply')"
              >
                <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-input-number
                v-else-if="variableDataType(row.variable) === 'NUMBER'"
                v-model="row.value"
                class="condition-value"
                controls-position="right"
                placeholder="比较值"
                @change="$emit('apply')"
              />
              <el-date-picker
                v-else-if="variableDataType(row.variable) === 'DATE'"
                v-model="row.value"
                class="condition-value"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                @change="$emit('apply')"
              />
              <el-input v-else v-model="row.value" class="condition-value" placeholder="比较值" @input="$emit('apply')" />
              <el-button link type="danger" :disabled="group.rows.length === 1" @click="$emit('remove-row', groupIndex, rowIndex)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-form-item label="条件表达式">
        <el-input
          :model-value="node.conditionExpression"
          :rows="3"
          placeholder="配置条件后自动生成表达式"
          readonly
          type="textarea"
        />
      </el-form-item>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import type { WorkflowDesignerNode } from '../../../../api/workflow';
import type { ApprovalOrgTreeOption, ApprovalTargetOption, ConditionEditMode, ConditionGroup, ConditionRow, WorkflowVariableGroup } from './types';

const props = defineProps<{
  node: WorkflowDesignerNode;
  groups: ConditionGroup[];
  variableGroups: WorkflowVariableGroup[];
  mode: ConditionEditMode;
  userOptions: ApprovalTargetOption[];
  roleOptions: ApprovalTargetOption[];
  postOptions: ApprovalTargetOption[];
  orgTreeOptions: ApprovalOrgTreeOption[];
  targetLoading: { users: boolean; roles: boolean; posts: boolean; orgs: boolean };
}>();

const emit = defineEmits<{
  sync: [];
  apply: [];
  'update-mode': [mode: ConditionEditMode];
  'parse-expression': [expression: string];
  'add-group': [];
  'add-row': [groupIndex: number];
  'remove-group': [groupIndex: number];
  'remove-row': [groupIndex: number, rowIndex: number];
  'ensure-users': [];
  'ensure-roles': [];
  'ensure-posts': [];
  'ensure-orgs': [];
}>();

const variableMap = computed(() => {
  const map = new Map<string, string>();
  props.variableGroups.forEach(group => {
    group.options.forEach(item => {
      map.set(item.value, normalizeDataType(item.dataType));
    });
  });
  return map;
});

function variableDataType(variable: string) {
  return variableMap.value.get(variable) || 'TEXT';
}

function normalizeDataType(type?: string) {
  const normalized = String(type || '').toUpperCase();
  if (['USER', 'ORG', 'POST', 'ROLE', 'NUMBER', 'DATE'].includes(normalized)) {
    return normalized;
  }
  return 'TEXT';
}

function operatorOptions(variable: string) {
  const dataType = variableDataType(variable);
  if (dataType === 'NUMBER' || dataType === 'DATE') {
    return [
      { label: '等于 ==', value: '==' },
      { label: '不等于 !=', value: '!=' },
      { label: '大于 >', value: '>' },
      { label: '大于等于 >=', value: '>=' },
      { label: '小于 <', value: '<' },
      { label: '小于等于 <=', value: '<=' },
    ];
  }
  return [
    { label: '是', value: '==' },
    { label: '不是', value: '!=' },
    { label: '属于/包含', value: 'contains' },
    { label: '不属于/不包含', value: 'notContains' },
  ];
}

function onVariableChange(row: ConditionRow) {
  const options = operatorOptions(row.variable);
  if (!options.some(item => item.value === row.operator)) {
    row.operator = options[0]?.value || '==';
  }
  row.value = '';
  emit('apply');
}

function onOperatorChange(row: ConditionRow) {
  const options = operatorOptions(row.variable);
  if (!options.some(item => item.value === row.operator)) {
    row.operator = options[0]?.value || '==';
  }
  emit('apply');
}

function dataTypeLabel(type?: string) {
  const map: Record<string, string> = {
    TEXT: '文本',
    NUMBER: '数字',
    DATE: '日期',
    USER: '人员',
    ORG: '部门',
    POST: '岗位',
    ROLE: '角色',
  };
  return map[normalizeDataType(type)] || '文本';
}
</script>

<style scoped>
.workflow-condition-config {
  display: grid;
  gap: 12px;
}

.condition-mode-switch {
  width: 100%;
}

.condition-mode-switch :deep(.el-radio-button) {
  flex: 1;
}

.condition-mode-switch :deep(.el-radio-button__inner) {
  width: 100%;
}

.condition-config-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 8px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 700;
}

.condition-builder {
  display: grid;
  gap: 8px;
  margin: -8px 0 18px;
}

.drawer-condition-builder {
  margin-bottom: 18px;
}

.drawer-condition-builder .condition-row {
  grid-template-columns: 76px minmax(0, 1fr) 40px;
}

.condition-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 42px;
  gap: 8px;
  align-items: center;
}

.condition-variable {
  grid-column: 2 / 4;
}

.condition-operator {
  grid-column: 1 / 2;
  width: 100%;
}

.condition-value {
  grid-column: 2 / 4;
  width: 100%;
}

.condition-connector,
.condition-connector-placeholder {
  width: 88px;
}

.condition-connector-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.condition-group {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.condition-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  background: var(--el-fill-color-extra-light);
}

.condition-group-meta,
.condition-group-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.condition-group-body {
  display: grid;
  gap: 12px;
  padding: 12px;
}

.condition-group-index {
  color: var(--el-text-color-primary);
  font-weight: 700;
}

.condition-group-connector {
  width: 104px;
}

.condition-option-label {
  float: left;
}

.condition-option-meta {
  float: right;
  margin-left: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
