<template>
  <el-tag
    :type="dictType"
    :effect="effect"
    :size="size"
    :hit="hit"
    :closable="closable"
    :disable-transitions="disableTransitions"
    @close="handleClose"
  >
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts" name="DictTag">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    dictType?: 'success' | 'warning' | 'info' | 'danger' | '';
    label?: string;
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
    effect: 'light',
    size: 'default',
    hit: false,
    closable: false,
    disableTransitions: false,
    type: '',
  }
);

const emit = defineEmits(['close']);

const dictType = computed(() => props.type || props.dictType);

const handleClose = () => {
  emit('close');
};
</script>

<style scoped lang="scss">
.el-tag {
  margin-right: 4px;
}
</style>
