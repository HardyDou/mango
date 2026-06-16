# @mango/system

## 1. 概览
`@mango/system` 是 Mango 系统管理前端包，提供字典、参数配置、租户、公共路径、地区、业务域、登录日志、操作日志和系统事件运维页面，并导出业务域选择、参与人选择等业务页面可复用组件。

这个包解决的是后台业务开发中“如何读取和维护系统基础数据”的问题。后端数据和接口分别来自 `mango-system`、`mango-domain`、`mango-identity`、`mango-org`、`mango-authorization` 和 `mango-infra-event`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 在 Mango Admin 中使用字典、系统配置、租户、地区、业务域和日志管理页面 | 前端注册 / 组件 / API 封装 |
| 业务页面读取字典、系统配置、租户列表、地区树或业务域树 | 前端注册 / 组件 / API 封装 |
| 在流程、模板、配置类页面中复用 ParticipantSelector 选择用户、组织、角色和岗位 | 前端注册 / 组件 / API 封装 |
| 在业务列表中复用 DomainSelector 或 DomainSideTree 做业务域筛选 | 前端注册 / 组件 / API 封装 |
| 运维排查事件 outbox 时使用 SystemEventView | 前端注册 / 组件 / API 封装 |

## 3. 适用场景
- 在 Mango Admin 中使用字典、系统配置、租户、地区、业务域和日志管理页面。
- 业务页面读取字典、系统配置、租户列表、地区树或业务域树。
- 在流程、模板、配置类页面中复用 `ParticipantSelector` 选择用户、组织、角色和岗位。
- 在业务列表中复用 `DomainSelector` 或 `DomainSideTree` 做业务域筛选。
- 运维排查事件 outbox 时使用 `SystemEventView`。

## 4. 边界说明
- 不负责系统表、租户、字典、地区或业务域的后端初始化。
- 不实现事件发布、消费、重试和 outbox 存储；`/system/events` 属于后端 `mango-infra-event`。
- 不作为官网、营销页或 C 端前台组件库。
- 不绕过后端权限、租户和数据范围校验。

## 5. 模块组成
本包只提供 Vue 页面、复用组件、API 封装和样式，不改变后端接口契约。

边界拆分：

- `mango-system`：字典、系统配置、租户、地区、登录日志、操作日志。
- `mango-domain`：业务域管理和启用业务域树。
- `mango-infra-event`：系统事件页面使用的事件查询、详情和重试接口。
- `mango-identity`、`mango-org`、`mango-authorization`：参与人选择器和租户菜单包授权所需的用户、组织、岗位、角色、菜单和菜单包数据。

## 6. 接入方式
依赖包：

```json
{
  "dependencies": {
    "@mango/system": "1.0.7"
  }
}
```

页面和 API 引入：

```ts
import {
  AreaView,
  ConfigView,
  DictView,
  DomainView,
  TenantView,
  dictDataApi,
  domainApi,
  tenantApi,
} from '@mango/system';
import '@mango/system/style.css';
```

业务组件引入：

```ts
import { DomainSelector, DomainSideTree, ParticipantSelector } from '@mango/system';
```

`@mango/system` 当前没有独立 `admin-pages` 注册入口；Mango Admin 默认页面注册在 `@mango/admin-pages` 中维护。业务项目自行接入时，需要把菜单 component 映射到导出的页面组件。

## 7. 配置说明
本包没有独立 Vite 环境变量。配置来自宿主应用请求层、菜单注册、后端数据和组件 props。

| 配置位置 | 字段 / 参数 | 含义 |
|----------|-------------|------|
| 宿主应用 | API baseURL / 代理 | 决定 `/system/**`、`/domain/**`、`/bff/permission/**` 请求转发目标。 |
| `@mango/admin-pages` | 页面 key | 默认注册 system 页面 component 映射。 |
| `DomainSelector` | `multiple`、`clearable`、`disabled`、`checkStrictly`、`placeholder` | 控制业务域树选择方式。 |
| `DomainSideTree` | `options`、`counts`、`showAll`、`searchable`、`allCode` | 控制左侧业务域树数据来源、数量展示和筛选。 |
| `ParticipantSelector` | `userOptions`、`roleOptions`、`postOptions`、`orgTreeOptions`、`targetLoading` | 控制参与人候选项。用户候选项未传时组件会读取 `/identity/users/page` 前 200 条。 |

后端系统配置通过 `configApi.byGroup(group)` 读取，字典选项通过 `dictDataApi.options(typeCode)` 读取；这些不是前端构建配置，而是后端数据。

