<template>
  <el-select
    class="dict-select"
    :model-value="selectValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :loading="loading"
    :multiple="multiple"
    :filterable="filterable"
    :collapse-tags="collapseTags"
    :collapse-tags-tooltip="collapseTagsTooltip"
    :multiple-limit="multipleLimit"
    :reserve-keyword="reserveKeyword"
    @update:model-value="handleUpdate"
    @change="handleChange"
  >
    <el-option
      v-if="showAnyOption && !multiple"
      :label="anyOptionLabel"
      :value="anyOptionValue"
    />
    <el-option
      v-for="item in normalizedOptions"
      :key="item.value"
      :label="item.label"
      :value="item.value"
    />
  </el-select>
</template>

<script setup lang="ts" name="DictSelect">
import { computed } from 'vue';
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
  showAnyOption?: boolean;
  anyOptionLabel?: string;
  anyOptionValue?: string | number;
  collapseTags?: boolean;
  collapseTagsTooltip?: boolean;
  multipleLimit?: number;
  reserveKeyword?: boolean;
}>(), {
  modelValue: undefined,
  placeholder: '请选择',
  clearable: true,
  disabled: false,
  multiple: false,
  filterable: false,
  numberValue: false,
  showAnyOption: false,
  anyOptionLabel: '不限',
  anyOptionValue: '__ALL__',
  collapseTags: true,
  collapseTagsTooltip: true,
  multipleLimit: 0,
  reserveKeyword: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | Array<string | number> | undefined): void;
  (e: 'change', value: string | number | Array<string | number> | undefined): void;
}>();

const { options, loading } = useDict(props.dictType);

const isEmptyValue = (value: unknown) => value === undefined || value === null || value === '';

const selectValue = computed(() => {
  if (props.showAnyOption && !props.multiple && isEmptyValue(props.modelValue)) {
    return props.anyOptionValue;
  }
  return props.modelValue;
});

const normalizedOptions = computed(() => {
  return options.value.map((item) => ({
    ...item,
    value: props.numberValue ? Number(item.value) : item.value,
  }));
});

function normalizeValue(value: string | number | Array<string | number> | undefined) {
  if (props.showAnyOption && !props.multiple && value === props.anyOptionValue) {
    return undefined;
  }
  return value;
}

function handleUpdate(value: string | number | Array<string | number> | undefined) {
  emit('update:modelValue', normalizeValue(value));
}

function handleChange(value: string | number | Array<string | number> | undefined) {
  emit('change', normalizeValue(value));
}
</script>

<style scoped>
.dict-select {
  min-width: 180px;
}
</style>
