# MangoDescriptionList

管理端详情描述列表组件。基于 `el-descriptions` 渲染键值对或边框表格，支持分组、空值占位、自定义插槽与富文本预览。

## 导入

```ts
import { MangoDescriptionList } from '@mango/common';
import type {
  MangoDescriptionItem,
  MangoDescriptionGroup,
  MangoDescriptionListProps,
} from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `items` | `MangoDescriptionItem[]` | - | 扁平描述项列表，与 `groups` 二选一且必传其一 |
| `groups` | `MangoDescriptionGroup[]` | - | 分组描述数据，与 `items` 二选一；`items` 为空的分组会被过滤 |
| `display` | `'key-value' \| 'table'` | `'key-value'` | 展示形态：`key-value` 无边框、标签后自动补全角冒号；`table` 渲染带边框表格 |
| `column` | `number` | `2` | 每行描述项列数，非正整数回退为 `2` |
| `labelWidth` | `string \| number` | `150` | 标签列宽度，数字按像素处理 |
| `emptyText` | `string` | `'-'` | 描述项值为空（`null`、`undefined`、`''`）时的占位文本 |
| `emptyDescription` | `string` | `'暂无信息'` | 没有任何可渲染内容时 `el-empty` 的描述文本 |
| `richTextPreviewComponent` | `Component` | `RichTextViewer` | `componentType` 为 `rich-text-preview` 时使用的渲染组件 |

## Slots

组件没有固定具名插槽，插槽名由数据项中的 `slot` / `extraSlot` 字段动态指定。

| 名称 | 说明 |
|------|------|
| 由 `item.slot` 指定 | 自定义单个描述项内容，插槽参数为 `MangoDescriptionItemSlotProps`；未提供插槽时回退显示文本 |
| `group-extra-${group.header.extraSlot}` | 自定义分组头部附加内容，插槽参数为 `MangoDescriptionGroupExtraSlotProps` |

描述项插槽参数：

| 字段 | 类型 | 说明 |
|------|------|------|
| `item` | `MangoDescriptionItem` | 当前描述项配置 |
| `index` | `number` | 项在分组内的下标 |
| `group` | `MangoDescriptionGroup` | 所属分组 |
| `groupIndex` | `number` | 分组下标 |
| `value` | `MangoDescriptionValue` | 原始值 |
| `displayValue` | `string` | 组件计算出的展示文本（含空值占位） |

分组附加插槽参数：`group`、`groupIndex`、`header`、`value`、`displayValue`。

## 类型定义

```ts
export type MangoDescriptionListDisplay = 'key-value' | 'table';
export type MangoDescriptionValue = string | number | boolean | null | undefined;
export type MangoDescriptionItemComponentType = 'rich-text-preview';

export interface MangoDescriptionItem {
  key: string;
  label: string;
  value: MangoDescriptionValue;
  span?: number;
  emptyText?: string;
  slot?: string;
  componentType?: MangoDescriptionItemComponentType;
}

export interface MangoDescriptionGroupHeader {
  title: string;
  description?: string;
  extra?: MangoDescriptionValue;
  extraSlot?: string;
  extraPlacement?: 'after-description' | 'end';
}

export interface MangoDescriptionGroup {
  key: string;
  header?: MangoDescriptionGroupHeader;
  items: MangoDescriptionItem[];
}
```

`MangoDescriptionListProps` 为上述公共 Props 与 `{ items }` / `{ groups }` 互斥联合类型的组合。

## 使用示例

### 基础键值对

```vue
<script setup lang="ts">
import { MangoDescriptionList, type MangoDescriptionItem } from '@mango/common';

const items: MangoDescriptionItem[] = [
  { key: 'name', label: '名称', value: '订单 A' },
  { key: 'code', label: '编号', value: 'SO-20261009-001' },
  { key: 'owner', label: '负责人', value: '张三' },
  { key: 'remark', label: '备注', value: '' },
];
</script>

<template>
  <MangoDescriptionList :items="items" :column="2" />
</template>
```

### 分组与表格形态

```vue
<script setup lang="ts">
import { MangoDescriptionList, type MangoDescriptionGroup } from '@mango/common';

const groups: MangoDescriptionGroup[] = [
  {
    key: 'base',
    header: {
      title: '基础信息',
      description: '系统创建后不可修改',
      extra: '已同步',
    },
    items: [
      { key: 'name', label: '名称', value: '节点 A' },
      { key: 'code', label: '节点编码', value: 'NODE-001' },
      { key: 'owner', label: '负责人', value: '张三', span: 2 },
    ],
  },
  {
    key: 'runtime',
    header: { title: '运行信息' },
    items: [
      { key: 'status', label: '状态', value: '运行中' },
      { key: 'updatedAt', label: '更新时间', value: '2026-10-09 10:00' },
    ],
  },
];
</script>

<template>
  <MangoDescriptionList :groups="groups" display="table" :column="2" :label-width="120" />
</template>
```

### 自定义插槽与富文本预览

```vue
<script setup lang="ts">
import { MangoDescriptionList, type MangoDescriptionItem } from '@mango/common';

const items: MangoDescriptionItem[] = [
  { key: 'status', label: '状态', value: 1, slot: 'status' },
  { key: 'content', label: '正文', value: '<p>富文本<strong>内容</strong></p>', componentType: 'rich-text-preview' },
];
</script>

<template>
  <MangoDescriptionList :items="items">
    <template #status="{ value }">
      <el-tag :type="value === 1 ? 'success' : 'info'">
        {{ value === 1 ? '启用' : '停用' }}
      </el-tag>
    </template>
  </MangoDescriptionList>
</template>
```

分组头部附加区域通过 `group-extra-${extraSlot}` 插槽定制；`header.extraPlacement` 为 `'after-description'` 时附加内容渲染在描述文案之后，否则渲染在头部右侧。

## 重要约束

- `items` 与 `groups` 不能同时配置，同时传入会抛出错误；两者都没有有效数据时渲染 `el-empty`
- 同一个描述项的 `slot` 与 `componentType` 不能同时配置，否则抛出错误
- `span` 会被规整为正整数并钳制在 `column` 范围内
- `componentType: 'rich-text-preview'` 仅在值非空时渲染富文本组件，默认使用内置 `RichTextViewer`
- 空值判断仅排除 `null`、`undefined`、空字符串，`false`、`0` 会按原值展示
- 视口宽度不超过 760px 时自动切换为单列纵向布局

## 概览

统一详情页键值信息展示，支持扁平列表与分组两种数据结构。

## 功能清单

支持键值对/表格两种形态、多列布局、分组标题与附加信息、空值占位、动态插槽和富文本预览。

## 接入方式

从 `@mango/common` 导入，在详情页、抽屉或弹框中传入 `items` 或 `groups` 数据即可使用。

## 配置说明

通过 `display`、`column`、`labelWidth` 控制布局，通过 `emptyText`、`emptyDescription` 配置空态，通过 `richTextPreviewComponent` 替换富文本渲染实现。

## API 与扩展

扩展点为数据驱动的动态插槽：描述项插槽与 `group-extra-*` 分组头部插槽；组件本身不抛出事件。

## 数据与初始化

无数据库、字典或默认数据初始化要求，展示数据全部由宿主通过 Props 提供。

## 管理入口

无独立管理入口，由各业务详情页面接入。

## 快速开始

参见基础键值对示例，传入 `items` 数组即可完成接入。

## 问题排查

内容区显示空态时检查是否同时传了 `items` 和 `groups`，以及分组的 `items` 是否全部为空；自定义插槽不生效时确认 `item.slot` 名称与模板插槽名一致，且没有同时配置 `componentType`。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
