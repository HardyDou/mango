<template>
  <div class="information-surface" :class="{ 'information-surface--plain': plain }">
    <div class="information-surface__head">
      <h2 class="information-surface__title">{{ formName || '未命名表单' }}</h2>
      <p v-if="description" class="information-surface__desc">{{ description }}</p>
    </div>
    <FormCreate
      :key="surfaceKey"
      :config="config"
      :model-value="values"
      :show-actions="false"
      :disabled="disabled"
      @update:model-value="emit('update:values', $event)"
    />
    <div v-if="showSubmit" class="information-surface__actions">
      <el-button type="primary" :loading="submitting" :disabled="disabled" @click="emit('submit')">
        {{ submitLabel }}
      </el-button>
    </div>
    <p v-if="footnote" class="information-surface__foot">{{ footnote }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { FormCreate } from '@mango/common';
import type { FormCreateRule } from '../domain/rules';
import { rulesToMangoFields } from '../domain/rules';

const props = withDefaults(
  defineProps<{
    formName?: string;
    description?: string;
    rules: FormCreateRule[];
    values?: Record<string, unknown>;
    submitLabel?: string;
    showSubmit?: boolean;
    submitting?: boolean;
    disabled?: boolean;
    plain?: boolean;
    footnote?: string;
    surfaceKey?: string | number;
  }>(),
  {
    formName: '',
    description: '',
    values: () => ({}),
    submitLabel: '提交',
    showSubmit: true,
    submitting: false,
    disabled: false,
    plain: false,
    footnote: '',
    surfaceKey: 0,
  },
);

const emit = defineEmits<{
  submit: [];
  'update:values': [values: Record<string, unknown>];
}>();

const config = computed(() => ({
  name: `information-surface-${props.surfaceKey}`,
  fields: rulesToMangoFields(props.rules),
  labelPosition: 'top' as const,
  showRequiredMark: true,
}));
</script>
