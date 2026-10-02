# Pagination

分页器组件。支持对齐、`page`/`limit` 双向绑定和同轮变更事件合并。

## 导入

```ts
import { Pagination } from '@mango/common';
import type { PaginationProps, PaginationChange } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `total` | `number` | `0` | 总条数 |
| `page` | `number` | `1` | 当前页码 |
| `limit` | `number` | `20` | 每页条数 |
| `pageSizes` | `number[]` | `[10, 20, 30, 50]` | 每页显示个数选择器选项 |
| `layout` | `string` | `'total, sizes, prev, pager, next, jumper'` | 组件布局，子组件名用逗号分隔 |
| `background` | `boolean` | `true` | 是否为分页按钮添加背景色 |
| `pagerCount` | `number` | `5` | 页码按钮数量，当总页数超过该值时会折叠 |
| `small` | `boolean` | `false` | 是否使用小型分页样式 |
| `disabled` | `boolean` | `false` | 是否禁用分页 |
| `align` | `'left' \| 'center' \| 'right'` | `'right'` | 对齐方式 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:page` | `number` | 当前页码变化 |
| `update:limit` | `number` | 每页条数变化 |
| `pagination` | `PaginationChange` | 同轮变更合并事件 |

## PaginationChange

```ts
interface PaginationChange {
  page: number;
  limit: number;
}
```

当 `page` 和 `limit` 在同一轮事件循环中同时变化时，`pagination` 事件会将两者合并为一次回调，避免触发两次数据请求。

## 使用示例

```vue
<script setup lang="ts">
import { reactive } from 'vue';
import { Pagination } from '@mango/common';
import type { PaginationChange } from '@mango/common';

const paging = reactive({ page: 1, limit: 20, total: 0 });

function handlePagination(change: PaginationChange) {
  paging.page = change.page;
  paging.limit = change.limit;
  // 调用业务 API
}
</script>

<template>
  <Pagination
    v-model:page="paging.page"
    v-model:limit="paging.limit"
    :total="paging.total"
    @pagination="handlePagination"
  />
</template>
```

## 响应式

- 在 `max-width: 640px` 时自动切换为左对齐并允许横向滚动
- `small` 仅控制 Element Plus 尺寸，不会改变分页数据结构

## 重要约束

- 组件不请求接口，分页变化由宿主监听后请求数据
- `pagination` 事件通过 `queueMicrotask` 合并同轮变更，确保只触发一次

## 概览

统一管理端分页器，封装页码、每页数量、对齐和同轮变更合并。

## 功能清单

支持页码/条数双向绑定、页大小选项、布局、禁用、小尺寸、响应式和分页事件。

## 接入方式

从 `@mango/common` 导入，放在 `MangoListPanel` 的 `pagination` slot 中使用。

## 配置说明

通过 `total`、`page`、`limit`、`pageSizes`、`layout` 和 `align` 配置。

## API 与扩展

支持 `update:page`、`update:limit` 和合并后的 `pagination` 事件。

## 数据与初始化

无数据库、字典或默认数据初始化要求；数据请求由宿主处理。

## 管理入口

无独立管理入口，由业务列表页面接入。

## 快速开始

参见上方使用示例。

## 问题排查

确认宿主监听 `pagination` 事件并同步页码、条数与总数，避免重复请求。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
