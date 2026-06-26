<template>
  <el-select
    v-bind="$attrs"
    :model-value="modelValue"
    :allow-create="allowCreate"
    :clearable="clearable"
    :default-first-option="allowCreate"
    :filterable="filterable"
    :loading="loading"
    :placeholder="placeholder"
    @change="handleChange"
    @update:model-value="handleUpdate"
  >
    <el-option v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useNoticeDomains } from './useNoticeDomains';

defineOptions({
  inheritAttrs: false,
});

const props = withDefaults(defineProps<{
  modelValue?: string;
  placeholder?: string;
  clearable?: boolean;
  filterable?: boolean;
  allowCreate?: boolean;
}>(), {
  modelValue: '',
  placeholder: '请选择业务域',
  clearable: true,
  filterable: true,
  allowCreate: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void;
  (event: 'change', value: string): void;
}>();

const { domainLoading: loading, domainOptions, loadDomains } = useNoticeDomains();

const options = computed(() => {
  const values = new Map(domainOptions.value.map(item => [item.value, item.label]));
  if (props.modelValue) {
    values.set(props.modelValue, values.get(props.modelValue) || props.modelValue);
  }
  return Array.from(values.entries())
    .map(([value, label]) => ({ value, label }))
    .sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'));
});

function handleUpdate(value: string) {
  emit('update:modelValue', value || '');
}

function handleChange(value: string) {
  emit('change', value || '');
}

onMounted(loadDomains);
</script>
