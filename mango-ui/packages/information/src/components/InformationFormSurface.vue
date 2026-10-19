<template>
  <div class="information-surface" :class="{ 'information-surface--plain': plain }">
    <div class="information-surface__head">
      <h2 class="information-surface__title">{{ formName || '未命名表单' }}</h2>
      <p v-if="description" class="information-surface__desc">{{ description }}</p>
    </div>
    <InformationFormRuntime
      ref="runtimeRef"
      :key="runtimeKeyValue"
      :rules="rules"
      :values="values"
      :runtime-key="runtimeKeyValue"
      :disabled="disabled"
      :preview="disabled"
      @update:values="emit('update:values', $event)"
    />
    <div v-if="showSubmit" class="information-surface__actions">
      <el-button type="primary" :loading="submitting" :disabled="disabled" @click="onSubmit">
        {{ submitLabel }}
      </el-button>
    </div>
    <p v-if="footnote" class="information-surface__foot">{{ footnote }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import InformationFormRuntime from './InformationFormRuntime.vue';
import type { FormCreateRule } from '../domain/rules';

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
    runtimeKey?: string | number;
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
    runtimeKey: 0,
  },
);

const emit = defineEmits<{
  submit: [];
  'update:values': [values: Record<string, unknown>];
}>();

const runtimeRef = ref<InstanceType<typeof InformationFormRuntime>>();
const runtimeKeyValue = computed(() => props.runtimeKey);

function onSubmit() {
  emit('submit');
}

defineExpose({
  validate: () => runtimeRef.value?.validate() ?? Promise.resolve(false),
  formData: () => runtimeRef.value?.formData() ?? {},
  reset: () => runtimeRef.value?.reset(),
});
</script>
