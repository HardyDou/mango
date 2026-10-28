# DictDisplay

只读回显组件：把字典 `value` 转成 `label` 展示，用于列表、详情、打印等不需要录入的场景。与 `DictSelect` 共用同一套字典编码和缓存，支持普通字典与树形字典、单值与多值。

## 概览

`DictDisplay` 是 `@mango/common` 的只读字典回显组件，支持 FLAT/TREE 字典、单值/多值、纯文本/标签和路径展示。

## 功能清单

| 能力 | 入口 |
|------|------|
| FLAT/TREE 字典回显 | `shape="FLAT"` 或 `shape="TREE"` |
| 单值/多值展示 | `value`、`separator` |
| 文本/标签模式 | `mode="text"` 或 `mode="tag"` |
| 树路径回显 | `display-format="path"` |
| 加载与刷新 | `loaded`、`error`、`reload(true)` |

## 与其它组件的关系

| 组件 | 场景 | 说明 |
|------|------|------|
| `DictSelect` | 录入 | 下拉/树形选择，可编辑、可校验 |
| `DictDisplay` | 回显 | 只读展示 value → label，支持多值、路径和标签样式 |
| `DictTag` | 回显（旧） | 仅 FLAT、单值、标签样式；新的回显优先用 `DictDisplay` |

## 接入方式

从 `@mango/common` 导入，在模板中传入 `dict-type` 与 `value`；数据由组件内部按编码加载并复用缓存，无需业务侧请求字典接口。

```vue
<script setup lang="ts">
import { DictDisplay } from '@mango/common';
</script>

<template>
  <DictDisplay dict-type="sys_normal_disable" :value="row.status" />
  <DictDisplay dict-type="system_demo_tree" shape="TREE" :value="row.category" display-format="path" />
</template>
```

## 配置说明

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `dictType` | 必填 | 字典编码。 |
| `shape` | `'FLAT'` | FLAT 使用 options 接口，TREE 使用 tree 接口。 |
| `value` | `undefined` | 单值或多值。 |
| `mode` | `'text'` | `text` 显示文本，`tag` 渲染标签。 |
| `separator` | `'、'` | 文本模式下的多值分隔符。 |
| `displayFormat` | `'label'` | TREE 显示末级 label 或完整 path。 |
| `displaySeparator` | `' / '` | 路径分隔符。 |
| `emptyText` | `'-'` | 空值文案。 |
| `tagType` / `tagEffect` / `tagSize` | `''` / `'light'` / `'default'` | 标签模式配置。 |
| `group` | `undefined` | 分组过滤；不传显示全量，传入后只显示该组内的值（多组按 OR）。 |
| `includeValues` | `undefined` | 白名单，在分组结果上继续限定显示值。 |
| `excludeValues` | `undefined` | 黑名单，在分组结果上继续剔除值。 |

## API 与扩展

- Events：`loaded(items)` 返回解析后的展示项；`error(error)` 表示加载失败并回退原始值。
- Expose：`reload(force?)`、`getItems()`。
- `DictDisplayItem` 包含 `value`、`label`、`path`、`text` 和 `known` 字段。

## 数据与初始化

FLAT 数据来自 `GET /system/dict/data/options`，TREE 数据来自 `GET /system/dict/data/tree`。组件不要求业务预先请求或转换数据，字典内容由后端 system 字典管理能力维护。

## 管理入口

字典内容在管理端系统字典页面维护；组件不注册菜单、按钮权限或独立管理入口，后端权限由 system 字典接口负责。

## 快速开始

安装并引入 `@mango/common` 后，将 `DictDisplay` 放在列表、详情或打印视图中，传入 `dict-type` 和 `value`。需要编辑时使用 `DictSelect`，不要在业务页面重复实现字典请求和 label 转换。

## 分组过滤

- 不传 `group`：显示全量，保持向后兼容。
- 传 `group`：只显示命中该分组的值，其余值直接不渲染（不显示原始 value）；未分组项不命中任何组。
- `includeValues` / `excludeValues` 与 `group` 叠加，顺序为：组过滤 → 白名单 → 黑名单。
- 列表 A 与详情 B 可用不同 `group` 展示同一字段的不同子集，例如 A 展示 `ORDER` 组、B 展示 `AFTER_SALE` 组。

```vue
<DictDisplay dict-type="system_demo_group" group="ORDER" :value="row.statuses" />
<DictDisplay dict-type="system_demo_group" group="AFTER_SALE" :value="row.statuses" />
```

## 默认行为与缓存

- 单值显示 label，多值默认用 `、` 连接；TREE 默认显示末级 label，需要上下文时使用 `display-format="path"`。
- 值为空显示 `-`；未命中字典显示原始 value；加载失败回退原始值并触发 `error`。
- 与 `DictSelect` 共用 `@mango/common` 字典缓存；管理页保存后自动失效，其它入口可调用 `reload(true)` 或缓存清理函数。

## 问题排查

- 显示原始 value：确认 `dictType`、后端字典编码和字典状态，并检查 `error` 事件。
- TREE 只显示末级名称：设置 `display-format="path"`。
- 修改字典后未更新：调用 `reload(true)` 或清理对应 FLAT/TREE 缓存。

## 相关文档

- [Common 包说明](../../README.md)
- [DictSelect](../DictSelect/README.md)
- [DictTag](../DictTag/README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
