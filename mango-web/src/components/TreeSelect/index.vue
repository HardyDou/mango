<template>
  <el-tree-select
    v-model="value"
    :data="data"
    :props="treeProps"
    :placeholder="placeholder"
    :clearable="clearable"
    :check-strictly="checkStrictly"
    :render-after-expand="renderAfterExpand"
    :show-checkbox="showCheckbox"
    :expand-on-click-node="expandOnClickNode"
    :filter-node-method="filterNodeMethod"
    :node-key="nodeKey"
    :default-expand-all="defaultExpandAll"
    :expand-on-click-node="expandOnClickNode"
    @change="handleChange"
    @node-click="handleNodeClick"
    @check-change="handleCheckChange"
  />
</template>

<script setup lang="ts" name="TreeSelect">
import { ref, computed, watch } from 'vue';
import type { TreeNodeProps, TreeSelectProps } from 'element-plus';

const props = withDefaults(
  defineProps<{
    modelValue: string | number | null;
    data: any[];
    placeholder?: string;
    clearable?: boolean;
    checkStrictly?: boolean;
    showCheckbox?: boolean;
    expandOnClickNode?: boolean;
    renderAfterExpand?: boolean;
    defaultExpandAll?: boolean;
    nodeKey?: string;
    filterNodeMethod?: (value: string, data: any) => boolean;
    props?: TreeNodeProps;
  }>(),
  {
    modelValue: null,
    data: () => [],
    placeholder: '请选择',
    clearable: true,
    checkStrictly: false,
    showCheckbox: false,
    expandOnClickNode: true,
    renderAfterExpand: true,
    defaultExpandAll: false,
    nodeKey: 'id',
  }
);

const emit = defineEmits(['update:modelValue', 'change', 'node-click', 'check-change']);

const value = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const treeProps = computed<TreeNodeProps>(() => ({
  label: 'name',
  children: 'children',
  ...props.props,
}));

const handleChange = (val: string | number | null) => {
  emit('change', val);
};

const handleNodeClick = (data: any) => {
  emit('node-click', data);
};

const handleCheckChange = (data: any, checked: boolean) => {
  emit('check-change', data, checked);
};
</script>

<style scoped lang="scss">
.el-tree-select {
  width: 100%;
}
</style>
