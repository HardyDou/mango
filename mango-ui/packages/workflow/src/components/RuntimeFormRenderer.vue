<template>
  <el-form :model="model" :label-width="labelWidth" class="runtime-form-renderer">
    <el-form-item v-for="field in visibleFields" :key="field.key" :label="fieldLabel(field)" :prop="fieldProp(field)" :rules="field.rules">
      <el-alert
        v-if="field.type === 'alert'"
        :title="field.content || field.label"
        :type="field.props?.type || 'info'"
        :closable="field.props?.closable !== false"
        show-icon
      />
      <span v-else-if="field.type === 'text'" class="display-text">{{ field.content || field.label }}</span>
      <span v-else-if="field.type === 'html'" class="display-html" v-html="field.content" />
      <el-divider v-else-if="field.type === 'divider'" :content-position="field.props?.contentPosition || 'left'">
        {{ field.content || field.label }}
      </el-divider>
      <el-tag v-else-if="field.type === 'tag'" :type="field.props?.type">{{ field.content || field.label }}</el-tag>
      <el-image v-else-if="field.type === 'image'" class="display-image" :src="field.props?.src || field.props?.url" fit="cover" />
      <el-button v-else-if="field.type === 'button'" :type="field.props?.type || 'primary'" disabled>{{ field.content || field.label }}</el-button>
      <el-alert v-else-if="field.type === 'container'" :title="`${field.label} 暂以内部字段渲染`" type="info" :closable="false" />
      <span v-else-if="readonly || fieldPermission(field) === 'READONLY'" class="readonly-value">{{ displayValue(field) }}</span>
      <template v-else>
        <el-input
          v-if="field.type === 'input' || field.type === 'password'"
          v-model="model[field.key]"
          :type="field.type === 'password' ? 'password' : 'text'"
          :placeholder="field.placeholder"
          :readonly="field.readonly"
          clearable
        />
        <el-input
          v-else-if="field.type === 'textarea'"
          v-model="model[field.key]"
          type="textarea"
          :rows="4"
          :placeholder="field.placeholder"
          :readonly="field.readonly"
        />
        <el-input-number
          v-else-if="field.type === 'number'"
          v-model="model[field.key]"
          :placeholder="field.placeholder"
          :min="field.min"
          :max="field.max"
          :step="field.step || 1"
          controls-position="right"
        />
        <el-select
          v-else-if="isSelectField(field)"
          v-model="model[field.key]"
          :placeholder="field.placeholder"
          :multiple="field.props?.multiple"
          :clearable="field.props?.clearable !== false"
          :filterable="field.props?.filterable !== false"
          collapse-tags
          collapse-tags-tooltip
        >
          <el-option v-for="option in field.options" :key="String(option.value)" :label="option.label" :value="option.value" />
        </el-select>
        <el-radio-group v-else-if="field.type === 'radio'" v-model="model[field.key]">
          <el-radio v-for="option in field.options" :key="String(option.value)" :label="option.value">
            {{ option.label }}
          </el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="field.type === 'checkbox'" v-model="model[field.key]">
          <el-checkbox v-for="option in field.options" :key="String(option.value)" :label="option.value">
            {{ option.label }}
          </el-checkbox>
        </el-checkbox-group>
        <el-switch v-else-if="field.type === 'switch'" v-model="model[field.key]" />
        <el-date-picker v-else-if="field.type === 'date'" v-model="model[field.key]" type="date" :placeholder="field.placeholder" value-format="YYYY-MM-DD" />
        <el-date-picker
          v-else-if="field.type === 'datetime'"
          v-model="model[field.key]"
          type="datetime"
          :placeholder="field.placeholder"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-date-picker
          v-else-if="field.type === 'daterange'"
          v-model="model[field.key]"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-date-picker
          v-else-if="field.type === 'datetimerange'"
          v-model="model[field.key]"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-time-picker v-else-if="field.type === 'time'" v-model="model[field.key]" :placeholder="field.placeholder" value-format="HH:mm:ss" />
        <el-time-picker
          v-else-if="field.type === 'timerange'"
          v-model="model[field.key]"
          is-range
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="HH:mm:ss"
        />
        <el-rate v-else-if="field.type === 'rate'" v-model="model[field.key]" />
        <el-slider v-else-if="field.type === 'slider'" v-model="model[field.key]" :min="field.min" :max="field.max" :step="field.step || 1" />
        <el-color-picker v-else-if="field.type === 'color'" v-model="model[field.key]" />
        <el-cascader
          v-else-if="field.type === 'cascader'"
          v-model="model[field.key]"
          :options="field.treeOptions || field.options"
          :props="cascaderProps(field)"
          :placeholder="field.placeholder"
          :clearable="field.props?.clearable !== false"
          :filterable="field.props?.filterable !== false"
        />
        <el-tree-select
          v-else-if="field.type === 'treeSelect' || field.type === 'systemOrg' || field.type === 'systemDept'"
          v-model="model[field.key]"
          :data="field.treeOptions"
          :props="treeProps(field)"
          :node-key="field.props?.nodeKey || 'value'"
          :multiple="field.props?.multiple"
          :check-strictly="field.props?.checkStrictly !== false"
          :placeholder="field.placeholder"
          :clearable="field.props?.clearable !== false"
          :filterable="field.props?.filterable !== false"
        />
        <el-transfer
          v-else-if="field.type === 'transfer'"
          v-model="model[field.key]"
          :data="transferOptions(field)"
          :filterable="field.props?.filterable !== false"
        />
        <ImageUpload
          v-else-if="field.type === 'imageUpload'"
          v-model="model[field.key]"
          :limit="field.props?.limit || 6"
          :multiple="field.props?.multiple !== false"
          :disabled="field.readonly"
        />
        <FileUpload
          v-else-if="field.type === 'upload'"
          v-model="model[field.key]"
          :accept="field.props?.accept || '*'"
          :limit="field.props?.limit || 5"
          :multiple="field.props?.multiple !== false"
          :disabled="field.readonly"
        />
        <el-input v-else-if="field.type === 'editor'" v-model="model[field.key]" type="textarea" :rows="6" :placeholder="field.placeholder" />
        <el-input v-else-if="field.type === 'signature' || field.type === 'serialNo'" v-model="model[field.key]" :placeholder="field.placeholder" readonly />
        <el-alert v-else :title="`暂不支持组件：${field.type}`" type="warning" :closable="false" />
      </template>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { FileUpload, ImageUpload } from '@mango/common';
