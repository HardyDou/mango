# MangoListPage

管理后台列表页外壳组件。按搜索区和列表区组织页面，不渲染额外页面标题。

## 导入

```ts
import { MangoListPage } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dataPage` | `string \| undefined` | `undefined` | 页面标识，用于埋点/定位 |
| `dense` | `boolean` | `false` | 紧凑模式，间距从 12px 缩小为 8px |

## Slots

| 名称 | 说明 |
|------|------|
| `search` | 搜索栏区域，通常放置 `MangoSearchPanel` |
| `default` | 列表内容区域，通常放置 `MangoListPanel` 或 `MangoDataTable` |

## 使用示例

```vue
<script setup lang="ts">
import { MangoListPage, MangoSearchPanel, MangoListPanel, Pagination } from '@mango/common';

const query = reactive({ keyword: '', page: 1, size: 20 });
const total = ref(0);

function search() {
  // 调用业务 API
}
function reset() {
  query.keyword = '';
  query.page = 1;
  search();
}
function loadData() {
  // 加载数据
}
</script>

<template>
  <MangoListPage data-page="demo.orders">
    <template #search>
      <MangoSearchPanel :model="query" collapsible :collapsed-count="3" @search="search" @reset="reset">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" clearable placeholder="请输入关键字" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="query.status" clearable placeholder="请选择状态" />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button type="primary" plain>新增</el-button>
      </template>

      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.page" v-model:limit="query.size" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>
  </MangoListPage>
</template>
```

## 紧凑模式

```vue
<MangoListPage dense>
  <!-- 内容间距从 12px 缩小为 8px -->
</MangoListPage>
```

## 重要约束

- 组件使用 `<main>` 语义标签，每个页面应只有一个 `MangoListPage` 实例
- `dataPage` 用于前端埋点和页面定位，建议按 `模块.功能` 格式命名（如 `file.files`、`demo.orders`）
- 组件不渲染额外页面标题，标题由路由或宿主应用处理
