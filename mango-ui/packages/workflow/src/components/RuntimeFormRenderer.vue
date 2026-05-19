<template>
  <el-form :model="model" :label-width="labelWidth" class="runtime-form-renderer">
    <template v-for="field in visibleFields" :key="field.key">
      <div v-if="field.type === 'container'" class="runtime-container" :class="containerClass(field)">
        <div v-if="field.label && field.props?.originalType !== 'space'" class="runtime-container-title">{{ field.label }}</div>
        <RuntimeFormRenderer
          v-if="field.children?.length"
          :fields="field.children"
          :model="model"
          :readonly="readonly"
          :label-width="labelWidth"
          :permissions="permissions"
        />
        <el-empty v-else description="暂无字段" :image-size="64" />
      </div>
      <el-form-item v-else :label="fieldLabel(field)" :prop="fieldProp(field)" :rules="field.rules">
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
        <MUpload
          v-else-if="field.type === 'imageUpload'"
          v-model="model[field.key]"
          fmt="image"
          display="thumbnail"
          value-type="id"
          :count="field.props?.limit || 6"
          :size="field.props?.size || field.props?.maxSize"
          :readonly="fieldReadonly(field)"
        />
        <MUpload
          v-else-if="field.type === 'upload'"
          v-model="model[field.key]"
          :fmt="uploadFmt(field)"
          value-type="id"
          :count="field.props?.limit || 5"
          :size="field.props?.size || field.props?.maxSize"
          :readonly="fieldReadonly(field)"
        />
        <div v-else-if="fieldReadonly(field) && field.type === 'signature'" class="readonly-signature">
          <el-image
            v-if="model[field.key]"
            :src="model[field.key]"
            fit="contain"
            :preview-src-list="[model[field.key]]"
            preview-teleported
          />
          <span v-else class="readonly-value">-</span>
        </div>
        <RuntimeDictReadonlyValue
          v-else-if="fieldReadonly(field) && field.type === 'systemDict'"
          :field="field"
          :value="model[field.key]"
        />
        <RuntimeDictSelect
          v-else-if="field.type === 'systemDict'"
          v-model="model[field.key]"
          :field="field"
          :disabled="fieldReadonly(field)"
        />
        <span v-else-if="fieldReadonly(field)" class="readonly-value">{{ displayValue(field) }}</span>
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
          <el-input v-else-if="field.type === 'editor'" v-model="model[field.key]" type="textarea" :rows="6" :placeholder="field.placeholder" />
          <Sign
            v-else-if="field.type === 'signature'"
            v-model="model[field.key]"
            class="runtime-signature"
            :width="signatureWidth(field)"
            :height="signatureHeight(field)"
            :placeholder="field.placeholder || '请在此处签名'"
          />
          <el-input v-else-if="field.type === 'serialNo'" v-model="model[field.key]" :placeholder="field.placeholder" readonly />
          <el-alert v-else :title="`暂不支持组件：${field.type}`" type="warning" :closable="false" />
        </template>
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue';
import { MUpload } from '@mango/file';
import { DictSelect, Sign, useDict } from '@mango/common';
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

const RuntimeDictSelect = defineComponent({
  name: 'RuntimeDictSelect',
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: undefined,
    },
    field: {
      type: Object as () => RuntimeFormField,
      required: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue'],
  setup(componentProps, { emit }) {
    return () => {
      const dictType = fieldDictType(componentProps.field);
      if (!dictType) {
        return h('div', { class: 'runtime-dict-missing' }, '请先在表单设计器中绑定字典类型');
      }
      return h(DictSelect, {
        modelValue: componentProps.modelValue,
        dictType,
        placeholder: componentProps.field.placeholder || '请选择字典值',
        clearable: componentProps.field.props?.clearable !== false,
        filterable: componentProps.field.props?.filterable !== false,
        multiple: Boolean(componentProps.field.props?.multiple),
        disabled: componentProps.disabled,
        'onUpdate:modelValue': (value: any) => emit('update:modelValue', value),
      });
    };
  },
});

