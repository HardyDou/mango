# MangoDetailPage

详情页外壳组件。提供返回栏、内容区和底部操作栏。

## 导入

```ts
import { MangoDetailPage } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `string` | - | 页面标题 |
| `backText` | `string` | `'返回'` | 返回按钮文案 |
| `dataPage` | `string \| undefined` | `undefined` | 页面标识，用于埋点/定位 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `back` | - | 点击返回按钮触发 |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 详情内容区域，通常放置 `MangoPageSection` 或描述列表 |
| `actions` | 底部操作栏，通常放置保存、取消等按钮 |

## 使用示例

```vue
<script setup lang="ts">
import { MangoDetailPage, MangoPageSection } from '@mango/common';

function handleBack() {
  // 返回上一页
  router.back();
}
</script>

<template>
  <MangoDetailPage title="订单详情" data-page="orders.detail" @back="handleBack">
    <MangoPageSection title="基本信息">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item>
      </el-descriptions>
    </MangoPageSection>

    <MangoPageSection title="商品信息">
      <!-- 商品列表 -->
    </MangoPageSection>

    <template #actions>
      <el-button @click="handleBack">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="save">保存</el-button>
    </template>
  </MangoDetailPage>
</template>
```

## 重要约束

- 组件使用 `<main>` 语义标签，每个页面应只有一个 `MangoDetailPage` 实例
- `dataPage` 用于前端埋点和页面定位，建议按 `模块.功能` 格式命名（如 `orders.detail`）
- 底部操作栏使用 `sticky` 定位，始终固定在视口底部
- 组件不处理返回逻辑，`back` 事件由宿主监听后调用 `router.back()` 或自定义逻辑

## 概览

统一管理端详情页外壳，提供返回栏、内容区域和操作区。

## 功能清单

支持标题、返回事件、页面标识、默认内容区和底部操作区。

## 接入方式

从 `@mango/common` 导入，在业务详情路由中使用。

## 配置说明

通过 `title`、`backText` 和 `dataPage` 配置页面外壳；详情数据由宿主加载。

## API 与扩展

Props、`back` 事件及 `default`/`actions` slot 见上方契约。

## 数据与初始化

无数据库或默认数据初始化要求。

## 管理入口

无独立管理入口，由业务详情页面接入。

## 快速开始

参见上方使用示例。

## 问题排查

确认页面只使用一个详情外壳，并由宿主监听 `back` 事件处理路由返回。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
