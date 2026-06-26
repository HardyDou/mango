# Resource Registry 身份组织授权基线设计

## 1. 背景

当前 Resource Registry 已支持菜单、接口资源、前端运行态、工作流、通知、文件配置、字典和系统配置等声明式同步，但角色、角色数据权限、组织、岗位、成员绑定和示例用户仍依赖 Flyway seed、控制台操作或自定义初始化代码。

本设计对应 GitHub issue #248，目标是让 starter 或业务模块可以声明一套可重复同步的运营基线数据，使清库启动后具备完整的菜单、角色、组织、岗位、成员和授权绑定基础。

用户安全策略不纳入本任务。首次登录是否强制改密、密码复杂度、登录失败锁定和锁定时长已登记为 GitHub issue #250 后续处理。本任务只按声明写入 demo/bootstrap 用户，并使用现有密码加密链路保存初始密码。

## 2. 目标

- Resource Registry 支持授权、组织和身份基线资源。
- 声明使用稳定业务键幂等同步，避免环境相关 ID。
- handler 写入目标模块自有表，并返回 `targetTable`、`targetId` 供资源注册表和日志排查。
- 引用 tenant、app、realm、actorType、role、org、post、member/user 时先校验存在。
- 默认不因声明文件移除而删除人管数据。
- `AUTH_MENU.roleCodes` 可继续假定角色存在，同时允许同一部署基线先通过 `AUTH_ROLE` 创建角色。

## 3. 范围

本次新增资源类型：

| 资源类型 | 目标模块 | 目标表 |
| --- | --- | --- |
| `AUTH_ROLE` | `mango-authorization` | `authorization_role` |
| `AUTH_ROLE_DATA_SCOPE` | `mango-authorization` | `authorization_role_data_scope` |
| `AUTH_SUBJECT_ROLE` | `mango-authorization` | `authorization_subject_role_binding` |
| `ORG_UNIT` | `mango-org` | `sys_org` |
| `ORG_POST` | `mango-org` | `sys_post` |
| `ORG_MEMBER_BINDING` | `mango-identity` | `identity_tenant_member_org` |
| `IDENTITY_USER` | `mango-identity` | `identity_user` / `identity_tenant_member` |

不包含：

- 用户安全策略和登录安全策略，见 issue #250。
- 缺失声明自动删除用户、成员、组织、岗位或角色。
- 跨租户授权放行。
- 把 Resource Registry 变成身份主数据管理系统。

## 4. 资源契约

### 4.1 通用键

所有声明保留 Resource Registry 通用字段：

- `id`：资源声明稳定 ID。
- `version`：声明版本，禁止回滚。
- `bizKey`：稳定业务键。
- `targetModule`：目标模块。
- `syncMode`：默认 `AUTO`，人管数据可改为 `MANUAL` 接管。
- `status`：`ACTIVE` 或 `DISABLED`。

tenant 建议优先使用 `tenantCode` 声明，handler 通过 `sys_tenant.tenant_code` 解析 `tenantId`。允许在字段中同时提供 `tenantId` 作为断言；两者同时存在时必须指向同一租户。

### 4.2 AUTH_ROLE

字段：

- `tenantCode` / `tenantId`
- `appCode`
- `realm`
- `actorType`
- `roleCode`
- `roleName`
- `roleType`
- `sort`
- `status`
- `remark`

幂等键：`tenantId + appCode + realm + actorType + roleCode`。

### 4.3 AUTH_ROLE_DATA_SCOPE

字段：

- `tenantCode` / `tenantId`
- `appCode`
- `roleCode`
- `resourceCode`
- `scopeMode`：`ALL`、`SELF`、`SELF_ORG`、`SELF_ORG_AND_CHILDREN`、`ORG`
- `orgCodes`：`scopeMode=ORG` 时使用组织编码列表，handler 解析为组织 ID 写入 `scopeValues`
- `includeChildren`
- `status`

幂等键：`tenantId + appCode + roleId + resourceCode`。

### 4.4 AUTH_SUBJECT_ROLE

字段：

- `tenantCode` / `tenantId`
- `appCode`
- `realm`
- `actorType`
- `subjectType`
- `subjectCode` 或 `subjectId`
- `roleCodes`
- `status`

handler 必须解析 subject 和 role。第一版支持绑定已存在的用户、成员或主体，不负责创建缺失主体。

### 4.5 ORG_UNIT

字段：

- `tenantCode` / `tenantId`
- `orgCode`
- `orgName`
- `parentOrgCode`
- `orgType`
- `sort`
- `leaderMemberCode` 或 `leaderMemberId`
- `status`
- `remark`