## 8. API 与扩展
页面导出：

| 导出 | 页面能力 | 默认页面 key |
|------|----------|--------------|
| `DictView` | 字典类型和字典数据管理 | `system/dict/index` |
| `OperationLogView` | 操作日志查询和清理 | `system/operation-log/index` |
| `LoginLogView` | 登录日志、登录统计和清理 | `system/login-log/index` |
| `TenantView` | 租户维护、状态切换、菜单包授权 | `system/tenant/index` |
| `ConfigView` | 系统配置管理和分组查询 | `system/config/index` |
| `PublicPathView` | 公共路径维护 | `system/public-path/index` |
| `AreaView` | 地区树管理 | `system/area/index` |
| `DomainView` | 业务域树管理 | `system/domain/index` |
| `SystemEventView` | 事件 outbox 查询、详情和重试 | `system/event/index` |

API 导出：

- `dictTypeApi`、`dictDataApi`：`/system/dict/type/**`、`/system/dict/data/**`。
- `configApi`、`paramApi`：`/system/config/**`。
- `tenantApi`：`/system/tenant/**`。
- `areaApi`：`/system/area/**`。
- `loginLogApi`、`operationLogApi`：`/system/log/login/**`、`/system/log/operation/**`。
- `publicPath` 方法：`/bff/permission/public-path`。
- `domainApi`：`/domain/domains/**`。
- `systemEventApi`：`/system/events`、`/system/events/detail`、`/system/events/reconsume`。

组件导出：

- `ParticipantSelector`
- `DomainSelector`
- `DomainSideTree`
- `ParticipantSelectorValue`
- `ParticipantTargetOption`
- `ParticipantOrgTreeOption`
- `ParticipantSelectorLoading`
- `ParticipantType`

## 9. 数据与初始化
本包不包含数据库 migration。

| 数据 | 初始化来源 |
|------|------------|
| 字典、系统配置、租户、地区、登录日志、操作日志 | 后端 `mango-system`。 |
| 业务域 | 后端 `mango-domain`。 |
| 菜单、菜单包、租户授权 | 后端 `mango-authorization`，租户页面通过 `@mango/rbac` 的 `menuApi` 和 `menuPackageApi` 协作。 |
| 用户、组织、岗位、角色 | 后端 `mango-identity`、`mango-org`、`mango-authorization`。 |
| 系统事件 | 后端 `mango-infra-event` outbox 数据。 |

## 10. 管理入口
前端只负责页面展示和 component 映射。系统配置、租户、公共路径、日志、业务域和事件操作都必须由后端按登录态、租户上下文、角色授权和数据范围校验。

注意事项：

- 租户页面会读取菜单包和菜单树，依赖 `@mango/rbac` 及 authorization 后端。
- 公共路径页面维护的是权限绕过路径，应只给平台管理员。
- 事件重试会影响消息消费，应只给运维或平台管理员。
- 业务域选择器返回 id 或 code 只是前端筛选条件，业务提交时仍由业务后端校验。

## 11. 快速开始
1. 后端启用 `mango-system`，按业务需要启用 `mango-domain` 和 `mango-infra-event`。
2. 前端引入 `@mango/system/style.css`，按菜单 component 映射页面组件。
3. 业务页面读取字典、配置、地区、租户或业务域 API，而不是硬编码枚举。
4. 需要选择用户、组织、角色、岗位时使用 `ParticipantSelector`，并明确候选项来自后端接口。
5. 验证无权限账号无法访问系统配置、租户授权、公共路径和事件重试。

## 12. 问题排查
- 字典为空：检查后端字典初始化和 `typeCode` 是否正确。
- 租户列表为空：检查 `mango-system` 租户数据和当前账号权限。
- 租户授权菜单树为空：检查 authorization 菜单、菜单包和 `internal-admin` 应用数据。
- 业务域树为空：检查 `mango-domain` 是否启用，业务域状态是否为启用。
- 事件页面为空：检查 `mango-infra-event` 是否启用 outbox，以及当前环境是否产生事件。
- 参与人名称不回显：检查传入候选项是否包含已选 id；组件不会凭空反查所有角色、岗位和组织名称。

## 13. 相关文档
- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [System 后端 README](../../../mango/mango-platform/mango-system/README.md)
- [Domain 后端 README](../../../mango/mango-platform/mango-domain/README.md)
- [Event 后端 README](../../../mango/mango-infra/mango-infra-event/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
