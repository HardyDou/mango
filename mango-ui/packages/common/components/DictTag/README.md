# DictTag

基于字典编码把字典值渲染成文本标签（Tag）的展示组件，封装 Element Plus `el-tag`。

## 导入

```ts
import { DictTag } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dictType` | `'success' \| 'warning' \| 'info' \| 'danger' \| ''` | `''` | 指定标签颜色类型；为空时按字典值自动推断 |
| `label` | `string` | `''` | 显式指定标签文本；传入后优先于字典翻译 |
| `value` | `string \| number \| boolean \| null` | `undefined` | 字典值，用于查询字典文本和自动推断颜色 |
| `dictCode` | `string` | `''` | 字典类型编码（`typeCode`），传入后自动加载该字典 |
| `effect` | `'light' \| 'dark' \| 'plain'` | `'light'` | 标签主题，透传给 `el-tag` |
| `size` | `'large' \| 'default' \| 'small'` | `'default'` | 标签尺寸 |
| `hit` | `boolean` | `false` | 是否显示描边 |
| `closable` | `boolean` | `false` | 是否显示关闭按钮 |
| `disableTransitions` | `boolean` | `false` | 是否禁用动画 |
| `type` | `string` | `''` | 标签颜色类型，优先级高于 `dictType` |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `close` | - | 点击关闭按钮时触发（需配合 `closable` 使用） |

## 使用示例

### 基础用法：自动翻译字典值

```vue
<script setup lang="ts">
import { DictTag } from '@mango/common';
</script>

<template>
  <DictTag dict-code="sys_normal_disable" :value="row.status" />
</template>
```

### 自定义颜色与文本

```vue
<template>
  <!-- 显式指定颜色，不再按 1/0 自动推断 -->
  <DictTag dict-code="sys_yes_no" :value="1" dict-type="warning" />

  <!-- 直接给定文本，无需依赖字典加载结果 -->
  <DictTag label="紧急" type="danger" />
</template>
```

### 可关闭标签

```vue
<script setup lang="ts">
import { DictTag } from '@mango/common';

function handleClose() {
  // 处理关闭逻辑，组件不会自行从列表移除
}
</script>

<template>
  <DictTag
    dict-code="sys_normal_disable"
    :value="0"
    closable
    @close="handleClose"
  />
</template>
```

## 标签文本与颜色的推断规则

- 文本：`label` 非空时直接使用 `label`；否则用 `value` 在字典选项中匹配，匹配到返回字典 `label`，匹配不到时回退显示 `value` 本身的字符串形式；`value` 为 `null`、`undefined` 或空字符串时显示空文本。
- 颜色：`type` 或 `dictType` 任一非空时，取 `type || dictType`；两者都为空时按字典值推断——值为 `'1'` 渲染为 `success`（绿色），值为 `'0'` 渲染为 `danger`（红色），其他值不设置颜色类型。

## 重要约束

- 依赖 system 服务字典接口：传入 `dictCode` 后，内部通过 `useDict` 调用 `listDictOptions`，发起 `GET /system/dict/data/options?typeCode={dictCode}` 请求；与 [DictSelect](../DictSelect/README.md) 共享同一份前端字典缓存。
- 自动着色只识别字符串 `'1'` 和 `'0'`（比较时把值转成字符串），其他字典值需要颜色时必须显式传入 `dictType` 或 `type`。
- 组件只负责展示和抛出 `close` 事件，关闭后不会自行从父级数据中移除，需要业务侧在事件中处理。
- 标签自带 `margin-right: 4px` 的间距，适合在表格单元格内并列渲染多个。

## 概览

字典标签展示组件，把字典值翻译为文本并以 `el-tag` 呈现，支持自动/手动着色与关闭按钮。

## 功能清单

字典文本翻译、空值兜底显示原值、`1`/`0` 自动颜色、显式颜色与文本覆盖、三种主题与尺寸、关闭事件。

## 接入方式

从 `@mango/common` 导入，在表格列、详情描述中传入 `dict-code` 与 `value` 即可。

## 配置说明

按上方 Props 表配置字典编码、当前值、颜色、主题、尺寸和可关闭状态；文本和颜色均可用 props 覆盖默认推断。

## API 与扩展

支持 `close` 事件；无 Expose 方法、无插槽。

## 数据与初始化

传入 `dictCode` 后自动请求 `GET /system/dict/data/options`（参数 `typeCode`），无需业务侧初始化；依赖后端 system 服务字典接口可用。

## 管理入口

字典数据在后端字典管理中维护，组件无独立管理入口。

## 快速开始

参见基础用法示例，`dict-code` 对应字典编码、`:value` 对应当前字典值。

## 问题排查

显示为原始值而非字典文本时，检查 `dictCode` 是否正确、字典是否加载成功；颜色不符时确认自动着色只支持 `'1'`/`'0'`，其余值需显式传 `dictType` 或 `type`。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
