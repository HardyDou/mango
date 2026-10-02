# MangoDataTable

统一数据表格组件。提供卡片切换、列配置、展开、选择、序号、操作、状态和分页事件。

业务字段、状态文案和 tone 判断由消费方提供，组件只负责表格结构、单元格交互和展示。

## 导入

```ts
import { MangoDataTable } from '@mango/common';
import type { MangoTableColumn, MangoTablePageChangeContext } from '@mango/common';
```

## 核心契约

| 类型   | 名称                                                               | 说明                                                                    |
| ------ | ------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| props  | `rows`、`columns`、`rowKey`                                        | 必填的行数据、列定义和稳定行标识。后端 ID 应按字符串传递。              |
| props  | `card`                                                             | 是否使用列表卡片表面，默认 `true`；`false` 仅移除表面样式，不切换实现。 |
| props  | `loading`、`error`、`retryable`、`emptyText`                       | 由消费方控制加载、失败、重试入口和空态。                                |
| props  | `mode`、`showModeSwitch`、`expand`                                 | 平铺/展开模式及列表展开列配置，支持 `v-model:mode`。                    |
| props  | `showIndex`、`showSelection`、`selectable`                         | 序号列、选择列及行是否可选判断；选择列不依赖必须传入判断函数。          |
| props  | `operation`、`pagination`                                          | 操作列和分页配置；操作只返回 key 与行上下文，不执行业务动作。           |
| emits  | `cell-change`、`action`、`page-change`、`selection-change`         | 单元格、操作、分页和选择变化事件。                                      |
| emits  | `update:mode`、`mode-change`、`retry`                              | 展示模式和重试意图事件。                                                |
| slots  | `actions`、`view-actions`、列 `slot` 或 `field` 名                 | 顶部操作区、视图操作区和业务自定义单元格。                              |
| expose | `clearSelection()`、`toggleRowSelection()`、`toggleRowExpansion()` | 类型安全访问底层 Element Plus Table 的常用选择和展开动作。              |

## 列和扩展类型

| 类型 | 字段 / 默认语义 |
| --- | --- |
| `MangoTableColumn` | `field`、`label` 必填；`type` 为 `text | status | tag | button | input | select | radio | custom`（默认 `text`）；支持 `width`、`minWidth`、`fixed`、`align`、`sortable`、`resizable`、`expandable`、`hidden`、`showOverflowTooltip`、`emptyText`、`options`、`loading`、`slot`、`props`、`formatter`、`disabled`。 |
| `MangoTableOption` | `label`、`value` 必填；`disabled?`、`tone?` 用于选择项状态。 |
| `MangoTableAction` | `key`、`text` 必填；`tone`、`visible`、`disabled`、`loading` 可为静态值或按行上下文计算，`props` 透传按钮属性。 |
| `MangoTableOperation` | 操作列配置：`visible`、`label`、`width`、`minWidth`、`fixed`、`align`、`resizable`、`moreCount`、`actions`。`actions` 必填；事件只返回意图，不自动调用接口。 |
| `MangoTablePagination` | `page`、`limit`、`total` 必填；`visible`、`pageSizes`、`align`、`layout`、`props` 可选。 |
| `MangoTableExpand` | `type`（当前为 `list`）、`labelWidth`、`emptyText`。 |

单元格上下文包含 `{ row, rowIndex, column, field, value }`；编辑事件额外包含 `previousValue`。`MangoTableCell` 的 `input/select/radio` 会改写行字段并触发 `cell-change`，`formatter` 只改变展示值。`MangoDataTable` 的分页、选择、操作和重试均由宿主监听后请求数据。

## 概览

公共管理端数据表格组件，负责表格结构与展示交互，不承载业务请求。

## 功能清单

支持列配置、状态/标签/操作单元格、选择、展开、分页、加载、空态、错误重试和自定义 slot。

## 接入方式

从 `@mango/common` 导入，在 `MangoListPanel` 或页面列表区中使用。

## 配置说明

通过 `rows`、`columns`、`rowKey`、`pagination` 等 props 配置；业务状态映射和接口参数由宿主维护。

