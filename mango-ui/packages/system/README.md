# @mango/system

## 1. 概览

`@mango/system` 是 Mango 系统管理前端包，提供字典、系统配置、后台品牌配置、租户、公共路径、地区、业务域、登录日志、操作日志、系统事件页面，并导出系统配置面板、业务域选择和参与人选择等后台业务组件。

集成形态：

| 标识 | 说明 |
|------|------|
| `admin-pages` | 系统管理、租户、日志、业务域和事件运维页面。 |
| `business-component` | `SystemConfigPanel`、`DomainSelector`、`DomainSideTree`、`ParticipantSelector` 可被后台业务页面复用。 |
| `api-client` | system、domain、event、public path 相关 API 封装。 |

它不是官网或 C 端站点组件库。页面和组件默认使用 Mango Admin 的请求、登录态、租户、权限和 Element Plus 样式。

## 2. 功能清单

| 能力 | 使用入口 | 后端依赖 |
|------|----------|----------|
| 字典类型和字典数据 | `DictView`、`dictTypeApi`、`dictDataApi` | `mango-system` |
| 系统配置 | `ConfigView`、`SystemConfigPanel`、`configApi`、`paramApi` | `mango-system` |
| 后台品牌配置 | `AdminBrandingView`、`adminBrandingApi` | `mango-system`、`@mango/file` |
| 租户 | `TenantView`、`tenantApi` | `mango-system`、`mango-authorization` |
| 公共路径 | `PublicPathView`、public path API | BFF permission / authorization |
| 地区 | `AreaView`、`areaApi` | `mango-system` |
| 业务域 | `DomainView`、`domainApi`、`DomainSelector`、`DomainSideTree` | `mango-domain` |
| 登录日志和操作日志 | `LoginLogView`、`OperationLogView`、log API | `mango-system` |
| 系统事件 outbox | `SystemEventView`、`systemEventApi` | `mango-infra-event` |
| 参与人选择 | `ParticipantSelector` | identity、org、authorization |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/system
```

宿主应用需要提供 Vue、Vue Router、Element Plus，并接入 `@mango/common` 请求上下文。部署时根据使用页面启用 system、domain、identity、org、authorization、infra-event 后端能力。

引入页面、组件和样式：

```ts
import {
  ConfigView,
  DictView,
  AdminBrandingView,
  DomainSelector,
  DomainSideTree,
  ParticipantSelector,
  SystemConfigPanel,
  dictDataApi,
  adminBrandingApi,
  domainApi,
  tenantApi,
} from '@mango/system';
import '@mango/system/style.css';
```

读取字典和业务域：

```ts
const statusOptions = await dictDataApi.options('order_status');
const domains = await domainApi.enabledTree();
```

复用业务域组件：

```vue
<script setup lang="ts">
import { DomainSelector } from '@mango/system';
import '@mango/system/style.css';
</script>

<template>
  <DomainSelector v-model="domainId" clearable />
</template>
```

复用系统配置面板：

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

允许业务控制页修改当前配置值：

```vue
<script setup lang="ts">
import { SystemConfigPanel } from '@mango/system';
import '@mango/system/style.css';
</script>

<template>
  <SystemConfigPanel
    :domain-codes="['ORDER', 'SETTLEMENT']"
    :domain-labels="{ ORDER: '订单中心', SETTLEMENT: '结算中心' }"
    :readonly="false"
  />
