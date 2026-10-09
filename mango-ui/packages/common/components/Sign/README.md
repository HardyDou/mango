# Sign

手写签名组件。基于 Canvas 采集鼠标/触屏笔迹，输出 PNG base64（dataURL），支持 `v-model`、笔触颜色切换和清除。

## 导入

```ts
import { Sign } from '@mango/common';
import type { SignProps, SignEmits, SignExpose, SignInstance } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string` | `''` | 签名图片 dataURL，支持 `v-model`；传入已有图片时会在挂载后绘制到画布 |
| `width` | `number` | `400` | Canvas 画布宽度（像素） |
| `height` | `number` | `200` | Canvas 画布高度（像素） |
| `strokeColor` | `string` | `'#000000'` | 初始笔触颜色，工具栏可切换 |
| `lineWidth` | `number` | `2` | 笔迹线条宽度（像素） |
| `disabled` | `boolean` | `false` | 是否禁用绘制；禁用后画布灰底、不响应书写，清除按钮也不可用 |
| `placeholder` | `string` | `'sign.placeholder'` | 画布为空时居中显示的提示文本，组件按传入字符串原样渲染 |

工具栏内置四种可选颜色：黑 `#000000`、红 `#FF0000`、蓝 `#0000FF`、绿 `#00FF00`。

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value: string` | 每次抬笔完成一段笔画后输出整张画布的 PNG dataURL；清除时输出空字符串 |
| `change` | `value: string` | 与 `update:modelValue` 同时、同载荷触发，供非 `v-model` 场景监听 |

## Expose

| 名称 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `clear()` | - | - | 清空画布、复位错误提示，并抛出空字符串的 `update:modelValue` / `change` |
| `getSignature()` | - | `string` | 实时读取画布的 PNG dataURL（空画布返回透明底图 dataURL） |
| `isEmpty()` | - | `boolean` | 当前画布是否没有任何笔迹 |

## 类型定义

```ts
interface SignProps {
  modelValue?: string;   // v-model 绑定的 base64 签名图片
  width?: number;        // 画布宽度 px
  height?: number;       // 画布高度 px
  strokeColor?: string;  // 笔触颜色
  lineWidth?: number;    // 线条宽度
  disabled?: boolean;    // 禁用状态
  placeholder?: string;  // 空画布提示文本
}

interface SignEmits {
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}

interface SignExpose {
  clear(): void;
  getSignature(): string;
  isEmpty(): boolean;
}

type SignInstance = {
  clear(): void;
  getSignature(): string;
  isEmpty(): boolean;
};
```

组件内置 i18n 文案位于 `Sign/i18n`：

| key | 中文 | English |
|-----|------|---------|
| `sign.placeholder` | 请在此处签名 | Please sign here |
| `sign.clear` | 清除 | Clear |
| `sign.color` | 颜色 | Color |
| `sign.error` | 签名生成失败，请重试 | Failed to generate signature, please retry |

## 使用示例

### 基础用法（v-model 收集签名）

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Sign } from '@mango/common';

const signature = ref('');

function submit() {
  // signature.value 即为 PNG dataURL，可直接上传或随表单提交
  console.log(signature.value);
}
</script>

<template>
  <Sign v-model="signature" placeholder="请在此处签名" />
  <el-button type="primary" @click="submit">提交签名</el-button>
</template>
```

### 自定义尺寸、颜色与禁用

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Sign, type SignExpose } from '@mango/common';

const signRef = ref<SignExpose | null>(null);
const disabled = ref(false);

function handleClear() {
  signRef.value?.clear();
}

function checkEmpty() {
  if (signRef.value?.isEmpty()) {
    console.warn('请先签名');
  }
}
</script>

<template>
  <Sign
    ref="signRef"
    :width="600"
    :height="240"
    stroke-color="#1d4ed8"
    :line-width="3"
    :disabled="disabled"
    placeholder="请手写签名"
  />
  <el-button @click="handleClear">重置</el-button>
  <el-button type="primary" @click="checkEmpty">校验</el-button>
</template>
```

### 回显已有签名

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Sign } from '@mango/common';

// 传入历史签名图片 dataURL（或同源可绘制图片地址），组件挂载后自动绘制
const saved = ref('data:image/png;base64,iVBORw0KGgo...');
</script>

<template>
  <Sign v-model="saved" />
</template>
```

## 重要约束

- 输出固定为 `canvas.toDataURL('image/png')` 生成的 PNG dataURL，不支持 JPEG、blob 或直接上传，文件体积随画布像素尺寸增长。
- 画布使用 `width`/`height` 属性像素作为内部分辨率，CSS 显示尺寸变化时组件按 `canvas.width / rect.width` 换算坐标；不要用 CSS 把画布拉伸到与 `width`/`height` 比例差异过大，否则笔迹会失真。
- `placeholder` 默认值是字符串 `'sign.placeholder'` 且模板原样渲染，需要中文提示时必须显式传入（如 `placeholder="请在此处签名"`）；工具栏“颜色/清除”与错误提示使用 i18n `t()` 翻译。
- 回显外部图片依赖浏览器 `Image` 绘制，跨域且未配置 CORS 的图片会污染画布，导致 `toDataURL` 抛错，此时组件显示“签名生成失败，请重试”。
- 组件只在每段笔画结束（`mouseup`/`mouseleave`/`touchend`）时生成一次 dataURL，书写过程中不会持续触发事件。
- 组件本身不发起任何后端请求，签名图片的存储与校验由业务方处理。

## 概览

表单与审批场景使用的手写签名板，输出可直接存储的 PNG base64 图片。

## 功能清单

支持鼠标与触屏书写、四色切换、线宽配置、空画布提示、清除/读取/判空方法、禁用态、外部 dataURL 回显以及中英文 i18n。

## 接入方式

从 `@mango/common` 导入 `Sign`，通过 `v-model` 绑定字符串类型的签名 dataURL，随业务表单一起提交。

## 配置说明

通过 `width`、`height` 设置画布像素，`strokeColor`、`lineWidth` 设置初始笔迹，`disabled` 控制只读，`placeholder` 设置空态文案；颜色在工具栏内切换。

## API 与扩展

提供 `update:modelValue`、`change` 事件和 `clear()`、`getSignature()`、`isEmpty()` 方法；类型见 `SignProps`、`SignEmits`、`SignExpose`、`SignInstance`。

## 数据与初始化

无数据库或默认数据初始化。传入 `modelValue` 时组件在挂载后将其作为图片绘制到画布；外部变更 `modelValue` 会重新加载图片，并通过时间戳丢弃过期的图片加载回调。

## 管理入口

无独立管理入口，由业务表单页面接入。

## 快速开始

放置 `<Sign v-model="signature" />`，在提交前可通过 ref 调用 `isEmpty()` 校验非空，参见基础用法示例。

## 问题排查

- 画布显示 `sign.placeholder` 原文：默认占位符未翻译，请显式传入 `placeholder`。
- 报错“签名生成失败”：多为回显跨域图片污染画布，需保证图片同源或服务端允许 CORS。
- 触屏页面签字时页面滚动：组件已对 `touchstart`/`touchmove` 调用 `preventDefault`，若仍滚动请检查外层容器的触屏样式。
- 清除按钮不可点：画布为空或处于 `disabled` 状态时按钮按设计禁用。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