## API 与扩展

事件、slot、expose 和类型定义以本 README 的核心契约为准。

## 数据与初始化

无数据库、字典或默认数据初始化要求；组件只接收宿主传入的数据。

## 管理入口

无独立管理入口，由各业务模块列表页面接入。

## 快速开始

参见上方使用示例。

## 问题排查

先确认 `rowKey` 稳定、列 `field` 与数据字段一致，并检查宿主是否处理分页和重试事件。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)

## 使用示例

```vue
<script setup lang="ts">
import { reactive, ref } from 'vue';
import { MangoDataTable, type MangoTableColumn, type MangoTablePageChangeContext } from '@mango/common';

interface OrderRow {
  id: string;
  orderNo: string;
  status: 'PENDING' | 'DONE';
}

const rows = ref<OrderRow[]>([]);
const columns: MangoTableColumn<OrderRow>[] = [
  { field: 'orderNo', label: '订单号', minWidth: 180 },
  {
    field: 'status',
    label: '状态',
    type: 'status',
    options: [
      { label: '处理中', value: 'PENDING', tone: 'warning' },
      { label: '已完成', value: 'DONE', tone: 'success' },
    ],
  },
];
const paging = reactive({ page: 1, limit: 20, total: 0 });

function loadPage(next: MangoTablePageChangeContext) {
  paging.page = next.page;
  paging.limit = next.limit;
  // 调用业务 API，并更新 rows 与 paging.total。
}
</script>

<template>
  <MangoDataTable
    :rows="rows"
    :columns="columns"
    row-key="id"
    :pagination="paging"
    :card="true"
    @page-change="loadPage"
  />
</template>
```

## 完整类型定义

### MangoTableProps

```ts
interface MangoTableProps {
  headerCellStyle?: never;
  'header-cell-style'?: never;
}
```

### MangoTableOption

```ts
interface MangoTableOption {
  label: string;
  value: MangoTableOptionValue; // string | number | boolean
  disabled?: boolean;
  tone?: MangoStatusTone; // primary | success | warning | danger | info | neutral
}
```

### MangoTableRowContext

```ts
interface MangoTableRowContext<Row extends object> {
  row: Row;
  rowIndex: number;
}
```

### MangoTableCellContext

```ts
interface MangoTableCellContext<Row extends object> extends MangoTableRowContext<Row> {
  column: MangoTableColumn<Row>;
  field: string;
  value: unknown;
}
```

### MangoTableCellChangeContext

```ts
interface MangoTableCellChangeContext<Row extends object> extends MangoTableCellContext<Row> {
  previousValue: unknown;
}
```

### MangoTableActionContext

```ts
interface MangoTableActionContext<Row extends object> extends MangoTableRowContext<Row> {
  key: string;
  tableKey?: string;
}
```

### MangoTableCondition / MangoTableCellCondition

```ts
type MangoTableCondition<Row extends object> = boolean | ((context: MangoTableRowContext<Row>) => boolean);
type MangoTableCellCondition<Row extends object> = boolean | ((context: MangoTableCellContext<Row>) => boolean);
```

### MangoTableColumn

```ts
interface MangoTableColumn<Row extends object> {
  field: string;
  label: string;
  type?: MangoTableColumnType; // text | status | tag | button | input | select | radio | custom
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  align?: 'left' | 'center' | 'right';
  sortable?: boolean;
  resizable?: boolean;
  expandable?: boolean;
  hidden?: boolean;
  showOverflowTooltip?: boolean;
  emptyText?: string;
  options?: MangoTableOption[];
  loading?: boolean;
  slot?: string;
  props?: Record<string, unknown>;
  formatter?: (context: MangoTableCellContext<Row>) => unknown;
  disabled?: MangoTableCellCondition<Row>;
}
```

### MangoTableAction

```ts
interface MangoTableAction<Row extends object> {
  key: string;
  text: string;
  tone?: MangoTableActionTone; // Exclude<MangoStatusTone, 'neutral'>
  visible?: MangoTableCondition<Row>;
  disabled?: MangoTableCondition<Row>;
  loading?: MangoTableCondition<Row>;
  props?: Record<string, unknown>;
}
```

