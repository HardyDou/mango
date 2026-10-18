<template>
  <div class="information-canvas-card">
    <div class="information-canvas-title">{{ formName || '未命名表单' }}</div>
    <div v-if="description" class="information-canvas-desc">{{ description }}</div>

    <div
      v-for="(field, index) in fields"
      :key="field.fieldCode + index"
      class="information-canvas-field"
      :class="{ 'is-selected': index === activeIndex }"
      :data-field-code="field.fieldCode"
      role="button"
      tabindex="0"
      @click="emit('select', index)"
      @keydown.enter.prevent="emit('select', index)"
      @keydown.space.prevent="emit('select', index)"
    >
      <template v-if="field.fieldType === 'divider'">
        <div class="information-canvas-divider" />
      </template>
      <template v-else-if="field.fieldType === 'static'">
        <div class="information-canvas-static">{{ field.content || '说明文字' }}</div>
      </template>
      <template v-else>
        <label class="information-canvas-label">
          {{ field.fieldName || '未命名字段' }}
          <span v-if="field.required" class="information-canvas-required">*</span>
        </label>
        <div
          class="information-canvas-placeholder"
          :class="{ 'information-canvas-placeholder--textarea': field.fieldType === 'textarea' }"
        >
          {{ placeholderText(field) }}
        </div>
      </template>
      <button
        type="button"
        class="information-canvas-remove"
        :aria-label="`删除字段 ${field.fieldName || field.fieldCode}`"
        @click.stop="emit('remove', index)"
      >
        ×
      </button>
    </div>

    <el-empty v-if="!fields.length" description="从左侧添加控件" :image-size="60" />
    <button type="button" class="information-canvas-submit" disabled>{{ submitLabel }}</button>
  </div>
</template>

<script setup lang="ts">
import { needsOptions, type InformationFieldDefinition } from '../domain/fields';

withDefaults(
  defineProps<{
    formName?: string;
    description?: string;
    fields: InformationFieldDefinition[];
    activeIndex?: number;
    submitLabel?: string;
  }>(),
  {
    formName: '',
    description: '',
    activeIndex: -1,
    submitLabel: '提交',
  },
);

const emit = defineEmits<{
  select: [index: number];
  remove: [index: number];
}>();

function placeholderText(field: InformationFieldDefinition): string {
  if (field.placeholder) {
    return field.placeholder;
  }
  if (needsOptions(field.fieldType)) {
    return `请选择${field.fieldName}`;
  }
  if (field.fieldType === 'divider') {
    return '分割线';
  }
  if (field.fieldType === 'static') {
    return '说明文字';
  }
  if (field.fieldType === 'date') {
    return `请选择${field.fieldName}`;
  }
  return `请输入${field.fieldName}`;
}
</script>
