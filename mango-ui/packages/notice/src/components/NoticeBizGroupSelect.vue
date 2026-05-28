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
    <el-option v-for="item in options" :key="item" :label="item" :value="item" />
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getBusinessTypes } from '../api/notice';

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

const loading = ref(false);
const bizGroups = ref<string[]>([]);

const options = computed(() => {
  const values = new Set<string>();
  bizGroups.value.forEach(item => values.add(item));
  if (props.modelValue) {
    values.add(props.modelValue);
  }
  return Array.from(values).sort((left, right) => left.localeCompare(right, 'zh-CN'));
});

function handleUpdate(value: string) {
  emit('update:modelValue', value || '');
}

function handleChange(value: string) {
  emit('change', value || '');
}

async function loadBizGroups() {
  loading.value = true;
  try {
    const result = await getBusinessTypes({ pageNum: 1, pageSize: 200 });
    bizGroups.value = Array.from(new Set(
      (result.list || [])
        .map(item => item.bizGroup?.trim())
        .filter((item): item is string => Boolean(item)),
    ));
  } finally {
    loading.value = false;
  }
}

onMounted(loadBizGroups);
</script>
