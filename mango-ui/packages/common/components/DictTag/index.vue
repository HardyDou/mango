<template>
  <el-tag
    v-bind="tagProps"
    :effect="effect"
    :size="size"
    :hit="hit"
    :closable="closable"
    :disable-transitions="disableTransitions"
    @close="handleClose"
  >
    {{ resolvedLabel }}
  </el-tag>
</template>

<script setup lang="ts" name="DictTag">
import { computed } from 'vue';
import { useDict } from '../../hooks/useDict';

const props = withDefaults(
  defineProps<{
    dictType?: 'success' | 'warning' | 'info' | 'danger' | '';
    label?: string;
    value?: string | number | boolean | null;
    dictCode?: string;
    effect?: 'light' | 'dark' | 'plain';
    size?: 'large' | 'default' | 'small';
    hit?: boolean;
    closable?: boolean;
    disableTransitions?: boolean;
    type?: string;
  }>(),
  {
    dictType: '',
    label: '',
    value: undefined,
    dictCode: '',
    effect: 'light',
    size: 'default',
    hit: false,
    closable: false,
    disableTransitions: false,
    type: '',
  }
);

const emit = defineEmits(['close']);

const { getLabel, getOption } = useDict(computed(() => props.dictCode));

const resolvedLabel = computed(() => {
  if (props.label) {
    return props.label;
  }
  return getLabel(props.value);
});

const resolvedTagType = computed(() => {
  if (props.type || props.dictType) {
    return props.type || props.dictType;
  }
  const option = getOption(props.value);
  if (option?.value === '1') {
    return 'success';
  }
  if (option?.value === '0') {
    return 'danger';
  }
  return '';
});

const tagProps = computed(() => {
  return resolvedTagType.value ? { type: resolvedTagType.value } : {};
});

const handleClose = () => {
  emit('close');
};
</script>

<style scoped lang="scss">
.el-tag {
  margin-right: 4px;
}
</style>
