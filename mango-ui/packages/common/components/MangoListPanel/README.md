# MangoListPanel

列表卡片组件。统一功能区、表格区和分页区位置。

## 导入

```ts
import { MangoListPanel } from '@mango/common';
```

## Slots

| 名称 | 说明 |
|------|------|
| `actions` | 顶部操作区，通常放置新增、导入、导出等按钮 |
| `view-actions` | 顶部视图操作区，通常放置模式切换、密度切换等 |
| `default` | 表格内容区域，通常放置 `MangoDataTable` 或 `el-table` |
| `pagination` | 分页区域，通常放置 `Pagination` 组件 |

## 使用示例

```vue
<script setup lang="ts">
import { MangoListPanel, MangoDataTable, Pagination } from '@mango/common';

const paging = reactive({ page: 1, limit: 20, total: 0 });
const rows = ref<OrderRow[]>([]);

function loadData() {
  // 调用业务 API
}
</script>

<template>
  <MangoListPanel>
    <template #actions>
      <el-button type="primary" plain>新增</el-button>
      <el-button plain>导入</el-button>
    </template>

    <template #view-actions>
      <el-button link>刷新</el-button>
    </template>

    <MangoDataTable
      :rows="rows"
      :columns="columns"
      row-key="id"
      :pagination="paging"
      @page-change="loadData"
    />

    <template #pagination>
      <Pagination
        v-model:page="paging.page"
        v-model:limit="paging.limit"
        :total="paging.total"
        @pagination="loadData"
      />
    </template>
  </MangoListPanel>
</template>
```

## 重要约束

- 当 `actions` 和 `view-actions` slot 同时存在时，顶部工具栏才会渲染
- 当 `pagination` slot 存在时，分页区域才会渲染
- 组件自带白底、边框、圆角和阴影，作为列表内容的外层卡片
- 在 `max-width: 720px` 时工具栏和分页自动切换为纵向布局
