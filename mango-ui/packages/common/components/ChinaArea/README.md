# ChinaArea

中国省市区级联选择组件，基于 Element Plus `el-cascader` 懒加载行政区划树。

## 导入

```ts
import { ChinaArea } from '@mango/common';
import type { ChinaAreaExpose, AreaNode } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `ApiId[]` | `[]` | 选中的区域 ID 路径数组（如 `['110000', '110100', '110101']`），支持 `v-model` |
| `placeholder` | `string` | `'chinaArea.placeholder'` | 占位文本；以 `chinaArea.` 开头时按 i18n key 翻译，否则按原文展示 |
| `level` | `number` | `3` | 联动层级，`3` 为省/市/区，`4` 为省/市/区/街道 |
| `showAllLevels` | `boolean` | `true` | 是否展示完整选中路径 |
| `showHot` | `boolean` | `true` | 类型定义中表示“是否热门城市优先”；当前组件实现未读取该字段，不影响请求与渲染顺序 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `clearable` | `boolean` | `true` | 是否可清空 |
| `filterable` | `boolean` | `true` | 是否可搜索 |
| `collapseTags` | `boolean` | `false` | 多选时是否折叠标签 |
| `separator` | `string` | `'/'` | 路径文本分隔符 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `ApiId[]` | 选中路径变化时触发，清空时为空数组 |
| `change` | `ApiId[]` | 与 `update:modelValue` 同时触发，参数相同 |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `getValue()` | 返回 `ApiId[]` | 获取当前选中的区域 ID 路径数组 |
| `clear()` | - | 清空选择并清空已加载节点缓存 |
| `clearNodeCache(parentId)` | `parentId: ApiId` | 清除指定父节点的缓存，下次展开时强制重新请求 |

## 类型定义

```ts
import type { ApiId } from '@mango/api-schema';

export interface AreaNode {
  /** 数据库ID */
  id: ApiId;
  /** 名称 */
  name: string;
  /** 父节点ID，0表示根节点 */
  parentId: ApiId;
  /** 层级: 1-省, 2-市, 3-区/县, 4-街道 */
  level?: number;
  /** 是否热门: 1-是, 0-否 */
  hot?: string;
  /** 子节点 */
  children?: AreaNode[];
  /** 是否叶子节点 */
  leaf?: boolean;
}

export interface ChinaAreaProps {
  modelValue?: ApiId[];
  placeholder?: string;
  level?: number;
  showAllLevels?: boolean;
  showHot?: boolean;
  disabled?: boolean;
  clearable?: boolean;
  filterable?: boolean;
  collapseTags?: boolean;
  separator?: string;
}

export interface ChinaAreaEmits {
  (e: 'update:modelValue', value: ApiId[]): void;
  (e: 'change', value: ApiId[]): void;
}

export interface ChinaAreaExpose {
  getValue(): ApiId[];
  clear(): void;
  clearNodeCache(parentId: ApiId): void;
}
```

## 使用示例

### 基础省市区选择

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ChinaArea } from '@mango/common';

const areaPath = ref<string[]>([]);
</script>

<template>
  <ChinaArea v-model="areaPath" />
</template>
```

### 四级联动（含街道）与自定义分隔符

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ChinaArea } from '@mango/common';

const areaPath = ref<string[]>([]);

function handleAreaChange(value: string[]) {
  // value 为完整路径：[省ID, 市ID, 区县ID, 街道ID]
  console.log(value);
}
</script>

<template>
  <ChinaArea
    v-model="areaPath"
    :level="4"
    separator="-"
    placeholder="请选择省/市/区/街道"
    @change="handleAreaChange"
  />
</template>
```

### 通过 ref 取值、清空与刷新缓存

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ChinaArea, type ChinaAreaExpose } from '@mango/common';

const areaRef = ref<ChinaAreaExpose | null>(null);

function submit() {
  const path = areaRef.value?.getValue() ?? [];
}

function reloadProvince() {
  // 根节点固定以 parentId '0' 缓存
  areaRef.value?.clearNodeCache('0');
}
</script>

<template>
  <ChinaArea ref="areaRef" />
  <el-button @click="submit">提交</el-button>
  <el-button @click="areaRef?.clear()">清空地区</el-button>
  <el-button @click="reloadProvince">刷新省份</el-button>
</template>
```

## 懒加载与容错行为

- 级联配置固定为：值字段 `id`、文本字段 `name`、子节点字段 `children`，悬停展开（`expandTrigger: 'hover'`），父子不允许独立选择，`emitPath: true`，开启懒加载。
- 首次展开请求省级（`type=1`、`parentId='0'`）；展开某节点时请求下一级（`type=当前层级 + 1`、`parentId=节点ID`）；层级达到 `level` 后节点标记为叶子，不再请求。
- 每个组件实例内按 `parentId` 缓存已加载的子节点，重复展开不重复请求。
- 单次请求带 10 秒超时，失败后间隔 1 秒重试，最多重试 2 次；最终失败时在控制台打印 `[ChinaArea] 加载失败`，并以空子节点返回（该节点按叶子处理，不会向外抛错）。

## 重要约束

- 依赖行政区划接口 `getAreaTree`：根节点请求 `GET /system/area/tree?type=1`，下级节点请求 `GET /system/area/children?parentId={id}`；开发环境设置 `VITE_USE_MOCK=true` 时使用内置 mock 数据。
- 后端字段经前端转换：`id`、`pid`(或 `parentId`) 转为字符串，层级取 `level` 或 `areaType`，名称取 `name`；节点结构需为 `{ id, name, level?, children? }`。
- 绑定值始终是完整路径 ID 数组（`emitPath: true`），不是单个区域 ID，也不包含区域名称。
- 组件使用 vue-i18n（`useI18n`），默认 key 为 `chinaArea.placeholder`、`chinaArea.noMatch`、`chinaArea.noData`，宿主需保证语言包存在，或直接传入普通占位文本。
- `showHot` 当前仅作为 props 保留，热门排序需后端返回顺序或后续组件支持，业务不要依赖该字段生效。
- 缓存为组件实例级：更换绑定值不会自动重新请求；需要强制刷新时调用 `clearNodeCache` 或 `clear`。

## 概览

行政区划级联选择器，支持省市区三级或省市区街道四级懒加载，带实例缓存、超时重试和取值/清缓存方法。

## 功能清单

懒加载级联、三级/四级配置、搜索、清空、路径分隔符配置、节点缓存、超时重试、Expose 取值/清空/清缓存。

## 接入方式

从 `@mango/common` 导入，在表单中以 `v-model` 绑定区域 ID 路径数组，按需要配置 `level`。

## 配置说明

按上方 Props 表配置联动层级、占位文本、搜索、清空和分隔符；区域数据全部由接口懒加载。

## API 与扩展

支持 `update:modelValue`、`change` 事件和 `getValue()`、`clear()`、`clearNodeCache(parentId)` 方法；无插槽。

## 数据与初始化

无需预加载，展开节点时自动请求 `/system/area/tree` 与 `/system/area/children`；依赖后端 system 服务行政区划接口可用。

## 管理入口

行政区划数据在后端区域管理中维护，组件无独立管理入口。

## 快速开始

参见基础省市区选择示例，绑定字符串数组即可。

## 问题排查

某一级展开始终为空时，查看控制台 `[ChinaArea] 加载失败` 日志并检查对应 `parentId` 的 `/system/area/children` 接口；数据更新后界面不变时用 `clearNodeCache` 清缓存；回显异常时确认 `modelValue` 是完整 ID 路径数组。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
