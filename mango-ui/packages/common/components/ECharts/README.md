# ECharts

图表组件。基于 ECharts 5 封装，传入 `options` 即可渲染，支持深浅主题、加载态、窗口自适应和实例方法透传。

## 导入

```ts
import { ECharts } from '@mango/common';
```

类型直接使用 `echarts` 包导出（组件内部静态引入完整 `echarts`）：

```ts
import type { EChartsOption, ECharts, LoadingOptions, ECElementEvent } from 'echarts';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `options` | `EChartsOption` | `{}` | ECharts 配置项，深度监听变化并以合并方式 `setOption` |
| `height` | `string` | `'300px'` | 容器高度 |
| `width` | `string` | `'100%'` | 容器宽度 |
| `autoresize` | `boolean` | `true` | 是否在浏览器窗口 `resize` 时自动调用图表 `resize()` |
| `loading` | `boolean` | `false` | 是否显示 ECharts 加载动画 |
| `loadingOptions` | `LoadingOptions` | `undefined` | 加载动画配置，透传给 `showLoading()` |
| `theme` | `'light' \| 'dark'` | `undefined` | 图表主题；不传时按系统 `prefers-color-scheme` 自动选择 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `click` | `params: ECElementEvent` | 点击图表元素时触发，参数为 ECharts 原生点击事件参数 |
| `ready` | `chart: ECharts` | 图表初始化完成时触发，回传底层 ECharts 实例 |

## Expose

| 名称 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setOption(options)` | `options: EChartsOption` | - | 合并设置图表配置 |
| `resize()` | - | - | 手动重绘尺寸 |
| `clear()` | - | - | 清空当前配置（保留实例） |
| `dispose()` | - | - | 销毁实例并释放资源 |
| `showLoading(options?)` | `options?: LoadingOptions` | - | 显示加载动画 |
| `hideLoading()` | - | - | 隐藏加载动画 |
| `getInstance()` | - | `ECharts \| null` | 获取底层 ECharts 实例 |

## 使用示例

### 基础折线图

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ECharts } from '@mango/common';
import type { EChartsOption } from 'echarts';

const options = ref<EChartsOption>({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五'] },
  yAxis: { type: 'value' },
  series: [{ type: 'line', data: [120, 200, 150, 80, 70], smooth: true }],
});
</script>

<template>
  <ECharts :options="options" height="320px" />
</template>
```

### 加载态、点击事件与 ready

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ECharts } from '@mango/common';
import type { ECharts, EChartsOption, ECElementEvent } from 'echarts';

const loading = ref(true);
const options = ref<EChartsOption>({
  series: [
    {
      type: 'pie',
      radius: '60%',
      data: [
        { value: 335, name: '直接访问' },
        { value: 310, name: '邮件营销' },
        { value: 234, name: '联盟广告' },
      ],
    },
  ],
});

function onChartClick(params: ECElementEvent) {
  console.log('点击了', params.name, params.value);
}

function onReady(chart: ECharts) {
  console.log('图表就绪', chart);
}

// 模拟数据加载
setTimeout(() => {
  loading.value = false;
}, 800);
</script>

<template>
  <ECharts
    :options="options"
    :loading="loading"
    theme="dark"
    height="360px"
    @click="onChartClick"
    @ready="onReady"
  />
</template>
```

### 通过 ref 手动控制

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ECharts } from '@mango/common';

const chartRef = ref<InstanceType<typeof ECharts> | null>(null);

function refreshChart() {
  chartRef.value?.resize();
}

function toggleLoading() {
  chartRef.value?.showLoading();
  setTimeout(() => chartRef.value?.hideLoading(), 1000);
}
</script>

<template>
  <ECharts ref="chartRef" :options="{}" height="300px" />
  <el-button @click="refreshChart">重新适配尺寸</el-button>
  <el-button @click="toggleLoading">加载动画</el-button>
</template>
```

## 重要约束

- 组件通过 `import * as 'echarts'` 静态引入完整 ECharts，打包体积包含全量图表与组件；按需裁剪需在宿主层改造引入方式。
- `options` 使用深度监听并以默认合并模式 `setOption` 更新，未传 `notMerge`；需要彻底替换配置时通过 ref 调用底层实例 `getInstance()?.setOption(option, true)`。
- `autoresize` 只监听浏览器 `window` 的 `resize` 事件，没有使用 `ResizeObserver`；当容器因侧边栏折叠、`v-if`、flex 布局等原因独立变化（窗口尺寸不变）时，需手动调用 `resize()`。
- 图表在 `onMounted` 后的 `nextTick` 初始化；挂载在初始隐藏的弹框/标签页中时，宽高可能为 0，显示后需手动 `resize()`。
- 切换 `theme` 会在 `nextTick` 中销毁并重建整个图表实例，旧实例上通过 `getInstance()` 注册的原生事件与状态不会保留；未传 `theme` 时仅在初始化时读取一次系统深色偏好，系统主题切换不会自动跟随。
- 组件卸载时自动 `dispose()` 并移除窗口监听；手动调用 `dispose()` 后实例置空，后续 `options` 变化不会自动重建。
- 组件不发起任何后端请求，图表数据由业务方通过 `options` 注入。

## 概览

管理端通用图表渲染组件，用统一的 Vue 接口承载 ECharts 配置。

## 功能清单

支持配置驱动渲染、深浅主题（含跟随系统）、加载动画、点击与就绪事件、窗口尺寸自适应，以及 `setOption`、`resize`、`clear`、`dispose`、loading 控制和实例获取方法。

## 接入方式

从 `@mango/common` 导入 `ECharts`，传入 ECharts `options` 与容器尺寸即可；需要复杂交互时监听 `click`/`ready` 或通过 `getInstance()` 操作原生实例。

## 配置说明

所有图表配置通过 `options`（`EChartsOption`）传入，外观尺寸用 `height`/`width`，主题用 `theme`，数据加载过程用 `loading`/`loadingOptions`。

## API 与扩展

组件透传常用实例方法并暴露 `getInstance()`，ECharts 原生的 `on`、`dispatchAction`、`getDataURL` 等能力均可在原生实例上使用。

## 数据与初始化

无数据库或默认数据初始化。`options` 默认为空对象，图表在挂载后初始化并立即应用当前配置。

## 管理入口

无独立管理入口，由各业务看板、报表页面接入。

## 快速开始

传入一份合法的 `EChartsOption`，设置高度（容器必须有实际高度，默认 `300px`）即可看到图表。

## 问题排查

- 图表不显示或只剩一条线：容器没有高度/宽度，确认父布局已渲染且 `height` 生效，必要时在显示后调用 `resize()`。
- 侧边栏折叠后图表留白：该场景不触发 window resize，需手动 `resize()`。
- 更新 `options` 后旧系列残留：合并更新属预期行为，需要全量替换时使用 `setOption(option, true)`。
- 切换主题后原生事件失效：切主题会重建实例，请改在 `ready` 事件中重新绑定。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
