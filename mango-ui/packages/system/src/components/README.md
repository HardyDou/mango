# @mango/system Components

## 1. 概览

本入口说明 `@mango/system` 导出的后台业务组件：

| 组件 | 标识 | 用途 |
|------|------|------|
| `ParticipantSelector` | `business-component` | 选择用户、部门范围、角色和岗位组合。 |
| `DomainSelector` | `business-component` | 业务域树下拉选择，返回业务域 id。 |
| `DomainSideTree` | `business-component` | 后台列表左侧业务域树，返回业务域 code。 |
| `SystemConfigPanel` | `business-component` | 按业务域集中展示和编辑系统配置卡片。 |

这些组件面向 Mango Admin 和后台业务页面，默认依赖 Element Plus、Mango 请求上下文、登录态、租户和后端平台数据，不适合作为官网或 C 端页面组件直接使用。

## 2. 功能清单

| 能力 | 组件 | 说明 |
|------|------|------|
| 用户、组织、角色、岗位组合选择 | `ParticipantSelector` | 适合审批人、抄送人、授权对象、流程节点候选人。 |
| 业务域下拉 | `DomainSelector` | 自动读取启用业务域树，返回 id 或 id 数组。 |
| 业务域侧边筛选 | `DomainSideTree` | 自动读取或接收外部业务域树，返回 domainCode，并支持数量展示。 |
| 业务域配置面板 | `SystemConfigPanel` | 传入一个或多个业务域编码，按 Tab 展示配置卡片；默认只读，传 `readonly=false` 后可修改当前值。 |
| 候选数据外部控制 | `ParticipantSelector`、`DomainSideTree` | 业务页面可传入候选项、loading 和数量。 |
| 已选值回显 | 所有组件 | 回显依赖传入值和后端/候选项数据。 |

## 3. 接入方式

```ts
import {
  DomainSelector,
  DomainSideTree,
  ParticipantSelector,
  SystemConfigPanel,
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

系统配置面板：

```vue
<script setup lang="ts">
import { SystemConfigPanel } from '@mango/system';
import '@mango/system/style.css';
</script>

<template>
  <SystemConfigPanel
    :domain-codes="['CMS', 'WORKFLOW', 'NOTICE']"
    :domain-labels="{ CMS: '内容管理', WORKFLOW: '流程审批', NOTICE: '通知中心' }"
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

### SystemConfigPanel

`SystemConfigPanel` 用于业务页面按业务域集中展示系统配置。它不是配置定义维护页：参数名称、业务域、类型、默认值、可选值、字典绑定等定义内容在 `参数配置` 页面维护；业务页面通常只嵌入面板查看或操作当前配置值。

props：

| prop | 默认值 | 含义 |
|------|--------|------|
| `domainCodes` | 必填参数 | 需要展示的业务域编码列表，例如 `['CMS', 'WORKFLOW']`。通常传入 `mango-domain` 已启用业务域编码。 |
| `domainLabels` | `{}` | 业务域 Tab 展示名映射。 |
| `keyword` | `''` | 按配置键、名称、介绍进行前端过滤。 |
| `readonly` | `true` | 是否整体只读。业务页面默认只读；只有明确允许业务用户修改配置值时才传 `false`。 |
| `showRefresh` | `true` | 是否显示刷新按钮。 |
| `typeFilter` | `[]` | 展示类型过滤，支持 `BOOLEAN`、`STRING`、`NUMBER`、`RADIO`、`SELECT`、`MULTI_SELECT`、`DATE`、`DATE_RANGE`。 |

事件：

| 事件 | 含义 |
|------|------|
| `loaded` | 当前业务域配置加载完成，返回 `domainCode` 和配置列表。 |
| `update` | 配置值保存成功，返回更新后的配置。 |

业务域配置面板接入步骤：

1. 在 `业务域` 管理中创建并启用业务域，例如 `ORDER`、`CRM`、`SETTLEMENT`。业务域编码应稳定，不要在页面里手写临时编码。
2. 在 `参数配置` 页面新增或维护系统参数，业务域选择上一步创建的 `domainCode`。这里维护参数定义、默认值、当前值、展示类型、可选值、绑定字典和是否可编辑。
3. 如果配置项需要下拉、单选或多选，优先在字典管理中创建字典类型和字典数据，再在参数配置中选择 `选项来源=字典` 并绑定 `dictType`；少量固定选项可以使用自定义 JSON 选项。
4. 在业务页面嵌入 `SystemConfigPanel`，传入本业务域编码。默认只读展示，配置中心或运营控制页可传 `readonly=false` 允许修改当前值。
5. 业务后端仍通过 `SysConfigApi` 或自己的业务服务读取配置值；前端面板只是操作入口，不替代后端权限、租户和业务校验。

只读业务页示例：

```vue
<script setup lang="ts">
import { SystemConfigPanel } from '@mango/system';
import '@mango/system/style.css';
</script>

<template>
  <SystemConfigPanel
    :domain-codes="['ORDER']"
    :domain-labels="{ ORDER: '订单中心' }"
  />
</template>
```

业务控制页允许修改当前值：

```vue
<script setup lang="ts">
import { SystemConfigPanel } from '@mango/system';
import '@mango/system/style.css';

function handleConfigUpdated(config: { configKey: string; configValue: string }) {
  console.log(config.configKey, config.configValue);
}
</script>

<template>
  <SystemConfigPanel
    :domain-codes="['ORDER', 'SETTLEMENT']"
    :domain-labels="{ ORDER: '订单中心', SETTLEMENT: '结算中心' }"
    :readonly="false"
    @update="handleConfigUpdated"
  />
</template>
```

配置类型与数据格式：

| valueType | 面板控件 | configValue 格式 |
|-----------|----------|------------------|
| `BOOLEAN` | 开关 | `true` 或 `false` |
| `STRING` | 文本输入 | 普通字符串 |
| `NUMBER` | 数字输入 | 数字字符串 |
| `RADIO` | 单选按钮 | 选项 value |
| `SELECT` | 下拉选择 | 选项 value |
| `MULTI_SELECT` | 多选下拉 | JSON 数组字符串，例如 `["site","email"]` |
| `DATE` | 日期选择 | `YYYY-MM-DD` |
| `DATE_RANGE` | 日期区间 | JSON 数组字符串，例如 `["2026-07-01","2026-12-31"]` |

## 5. 后端依赖

| 组件 | 后端接口 | 说明 |
|------|----------|------|
| `DomainSelector` | `GET /domain/domains/enabled-tree` | 读取启用业务域树。 |
| `DomainSideTree` | `GET /domain/domains/enabled-tree` | 未传 `options` 时读取启用业务域树。 |
| `SystemConfigPanel` | `GET /system/config/list`、`PUT /system/config/value` | 按业务域读取配置并保存配置值。 |
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
| 配置面板业务域为空 | `domainCodes` 未传、编码和业务域不一致，或该业务域没有启用配置 | 先查业务域，再查 `/system/config/list?domainCode=...`。 |
| 配置面板只读不可改 | `readonly` 默认是 `true`，或参数配置中 `editable=false` | 业务控制页传 `readonly=false`，并确认参数定义允许编辑。 |
| 下拉、多选无选项 | 参数配置未维护 `options`，或绑定字典没有启用数据 | 查参数配置的选项来源和 `/system/dict/data/options`。 |
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
