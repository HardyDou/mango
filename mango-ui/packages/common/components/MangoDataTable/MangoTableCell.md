# MangoTableCell

通用单元格组件。提供 `text` / `status` / `tag` / `button` / `input` / `select` / `radio` / `switch` / `custom` 类型。

作为 `MangoDataTable` 的内部子组件，通常不单独使用。

## 导入

```ts
// 作为 MangoDataTable 的列 type 使用
import { MangoDataTable, type MangoTableColumn } from '@mango/common';

const columns: MangoTableColumn<OrderRow>[] = [
  { field: 'name', label: '名称', type: 'text' },        // 纯文本
  { field: 'status', label: '状态', type: 'status', options: [...] }, // 状态文本
  { field: 'type', label: '类型', type: 'tag', options: [...] },       // 标签
  { field: 'action', label: '操作', type: 'button' },    // 按钮
  { field: 'remark', label: '备注', type: 'input' },     // 可编辑输入
  { field: 'level', label: '等级', type: 'select', options: [...] },   // 下拉选择
  { field: 'enabled', label: '启用', type: 'radio', options: [...] },  // 单选
  { field: 'active', label: '激活', type: 'switch' },   // 开关
  { field: 'custom', label: '自定义', type: 'custom' },  // 自定义插槽
];
```

## 类型说明

| 类型 | 说明 | 是否可编辑 |
|------|------|-----------|
| `text` | 纯文本展示 | 否 |
| `status` | 状态文本，通过 `options` 映射 tone 和文案 | 否 |
| `tag` | 标签展示，通过 `options` 映射 tone 和文案 | 否 |
| `button` | 按钮，通过 `options` 配置 | 否 |
| `input` | 输入框，直接修改行字段 | 是 |
| `select` | 下拉选择，直接修改行字段 | 是 |
| `radio` | 单选，直接修改行字段 | 是 |
| `switch` | 开关，直接修改行字段，支持 boolean 或 0/1 | 是 |
| `custom` | 完全自定义，通过 `slot` 或 `formatter` | 否 |

## 编辑行为

`input` / `select` / `radio` / `switch` 类型会直接修改传入行的可编辑字段，并在提交后通过 `cell-change` 事件提供 `previousValue` 和新值。只读业务可仅使用 `text` / `status` / `tag` / `custom`。

## 单元格上下文

```ts
interface MangoTableCellContext<Row extends object> {
  row: Row;
  rowIndex: number;
  column: MangoTableColumn<Row>;
  field: string;
  value: unknown;
}
```

编辑事件额外包含 `previousValue`：

```ts
interface MangoTableCellChangeContext<Row extends object> extends MangoTableCellContext<Row> {
  previousValue: unknown;
}
```

## 自定义单元格

当 `type` 为 `custom` 时，可以通过列配置中的 `slot` 或 `formatter` 完全自定义渲染：

```ts
const columns: MangoTableColumn<OrderRow>[] = [
  {
    field: 'customField',
    label: '自定义',
    type: 'custom',
    slot: 'my-custom-cell', // 对应模板中的 <template #my-custom-cell="...">
    // 或使用 formatter
    // formatter: (ctx) => `值: ${ctx.value}`,
  },
];
```

```vue
<template>
  <MangoDataTable :rows="rows" :columns="columns" row-key="id">
    <template #my-custom-cell="{ row, rowIndex }">
      <el-tag>{{ row.customField }}</el-tag>
    </template>
  </MangoDataTable>
</template>
```

## 与 MangoDataTable 的关系

- `MangoTableCell` 是 `MangoDataTable` 的内部实现，不单独导出为公共组件
- 列配置中的 `type` 字段决定使用哪种单元格渲染逻辑
- `options` 提供选择项和状态映射
- `formatter` 只改变展示值，不修改数据
- `props` 透传给底层 Element Plus 组件
