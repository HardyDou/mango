# MangoAvatar

头像组件，基于 Element Plus `el-avatar` 封装，支持直接图片地址和 `mango-file:` 文件令牌两种来源，令牌会自动下载为 Object URL 展示。

## 导入

```ts
import { MangoAvatar } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `source` | `string` | `''` | 头像来源：可直接访问的图片地址（`http(s):`、`data:`、`blob:` 或以 `/` 开头的站内路径），或 `mango-file:<文件ID>` 形式的文件令牌 |
| `size` | `number \| string` | `undefined` | 头像尺寸，语义与 `el-avatar` 的 `size` 一致 |
| `shape` | `'circle' \| 'square'` | `'circle'` | 头像形状 |
| `fit` | `'fill' \| 'contain' \| 'cover' \| 'none' \| 'scale-down'` | `'cover'` | 图片填充方式 |

除上述 props 外，组件设置了 `inheritAttrs: false` 并通过 `v-bind="$attrs"` 将其余属性透传给内部 `el-avatar`。

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `error` | `error: unknown` | 文件令牌下载失败，或内部 `el-avatar` 触发图片加载错误时触发 |

## Slots

| 名称 | 说明 |
|------|------|
| `default` | 图片无法展示时的兜底内容（如用户名首字、图标），透传给 `el-avatar` 默认插槽 |

## 使用示例

### 直接使用图片地址

```vue
<script setup lang="ts">
import { MangoAvatar } from '@mango/common';
</script>

<template>
  <MangoAvatar
    :size="40"
    source="https://example.com/avatar.png"
  />
</template>
```

### 使用 mango-file 文件令牌

`source` 为 `mango-file:<正整数文件ID>` 时，组件会调用文件下载接口（`GET /file/files/download`，blob 响应）生成临时 Object URL。

```vue
<script setup lang="ts">
import { MangoAvatar } from '@mango/common';

const fileToken = 'mango-file:1024';
</script>

<template>
  <MangoAvatar :size="56" :source="fileToken" />
</template>
```

### 方形头像与兜底内容

```vue
<script setup lang="ts">
import { MangoAvatar } from '@mango/common';

function handleAvatarError(error: unknown) {
  console.error('头像加载失败', error);
}
</script>

<template>
  <MangoAvatar
    :size="48"
    shape="square"
    fit="contain"
    source="mango-file:1025"
    @error="handleAvatarError"
  >
    张
  </MangoAvatar>
</template>
```

## 重要约束

- 文件令牌必须严格匹配 `mango-file:<正整数>`（正则 `^mango-file:([1-9]\d*)$`），否则不会发起下载，组件回退到默认插槽内容
- 以 `http:`、`https:`、`data:`、`blob:` 或 `/` 开头的 `source` 视为可直接展示的地址原样透传；其它非空、非令牌字符串同样不发起请求
- 下载得到的 Object URL 会在 `source` 变化或组件卸载时自动 `revokeObjectURL` 释放，无需调用方处理；快速切换 `source` 时通过递增序号丢弃过期响应，避免旧图覆盖新图
- 组件不内置加载中样式；令牌下载期间展示默认插槽内容
- 组件无 `types.ts`，未导出独立类型

## 概览

统一头像来源解析，屏蔽文件令牌下载细节，对外保持 `el-avatar` 的尺寸、形状和填充能力。

## 功能清单

支持直接地址与文件令牌、令牌自动下载为 Object URL、过期请求竞态保护、URL 生命周期回收、加载错误事件和默认插槽兜底。

## 接入方式

从 `@mango/common` 导入 `MangoAvatar`，将用户头像字段（URL 或 `mango-file:` 令牌）绑定到 `source` 即可。

## 配置说明

通过 `source` 指定来源，`size`、`shape`、`fit` 控制外观；其余 Element Plus `el-avatar` 属性可直接透传。

## API 与扩展

支持 `error` 事件和 `default` 插槽，未暴露方法；如需更深定制可通过 `$attrs` 传递原生属性。

## 数据与初始化

无本地数据初始化；文件令牌依赖 Common 包的文件下载接口与登录态。

## 管理入口

无独立管理入口，由业务页面或用户信息组件接入。

## 快速开始

参见「直接使用图片地址」示例；使用文件中心返回的头像时参见「使用 mango-file 文件令牌」。

## 问题排查

- 头像一直显示兜底内容：检查 `source` 是否为合法令牌或可直接访问的地址，文件 ID 必须是正整数
- 令牌图片偶发不显示：确认登录态有效且下载接口返回二进制内容，失败会通过 `error` 事件上报
- 切换用户后出现旧图：组件已内置竞态保护，如仍复现请确认是否在外部缓存了旧的 Object URL

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
