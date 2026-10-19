<template>
  <form-create
    v-model:api="api"
    :rule="rules"
    :option="options"
    :model-value="values"
    :disabled="disabled"
    :preview="preview"
    class="information-runtime-form"
    @update:model-value="emit('update:values', $event)"
  />
</template>

<script setup lang="ts">
import { getCurrentInstance, ref, watch } from 'vue';
import formCreate from '@form-create/element-ui';
import type {
  Api as FormCreateApi,
  Options as FormCreateOptions,
  Rule as FormCreateRule,
} from '@form-create/element-ui';
import type { FormCreateRule as InformationRule } from '../domain/rules';

const props = withDefaults(
  defineProps<{
    rules: InformationRule[];
    values?: Record<string, unknown>;
    disabled?: boolean;
    preview?: boolean;
    /** 结构变化时重建表单，避免规则与内部状态不一致 */
    runtimeKey?: string | number;
  }>(),
  {
    values: () => ({}),
    disabled: false,
    preview: false,
    runtimeKey: 0,
  },
);

const emit = defineEmits<{
  'update:values': [values: Record<string, unknown>];
  submit: [values: Record<string, unknown>];
}>();

/**
 * 运行时直接使用表单设计器同源的 form-create 渲染，
 * 设计器里能用到的控件（评分、滑块、上传、子表单、布局等）在托管页面保持同样形态。
 */
const instance = getCurrentInstance();
instance?.appContext.app.use(formCreate as never);

const api = ref<FormCreateApi>();
const options: FormCreateOptions = {
  form: { labelPosition: 'top', labelWidth: 'auto', showMessage: true },
  submitBtn: { show: false },
  resetBtn: { show: false },
  onSubmit: (formData: Record<string, unknown>) => emit('submit', formData),
};

/** 设计器产出的规则结构与 form-create 的 Rule 一致，这里只做类型收窄。 */
const rules = ref(props.rules as unknown as FormCreateRule[]);

watch(
  () => props.rules,
  (next) => {
    rules.value = next as unknown as FormCreateRule[];
    api.value?.reloadRule?.(rules.value as never);
  },
);
watch(
  () => props.runtimeKey,
  () => {
    rules.value = props.rules as unknown as FormCreateRule[];
  },
);

watch(
  () => props.values,
  (next) => {
    if (next && api.value) {
      api.value.coverValue(next);
    }
  },
);

defineExpose({
  /** 校验通过返回 true，失败时由 form-create 在字段上展示错误。 */
  async validate(): Promise<boolean> {
    if (!api.value) {
      return false;
    }
    try {
      await api.value.validate();
      return true;
    } catch {
      return false;
    }
  },
  formData(): Record<string, unknown> {
    return (api.value?.formData() as Record<string, unknown>) || {};
  },
  reset(): void {
    api.value?.resetFields();
  },
  clear(): void {
    if (api.value) {
      api.value.coverValue({});
    }
  },
});
</script>
