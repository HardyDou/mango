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

## 概览

统一管理端列表内容卡片，承载工具栏、表格和分页区域。

## 功能清单

支持操作区、视图操作区、默认列表内容区和分页 slot，并提供响应式布局。

## 接入方式

从 `@mango/common` 导入，作为 `MangoListPage` 的列表内容区使用。

## 配置说明

组件主要通过 slots 接收业务内容；分页和操作状态由宿主维护。

## API 与扩展

支持 `actions`、`view-actions`、`default` 和 `pagination` slot。

## 数据与初始化

无数据库、字典或默认数据初始化要求。

## 管理入口

无独立管理入口，由业务列表页面接入。

## 快速开始

参见上方列表面板示例。

## 问题排查

确认分页 slot 只放置一个分页组件，并检查工具栏 slot 是否按页面需求传入。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
