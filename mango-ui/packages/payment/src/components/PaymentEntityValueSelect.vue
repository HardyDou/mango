<template>
  <el-select
    :model-value="resolvedModelValue"
    :multiple="!single"
    clearable
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
      :key="option.value"
      :label="option.label"
      :value="option.value"
    >
      <span>{{ option.label }}</span>
      <span v-if="option.description" class="payment-select__description">{{ option.description }}</span>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { PaymentRecord, PaymentResourceApi } from '../api/payment';

interface PaymentValueOption {
  value: string;
  label: string;
  description?: string;
}

const props = withDefaults(defineProps<{
  modelValue?: string;
  api: PaymentResourceApi<PaymentRecord>;
  valueField: string;
  labelField: string;
  descriptionField?: string;
  placeholder?: string;
  single?: boolean;
}>(), {
  placeholder: '请选择',
  single: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
}>();

const loading = ref(false);
const options = ref<PaymentValueOption[]>([]);
const modelArray = computed(() => (props.modelValue || '').split(',').map(item => item.trim()).filter(Boolean));
const resolvedModelValue = computed(() => (props.single ? props.modelValue || '' : modelArray.value));

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
      .map(item => ({
        value: String(item[props.valueField] || ''),
        label: String(item[props.labelField] || item[props.valueField] || ''),
        description: props.descriptionField ? String(item[props.descriptionField] || '') : '',
      }))
      .filter(option => option.value);
  } finally {
    loading.value = false;
  }
}

function updateValue(value: string[] | string) {
  emit('update:modelValue', Array.isArray(value) ? value.join(',') : value);
}
</script>

<style scoped>
.payment-select__description {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
