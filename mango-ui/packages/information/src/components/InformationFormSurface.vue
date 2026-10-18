<template>
  <div class="information-surface" :class="{ 'information-surface--plain': plain }">
    <div class="information-surface__head">
      <h2 class="information-surface__title">{{ formName || '未命名表单' }}</h2>
      <p v-if="description" class="information-surface__desc">{{ description }}</p>
    </div>
    <div class="information-surface__fields">
      <template v-for="field in fields" :key="field.fieldCode">
        <div v-if="field.fieldType === 'divider'" class="information-surface__divider" />
        <div v-else-if="field.fieldType === 'static'" class="information-surface__static">{{ field.content }}</div>
        <div v-else class="information-surface__field">
          <label class="information-surface__label">
            {{ field.fieldName }}
            <span v-if="field.required" class="information-surface__required">*</span>
          </label>
          <el-select
            v-if="field.fieldType === 'select'"
            :model-value="values[field.fieldCode] as string"
            class="information-surface__control"
            :placeholder="field.placeholder || `请选择${field.fieldName}`"
            :disabled="disabled"
            @update:model-value="(value: string) => emitValue(field, value)"
          >
            <el-option
              v-for="option in field.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-radio-group
            v-else-if="field.fieldType === 'radio'"
            :model-value="values[field.fieldCode] as string"
            :disabled="disabled"
            @update:model-value="(value: string | number | boolean | undefined) => emitValue(field, value)"
          >
            <el-radio v-for="option in field.options || []" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group
            v-else-if="field.fieldType === 'checkbox'"
            :model-value="(values[field.fieldCode] as string[]) || []"
            :disabled="disabled"
            @update:model-value="(value: (string | number)[]) => emitValue(field, value)"
          >
            <el-checkbox v-for="option in field.options || []" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
          <el-date-picker
            v-else-if="field.fieldType === 'date'"
            :model-value="values[field.fieldCode] as string"
            type="date"
            value-format="YYYY-MM-DD"
            class="information-surface__control"
            :placeholder="field.placeholder || `请选择${field.fieldName}`"
            :disabled="disabled"
            @update:model-value="(value: string) => emitValue(field, value)"
          />
          <el-input-number
            v-else-if="field.fieldType === 'number'"
            :model-value="values[field.fieldCode] as number"
            class="information-surface__control"
            :disabled="disabled"
            @update:model-value="(value: number | undefined) => emitValue(field, value)"
          />
          <el-input
            v-else-if="field.fieldType === 'textarea'"
            :model-value="values[field.fieldCode] as string"
            type="textarea"
            :rows="3"
            :placeholder="field.placeholder || `请输入${field.fieldName}`"
            :disabled="disabled"
            @update:model-value="(value: string) => emitValue(field, value)"
          />
          <el-input
            v-else
            :model-value="values[field.fieldCode] as string"
            :placeholder="field.placeholder || `请输入${field.fieldName}`"
            :disabled="disabled"
            @update:model-value="(value: string) => emitValue(field, value)"
          />
        </div>
      </template>
      <el-empty v-if="!fields.length" description="尚未配置采集项" />
    </div>
    <div v-if="showSubmit" class="information-surface__actions">
      <el-button type="primary" :loading="submitting" :disabled="disabled" @click="emit('submit')">
        {{ submitLabel }}
      </el-button>
    </div>
    <p v-if="footnote" class="information-surface__foot">{{ footnote }}</p>
  </div>
</template>

<script setup lang="ts">
import type { InformationFieldDefinition } from '../domain/fields';

withDefaults(
  defineProps<{
    formName?: string;
    description?: string;
    fields: InformationFieldDefinition[];
    values?: Record<string, unknown>;
    submitLabel?: string;
    showSubmit?: boolean;
    submitting?: boolean;
    disabled?: boolean;
    plain?: boolean;
    footnote?: string;
  }>(),
  {
    description: '',
    values: () => ({}),
    submitLabel: '提交',
    showSubmit: true,
    submitting: false,
    disabled: false,
    plain: false,
    footnote: '',
  },
);

const emit = defineEmits<{
  submit: [];
  'update:value': [fieldCode: string, value: unknown];
}>();

function emitValue(field: InformationFieldDefinition, value: unknown) {
  emit('update:value', field.fieldCode, value);
}
</script>
