<template>
  <el-pagination
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :total="total"
    :page-sizes="pageSizes"
    :layout="layout"
    :background="background"
    :pager-count="pagerCount"
    :small="small"
    :disabled="disabled"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
  />
</template>

<script setup lang="ts" name="Pagination">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    total?: number;
    page?: number;
    limit?: number;
    pageSizes?: number[];
    layout?: string;
    background?: boolean;
    pagerCount?: number;
    small?: boolean;
    disabled?: boolean;
  }>(),
  {
    total: 0,
    page: 1,
    limit: 20,
    pageSizes: () => [10, 20, 30, 50],
    layout: 'total, sizes, prev, pager, next, jumper',
    background: true,
    pagerCount: 5,
    small: false,
    disabled: false,
  }
);

const emit = defineEmits(['update:page', 'update:limit', 'pagination']);

const currentPage = computed({
  get: () => props.page,
  set: (val) => emit('update:page', val),
});

const pageSize = computed({
  get: () => props.limit,
  set: (val) => emit('update:limit', val),
});

const handleSizeChange = (val: number) => {
  emit('pagination', { page: currentPage.value, limit: val });
};

const handleCurrentChange = (val: number) => {
  emit('pagination', { page: val, limit: pageSize.value });
};
</script>

<style scoped lang="scss">
.el-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
