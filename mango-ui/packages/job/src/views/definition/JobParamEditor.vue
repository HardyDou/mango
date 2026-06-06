<template>
  <div class="job-param-editor">
    <div class="job-param-editor-head">
      <el-segmented v-model="mode" :options="modeOptions" />
      <span class="job-muted">{{ schemaFields.length > 0 ? '按参数 Schema 生成表单' : '未配置可渲染 Schema' }}</span>
    </div>

    <el-alert
      v-if="schemaError"
      class="job-param-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="schemaError"
    />
    <el-alert
      v-if="jsonError"
      class="job-param-alert"
      type="error"
      :closable="false"
      show-icon
      :title="jsonError"
    />

    <el-form v-if="mode === 'FORM' && schemaFields.length > 0" class="job-param-form" label-width="120px">
      <el-row :gutter="14">
        <el-col v-for="field in schemaFields" :key="field.key" :span="fieldSpan(field)">
          <el-form-item :label="field.label" :required="field.required" :error="fieldErrors[field.key] || ''">
            <el-select
              v-if="field.enumValues"
              :model-value="formState[field.key]"
              clearable
              :placeholder="field.description || '请选择'"
              style="width: 100%"
              @update:model-value="value => updateField(field, value)"
            >
              <el-option
                v-for="option in field.enumValues"
                :key="String(option.value)"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <el-switch
              v-else-if="field.inputType === 'boolean'"
              :model-value="Boolean(formState[field.key])"
              @update:model-value="value => updateField(field, value)"
            />
            <el-input-number
              v-else-if="field.inputType === 'number'"
              :model-value="numericValue(formState[field.key])"
              :min="field.minimum"
              :max="field.maximum"
              style="width: 100%"
              @update:model-value="value => updateField(field, value)"
            />
            <el-date-picker
              v-else-if="field.inputType === 'date'"
              :model-value="stringValue(formState[field.key])"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
              @update:model-value="value => updateField(field, value || '')"
            />
            <el-date-picker
              v-else-if="field.inputType === 'datetime'"
              :model-value="stringValue(formState[field.key])"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择时间"
              style="width: 100%"
              @update:model-value="value => updateField(field, value || '')"
            />
            <el-input
              v-else
              :model-value="stringValue(formState[field.key])"
              clearable
              :placeholder="field.description || '请输入'"
              @update:model-value="value => updateField(field, value)"
            />
            <div v-if="field.description" class="job-param-help">{{ field.description }}</div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-empty v-else-if="mode === 'FORM'" description="当前 Schema 暂不能渲染为表单，请使用 JSON 模式" />

    <el-input
      v-else
      :model-value="jsonText"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      @update:model-value="updateJsonText"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';

type EditorMode = 'FORM' | 'JSON';
type JsonValue = string | number | boolean | null | JsonObject | JsonValue[];
type JsonObject = Record<string, JsonValue>;

interface JsonSchemaProperty {
  type?: string | string[];
  title?: string;
  description?: string;
  default?: JsonValue;
  enum?: JsonValue[];
  format?: string;
  minimum?: number;
  maximum?: number;
}

interface JsonObjectSchema {
  type?: string;
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
}

interface SchemaField {
  key: string;
  label: string;
  description?: string;
  required: boolean;
  inputType: 'string' | 'number' | 'boolean' | 'date' | 'datetime';
  enumValues?: Array<{ label: string; value: JsonValue }>;
  defaultValue?: JsonValue;
  minimum?: number;
  maximum?: number;
}

