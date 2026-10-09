# MangoSideDrawerShell

详情页侧边抽屉外壳组件。包裹页面主内容，并提供固定悬浮触发按钮和右侧滑出抽屉。

## 导入

```ts
import { MangoSideDrawerShell } from '@mango/common';
import type { MangoSideDrawerShellExpose } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `boolean` | `false` | 抽屉显示状态，支持 `v-model` |
| `title` | `string` | `'节点过程'` | 抽屉标题，同时作为悬浮按钮的 `aria-label` 与 `title` |
| `showTrigger` | `boolean` | `true` | 是否展示右下角固定悬浮触发按钮 |
| `drawerSize` | `string \| number` | `'min(420px, 100vw)'` | 抽屉宽度，透传给 `el-drawer` 的 `size` |
| `destroyOnClose` | `boolean` | `false` | 关闭时是否销毁抽屉内容 |
| `dataSurface` | `string` | `'detail.side-drawer'` | 抽屉的 `data-surface` 标识，用于埋点/定位 |
| `dataAction` | `string` | `'detail.side-drawer.open'` | 悬浮按钮的 `data-action` 标识 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `boolean` | 抽屉显示状态变化 |
| `open` | - | 抽屉打开（透传 `el-drawer` 的 `open`） |
| `close` | - | 抽屉关闭（透传 `el-drawer` 的 `close`） |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `open()` | - | 打开抽屉 |
| `close()` | - | 关闭抽屉 |
| `toggle()` | - | 切换抽屉显示状态 |

## Slots

| 名称 | 说明 |
|------|------|
| `main` | 页面主内容区，始终渲染在组件根节点内 |
| `default` | 抽屉主体内容 |

## 类型定义

```ts
export interface MangoSideDrawerShellProps {
  modelValue?: boolean;
  title?: string;
  showTrigger?: boolean;
  drawerSize?: string | number;
  destroyOnClose?: boolean;
  dataSurface?: string;
  dataAction?: string;
}

export interface MangoSideDrawerShellEmits {
  (event: 'update:modelValue', value: boolean): void;
  (event: 'open'): void;
  (event: 'close'): void;
}

export interface MangoSideDrawerShellExpose {
  open: () => void;
  close: () => void;
  toggle: () => void;
}
```

## 使用示例

### 包裹主内容与抽屉内容

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoSideDrawerShell } from '@mango/common';

const visible = ref(false);
</script>

<template>
  <MangoSideDrawerShell v-model="visible" title="节点过程">
    <template #main>
      <NodeDetail :node-id="nodeId" />
    </template>

    <ProcessTimeline :node-id="nodeId" />
  </MangoSideDrawerShell>
</template>
```

抽屉默认提供右下角分享图标悬浮按钮，点击即可打开，无需额外编码。

### 通过实例方法控制

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoSideDrawerShell, type MangoSideDrawerShellExpose } from '@mango/common';

const drawerRef = ref<MangoSideDrawerShellExpose | null>(null);
</script>

<template>
  <MangoSideDrawerShell ref="drawerRef" title="节点过程" :show-trigger="false">
    <template #main>
      <el-button @click="drawerRef?.toggle()">查看过程</el-button>
    </template>

    <ProcessTimeline :node-id="nodeId" />
  </MangoSideDrawerShell>
</template>
```

### 自定义宽度与关闭销毁

```vue
<template>
  <MangoSideDrawerShell
    v-model="visible"
    title="审批记录"
    drawer-size="480px"
    destroy-on-close
    data-surface="detail.approval-drawer"
    data-action="detail.approval.open"
  >
    <template #main>
      <NodeDetail :node-id="nodeId" />
    </template>

    <ApprovalLog :node-id="nodeId" />
  </MangoSideDrawerShell>
</template>
```

## 重要约束

- 页面主内容必须放入 `main` 插槽，抽屉内容放入默认插槽；两者属于同一组件实例，不要把 `main` 内容放到抽屉插槽内
- 悬浮按钮为 `position: fixed`（距右 32px、距底 124px，`z-index: 100`），仅在 `showTrigger` 为 `true` 且抽屉关闭时显示；视口不超过 760px 时位置变为距右 16px、距底 144px
- 抽屉固定从右侧滑出（`direction="rtl"`）并 `append-to-body`，宽度由 `drawerSize` 控制
- 使用 `v-model` 受控时仍可调用 `open()`、`close()`、`toggle()`，状态变化通过 `update:modelValue` 同步
- 抽屉标题区域样式（边框、内边距）由组件统一封装，不提供标题/底部插槽

## 概览

在详情页上叠加右侧抽屉的标准外壳，内置悬浮入口与开关控制。

## 功能清单

支持 v-model、悬浮触发按钮、右侧抽屉、标题与宽度配置、关闭销毁、埋点标识及 open/close/toggle 实例方法。

## 接入方式

从 `@mango/common` 导入，用 `main` 插槽包裹原页面内容，抽屉内容通过默认插槽传入。

## 配置说明

通过 `title`、`drawerSize`、`destroyOnClose` 配置外观与生命周期，通过 `showTrigger` 控制悬浮入口，通过 `dataSurface`、`dataAction` 配置埋点标识。

## API 与扩展

提供 `update:modelValue`、`open`、`close` 事件和 `open()`、`close()`、`toggle()` 方法，扩展内容通过 `main` 与默认插槽承载。

## 数据与初始化

无数据库、字典或默认数据初始化要求，抽屉内数据由插槽内容自行加载。

## 管理入口

无独立管理入口，由业务详情页面接入。

## 快速开始

参见包裹主内容与抽屉内容示例，使用默认配置即可获得悬浮入口和右侧抽屉。

## 问题排查

悬浮按钮不显示时检查 `showTrigger` 是否开启以及抽屉是否已处于打开状态；主内容被渲染进抽屉通常是误用了默认插槽，应放入 `main` 插槽；抽屉宽度在窄屏超出视口时调整 `drawerSize`（默认值已用 `min(420px, 100vw)` 限制）。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
