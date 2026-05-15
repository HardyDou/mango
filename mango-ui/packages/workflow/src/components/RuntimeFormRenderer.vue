<template>
  <el-form :model="model" :label-width="labelWidth" class="runtime-form-renderer">
    <el-form-item v-for="field in visibleFields" :key="field.key" :label="field.label">
      <span v-if="readonly || fieldPermission(field) === 'READONLY'" class="readonly-value">{{ displayValue(field) }}</span>
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
        <el-select v-else-if="field.type === 'select'" v-model="model[field.key]" :placeholder="field.placeholder" clearable filterable>
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
          v-else-if="field.type === 'daterange'"
          v-model="model[field.key]"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </template>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { computed } from 'vue';
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

function displayValue(field: RuntimeFormField) {
  const value = props.model?.[field.key];
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.join('，');
  }
  if (field.type === 'switch') {
    return value ? '是' : '否';
  }
  const matchedOption = field.options?.find(option => option.value === value);
  if (matchedOption) {
    return matchedOption.label;
  }
  return String(value);
}
</script>

<style scoped>
.runtime-form-renderer {
  max-width: 760px;
}

.readonly-value {
  color: #1f2937;
  word-break: break-word;
}
</style>