const props = withDefaults(defineProps<{
  schemaText?: string;
  modelValue?: string;
  rows?: number;
  placeholder?: string;
}>(), {
  schemaText: '',
  modelValue: '',
  rows: 4,
  placeholder: 'JSON，可为空',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
}>();

const modeOptions = [
  { label: '表单', value: 'FORM' },
  { label: 'JSON', value: 'JSON' },
] as const;

const mode = ref<EditorMode>('FORM');
const jsonText = ref('');
const jsonError = ref('');
const originalObject = ref<JsonObject>({});
const formState = reactive<Record<string, JsonValue>>({});
const fieldErrors = reactive<Record<string, string>>({});
let syncingFromProps = false;

const parsedSchema = computed(() => parseSchema(props.schemaText));
const schemaError = computed(() => parsedSchema.value.error);
const schemaFields = computed(() => parsedSchema.value.fields);

watch(
  () => [props.modelValue, props.schemaText] as const,
  () => syncFromProps(),
  { immediate: true },
);

watch(schemaFields, (fields) => {
  if (fields.length === 0) {
    mode.value = 'JSON';
    return;
  }
  if (mode.value === 'JSON') {
    mode.value = 'FORM';
  }
});

function syncFromProps() {
  syncingFromProps = true;
  jsonText.value = props.modelValue || '';
  const objectValue = parseObjectValue(props.modelValue);
  originalObject.value = objectValue;
  schemaFields.value.forEach((field) => {
    formState[field.key] = normalizeFieldValue(field, objectValue[field.key]);
  });
  clearRemovedFields();
  syncingFromProps = false;
}

function clearRemovedFields() {
  const keys = new Set(schemaFields.value.map(field => field.key));
  Object.keys(formState).forEach((key) => {
    if (!keys.has(key)) {
      delete formState[key];
    }
  });
  Object.keys(fieldErrors).forEach((key) => {
    if (!keys.has(key)) {
      delete fieldErrors[key];
    }
  });
}

function updateField(field: SchemaField, value: JsonValue) {
  formState[field.key] = normalizeFieldValue(field, value);
  delete fieldErrors[field.key];
  emitObjectValue();
}

function emitObjectValue() {
  if (syncingFromProps) {
    return;
  }
  const objectValue: JsonObject = { ...originalObject.value };
  schemaFields.value.forEach((field) => {
    const value = formState[field.key];
    if (value !== '' && value !== undefined && value !== null) {
      objectValue[field.key] = value;
    } else {
      delete objectValue[field.key];
    }
  });
  const next = Object.keys(objectValue).length === 0 ? '' : JSON.stringify(objectValue, null, 2);
  originalObject.value = objectValue;
  jsonText.value = next;
  jsonError.value = '';
  emit('update:modelValue', next);
}

function updateJsonText(value: string) {
  jsonText.value = value;
  jsonError.value = '';
  emit('update:modelValue', value);
}

function validate() {
  jsonError.value = '';
  Object.keys(fieldErrors).forEach(key => delete fieldErrors[key]);
  if (mode.value === 'JSON' || schemaFields.value.length === 0) {
    if (!jsonText.value.trim()) {
      return true;
    }
    try {
      JSON.parse(jsonText.value);
      return true;
    } catch {
      jsonError.value = '参数 JSON 不是合法 JSON';
      return false;
    }
  }
  let valid = true;
  schemaFields.value.forEach((field) => {
    const value = formState[field.key];
    if (field.required && (value === '' || value === undefined || value === null)) {
      fieldErrors[field.key] = '必填';
      valid = false;
      return;
    }
    if (value === '' || value === undefined || value === null) {
      return;
    }
    if (field.inputType === 'number' && typeof value !== 'number') {
      fieldErrors[field.key] = '请输入数字';
      valid = false;
    }
  });
  return valid;
}

function parseSchema(schemaText?: string): { fields: SchemaField[]; error: string } {
  if (!schemaText?.trim()) {
    return { fields: [], error: '' };
  }
  let schema: unknown;
  try {
    schema = JSON.parse(schemaText);
  } catch {
    return { fields: [], error: '参数 Schema 不是合法 JSON，无法生成表单' };
  }
  if (!isRecord(schema)) {
    return { fields: [], error: '参数 Schema 必须是对象' };
  }
  const objectSchema = schema as JsonObjectSchema;
  if (!isRecord(objectSchema.properties)) {
    return { fields: [], error: '参数 Schema 未声明 properties' };
  }
  const required = new Set(Array.isArray(objectSchema.required) ? objectSchema.required : []);
  const fields = Object.entries(objectSchema.properties)
    .filter((entry): entry is [string, JsonSchemaProperty] => isRecord(entry[1]))
    .map(([key, property]) => toSchemaField(key, property, required.has(key)));
  return { fields, error: '' };
}

function toSchemaField(key: string, property: JsonSchemaProperty, required: boolean): SchemaField {
  const type = resolveType(property);
  const inputType = resolveInputType(type, property.format);
  return {
    key,
    label: property.title || key,
    description: property.description,
    required,
    inputType,
    enumValues: Array.isArray(property.enum)
      ? property.enum.map(value => ({ label: String(value), value }))
      : undefined,
    defaultValue: property.default,
    minimum: property.minimum,
    maximum: property.maximum,
  };
}

function resolveType(property: JsonSchemaProperty) {
  if (Array.isArray(property.type)) {
    return property.type.find(item => item !== 'null') || 'string';
  }
  return property.type || 'string';
}

function resolveInputType(type: string, format?: string): SchemaField['inputType'] {
  if (type === 'boolean') {
    return 'boolean';
  }
  if (type === 'number' || type === 'integer') {
    return 'number';
  }
  if (format === 'date') {
    return 'date';
  }
  if (format === 'date-time' || format === 'datetime') {
    return 'datetime';
  }
  return 'string';
}

function parseObjectValue(value?: string): JsonObject {
  if (!value?.trim()) {
    return {};
  }
  try {
    const parsed = JSON.parse(value);
    return isRecord(parsed) ? parsed as JsonObject : {};
  } catch {
    return {};
  }
}

function normalizeFieldValue(field: SchemaField, value: unknown): JsonValue {
  const resolved = value === undefined ? field.defaultValue : value;
  if (field.inputType === 'boolean') {
    if (resolved === undefined || resolved === null || resolved === '') {
      return null;
    }
    return Boolean(resolved);
  }
  if (field.inputType === 'number') {
    if (typeof resolved === 'number') {
      return resolved;
    }
    if (typeof resolved === 'string' && resolved.trim()) {
      const numberValue = Number(resolved);
      return Number.isFinite(numberValue) ? numberValue : null;
    }
    return null;
  }
  if (typeof resolved === 'string') {
    return resolved;
  }
  if (resolved === undefined || resolved === null) {
    return '';
  }
  return String(resolved);
}

function numericValue(value: JsonValue | undefined) {
  return typeof value === 'number' ? value : null;
}

function stringValue(value: JsonValue | undefined) {
  return typeof value === 'string' ? value : '';
}

function fieldSpan(field: SchemaField) {
  return field.inputType === 'boolean' ? 12 : 24;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

defineExpose({ validate });
</script>

<style scoped>
.job-param-editor {
  width: 100%;
}

.job-param-editor-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.job-param-alert {
  margin-bottom: 10px;
}

.job-param-form {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px 12px 0;
  background: var(--el-fill-color-extra-light);
}

.job-param-help {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}
</style>
