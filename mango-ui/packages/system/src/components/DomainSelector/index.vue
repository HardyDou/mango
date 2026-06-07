<template>
  <el-tree-select
    v-model="innerValue"
    :data="options"
    :props="treeProps"
    node-key="id"
    :multiple="multiple"
    :check-strictly="checkStrictly"
    :clearable="clearable"
    :disabled="disabled"
    :placeholder="placeholder"
    :loading="loading"
    filterable
    default-expand-all
    class="domain-selector"
    @change="emitChange"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { ApiId } from '@mango/api-schema';
import { domainApi, type DomainItem } from '../../api/domain';

type DomainSelectorValue = ApiId | ApiId[] | undefined;

const props = withDefaults(defineProps<{
  modelValue?: DomainSelectorValue;
  multiple?: boolean;
  clearable?: boolean;
  disabled?: boolean;
  checkStrictly?: boolean;
  placeholder?: string;
}>(), {
  multiple: false,
  clearable: true,
  disabled: false,
  checkStrictly: true,
  placeholder: '请选择业务域',
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: DomainSelectorValue): void;
  (event: 'change', value: DomainSelectorValue): void;
}>();

const loading = ref(false);
const options = ref<DomainItem[]>([]);
const innerValue = ref<DomainSelectorValue>(props.modelValue);

const treeProps = {
  label: 'domainName',
  value: 'id',
  children: 'children',
};

watch(
  () => props.modelValue,
  value => {
    innerValue.value = value;
  },
);

const normalizedOptions = computed(() => options.value);

onMounted(loadOptions);

async function loadOptions() {
  loading.value = true;
  try {
    options.value = await domainApi.enabledTree();
  } finally {
    loading.value = false;
  }
}

function emitChange(value: DomainSelectorValue) {
  emit('update:modelValue', value);
  emit('change', value);
}

defineExpose({
  reload: loadOptions,
  options: normalizedOptions,
});
</script>

<style scoped>
.domain-selector {
  width: 100%;
}
</style>
