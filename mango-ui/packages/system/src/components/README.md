# @mango/system Components

## 1. 概览

本入口说明 `@mango/system` 导出的后台业务组件：

| 组件 | 标识 | 用途 |
|------|------|------|
| `ParticipantSelector` | `business-component` | 选择用户、部门范围、角色和岗位组合。 |
| `DomainSelector` | `business-component` | 业务域树下拉选择，返回业务域 id。 |
| `DomainSideTree` | `business-component` | 后台列表左侧业务域树，返回业务域 code。 |

这些组件面向 Mango Admin 和后台业务页面，默认依赖 Element Plus、Mango 请求上下文、登录态、租户和后端平台数据，不适合作为官网或 C 端页面组件直接使用。

## 2. 功能清单

| 能力 | 组件 | 说明 |
|------|------|------|
| 用户、组织、角色、岗位组合选择 | `ParticipantSelector` | 适合审批人、抄送人、授权对象、流程节点候选人。 |
| 业务域下拉 | `DomainSelector` | 自动读取启用业务域树，返回 id 或 id 数组。 |
| 业务域侧边筛选 | `DomainSideTree` | 自动读取或接收外部业务域树，返回 domainCode，并支持数量展示。 |
| 候选数据外部控制 | `ParticipantSelector`、`DomainSideTree` | 业务页面可传入候选项、loading 和数量。 |
| 已选值回显 | 所有组件 | 回显依赖传入值和后端/候选项数据。 |

## 3. 接入方式

```ts
import {
  DomainSelector,
  DomainSideTree,
  ParticipantSelector,
  type ParticipantSelectorValue,
} from '@mango/system';
import '@mango/system/style.css';
```

参与人选择：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ParticipantSelector, type ParticipantSelectorValue } from '@mango/system';

const participants = ref<ParticipantSelectorValue>({
  userIds: ['10001'],
  orgIds: [],
  roleIds: ['30001'],
  postIds: [],
});
</script>

<template>
  <ParticipantSelector
    v-model="participants"
    :role-options="roleOptions"
    :post-options="postOptions"
    :org-tree-options="orgTreeOptions"
    @ensure-roles="loadRoles"
    @ensure-posts="loadPosts"
    @ensure-orgs="loadOrgs"
  />
</template>
```

业务域下拉：

```vue
<template>
  <DomainSelector v-model="domainId" clearable />
</template>
```

业务域侧边树：

```vue
<template>
  <DomainSideTree
    v-model="domainCode"
    :counts="domainCounts"
    @change="loadRows"
    @loaded="cacheDomains"
  />
</template>
```

## 4. 参数与事件

### ParticipantSelector

`v-model` 类型：

```ts
interface ParticipantSelectorValue {
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
}
```

候选项类型：

```ts
interface ParticipantTargetOption {
  label: string;
  value: string;
}

