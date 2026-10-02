# MangoStatusText

主题化状态文本组件。文案和 tone 由消费方传入，不内置业务状态映射。

## 导入

```ts
import { MangoStatusText } from '@mango/common';
import type { MangoStatusTone } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `tone` | `MangoStatusTone` | `'neutral'` | 状态色调 |

## MangoStatusTone

```ts
type MangoStatusTone = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';
```

| 值 | 说明 | CSS 变量 |
|---|------|---------|
| `primary` | 主色调 | `--mango-color-status-primary` |
| `success` | 成功/完成 | `--mango-color-status-success` |
| `warning` | 警告/处理中 | `--mango-color-status-warning` |
| `danger` | 危险/失败 | `--mango-color-status-danger` |
| `info` | 信息/提示 | `--mango-color-status-info` |
| `neutral` | 中性/默认 | `--mango-color-status-neutral` |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 状态文案内容 |

## 使用示例

```vue
<script setup lang="ts">
import { MangoStatusText } from '@mango/common';
</script>

<template>
  <!-- 基础用法 -->
  <MangoStatusText tone="success">已完成</MangoStatusText>
  <MangoStatusText tone="warning">处理中</MangoStatusText>
  <MangoStatusText tone="danger">已失败</MangoStatusText>

  <!-- 在表格列中使用 -->
  <MangoDataTable :rows="rows" :columns="columns" row-key="id">
    <template #status="{ value }">
      <MangoStatusText :tone="value === 'DONE' ? 'success' : 'warning'">
        {{ value === 'DONE' ? '已完成' : '处理中' }}
      </MangoStatusText>
    </template>
  </MangoDataTable>
</template>
```

## 主题定制

组件只映射主题 CSS 变量，不内置业务状态到 tone 的转换。消费系统可在主题容器覆盖对应变量：

```css
.my-theme {
  --mango-color-status-success: #67c23a;
  --mango-color-status-warning: #e6a23c;
  --mango-color-status-danger: #f56c6c;
}
```

## 重要约束

- 组件只负责颜色映射，不内置业务状态到 tone 的转换
- 不提供事件或接口
- 不根据业务状态自动推断文案或 tone

## 概览

主题化状态文本组件，用于在表格、详情和列表中展示业务状态。

## 功能清单

支持统一状态色调、默认 slot 文案和主题 CSS 变量覆盖。

## 接入方式

从 `@mango/common` 导入，在业务状态展示位置使用。

## 配置说明

通过 `tone` 传入展示色调；业务状态到 tone 的映射由消费方决定。

## API 与扩展

支持 `tone` prop 和 `default` slot，不提供业务事件或接口。

## 数据与初始化

无数据库、字典或默认数据初始化要求。

## 管理入口

无独立管理入口，由业务页面接入。

## 快速开始

参见上方使用示例。

## 问题排查

确认主题容器提供对应 CSS 变量，并由业务侧显式传入状态文案和 tone。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
