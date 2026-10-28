# DictSelect

统一的 FLAT/TREE 字典选择组件。业务页面只传 `dictType` 和 `v-model`；通过 `shape` 控制内部使用普通下拉选择还是树形选择。

## 普通字典

```vue
<DictSelect
  v-model="status"
  dict-type="sys_normal_disable"
  shape="FLAT"
  clearable
/>
```

支持单选、多选、搜索、清空和已有值回显。

## TREE 字典

```vue
<DictSelect
  v-model="categoryValues"
  dict-type="system_demo_tree"
  shape="TREE"
  multiple
  :leaf-only="false"
  display-format="path"
  display-separator=" / "
  clearable
  filterable
>
  <template #selected="{ text }">
    分类：{{ text }}
  </template>
</DictSelect>
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string \| number \| Array<string \| number>` | `undefined` | 选中值；多选时为数组。 |
| `dictType` | `string` | 必填 | 字典编码。 |
| `shape` | `'FLAT' \| 'TREE'` | `'FLAT'` | `FLAT` 使用普通下拉，`TREE` 使用树形选择。 |
| `multiple` | `boolean` | `false` | 是否多选。TREE 多选显示复选框。 |
| `leafOnly` | `boolean` | `true` | TREE 专用；true 禁用父节点，仅允许叶子节点；false 时父节点可选。 |
| `checkStrictly` | `boolean` | `false` | TREE 专用；false 时父子联动（勾选父节点即级联子节点，部分选中为半选），true 时父子不联动；单选需要选父节点时需同时设置该项。 |
| `displayFormat` | `'label' \| 'path'` | `'label'` | TREE 回显格式；label 显示节点 label，path 显示完整路径。 |
| `displaySeparator` | `string` | `' / '` | `displayFormat=path` 时的路径分隔符，可配置为 `-`。 |
| `placeholder` | `string` | `'请选择'` | 占位文本。 |
| `clearable` | `boolean` | `true` | 是否可清空。 |
| `disabled` | `boolean` | `false` | 是否禁用。 |
| `filterable` | `boolean` | `false` | 是否支持搜索；TREE 默认由示例显式开启。 |
| `numberValue` | `boolean` | `false` | FLAT 是否将 value 转为 number。 |
| `showAnyOption` | `boolean` | `false` | FLAT 单选是否显示“不限”。 |
| `collapseTags` | `boolean` | `true` | 多选时是否折叠标签；true 显示 `+N` 数量，false 显示全部标签。 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `value` | v-model 值变化。 |
| `change` | `(value, nodes, paths)` | 返回值、选中节点和完整路径；FLAT 的 paths 为空数组。 |
| `clear` | 无 | 清空后触发。 |
| `check-change` | `(node, checked, indeterminate)` | TREE 复选框变化；`indeterminate=true` 表示半选中。 |
| `error` | `(error)` | 字典接口请求失败。 |

## Slots

| 名称 | 参数 | 说明 |
|------|------|------|
| `selected` | `{ value, label, text }` | 自定义 TREE 选中项在选择框内的显示内容；多选时按选中项分别渲染。 |

## Expose 方法

- `reload(force?: boolean)`：重新加载当前字典；`force=true` 时忽略缓存。
- `getSelectedNodes()`：获取选中节点。
- `getSelectedPaths()`：获取选中节点的完整路径。
- `getHalfCheckedNodes()`：获取 TREE 半选中的父节点，便于提交父级 ID。
- `clear()`：清空当前选择。
- `focus()` / `blur()`：控制输入焦点。

## 数据来源

- `shape=FLAT`：组件内部调用 `/system/dict/data/options`，同一字典在会话内共享缓存。
- `shape=TREE`：组件内部调用 `/system/dict/data/tree`，同一字典共享缓存；`reload(true)` 可强制刷新。
- `system_demo_tree` 是系统内置的 TREE 示例字典，包含办公用品、数码设备及其子节点。

业务页面不需要手工请求 API、转换 children、编写路径格式化函数或猜测试数据；这些能力由组件属性、事件、插槽和 Expose 方法提供。

## 缓存与刷新

- 选项缓存按字典编码共享，`listDictOptions` / `listDictTree` 并发请求会自动合并。
- 字典管理页在新增、修改、删除字典数据或类型后会自动清理对应缓存。
- 其它入口修改字典后，可调用 `clearDictOptionCache(typeCode)` / `clearDictTreeCache(typeCode)`，或 `reload(true)` 强制刷新。
