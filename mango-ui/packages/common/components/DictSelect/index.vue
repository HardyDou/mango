<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :loading="loading"
    :multiple="multiple"
    :filterable="filterable"
    @update:model-value="emit('update:modelValue', $event)"
    @change="emit('change', $event)"
  >
    <el-option
      v-for="item in options"
      :key="item.value"
      :label="item.label"
      :value="numberValue ? Number(item.value) : item.value"
    />
  </el-select>
</template>

<script setup lang="ts" name="DictSelect">
import { useDict } from '../../hooks/useDict';

const props = withDefaults(defineProps<{
  modelValue?: string | number | Array<string | number>;
  dictType: string;
  placeholder?: string;
  clearable?: boolean;
  disabled?: boolean;
  multiple?: boolean;
  filterable?: boolean;
  numberValue?: boolean;
}>(), {
  modelValue: undefined,
  placeholder: '请选择',
  clearable: true,
  disabled: false,
  multiple: false,
  filterable: false,
  numberValue: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | Array<string | number> | undefined): void;
  (e: 'change', value: string | number | Array<string | number> | undefined): void;
}>();

const { options, loading } = useDict(props.dictType);
</script>