interface ParticipantOrgTreeOption extends ParticipantTargetOption {
  children?: ParticipantOrgTreeOption[];
}
```

props：

| prop | 默认值 | 含义 |
|------|--------|------|
| `modelValue` | 必填 | 当前选中的用户、组织、角色、岗位 id 集合。 |
| `userOptions` | `[]` | 用户候选项；为空且打开用户页签时会读取 `/identity/users/page`。 |
| `roleOptions` | `[]` | 角色候选项。 |
| `postOptions` | `[]` | 岗位候选项。 |
| `orgTreeOptions` | `[]` | 组织树候选项。 |
| `targetLoading` | `{}` | `users`、`roles`、`posts`、`orgs` 加载态。 |
| `placeholder` | `请选择用户、部门、角色或岗位` | 未选择时的占位文案。 |
| `searchPlaceholder` | `搜索名称/编码` | 弹窗内搜索框占位文案。 |

事件：

| 事件 | 含义 |
|------|------|
| `update:modelValue` | 确认选择后输出选中值。 |
| `ensure-users` | 用户候选即将使用；调用方可加载用户。 |
| `ensure-roles` | 角色候选即将使用；调用方应加载角色。 |
| `ensure-posts` | 岗位候选即将使用；调用方应加载岗位。 |
| `ensure-orgs` | 组织候选即将使用；调用方应加载组织树。 |

### DomainSelector

props：

| prop | 默认值 | 含义 |
|------|--------|------|
| `modelValue` | `undefined` | 选中的业务域 id；`multiple=true` 时是 id 数组。 |
| `multiple` | `false` | 是否多选。 |
| `clearable` | `true` | 是否可清空。 |
| `disabled` | `false` | 是否禁用。 |
| `checkStrictly` | `true` | 父子节点是否不联动。 |
| `placeholder` | `请选择业务域` | 占位文案。 |

事件和暴露：

| 名称 | 含义 |
|------|------|
| `update:modelValue` | 选中值变化。 |
| `change` | 选中值变化。 |
| `reload()` | 重新读取启用业务域树。 |
| `options` | 当前业务域树。 |

### DomainSideTree

props：

| prop | 默认值 | 含义 |
|------|--------|------|
| `modelValue` | 空字符串 | 当前选中的业务域 code。 |
| `title` | `业务域` | 侧边树标题。 |
| `subtitle` | 空字符串 | 副标题。 |
| `allLabel` | `全部` | 全部节点文案。 |
| `allCode` | `ALL` | 全部节点 code。 |
| `allCount` | 未设置 | 全部节点数量。 |
| `counts` | `{}` | 按业务域 code 展示数量。 |
| `options` | 未设置 | 外部业务域树；不传时组件读取后端启用树。 |
| `searchable` | `true` | 是否展示搜索框。 |
| `searchPlaceholder` | `请输入业务域名称` | 搜索框占位文案。 |
| `showAll` | `true` | 是否展示全部节点。 |

事件和暴露：

| 名称 | 含义 |
|------|------|
| `update:modelValue` | 选中的业务域 code 变化。 |
| `change` | 返回选中的业务域对象；选择全部时返回空。 |
| `loaded` | 业务域树加载完成或外部 options 更新。 |
| `reload()` | 重新读取启用业务域树；传入 `options` 时只触发 `loaded`。 |

## 5. 后端依赖

| 组件 | 后端接口 | 说明 |
|------|----------|------|
| `DomainSelector` | `GET /domain/domains/enabled-tree` | 读取启用业务域树。 |
| `DomainSideTree` | `GET /domain/domains/enabled-tree` | 未传 `options` 时读取启用业务域树。 |
| `ParticipantSelector` | `GET /identity/users/page` | 未传 `userOptions` 时读取前 200 条用户。 |
| `ParticipantSelector` | 调用方提供 | 角色、岗位、组织候选项通常来自 authorization、org 或业务封装 API。 |

## 6. 权限与数据边界

| 边界 | 说明 |
|------|------|
| 选择结果不是授权结果 | 组件只返回 id 或 code，业务提交后仍要由后端校验。 |
| 候选项要按租户过滤 | 多租户场景下，候选项应该由后端按租户和权限过滤后再传入。 |
| 回显依赖候选项 | 已选角色、岗位、组织不在候选项里时，组件无法显示友好名称。 |
| 业务域筛选要后端兜底 | 前端传 domainId 或 domainCode 后，业务接口继续校验数据范围。 |

## 7. 快速开始

1. 后端启用 `mango-domain`，保证 `/domain/domains/enabled-tree` 有启用业务域。
2. 需要参与人选择时，业务页面准备角色、岗位和组织候选项；用户候选可传入，也可让组件读取 `/identity/users/page`。
3. 页面引入 `@mango/system/style.css`。
4. 提交业务表单时，把选择结果作为参数传给业务后端，由业务后端做最终校验。

## 8. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 业务域树为空 | `mango-domain` 未启用、无启用业务域或无权限 | 查 `/domain/domains/enabled-tree`。 |
| 参与人用户为空 | 未传 `userOptions` 且 `/identity/users/page` 无数据或无权限 | 查用户接口和当前账号权限。 |
| 角色、岗位、组织为空 | 组件不会自动读取这些候选项 | 监听 `ensure-*` 事件并传入候选项。 |
| 已选名称不回显 | 候选项不包含已选 id | 加载包含已选项的候选数据。 |
| 业务提交失败 | 后端权限、租户或数据范围校验不通过 | 查业务接口返回，不要只看前端选择器。 |

## 9. 相关文档

- [@mango/system README](../../README.md)
- [Domain 后端 README](../../../../../mango/mango-platform/mango-domain/README.md)
- [Identity 后端 README](../../../../../mango/mango-platform/mango-identity/README.md)
- [Org 后端 README](../../../../../mango/mango-platform/mango-org/README.md)
- [Authorization 后端 README](../../../../../mango/mango-platform/mango-authorization/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
