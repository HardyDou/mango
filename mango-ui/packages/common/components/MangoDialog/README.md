# MangoDialog

管理端通用弹框外壳组件。统一标题区、关闭按钮、内容滚动区和底部按钮区。

## 导入

```ts
import { MangoDialog } from '@mango/common';
import type { MangoDialogExpose } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `boolean` | - | 弹框显示状态，支持 `v-model` |
| `title` | `string` | `undefined` | 标题文本，也可通过 `title` slot 覆盖 |
| `width` | `string \| number` | `'50%'` | 弹框宽度，语义与 Element Plus Dialog 一致 |
| `showHeader` | `boolean` | `true` | 是否展示完整顶部标题区 |
| `showClose` | `boolean` | `true` | 是否展示关闭按钮 |
| `footerAlign` | `'left' \| 'center' \| 'right'` | `'right'` | 底部插槽对齐方式 |
| `destroyOnClose` | `boolean` | `false` | 关闭后是否销毁内容 |
| `modal` | `boolean` | `undefined` | 是否保留遮罩；未指定时普通弹框为 `true`，拖拽弹框为 `false` |
| `closeOnClickModal` | `boolean` | `false` | 点击遮罩是否关闭 |
| `lockScroll` | `boolean` | `undefined` | 是否锁定页面滚动；未指定时跟随 `modal` |
| `zIndex` | `number` | `undefined` | 弹框动态置顶使用的最低层级 |
| `draggable` | `boolean` | `false` | 是否允许通过标题区或内容区空白内边距拖动整个弹框 |
| `resizable` | `boolean` | `false` | 是否允许通过四个角自由调整宽高 |
| `minWidth` | `number` | `320` | 交互调整时的最小宽度（像素） |
| `minHeight` | `number` | `240` | 交互调整时的最小高度（像素） |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `boolean` | 弹框显示状态变化 |
| `open` | - | 弹框打开 |
| `opened` | - | 弹框打开动画完成 |
| `close` | - | 弹框关闭 |
| `closed` | - | 弹框关闭动画完成 |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `bringToFront()` | - | 将已打开的当前实例提升到其它 `MangoDialog` 之上 |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 内容区域。内容区独立滚动，弹框最大高度为视口高度的 90% |
| `title` | 自定义标题内容 |
| `headerExtra` | 标题右侧扩展区域 |
| `footer` | 底部按钮区域。未传入时不渲染底部 |

## 使用示例

### 基础用法

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoDialog } from '@mango/common';

const visible = ref(false);
</script>

<template>
  <el-button @click="visible = true">打开弹框</el-button>

  <MangoDialog v-model="visible" title="新增应用" width="720px">
    <template #default>
      弹框内容
    </template>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary">确定</el-button>
    </template>
  </MangoDialog>
</template>
```

### 可拖拽和可调整大小

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoDialog, type MangoDialogExpose } from '@mango/common';

const visible = ref(false);
const dialogRef = ref<MangoDialogExpose | null>(null);

function focusDialog() {
  dialogRef.value?.bringToFront();
}
</script>

<template>
  <MangoDialog
    ref="dialogRef"
    v-model="visible"
    title="可拖拽弹框"
    width="720px"
    footer-align="right"
    draggable
    resizable
  >
    <template #default> 弹框内容 </template>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary">确定</el-button>
    </template>
  </MangoDialog>
</template>
```

## 拖拽和缩放行为

- `draggable` 和 `resizable` 相互独立，内容仍由 slots 提供
- 开启拖拽后，标题区和内容区四周实际生效的空白 padding 都可以启动拖拽，并在悬停时显示四向移动光标
- 内容盒、slot 子元素和滚动条不属于拖拽热区，也不会显示拖拽光标，点击、输入、文本选择、预览和滚动行为保持不变
- 拖拽默认关闭遮罩，并允许弹框移出浏览器可视区域；需要拖拽且保留遮罩时显式传入 `:modal="true"`
- 多个无模态弹框同时存在时，用户按下弹框的标题、内容或 footer 任意区域都会将该实例提升到最高层级
- 显式传入的 `zIndex` 会作为动态层级的最低基线
- 四角调整以每次按下时读取的真实宽高为起点，浏览器视口缩小时只收缩弹框宽高，不强制修改已经拖动的位置
- 弹框关闭后重新按 `width` 和当前视口恢复自适应布局

## 可访问性

- 组件会使用 `title` prop 作为弹框的可访问名称
- 只提供 `#title` slot 时，会通过 `aria-labelledby` 关联可见标题
- 若同时提供 `title` prop 和 `#title` slot，slot 只覆盖视觉展示，辅助技术仍使用 `title` prop 识别弹框

## 重要约束

- 短表单和标准弹框使用 `MangoDialog`；独立详情页和表单页使用 `MangoDetailPage` / `MangoFormPage`
- 不要把 `MangoDetailPage` 或 `MangoFormPage` 塞进弹框、抽屉
- 内容区独立滚动，弹框最大高度为视口高度的 90%

## 概览

统一管理端弹窗外壳，封装标题、内容、关闭和底部操作区域。

## 功能清单

支持 v-model、拖拽、缩放、遮罩、动态层级、销毁内容和自定义标题/底部 slot。

## 接入方式

从 `@mango/common` 导入，在短表单、确认操作和局部编辑场景中使用。

## 配置说明

按上方 Props 表配置尺寸、模态、关闭行为和交互能力；业务状态由宿主管理。

## API 与扩展

支持 `update:modelValue`、生命周期事件、`bringToFront()` 以及 `default`、`title`、`headerExtra`、`footer` slot。

## 数据与初始化

无数据库或默认数据初始化要求。

## 管理入口

无独立管理入口，由业务页面或公共页面组件接入。

## 快速开始

参见基础用法示例。

## 问题排查

确认使用 `v-model` 控制显示状态，关闭后的数据清理按 `destroyOnClose` 预期配置，并避免在弹框内嵌套详情页外壳。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
