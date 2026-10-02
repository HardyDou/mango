# MangoPageSection

详情页和表单页的业务分组容器组件。用于将页面内容按业务语义分组展示。

## 导入

```ts
import { MangoPageSection } from '@mango/common';
```

## Props

| 名称       | 类型                  | 默认值      | 说明               |
| ---------- | --------------------- | ----------- | ------------------ |
| `title`    | `string \| undefined` | `undefined` | 分组标题           |
| `subtitle` | `string \| undefined` | `undefined` | 标题下方的辅助说明 |

## Slots

| 名称      | 说明                                                   |
| --------- | ------------------------------------------------------ |
| `default` | 分组内容区域，通常放置 `el-descriptions`、`el-form` 等 |
| `extra`   | 分组头部右侧扩展区域                                   |

## 使用示例

### 在详情页中使用

```vue
<script setup lang="ts">
import { MangoDetailPage, MangoPageSection } from '@mango/common';
</script>

<template>
  <MangoDetailPage title="订单详情" data-page="orders.detail">
    <MangoPageSection title="基本信息">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">BH20260920001</el-descriptions-item>
        <el-descriptions-item label="状态">已完成</el-descriptions-item>
      </el-descriptions>
    </MangoPageSection>

    <MangoPageSection title="商品信息">
      <template #extra>
        <el-button link type="primary">导出</el-button>
      </template>
      <el-table :data="products">
        <el-table-column prop="name" label="商品名称" />
      </el-table>
    </MangoPageSection>
  </MangoDetailPage>
</template>
```

### 在表单页中使用

```vue
<template>
  <MangoFormPage title="编辑订单" data-page="orders.edit">
    <MangoPageSection title="基本信息">
      <el-form :model="form" label-width="120px">
        <el-form-item label="订单名称">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>
    </MangoPageSection>

    <MangoPageSection title="收货信息">
      <el-form :model="form" label-width="120px">
        <el-form-item label="收货地址">
          <el-input v-model="form.address" />
        </el-form-item>
      </el-form>
    </MangoPageSection>
  </MangoFormPage>
</template>
```

## 重要约束

- 当 `title`、`subtitle` 或 `extra` slot 存在时，分组头部才会渲染
- 组件自带白底、边框、圆角和阴影，作为内容分组的外层卡片
- 通常与 `MangoDetailPage` 或 `MangoFormPage` 配合使用

## 概览

详情页和表单页的业务分组容器，统一标题、副标题和内容卡片样式。

## 功能清单

支持标题、副标题、头部扩展 slot 和默认内容 slot。

## 接入方式

从 `@mango/common` 导入，在详情页、表单页或个人中心分组中使用。

## 配置说明

通过 `title` 和 `subtitle` 配置分组文案，通过 `extra` slot 放置右侧操作。

## API 与扩展

Props 和 `default`/`extra` slot 见上方契约。

## 数据与初始化

无数据库、字典或默认数据初始化要求。

## 管理入口

无独立管理入口，由详情和表单页面接入。

## 快速开始

参见上方详情页和表单页示例。

## 问题排查

没有标题、说明或 extra slot 时不会渲染分组头部；确认内容应放在默认 slot 中。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
