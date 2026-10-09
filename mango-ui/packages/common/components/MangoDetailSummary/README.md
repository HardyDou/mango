# MangoDetailSummary

详情页顶部摘要组件。在一行内展示主标题、状态标签组和右侧关键字段，窄屏自动纵向堆叠。

## 导入

```ts
import { MangoDetailSummary } from '@mango/common';
import type {
  MangoDetailSummaryTag,
  MangoDetailSummaryField,
  MangoDetailSummaryProps,
} from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dataSurface` | `string` | `undefined` | 透传到根节点的 `data-surface` 标识，用于埋点/定位 |
| `title` | `unknown` | `undefined` | 主标题内容；空值或纯空白显示为 `-` |
| `tags` | `MangoDetailSummaryTag[]` | `[]` | 标题下方标签列表，空数组不渲染标签区 |
| `fields` | `MangoDetailSummaryField[]` | `[]` | 右侧摘要字段列表，空数组不渲染字段区 |

## 类型定义

```ts
import type { TagProps } from 'element-plus';

export interface MangoDetailSummaryTag {
  key: string;
  label: unknown;
  type?: TagProps['type'];
  effect?: TagProps['effect'];
}

export interface MangoDetailSummaryField {
  key: string;
  label: string;
  value: unknown;
}

export interface MangoDetailSummaryProps {
  dataSurface?: string;
  title?: unknown;
  tags?: MangoDetailSummaryTag[];
  fields?: MangoDetailSummaryField[];
}
```

标签的 `type`（如 `success`、`warning`、`danger`）和 `effect`（`dark`、`light`、`plain`）与 Element Plus `el-tag` 一致；未传 `effect` 时组件使用 `plain`。

## 使用示例

### 标题、标签与摘要字段

```vue
<script setup lang="ts">
import {
  MangoDetailSummary,
  type MangoDetailSummaryTag,
  type MangoDetailSummaryField,
} from '@mango/common';

const tags: MangoDetailSummaryTag[] = [
  { key: 'status', label: '运行中', type: 'success' },
  { key: 'level', label: '重要', type: 'warning', effect: 'dark' },
];

const fields: MangoDetailSummaryField[] = [
  { key: 'owner', label: '负责人', value: '张三' },
  { key: 'updatedAt', label: '更新时间', value: '2026-10-09 10:00' },
];
</script>

<template>
  <MangoDetailSummary
    title="节点详情"
    :tags="tags"
    :fields="fields"
    data-surface="detail.node.summary"
  />
</template>
```

### 仅标题与空值占位

```vue
<script setup lang="ts">
import { MangoDetailSummary } from '@mango/common';

const detail = ref<{ name?: string }>({});
</script>

<template>
  <!-- name 为 undefined 或空白字符串时，标题位置显示 - -->
  <MangoDetailSummary :title="detail.name" />
</template>
```

标题、标签文本、字段值都支持任意类型，组件内部按 `String(value ?? '').trim()` 转换，转换结果为空时统一回退为 `-`。字号可通过 CSS 变量 `--mango-detail-title-size`、`--mango-detail-meta-size`、`--mango-detail-body-size` 在宿主侧覆盖。

## 重要约束

- 纯展示组件，不提供插槽、事件和实例方法，所有内容均由 Props 驱动
- `tags`、`fields` 为空数组时对应区域不渲染，也不占位
- 标签的 `type`、`effect` 语义与 Element Plus `el-tag` 完全一致，标签固定为 `size="small"`
- 根节点为 `<section>` 并带有 `aria-label="详情摘要"`，一个详情页只应放置一个摘要实例
- 视口宽度不超过 768px 时字段区换到标题下方并左对齐

## 概览

统一详情页头部摘要样式，组合主标题、状态标签和关键字段。

## 功能清单

支持主标题、多标签、右侧字段列、空值占位、埋点标识和响应式布局。

## 接入方式

从 `@mango/common` 导入，在详情页顶部、返回栏下方传入标题、标签和字段数据。

## 配置说明

通过 `title`、`tags`、`fields` 配置展示内容，通过 `dataSurface` 配置埋点标识；组件不持有业务状态。

## API 与扩展

无事件、无插槽、无 Expose；标签外观通过 Element Plus 标签原生属性扩展，字号通过 CSS 变量覆盖。

## 数据与初始化

无数据库、字典或默认数据初始化要求，数据由宿主接口返回后通过 Props 传入。

## 管理入口

无独立管理入口，由各业务详情页面接入。

## 快速开始

参见标题、标签与摘要字段示例。

## 问题排查

字段或标签未显示时先确认传入的是非空数组；标题出现 `-` 表示值为 `null`、`undefined` 或纯空白字符串；标签颜色不符合预期时检查 `type`、`effect` 是否为 Element Plus 支持的取值。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
