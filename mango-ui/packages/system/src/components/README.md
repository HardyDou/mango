# @mango/system Components

## 1. 概览
本入口说明 `@mango/system` 导出的复用组件：`ParticipantSelector`、`DomainSelector` 和 `DomainSideTree`。它们是后台业务页面组件，适合管理端、流程配置、模板配置和业务数据筛选，不适合官网或 C 端页面直接使用。

## 2. 功能清单
来自 `@mango/system`：

- `ParticipantSelector`
- `DomainSelector`
- `DomainSideTree`
- `ParticipantSelectorValue`
- `ParticipantTargetOption`
- `ParticipantOrgTreeOption`
- `ParticipantSelectorLoading`
- `ParticipantType`

## 3. 适用场景
- 工作流节点配置选择用户、组织范围、角色或岗位。
- 业务表单选择处理人、审批人、抄送人或授权对象。
- 模板、流程、配置或业务数据按业务域过滤。
- 后台列表左侧展示业务域树，并显示每个业务域下的数据数量。

## 4. 接入方式
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

未传 `userOptions` 时，组件打开用户页签会读取 `/identity/users/page` 前 200 条用户；角色、岗位和组织候选项需要调用方通过 props 或 `ensure-*` 事件提供。

业务域下拉：

```vue
<DomainSelector v-model="domainId" clearable />
```

业务域侧边树：

```vue
<DomainSideTree
  v-model="domainCode"
  :counts="domainCounts"
  @change="loadRows"
  @loaded="cacheDomains"
/>
```

## 5. 参数与事件
`ParticipantSelectorValue`：

```ts
interface ParticipantSelectorValue {
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
}
```

候选项：

```ts
const roleOptions = [{ label: '财务审批人', value: 'role_finance' }];
const orgTreeOptions = [
  {
    label: '研发中心',
    value: 'org_rd',
    children: [{ label: '平台组', value: 'org_platform' }],
  },
];
```

`ParticipantSelector` props：

| prop | 含义 |
|------|------|
| `modelValue` | 当前选中的用户、组织、角色、岗位 id 集合。 |
| `userOptions` | 用户候选项；不传时组件会读 `/identity/users/page`。 |
| `roleOptions` | 角色候选项。 |
| `postOptions` | 岗位候选项。 |
| `orgTreeOptions` | 组织树候选项。 |
| `targetLoading` | 各候选数据加载态。 |
| `placeholder` | 已选区域空态文案。 |
| `searchPlaceholder` | 候选项搜索框文案。 |

`ParticipantSelector` 事件：

- `update:modelValue`
- `ensure-users`
- `ensure-roles`
- `ensure-posts`
- `ensure-orgs`

`DomainSelector` props：

| prop | 默认值 | 含义 |
|------|--------|------|
| `modelValue` | `undefined` | 选中的业务域 id，`multiple` 为 true 时是 id 数组。 |
| `multiple` | `false` | 是否多选。 |
| `clearable` | `true` | 是否可清空。 |
| `disabled` | `false` | 是否禁用。 |
| `checkStrictly` | `true` | 父子节点是否不联动。 |
| `placeholder` | `请选择业务域` | 占位文案。 |

`DomainSelector` 事件：

- `update:modelValue`
- `change`

`DomainSelector` 暴露：

- `reload()`：重新读取启用业务域树。
- `options`：当前业务域树。

`DomainSideTree` props：

| prop | 默认值 | 含义 |
|------|--------|------|
| `modelValue` | 空字符串 | 选中的业务域 code。 |
| `title` | `业务域` | 侧边树标题。 |
| `subtitle` | 空字符串 | 副标题。 |
| `allLabel` | `全部` | 全部节点文案。 |
| `allCode` | `ALL` | 全部节点 code。 |
| `allCount` | 未设置 | 全部节点数量。 |
| `counts` | `{}` | 按业务域 code 展示数量。 |
| `options` | 未设置 | 外部传入业务域树；未传时读取后端启用树。 |
| `searchable` | `true` | 是否展示搜索框。 |
| `searchPlaceholder` | `请输入业务域名称` | 搜索框文案。 |
| `showAll` | `true` | 是否展示全部节点。 |

`DomainSideTree` 事件：

- `update:modelValue`
- `change`
- `loaded`

## 6. 后端依赖
- `DomainSelector` 和 `DomainSideTree` 使用 `domainApi.enabledTree()`，接口为 `/domain/domains/enabled-tree`。
- `ParticipantSelector` 默认用户候选接口为 `/identity/users/page`。
- 角色、岗位和组织候选数据通常来自 `mango-authorization`、`mango-org` 和业务方封装的 API。
- 权限和租户过滤由后端完成，组件只消费已经返回的数据。

## 7. 权限与数据边界
- 选择器返回 id 或 code，不代表最终授权成功。
- 业务提交时由后端校验当前用户是否能选择这些用户、组织、角色、岗位或业务域。
- 多租户场景下，候选项应由后端按租户过滤后再传入组件。
- 组织、角色、岗位名称回显依赖候选项；已选 id 不在候选项内时，组件只能显示原始值或空状态。

## 8. 快速开始

1. 在后台业务页面引入 `@mango/system/style.css` 和需要的选择组件。
2. 参与人选择优先由业务页面传入用户、角色、岗位和组织候选项；未传用户候选时组件会读取 `/identity/users/page`。
3. 业务域下拉或侧边树依赖 `/domain/domains/enabled-tree`，提交时保存业务需要的 id 或 code。
4. 业务接口收到选择结果后继续校验权限、租户和数据范围。

## 9. 问题排查
- 参与人候选为空：检查调用方是否传入角色、岗位、组织候选项；用户候选还要检查 `/identity/users/page`。
- 已选名称不回显：检查候选项是否包含已选 id。
- 业务域树为空：检查 `mango-domain` 后端、业务域状态和接口权限。
- 选择后业务提交失败：继续检查业务接口权限、租户和数据范围，而不是只看前端选择器。

## 10. 相关文档
- [@mango/system README](../../README.md)
- [System 后端 README](../../../../../mango/mango-platform/mango-system/README.md)
- [Domain 后端 README](../../../../../mango/mango-platform/mango-domain/README.md)
- [Identity 后端 README](../../../../../mango/mango-platform/mango-identity/README.md)
- [Org 后端 README](../../../../../mango/mango-platform/mango-org/README.md)
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
