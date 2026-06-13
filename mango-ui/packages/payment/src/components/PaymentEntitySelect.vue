<template>
  <el-select
    :model-value="resolvedModelValue"
    :multiple="multiple"
    :clearable="clearable"
    filterable
    remote
    :reserve-keyword="false"
    :remote-method="search"
    :loading="loading"
    :placeholder="placeholder"
    @update:model-value="updateValue"
  >
    <el-option
      v-for="option in options"
      :key="String(option.value)"
      :label="option.label"
      :value="option.value"
    >
      <span>{{ option.label }}</span>
      <span v-if="option.description" class="payment-select__description">{{ option.description }}</span>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { ApiId } from '@mango/api-schema';
import type { PaymentRecord, PaymentResourceApi } from '../api/payment';

export interface PaymentSelectOption {
  value: ApiId;
  label: string;
  description?: string;
}

const props = withDefaults(defineProps<{
  modelValue?: ApiId | ApiId[] | string;
  api: PaymentResourceApi<PaymentRecord>;
  labelField: string;
  descriptionField?: string;
  placeholder?: string;
  multiple?: boolean;
  clearable?: boolean;
  valueFormat?: 'array' | 'csv';
}>(), {
  placeholder: '请选择',
  multiple: false,
  clearable: true,
  valueFormat: 'array',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: ApiId | ApiId[] | string): void;
  (event: 'loaded', options: PaymentSelectOption[]): void;
}>();

const loading = ref(false);
const options = ref<PaymentSelectOption[]>([]);
const resolvedModelValue = computed(() => {
  if (!props.multiple || props.valueFormat !== 'csv' || typeof props.modelValue !== 'string') {
    return props.modelValue;
  }
  return props.modelValue.split(',').map(item => item.trim()).filter(Boolean);
});

watch(options, value => emit('loaded', value), { deep: true });
onMounted(() => search(''));

async function search(keyword: string) {
  loading.value = true;
  try {
    const result = await props.api.page({
      pageNum: 1,
      pageSize: 50,
      keyword,
      status: 1,
    });
    options.value = result.list
      .filter(item => item.id !== undefined && item.id !== null)
      .map(item => ({
        value: item.id as ApiId,
        label: String(item[props.labelField] || item.name || item.id),
        description: props.descriptionField ? String(item[props.descriptionField] || '') : '',
      }));
  } finally {
    loading.value = false;
  }
}

function updateValue(value: ApiId | ApiId[] | string) {
  if (props.multiple && props.valueFormat === 'csv' && Array.isArray(value)) {
    emit('update:modelValue', value.map(item => String(item)).join(','));
    return;
  }
  emit('update:modelValue', value);
}
</script>

<style scoped>
.payment-select__description {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
