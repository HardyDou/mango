<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    :label-width="computedLabelWidth"
    :inline="config.inline"
    :label-position="config.labelPosition"
    :size="config.size"
    :disabled="disabled"
    :class="['form-create', { 'form-create-inline': config.inline }]"
  >
    <template
      v-for="field in visibleFields"
      :key="field.key"
    >
      <!-- 分隔线 -->
      <el-form-item
        v-if="field.type === 'divider'"
        :label="field.title"
        class="form-create-divider"
      >
        <el-divider />
      </el-form-item>

      <!-- 输入框 -->
      <el-form-item
        v-else-if="field.type === 'input'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-input
          v-model="formData[field.key]"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          v-bind="field.props"
        >
          <template
            v-if="field.prefix"
            #prefix
          >
            {{ field.prefix }}
          </template>
          <template
            v-if="field.suffix"
            #suffix
          >
            {{ field.suffix }}
          </template>
        </el-input>
      </el-form-item>

      <!-- 多行文本 -->
      <el-form-item
        v-else-if="field.type === 'textarea'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-input
          v-model="formData[field.key]"
          type="textarea"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 数字输入 -->
      <el-form-item
        v-else-if="field.type === 'number'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-input-number
          v-model="formData[field.key]"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          :step="field.step"
          :min="field.rules?.min"
          :max="field.rules?.max"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 密码输入 -->
      <el-form-item
        v-else-if="field.type === 'password'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-input
          v-model="formData[field.key]"
          type="password"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          show-password
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 下拉选择 -->
      <el-form-item
        v-else-if="field.type === 'select'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-select
          v-model="formData[field.key]"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          v-bind="field.props"
        >
          <el-option
            v-for="opt in field.options"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
            :disabled="opt.disabled"
          />
        </el-select>
      </el-form-item>

      <!-- 单选按钮 -->
      <el-form-item
        v-else-if="field.type === 'radio'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-radio-group
          v-model="formData[field.key]"
          :disabled="field.disabled || disabled"
          v-bind="field.props"
        >
          <el-radio
            v-for="opt in field.options"
            :key="opt.value"
            :value="opt.value"
            :disabled="opt.disabled"
          >
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 多选框 -->
      <el-form-item
        v-else-if="field.type === 'checkbox'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-checkbox-group
          v-model="formData[field.key]"
          :disabled="field.disabled || disabled"
          v-bind="field.props"
        >
          <el-checkbox
            v-for="opt in field.options"
            :key="opt.value"
            :value="opt.value"
            :disabled="opt.disabled"
          >
            {{ opt.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <!-- 开关 -->
      <el-form-item
        v-else-if="field.type === 'switch'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-switch
          v-model="formData[field.key]"
          :disabled="field.disabled || disabled"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 日期选择 -->
      <el-form-item
        v-else-if="field.type === 'date'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-date-picker
          v-model="formData[field.key]"
          type="date"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          :format="field.format || 'YYYY-MM-DD'"
          :value-format="field.format || 'YYYY-MM-DD'"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 日期时间选择 -->
      <el-form-item
        v-else-if="field.type === 'datetime'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-date-picker
          v-model="formData[field.key]"
          type="datetime"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          :format="field.format || 'YYYY-MM-DD HH:mm:ss'"
          :value-format="field.format || 'YYYY-MM-DD HH:mm:ss'"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 日期范围 -->
      <el-form-item
        v-else-if="field.type === 'daterange'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-date-picker
          v-model="formData[field.key]"
          type="daterange"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          :format="field.format || 'YYYY-MM-DD'"
          :value-format="field.format || 'YYYY-MM-DD'"
          :separator="field.separator || '至'"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 级联选择 -->
      <el-form-item
        v-else-if="field.type === 'cascader'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-cascader
          v-model="formData[field.key]"
          :options="field.cascaderOptions"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          v-bind="field.props"
        />
      </el-form-item>

      <!-- 树形选择 -->
      <el-form-item
        v-else-if="field.type === 'tree-select'"
        :label="field.label"
        :prop="field.key"
        :rules="getFieldRules(field)"
      >
        <el-tree-select
          v-model="formData[field.key]"
          :data="field.treeData"
          :placeholder="field.placeholder"
          :disabled="field.disabled || disabled"
          :readonly="field.readonly"
          v-bind="field.props"
        />
      </el-form-item>
    </template>

    <!-- 操作按钮 -->
    <el-form-item
      v-if="showActions"
      class="form-create-actions"
    >
      <el-button
        type="primary"
        @click="handleSubmit"
      >
        {{ submitText }}
      </el-button>
      <el-button @click="handleReset">
        {{ resetText }}
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
/**
 * FormCreate Component - Dynamic Form Generator
 *
 * Features:
 * - Dynamic form field configuration
 * - Support for required/optional/regex validation
 * - Support for conditional show/hide fields
 * - Multiple field types (input, select, date, etc.)
 *
 * Usage:
 * <FormCreate
 *   :config="formConfig"
 *   v-model="formData"
 *   @submit="onSubmit"
 * />
 */

import { ref, computed, watch, reactive } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import type {
  FormConfig,
  FormField,
  FormCreateProps,
  FormCreateEmits,
  FormCreateExpose,
} from './types';

const props = withDefaults(defineProps<FormCreateProps>(), {
  disabled: false,
  readonly: false,
  showActions: true,
  submitText: '提交',
  resetText: '重置',
});

const emit = defineEmits<FormCreateEmits>();

const formRef = ref<FormInstance>();

// 表单数据
const formData = ref<Record<string, any>>({});

// 表单验证规则
const formRules = reactive<FormRules>({});

function isSameValue(left: any, right: any) {
  return JSON.stringify(left ?? {}) === JSON.stringify(right ?? {});
}

// 计算标签宽度
const computedLabelWidth = computed(() => {
  if (!props.config.labelWidth) {
    return props.config.labelPosition === 'top' ? undefined : '100px';
  }
  return props.config.labelWidth;
});

// 可见字段（根据 show 条件过滤）
const visibleFields = computed(() => {
  return props.config.fields.filter((field) => {
    if (field.type === 'divider') return true;
    if (typeof field.show === 'function') {
      return field.show(formData.value);
    }
    return field.show !== false;
  });
});

// 初始化表单数据
function initFormData() {
  const data: Record<string, any> = {};
  props.config.fields.forEach((field) => {
    if (field.type === 'divider') return;
    data[field.key] = field.defaultValue ?? null;
  });
  formData.value = data;
}

// 初始化验证规则
function initFormRules() {
  Object.keys(formRules).forEach((key) => delete formRules[key]);
  props.config.fields.forEach((field) => {
    if (field.type === 'divider') return;
    const rules = getFieldRules(field);
    if (rules && rules.length > 0) {
      formRules[field.key] = rules;
    }
  });
}

// 获取字段验证规则
function getFieldRules(field: FormField): any[] {
  if (!field.rules || field.rules.length === 0) return [];

  return field.rules.map((rule) => {
    const elRule: any = {};

    if (rule.required) {
      elRule.required = true;
      elRule.message = rule.message || `${field.label || field.key}不能为空`;
    }

    if (rule.pattern) {
      elRule.pattern = rule.pattern instanceof RegExp ? rule.pattern : new RegExp(rule.pattern);
      elRule.message = rule.message || `${field.label || field.key}格式不正确`;
    }

    if (rule.min !== undefined) {
      elRule.min = rule.min;
      elRule.message = rule.message || `${field.label || field.key}长度不能少于${rule.min}`;
    }

    if (rule.max !== undefined) {
      elRule.max = rule.max;
      elRule.message = rule.message || `${field.label || field.key}长度不能超过${rule.max}`;
    }

    if (rule.validator) {
      elRule.validator = rule.validator;
    }

    return elRule;
  });
}

// 监听外部 modelValue 变化
watch(
  () => props.modelValue,
  (newVal) => {
    if (!newVal) {
      return;
    }
    const nextValue = { ...formData.value, ...newVal };
    if (!isSameValue(formData.value, nextValue)) {
      formData.value = nextValue;
    }
  },
  { deep: true, immediate: true }
);

// 表单配置变化时重建默认数据和验证规则，支撑动态表单设计器实时预览。
watch(
  () => props.config,
  () => {
    initFormData();
    initFormRules();
  },
  { deep: true }
);

// 监听表单数据变化，同步到外部
watch(
  formData,
  (newVal) => {
    if (!isSameValue(props.modelValue, newVal)) {
      emit('update:modelValue', { ...newVal });
    }
  },
  { deep: true }
);

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return;

  try {
    await formRef.value.validate();
    emit('submit', formData.value);
  } catch (error) {
    emit('validate', false);
  }
}

// 重置表单
function handleReset() {
  if (!formRef.value) return;
  formRef.value.resetFields();
  emit('reset');
  emit('validate', true);
}

// 获取表单数据
function getValue(): Record<string, any> {
  return { ...formData.value };
}

// 设置表单数据
function setValue(value: Record<string, any>) {
  Object.keys(value).forEach((key) => {
    if (formData.value.hasOwnProperty(key)) {
      formData.value[key] = value[key];
    }
  });
}

// 获取指定字段值
function getFieldValue(key: string): any {
  return formData.value[key];
}

// 设置指定字段值
function setFieldValue(key: string, value: any) {
  if (formData.value.hasOwnProperty(key)) {
    formData.value[key] = value;
  }
}

// 显示指定字段
function showField(key: string) {
  const field = props.config.fields.find((f) => f.key === key);
  if (field) {
    field.show = true;
  }
}

// 隐藏指定字段
function hideField(key: string) {
  const field = props.config.fields.find((f) => f.key === key);
  if (field) {
    field.show = false;
  }
}

// 启用指定字段
function enableField(key: string) {
  const field = props.config.fields.find((f) => f.key === key);
  if (field) {
    field.disabled = false;
  }
}

// 禁用指定字段
function disableField(key: string) {
  const field = props.config.fields.find((f) => f.key === key);
  if (field) {
    field.disabled = true;
  }
}

// 验证表单
async function validate(): Promise<boolean> {
  if (!formRef.value) return false;

  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
}

// 重置表单（内部）
function reset() {
  if (!formRef.value) return;
  formRef.value.resetFields();
}

// 初始化
initFormData();
initFormRules();

// 暴露方法
defineExpose<FormCreateExpose>({
  getValue,
  setValue,
  reset,
  validate,
  getFieldValue,
  setFieldValue,
  showField,
  hideField,
  enableField,
  disableField,
});
</script>

<style scoped lang="scss">
.form-create {
  &.form-create-inline {
    :deep(.el-form-item) {
      display: inline-block;
      margin-right: 16px;
    }
  }
}

.form-create-divider {
  margin-bottom: 16px;

  :deep(.el-divider) {
    margin: 0;
  }
}

.form-create-actions {
  margin-top: 24px;

  :deep(.el-form-item__content) {
    justify-content: flex-end;
  }
}
</style>
