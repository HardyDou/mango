# MangoPageSection

详情页和表单页的业务分组容器组件。用于将页面内容按业务语义分组展示。

## 导入

```ts
import { MangoPageSection } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `string \| undefined` | `undefined` | 分组标题 |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 分组内容区域，通常放置 `el-descriptions`、`el-form` 等 |
| `extra` | 分组头部右侧扩展区域 |

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

- 当 `title` 和 `extra` slot 同时存在时，分组头部才会渲染
- 组件自带白底、边框、圆角和阴影，作为内容分组的外层卡片
- 通常与 `MangoDetailPage` 或 `MangoFormPage` 配合使用