const RuntimeDictReadonlyValue = defineComponent({
  name: 'RuntimeDictReadonlyValue',
  props: {
    value: {
      type: [String, Number, Array],
      default: undefined,
    },
    field: {
      type: Object as () => RuntimeFormField,
      required: true,
    },
  },
  setup(componentProps) {
    const dictType = computed(() => fieldDictType(componentProps.field));
    const { getLabel } = useDict(dictType);
    return () => {
      const value = componentProps.value;
      if (value === undefined || value === null || value === '') {
        return h('span', { class: 'readonly-value' }, '-');
      }
      const text = Array.isArray(value)
        ? value.map(item => getLabel(item)).filter(Boolean).join('，')
        : getLabel(value);
      return h('span', { class: 'readonly-value' }, text || '-');
    };
  },
});

function fieldPermission(field: RuntimeFormField): RuntimeFormPermission {
  return (props.permissions?.[field.key] as RuntimeFormPermission) || (props.readonly || field.readonly ? 'READONLY' : 'EDITABLE');
}

function fieldReadonly(field: RuntimeFormField) {
  return fieldPermission(field) === 'READONLY';
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

function fieldDictType(field: RuntimeFormField) {
  return String(field.props?.dictType || field.props?.dictCode || field.props?.typeCode || '').trim();
}

function signatureWidth(field: RuntimeFormField) {
  return Number(field.props?.width || 520);
}

function signatureHeight(field: RuntimeFormField) {
  return Number(field.props?.height || 180);
}

function uploadFmt(field: RuntimeFormField) {
  const accept = String(field.props?.fmt || field.props?.accept || '').trim();
  if (!accept || accept === '*') {
    return undefined;
  }
  if (accept === 'image/*') {
    return 'image';
  }
  return accept
    .split(',')
    .map(item => acceptItemToFmt(item))
    .filter(Boolean)
    .join(',');
}

function acceptItemToFmt(value: string) {
  const item = value.trim().toLowerCase();
  const groups: Record<string, string> = {
    'image/*': 'image',
    'video/*': 'video',
    'audio/*': 'audio',
    'application/pdf': 'pdf',
    'application/msword': 'doc',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'docx',
    'application/vnd.ms-excel': 'xls',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'xlsx',
    'application/vnd.ms-powerpoint': 'ppt',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'pptx',
  };
  if (groups[item]) {
    return groups[item];
  }
  if (item.includes('/')) {
    return '';
  }
  return item.replace(/^\./, '').replace(/\/\*$/, '');
}

function isSelectField(field: RuntimeFormField) {
  return ['select', 'systemUser', 'systemPost', 'systemRole', 'businessType'].includes(field.type);
}

function isDisplayField(field: RuntimeFormField) {
  return ['alert', 'text', 'html', 'divider', 'tag', 'image', 'button', 'container'].includes(field.type);
}

function containerClass(field: RuntimeFormField) {
  const originalType = String(field.props?.originalType || '');
  return {
    'is-card': ['elCard', 'ElCard', 'group', 'subForm'].includes(originalType),
    'is-row': ['fcRow', 'FcRow', 'col', 'elCol', 'ElCol'].includes(originalType),
    'is-tabs': ['elTabs', 'ElTabs', 'elTabPane', 'ElTabPane'].includes(originalType),
    'is-collapse': ['elCollapse', 'ElCollapse', 'elCollapseItem', 'ElCollapseItem'].includes(originalType),
    'is-table': originalType === 'fcTable' || originalType === 'tableForm',
    'is-space': originalType === 'space',
  };
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
.runtime-form-renderer :deep(.dict-select),
.runtime-form-renderer :deep(.mango-file-upload) {
  width: 100%;
}

.runtime-signature :deep(canvas) {
  width: 100%;
  max-width: 100%;
}

.readonly-signature {
  width: 100%;
  max-width: 420px;
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #fff;
}

.readonly-signature :deep(.el-image) {
  display: block;
  width: 100%;
  max-height: 180px;
}

.runtime-dict-missing {
  color: var(--el-color-danger);
  font-size: 13px;
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

.runtime-container {
  width: 100%;
  min-width: 0;
}

.runtime-container-title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.runtime-container.is-card,
.runtime-container.is-table {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.runtime-container.is-row :deep(.runtime-form-renderer),
.runtime-container.is-space :deep(.runtime-form-renderer) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

@media (max-width: 760px) {
  .runtime-container.is-row :deep(.runtime-form-renderer),
  .runtime-container.is-space :deep(.runtime-form-renderer) {
    grid-template-columns: 1fr;
  }
}
</style>
