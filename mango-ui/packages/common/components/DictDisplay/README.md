# DictDisplay

只读回显组件：把字典 `value` 转成 `label` 展示，用于列表、详情、打印等不需要录入的场景。与 `DictSelect` 共用同一套字典编码和缓存，支持普通字典与树形字典、单值与多值。

## 与其它组件的关系

| 组件 | 场景 | 说明 |
|------|------|------|
| `DictSelect` | 录入 | 下拉/树形选择，可编辑、可校验 |
| `DictDisplay` | 回显 | 只读展示 value → label，支持多值、路径和标签样式 |
| `DictTag` | 回显（旧） | 仅 FLAT、单值、标签样式；保留兼容，新的回显优先用 `DictDisplay` |

## 基础用法

```vue
<script setup lang="ts">
import { DictDisplay } from '@mango/common';
</script>

<template>
  <!-- 单值：默认显示 label，未命中时显示原始 value -->
  <DictDisplay dict-type="sys_normal_disable" :value="row.status" />

  <!-- 多值：默认用顿号连接 -->
  <DictDisplay dict-type="sys_normal_disable" :value="row.statuses" />
</template>
```

## 树形字典

```vue
<!-- 默认显示末级 label -->
<DictDisplay dict-type="system_demo_tree" shape="TREE" :value="row.category" />

<!-- 显示完整路径 -->
<DictDisplay
  dict-type="system_demo_tree"
  shape="TREE"
  :value="row.categories"
  display-format="path"
  display-separator=" / "
/>
```

## 显示样式

```vue
<!-- 标签样式（适合状态列） -->
<DictDisplay
  dict-type="sys_normal_disable"
  :value="row.statuses"
  mode="tag"
  tag-type="success"
/>

<!-- 自定义多值分隔符与空值文案 -->
<DictDisplay
  dict-type="sys_normal_disable"
  :value="row.statuses"
  separator=", "
  empty-text="未设置"
/>
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `dictType` | `string` | 必填 | 字典编码。 |
| `shape` | `'FLAT' \| 'TREE'` | `'FLAT'` | `FLAT` 走 `/options`，`TREE` 走 `/tree`。 |
| `value` | `string \| number \| Array<string \| number> \| null` | `undefined` | 单值或多值；由类型自动判断展示个数。 |
| `mode` | `'text' \| 'tag'` | `'text'` | 默认纯文本；`tag` 每个值渲染一个 `el-tag`。 |
| `separator` | `string` | `'、'` | `mode=text` 时的多值分隔符。 |
| `displayFormat` | `'label' \| 'path'` | `'label'` | TREE 专用：末级 label 或完整路径。 |
| `displaySeparator` | `string` | `' / '` | `displayFormat=path` 时的路径分隔符。 |
| `emptyText` | `string` | `'-'` | 无值时展示文本。 |
| `tagType` | `'' \| 'primary' \| 'success' \| 'info' \| 'warning' \| 'danger'` | `''` | `mode=tag` 的标签类型。 |
| `tagEffect` | `'light' \| 'dark' \| 'plain'` | `'light'` | 标签风格。 |
| `tagSize` | `'large' \| 'default' \| 'small'` | `'default'` | 标签尺寸。 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `loaded` | `(items: DictDisplayItem[])` | 字典加载完成，返回解析后的展示项。 |
| `error` | `(error)` | 字典加载失败；此时回退展示原始 value。 |

## 类型

```ts
interface DictDisplayItem {
  value: string;   // 原始值（字符串化）
  label: string;   // 命中的 label，未命中回退为原始值
  path: string[];  // 根到当前节点的 label 路径，FLAT 为空数组
  text: string;    // 最终展示文本，受 displayFormat 影响
  known: boolean;  // 是否命中字典
}
```

## Expose 方法

- `reload(force?: boolean)`：重新加载字典，`force=true` 时忽略缓存。
- `getItems()`：获取当前解析结果，便于导出、复制或做二次处理。

## 默认行为

- 单值显示 `label`，多值用 `、` 连接。
- 树形字典默认显示末级 `label`，需要上下文时改 `display-format="path"`。
- 值为空显示 `-`；未命中字典显示原始 `value`，不隐藏数据问题。
- 字典加载失败不抛异常，回退原始值并触发 `error` 事件。

## 缓存

与 `DictSelect` 共用 `@mango/common` 的字典缓存：同一编码在会话内只请求一次并合并并发请求；字典管理页保存后会自动失效，其它入口可用 `clearDictOptionCache` / `clearDictTreeCache` 或 `reload(true)`。

## 相关文档

- [Common 包说明](../../README.md)
- [DictSelect](../DictSelect/README.md)

## 概览

字典只读回显组件：输入字典编码 + value，输出 label/路径文本或标签，供列表、详情、打印等场景使用。支持 FLAT/TREE、单值/多值、纯文本/标签两种样式，默认按最常见情况展示。

## 接入方式

从 `@mango/common` 导入后，在模板中传入 `dict-type` 与 `value`；数据由组件内部按编码加载并复用缓存，无需业务侧请求字典接口。样式与 Element Plus 变量一致，随包样式自动生效。

## API 与扩展

- Props、Events、类型与 Expose 方法见上方对应章节。
- 需要自定义展示结构时，可在业务页面基于 `getItems()` 的返回值自行渲染；需要可编辑输入时改用 `DictSelect`。
- 新增样式变体应通过新增 `mode` 或插槽实现，不在业务页面复制字典解析逻辑。

## 问题排查

- 显示原始 value：确认 `dictType` 与后端字典编码一致、字典数据已启用，并检查 `error` 事件。
- 树形只显示末级名称：需要完整上下文时设置 `display-format="path"`。
- 修改字典后未更新：管理页保存会自动清缓存，其它入口可调用 `reload(true)` 或清理缓存函数。