</template>
```

## 4. 配置说明

本包没有独立 Vite 环境变量。配置来自宿主请求层、页面注册、后端数据和组件 props。

| 配置位置 | 字段 | 含义 |
|----------|------|------|
| 宿主应用 | API baseURL / 代理 | 决定 `/system/**`、`/domain/**`、`/bff/permission/**` 请求转发目标。 |
| 页面注册 | 页面 key | Mango Admin 默认页面通常由 `@mango/admin-pages` 注册。 |
| `DomainSelector` | `multiple`、`clearable`、`disabled`、`checkStrictly`、`placeholder` | 控制业务域下拉选择。 |
| `DomainSideTree` | `options`、`counts`、`showAll`、`searchable`、`allCode` | 控制左侧业务域树、数量和筛选。 |
| `ParticipantSelector` | `userOptions`、`roleOptions`、`postOptions`、`orgTreeOptions`、`targetLoading` | 控制参与人候选项。 |
| `SystemConfigPanel` | `domainCodes`、`domainLabels`、`keyword`、`readonly`、`showRefresh`、`typeFilter` | 控制业务域配置卡片、详情和编辑能力。 |

系统配置和字典是后端数据，不是前端构建配置。业务页面要通过 `configApi.list({ domainCode })`、`dictDataApi.options(typeCode)` 读取。

## 5. API 与扩展

页面导出：

| 导出 | 默认页面 key | 管理能力 |
|------|--------------|----------|
| `DictView` | `system/dict/index` | 字典类型和字典数据。 |
| `OperationLogView` | `system/operation-log/index` | 操作日志查询和清理。 |
| `LoginLogView` | `system/login-log/index` | 登录日志、统计和清理。 |
| `TenantView` | `system/tenant/index` | 租户维护、状态切换、菜单包授权。 |
| `ConfigView` | `system/config/index` | 系统配置管理和分组查询。 |
| `AdminBrandingView` | `system/admin-branding/index` | Admin 登录页、后台框架 Logo、favicon、页脚等品牌配置。 |
| `PublicPathView` | `system/public-path/index` | 公共路径维护。 |
| `AreaView` | `system/area/index` | 地区树管理。 |
| `DomainView` | `system/domain/index` | 业务域树管理。 |
| `SystemEventView` | `system/event/index` | 事件 outbox 查询、详情和重试。 |

组件导出：

| 导出 | 用途 |
|------|------|
| `ParticipantSelector` | 选择用户、部门范围、角色、岗位组合。 |
| `DomainSelector` | 业务域树下拉选择，返回业务域 id。 |
| `DomainSideTree` | 后台列表左侧业务域树，返回业务域 code。 |
| `SystemConfigPanel` | 按业务域 Tab 展示配置卡片，支持开关、文本、数字、单选、日期和日期区间配置。 |
| `ParticipantSelectorValue` 等类型 | 参与人选择器类型。 |

主要 API：

| API | 主要接口 | 能力 |
|-----|----------|------|
| `dictTypeApi` | `/system/dict/type/list` | 字典类型列表、详情、创建、更新、删除。 |
| `dictDataApi` | `/system/dict/data/list`、`/system/dict/data/options` | 字典数据维护和选项读取。 |
| `configApi` | `/system/config/list`、`/system/config/type`、`/system/config/value` | 系统配置 CRUD、按组读取、分组列表和值更新。 |
| `adminBrandingApi` | `/system/admin-branding`、`/system/admin-branding/public` | 后台品牌配置读取、保存和未登录公共读取。 |
| `paramApi` | `/system/config/list`、`/system/config/value` | 参数维护和值更新。 |
| `tenantApi` | `/system/tenant/list` | 租户 CRUD 和状态切换。 |
| `areaApi` | `/system/area/tree`、`/system/area/children` | 地区树、子节点、详情、维护。 |
| public path API | `/bff/permission/public-path` | 公共路径增删改查。 |
| `domainApi` | `/domain/domains/page`、`/domain/domains/enabled-tree` | 业务域分页、树、启用树、详情、编码查询、状态切换。 |
| `loginLogApi` | `/system/log/login/list` | 登录日志查询、详情、清理、统计。 |
| `operationLogApi` | `/system/log/operation/list` | 操作日志查询、详情、清理。 |
| `systemEventApi` | `/system/events` | 事件分页、详情和重新消费。 |

常用返回字段：

| 数据 | 字段 |
|------|------|
| 字典选项 | `label`、`value`、`sort`、`status` |
| 系统配置 | `id`、`configKey`、`configValue`、`configName`、`domainCode`、`valueType`、`options`、`editable`、`status` |
| 后台品牌配置 | `title`、`shortTitle`、`subtitle`、`loginTitle`、`loginSubtitle`、`logoFile`、`faviconFile`、`loginImageFile`、`footerCopyright`、`icp`、`contact` |
| 租户 | `id`、`tenantCode`、`tenantName`、`status` |
| 业务域 | `id`、`domainCode`、`domainShortCode`、`domainName`、`children` |
| 系统事件 | `messageId`、`eventType`、`status`、`retryCount`、`createTime` |

## 6. 数据与初始化

`@mango/system` 不包含 migration。页面和组件依赖后端数据：

| 数据 | 来源 | 前端消费 |
|------|------|----------|
| 字典、配置、租户、地区、日志 | `mango-system` | 系统管理页面、字典和配置 API。 |
| 后台品牌配置 | `mango-system` 的 `sys_config` 和文件中心 ID | `AdminBrandingView`、Admin Shell 登录页和框架展示。 |
| 业务域 | `mango-domain` | 业务域页面、`DomainSelector`、`DomainSideTree`。 |
| 菜单、菜单包、租户授权 | `mango-authorization`、`@mango/rbac` API | 租户菜单包授权。 |
| 用户、组织、岗位、角色 | `mango-identity`、`mango-org`、`mango-authorization` | `ParticipantSelector` 候选项。 |
| 系统事件 | `mango-infra-event` outbox | 事件运维页面。 |

### 6.1 业务域配置面板数据准备

业务模块要把自己的配置接入 `SystemConfigPanel`，先准备后端数据，再在业务页面传入业务域编码：

1. 在 `业务域` 管理或本模块初始化数据中登记业务域，保持 `domainCode` 稳定，例如 `ORDER`、`CRM`、`SETTLEMENT`。
2. 在 `参数配置` 页面为该业务域维护配置定义。配置定义包括参数名称、参数键、展示类型、默认值、当前值、是否可编辑、配置介绍、可选值和绑定字典。
3. 固定选项少且不复用时，使用参数配置的自定义选项；跨页面复用或需要运营维护时，先在字典管理维护字典类型和字典数据，再在参数配置中绑定 `dictType`。
4. 业务页面嵌入 `SystemConfigPanel` 时传入同一个 `domainCode`。普通业务页面保持默认只读；业务控制台或运营面板再传 `readonly=false`。
5. 业务后端通过 `SysConfigApi` 读取配置值，前端面板修改值后只影响 `sys_config.config_value`，不替代后端权限、租户和业务规则校验。

配置展示类型：

| valueType | 面板控件 | 选项来源 |
|-----------|----------|----------|
| `BOOLEAN` | 开关 | 不需要选项。 |
| `STRING` | 文本输入 | 不需要选项。 |
| `NUMBER` | 数字输入 | 不需要选项。 |
| `RADIO` | 单选按钮 | 自定义 `options` 或绑定字典。 |
| `SELECT` | 下拉选择 | 自定义 `options` 或绑定字典。 |
| `MULTI_SELECT` | 多选下拉 | 自定义 `options` 或绑定字典。 |
| `DATE` | 日期选择 | 不需要选项。 |
| `DATE_RANGE` | 日期区间 | 不需要选项。 |

## 7. 管理入口

系统类页面通常只给平台管理员或运维角色。特别注意：

| 入口 | 风险 |
|------|------|
| 系统配置 | 可能影响全局业务开关。 |
| 后台品牌配置 | 会影响登录页、后台 Logo、浏览器标题、favicon 和页脚展示；图片字段只保存文件中心 ID。 |
| 租户 | 可能影响租户状态和菜单授权。 |
| 公共路径 | 可能绕过接口权限，必须严格授权。 |
| 操作日志和登录日志清理 | 会删除审计数据。 |
| 系统事件重试 | 会触发消息重新消费，可能改变业务状态。 |

前端选择器返回 id 或 code 只是业务提交参数，最终仍由业务后端校验权限、租户和数据范围。

## 8. 快速开始

1. 后端启用 `mango-system`，按需启用 `mango-domain`、`mango-infra-event`、identity、org、authorization。
2. 前端安装 `@mango/system`，引入 `@mango/system/style.css`。
3. 用 `@mango/admin-pages` 默认注册系统页面，或手工把页面 key 映射到导出组件。
4. 业务页面通过 `dictDataApi.options()`、`configApi.list()`、`domainApi.enabledTree()` 读取基础数据。
5. 需要按业务域集中管理配置时使用 `SystemConfigPanel`，传入一个或多个业务域编码。
6. 需要维护 Admin 品牌时注册 `system/admin-branding/index`，并确保后端启用 `GET /system/admin-branding/public` 供登录页读取。
7. 需要业务域筛选时使用 `DomainSelector` 或 `DomainSideTree`。
8. 需要选择用户、组织、角色、岗位时使用 `ParticipantSelector` 并准备候选项。

## 9. 问题排查

| 问题 | 常见原因 | 处理方式 |
|------|----------|----------|
| 字典为空 | `typeCode` 错误、字典未初始化或无权限 | 查 `/system/dict/data/options`。 |
| 配置读取为空 | 分组或配置 key 不一致 | 查 `/system/config/type`。 |
| 配置面板没有业务配置 | `domainCodes` 和业务域编码不一致，或参数配置未绑定该业务域 | 查业务域启用树和 `/system/config/list?domainCode=...`。 |
| 配置面板不能编辑 | `SystemConfigPanel` 默认只读，或参数定义 `editable=false` | 业务控制页传 `readonly=false`，并检查参数配置的可编辑状态。 |
| 后台品牌图片不显示 | 图片字段为空、文件 ID 无效或文件中心预览失败 | 确认保存值是文件中心 ID，再查文件中心预览接口。 |
| 选择类配置没有选项 | 未维护 `options` 或绑定字典无启用数据 | 查参数配置的选项来源和 `/system/dict/data/options`。 |
| 租户授权菜单树为空 | 菜单包、菜单或 appCode 缺失 | 查 authorization 菜单和菜单包。 |
| 业务域树为空 | `mango-domain` 未启用、无启用业务域或无权限 | 查 `/domain/domains/enabled-tree`。 |
| 事件页面为空 | 当前环境没有 outbox 事件或后端未启用 event | 查 `/system/events`。 |
| 参与人名称不回显 | 候选项里没有已选 id | 给组件传入包含已选项的候选数据。 |

## 10. 相关文档

- [System 后端 README](../../../mango/mango-platform/mango-system/README.md)
- [Domain 后端 README](../../../mango/mango-platform/mango-domain/README.md)
- [Event 后端 README](../../../mango/mango-infra/mango-infra-event/README.md)
- [@mango/system Components](./src/components/README.md)
- [@mango/rbac](../rbac/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