幂等键：`tenantId + orgCode`。父级优先用 `parentOrgCode` 解析，避免声明环境相关 ID。

### 4.6 ORG_POST

字段：

- `tenantCode` / `tenantId`
- `postCode`
- `postName`
- `postType`
- `sort`
- `status`
- `remark`

幂等键：`tenantId + postCode`。

### 4.7 ORG_MEMBER_BINDING

字段：

- `tenantCode` / `tenantId`
- `memberCode`、`username` 或 `memberId`
- `orgCode`
- `postCode`
- `primaryOrg`
- `leader`
- `status`

handler 只绑定已存在成员、组织和岗位。成员不存在时失败，避免隐式创建身份数据。

### 4.8 IDENTITY_USER

字段：

- `tenantCode` / `tenantId`
- `username`
- `password`
- `nickname`
- `realm`
- `actorType`
- `partyType`
- `partyCode` 或 `partyId`
- `email`
- `phone`
- `avatar`
- `status`
- `remark`
- `memberCode`
- `displayName`
- `memberType`

`password` 允许用于 demo、示例、测试和 starter bootstrap 账号。handler 不保存明文密码，必须调用现有 `PasswordEncoder` 加密后写入 `identity_user.password`。本任务不实现首次登录改密和密码复杂度策略；这些安全策略由 issue #250 统一处理。

幂等键：`realm + actorType + username`。成员幂等键：`tenantId + userId` 或身份模块现有成员唯一键。

## 5. 同步行为

### 5.1 Upsert

- handler 根据业务键查找目标记录。
- 不存在则创建。
- 存在且属于 `AUTO` 管理时更新声明字段。
- 存在但已被资源注册表标记为 `MANUAL` 时，Registry 层保持既有接管行为，不覆盖目标数据。
- 引用对象不存在时失败并写同步失败日志。

### 5.2 Disable 和 Delete

- `status=DISABLED` 可以触发目标记录逻辑禁用。
- 声明文件移除时不物理删除用户、成员、组织、岗位、角色或绑定。
- `delete` 默认降级为 `disable`，不新增物理删除语义。
- 绑定类资源只在声明显式禁用时停用或删除关联，缺失声明不清理人管绑定。

### 5.3 批次顺序

声明消费推荐顺序：

1. `ORG_UNIT`
2. `ORG_POST`
3. `IDENTITY_USER`
4. `ORG_MEMBER_BINDING`
5. `AUTH_ROLE`
6. `AUTH_ROLE_DATA_SCOPE`
7. `AUTH_SUBJECT_ROLE`
8. `AUTH_MENU`

Resource Registry 当前按资源声明和 handler 分发；本任务若需要跨类型顺序，将在 sync service 中增加内置资源类型优先级，保证 `AUTH_MENU.roleCodes` 可在角色声明之后消费。

## 6. 模块改动

| 模块 | 改动 |
| --- | --- |
| `mango-resource-api` | 新增资源类型常量 |
| `mango-resource-core` | 如有必要新增内置资源类型排序 |
| `mango-authorization-starter` | 新增授权资源 handler |
| `mango-org-starter` | 新增组织和岗位资源 handler |
| `mango-identity-starter` | 新增身份用户和成员组织绑定 handler |
| `mango-authorization-api` / `mango-org-api` / `mango-identity-api` | 只在现有 service/API 不足以安全复用时补充内部命令 |
| README / 能力地图 | 更新 Resource Registry 可管理的基线数据说明 |

## 7. 测试

- handler 单测覆盖必填字段、幂等 upsert、引用缺失失败、禁用行为和返回 `targetTable/targetId`。
- 角色数据权限测试覆盖 `ORG` 的 `orgCodes` 到 `scopeValues` 转换。
- 组织测试覆盖父组织编码解析。
- 身份测试覆盖 `IDENTITY_USER.password` 经 `PasswordEncoder` 加密保存。
- Resource Registry 集成测试覆盖同一批次中 `AUTH_ROLE` 先于 `AUTH_MENU` 消费。

## 8. 风险

- 现有身份和组织模块 API 可能缺少按业务键 upsert 的内部入口，可能需要在 core service 增加面向 handler 的方法。
- 用户安全策略暂不处理，demo/bootstrap 初始密码的生产风险由 issue #250 后续收敛。
- 业务项目若把真实客户用户放入声明文件，会形成配置泄露和变更治理风险；README 需明确 `IDENTITY_USER` 适用于 demo、测试、starter 和平台基线账号。