import type { RuntimeFormField } from './runtimeForm';

type RuntimeFormPermission = 'HIDDEN' | 'READONLY' | 'EDITABLE';

const props = withDefaults(defineProps<{
  fields: RuntimeFormField[];
  model: Record<string, any>;
  readonly?: boolean;
  labelWidth?: string;
  permissions?: Record<string, RuntimeFormPermission | string>;
}>(), {
  readonly: false,
  labelWidth: '96px',
  permissions: () => ({}),
});

const visibleFields = computed(() => props.fields.filter(field => fieldPermission(field) !== 'HIDDEN'));

function fieldPermission(field: RuntimeFormField): RuntimeFormPermission {
  return (props.permissions?.[field.key] as RuntimeFormPermission) || (props.readonly || field.readonly ? 'READONLY' : 'EDITABLE');
}

function fieldLabel(field: RuntimeFormField) {
  return isDisplayField(field) ? '' : field.label;
}

function fieldProp(field: RuntimeFormField) {
  return isDisplayField(field) ? undefined : field.key;
}

function displayValue(field: RuntimeFormField) {
  const value = props.model?.[field.key];
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.map(item => displaySingleValue(field, item)).join('，');
  }
  if (field.type === 'switch') {
    return value ? '是' : '否';
  }
  return displaySingleValue(field, value);
}

function displaySingleValue(field: RuntimeFormField, value: any) {
  if (value && typeof value === 'object') {
    return value.fileName || value.name || value.label || value.value || JSON.stringify(value);
  }
  const matchedOption = field.options?.find(option => option.value === value);
  if (matchedOption) {
    return matchedOption.label;
  }
  const matchedTreeNode = findTreeNode(field.treeOptions || [], value);
  if (matchedTreeNode) {
    return matchedTreeNode.label || matchedTreeNode.name || matchedTreeNode.value;
  }
  return String(value);
}

function isSelectField(field: RuntimeFormField) {
  return ['select', 'systemUser', 'systemPost', 'systemRole', 'systemDict', 'businessType'].includes(field.type);
}

function isDisplayField(field: RuntimeFormField) {
  return ['alert', 'text', 'html', 'divider', 'tag', 'image', 'button', 'container'].includes(field.type);
}

function treeProps(field: RuntimeFormField) {
  return {
    children: field.props?.children || 'children',
    label: field.props?.label || 'label',
    value: field.props?.value || 'value',
  };
}

function cascaderProps(field: RuntimeFormField) {
  return {
    multiple: Boolean(field.props?.multiple),
    emitPath: field.props?.emitPath !== false,
    checkStrictly: Boolean(field.props?.checkStrictly),
    value: field.props?.value || 'value',
    label: field.props?.label || 'label',
    children: field.props?.children || 'children',
  };
}

function transferOptions(field: RuntimeFormField) {
  return (field.options || []).map(option => ({
    key: option.value,
    label: option.label,
  }));
}

function findTreeNode(nodes: any[], value: any): any | null {
  for (const node of nodes || []) {
    if (node?.value === value || node?.id === value) {
      return node;
    }
    const matched = findTreeNode(node?.children || [], value);
    if (matched) {
      return matched;
    }
  }
  return null;
}
</script>

<style scoped>
.runtime-form-renderer {
  max-width: 760px;
}

.runtime-form-renderer :deep(.el-select),
.runtime-form-renderer :deep(.el-cascader),
.runtime-form-renderer :deep(.el-date-editor),
.runtime-form-renderer :deep(.el-input-number),
.runtime-form-renderer :deep(.el-slider),
.runtime-form-renderer :deep(.file-upload-container),
.runtime-form-renderer :deep(.image-upload-container) {
  width: 100%;
}

.readonly-value {
  color: #1f2937;
  word-break: break-word;
}

.display-text,
.display-html {
  color: var(--el-text-color-regular);
  line-height: 1.6;
  word-break: break-word;
}

.display-image {
  max-width: 240px;
  border-radius: 6px;
}
</style>