### MangoTableOperation

```ts
interface MangoTableOperation<Row extends object> {
  visible?: boolean;
  label?: string;
  width?: number | string;
  minWidth?: number | string;
  fixed?: 'left' | 'right';
  align?: 'left' | 'center' | 'right';
  resizable?: boolean;
  moreCount?: number;
  actions: MangoTableAction<Row>[];
}
```

### MangoTablePagination

```ts
interface MangoTablePagination {
  visible?: boolean;
  page: number;
  limit: number;
  total: number;
  pageSizes?: number[];
  align?: PaginationAlign;
  layout?: string;
  props?: Record<string, unknown>;
}
```

### MangoTableExpand

```ts
interface MangoTableExpand {
  type?: MangoTableExpandType; // 当前为 'list'
  labelWidth?: number | string;
  emptyText?: string;
}
```

### MangoTablePageChangeContext

```ts
interface MangoTablePageChangeContext {
  page: number;
  limit: number;
}
```

### MangoDataTableProps

```ts
interface MangoDataTableProps<Row extends object> {
  rows: Row[];
  columns: MangoTableColumn<Row>[];
  rowKey: Extract<keyof Row, string> | ((row: Row) => string);
  tableKey?: string;
  loading?: boolean;
  error?: string;
  errorTitle?: string;
  retryText?: string;
  retryable?: boolean;
  emptyText?: string;
  card?: boolean;
  headerCellStyle?: MangoTableHeaderCellStyle;
  resizable?: boolean;
  mode?: MangoTableMode; // flat | expand
  showModeSwitch?: boolean;
  flatModeLabel?: string;
  expandModeLabel?: string;
  expand?: MangoTableExpand;
  showIndex?: boolean;
  showSelection?: boolean;
  selectable?: (row: Row, index: number) => boolean;
  tableProps?: MangoTableProps;
  operation?: MangoTableOperation<Row>;
  pagination?: MangoTablePagination;
  onCellChange?: (context: MangoTableCellChangeContext<Row>) => void;
  onAction?: (context: MangoTableActionContext<Row>) => void;
  onPageChange?: (context: MangoTablePageChangeContext) => void;
  onSelectionChange?: (rows: Row[]) => void;
  onModeChange?: (mode: MangoTableMode) => void;
  onRetry?: () => void;
}
```

### MangoDataTableEmits

```ts
interface MangoDataTableEmits<Row extends object> {
  (event: 'cell-change', context: MangoTableCellChangeContext<Row>): void;
  (event: 'action', context: MangoTableActionContext<Row>): void;
  (event: 'page-change', context: MangoTablePageChangeContext): void;
  (event: 'selection-change', rows: Row[]): void;
  (event: 'update:mode', mode: MangoTableMode): void;
  (event: 'mode-change', mode: MangoTableMode): void;
  (event: 'retry'): void;
}
```

### MangoDataTableExpose

```ts
interface MangoDataTableExpose<Row extends object> {
  tableRef: TableInstance | undefined;
  clearSelection: () => void;
  toggleRowSelection: (row: Row, selected?: boolean) => void;
  toggleRowExpansion: (row: Row, expanded?: boolean) => void;
}
```

## CSS 变量

表头背景和文字颜色分别由 `--mango-table-header-bg`、`--mango-table-header-text` 提供。默认主题和 `admin-standard` 使用 `#eef1f5` / `#000000`，dark 与 compact 主题使用各自语义色；消费系统可在主题容器覆盖这两个变量。

## 重要约束

- `MangoDataTable` 不请求接口、不读取路由或 store，也不根据业务状态推断文案、tone 或权限。
- `MangoTableCell` 会直接修改传入行的可编辑字段，并在提交后通过 `change` 提供 `previousValue` 和新值；只读业务可仅使用 text/status/tag/custom。
- 分页、选择、操作和重试均由宿主监听后请求数据。
