<template>
  <div class="mango-pagination" :class="`mango-pagination--${align}`">
    <el-pagination
      v-bind="$attrs"
      :current-page="page"
      :page-size="limit"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      :background="background"
      :pager-count="pagerCount"
      :size="small ? 'small' : 'default'"
      :disabled="disabled"
      @update:current-page="handleCurrentPageUpdate"
      @update:page-size="handlePageSizeUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import type { PaginationChange, PaginationEmits, PaginationProps } from './types';

defineOptions({
  // Keep the published runtime name for KeepAlive and component-name consumers.
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Pagination',
  inheritAttrs: false,
});

const props = withDefaults(defineProps<PaginationProps>(), {
  total: 0,
  page: 1,
  limit: 20,
  pageSizes: () => [10, 20, 30, 50],
  layout: 'total, sizes, prev, pager, next, jumper',
  background: true,
  pagerCount: 5,
  small: false,
  disabled: false,
  align: 'right',
});

const emit = defineEmits<PaginationEmits>();
let pendingChange: PaginationChange | null = null;
let changeScheduled = false;

const handleCurrentPageUpdate = (val: number) => {
  emit('update:page', val);
  pendingChange = {
    page: val,
    limit: pendingChange?.limit ?? props.limit,
  };
  schedulePaginationChange();
};

const handlePageSizeUpdate = (val: number) => {
  emit('update:limit', val);
  pendingChange = {
    page: pendingChange?.page ?? props.page,
    limit: val,
  };
  schedulePaginationChange();
};

function schedulePaginationChange() {
  if (changeScheduled) return;
  changeScheduled = true;
  queueMicrotask(() => {
    changeScheduled = false;
    const change = pendingChange;
    pendingChange = null;
    if (change) emit('pagination', change);
  });
}
</script>

<style scoped>
.mango-pagination {
  display: flex;
  padding: 16px 0;
}

.mango-pagination--left {
  justify-content: flex-start;
}

.mango-pagination--center {
  justify-content: center;
}

.mango-pagination--right {
  justify-content: flex-end;
}

.mango-pagination :deep(.el-pagination) {
  flex-wrap: wrap;
  gap: 8px;
  padding: 0;
}

@media (width <= 640px) {
  .mango-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }

  .mango-pagination :deep(.el-pagination) {
    flex-wrap: nowrap;
  }
}
</style>
