# MangoPageBackBar

详情页顶部返回栏组件。提供圆形返回按钮、页面标题、内置刷新按钮和右侧操作插槽。

## 导入

```ts
import { MangoPageBackBar } from '@mango/common';
import type { MangoPageBackBarProps } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `string` | - | 标题文本（必填） |
| `backLabel` | `string` | `'返回'` | 返回按钮的 `aria-label`，用于无障碍读屏 |
| `backTo` | `RouteLocationRaw` | `undefined` | `navigateOnBack` 开启时传给 `router.push` 的目标地址 |
| `navigateOnBack` | `boolean` | `false` | 点击返回按钮时是否自动执行路由跳转 |
| `showRefresh` | `boolean` | `true` | 是否展示内置「刷新」按钮 |
| `refreshLoading` | `boolean` | `false` | 刷新按钮的加载状态，为 `true` 时按钮同时处于 loading 与 disabled |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `back` | - | 点击返回按钮时触发，先于路由跳转 |
| `refresh` | - | 点击内置刷新按钮时触发 |

## Slots

| 名称 | 说明 |
|------|------|
| `extra` | 标题右侧操作区，渲染在刷新按钮之前；无插槽且 `showRefresh` 为 `false` 时整个右侧区域不渲染 |

## 类型定义

```ts
import type { RouteLocationRaw } from 'vue-router';

export interface MangoPageBackBarProps {
  title: string;
  backLabel?: string;
  backTo?: RouteLocationRaw;
  navigateOnBack?: boolean;
  showRefresh?: boolean;
  refreshLoading?: boolean;
}

export interface MangoPageBackBarEmits {
  (event: 'back'): void;
  (event: 'refresh'): void;
}
```

## 使用示例

### 监听返回与刷新

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoPageBackBar } from '@mango/common';

const loading = ref(false);

function handleBack() {
  // 宿主自行控制返回逻辑
}

async function loadDetail() {
  loading.value = true;
  // 调用详情接口
  loading.value = false;
}
</script>

<template>
  <MangoPageBackBar
    title="订单详情"
    :refresh-loading="loading"
    @back="handleBack"
    @refresh="loadDetail"
  />
</template>
```

### 自动路由返回

```vue
<template>
  <!-- 点击返回先抛出 back 事件，再执行 router.push('/orders') -->
  <MangoPageBackBar title="订单详情" back-to="/orders" navigate-on-back @refresh="loadDetail" />
</template>
```

### 自定义右侧操作

```vue
<template>
  <MangoPageBackBar title="订单详情" :show-refresh="false" @back="handleBack">
    <template #extra>
      <el-button>删除</el-button>
      <el-button type="primary">编辑</el-button>
    </template>
  </MangoPageBackBar>
</template>
```

## 重要约束

- `navigateOnBack` 开启时必须提供 `backTo`，否则组件抛出错误
- 自动跳转依赖应用通过 `app.use(router)` 安装 Vue Router，未安装时抛出错误
- 无论是否开启 `navigateOnBack`，点击返回按钮都会先触发 `back` 事件，宿主可在跳转前执行清理
- 返回按钮固定为圆形左箭头图标，标题样式不支持通过 Props 覆盖，需要定制时使用外层样式
- 视口宽度不超过 760px 时右侧操作区改为纵向排列并右对齐

## 概览

统一详情页顶部的返回、标题和刷新操作栏。

## 功能清单

支持返回按钮、标题、内置刷新按钮（含加载态）、自动路由返回和右侧操作插槽。

## 接入方式

从 `@mango/common` 导入，放置在详情页或表单页顶部，监听 `back`、`refresh` 事件执行业务逻辑。

## 配置说明

通过 `title`、`backLabel` 配置标题与无障碍文案，通过 `navigateOnBack`、`backTo` 控制返回行为，通过 `showRefresh`、`refreshLoading` 控制刷新按钮。

## API 与扩展

提供 `back`、`refresh` 事件与 `extra` 插槽，可在操作区放置任意业务按钮。

## 数据与初始化

无数据库、字典或默认数据初始化要求。

## 管理入口

无独立管理入口，由业务详情页面接入。

## 快速开始

参见监听返回与刷新示例。

## 问题排查

开启 `navigateOnBack` 后报错时，先检查是否传入 `backTo`、应用是否安装 Vue Router；刷新按钮未显示时检查 `showRefresh`；右侧区域整体不显示通常是既没有 `extra` 插槽又关闭了刷新按钮。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
