# Mango Identity

## 1. 概览
`mango-identity` 是 Mango 的身份事实模块，负责全局账号、租户成员、成员组织关系、认证用户事实和第三方登录身份绑定。它回答“这个人是谁、属于哪个机构成员、账号状态如何”，不回答“他有什么菜单和权限”。

代码事实：

- Maven 聚合模块：`io.mango.platform.identity:mango-identity`。
- 子模块：`mango-identity-api`、`mango-identity-core`、`mango-identity-starter`、`mango-identity-starter-remote`。
- 本地 HTTP 路径：`/identity`。
- Remote starter Feign 服务名：`mango-identity`，路径 `/identity`。
- 默认登录域：`INTERNAL`。
- 默认操作者类型：`INTERNAL_USER`。
- 默认归属主体类型：`INTERNAL_ORG`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 创建和维护内部账号、昵称、手机号、邮箱、头像、状态 | Maven 依赖 / HTTP API / Java API |
| 为 mango-auth 提供认证所需的用户名、密码哈希、登录域和账号状态 | Maven 依赖 / HTTP API / Java API |
| 维护当前机构下的 tenant_member，让登录能解析到 memberId | Maven 依赖 / HTTP API / Java API |
| 按组织、岗位、角色或用户解析通知接收人 | Maven 依赖 / HTTP API / Java API |
| 绑定企业微信等第三方登录身份，让企微 code 能定位 Mango 用户 | Maven 依赖 / HTTP API / Java API |
| 新建机构时给创建者补建机构管理员成员，并尝试绑定 ROLE_ADMIN | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 创建和维护内部账号、昵称、手机号、邮箱、头像、状态。
- 为 `mango-auth` 提供认证所需的用户名、密码哈希、登录域和账号状态。
- 维护当前机构下的 `tenant_member`，让登录能解析到 `memberId`。
- 按组织、岗位、角色或用户解析通知接收人。
- 绑定企业微信等第三方登录身份，让企微 code 能定位 Mango 用户。
- 新建机构时给创建者补建机构管理员成员，并尝试绑定 `ROLE_ADMIN`。

## 4. 边界说明
- 不签发 token，不刷新 token；这些属于 `mango-auth`。
- 不维护角色、菜单、按钮权限和 API 资源；这些属于 `mango-authorization`。
- 不执行边界入口访问控制；这些属于 `mango-access`。
- 不保存业务客户、订单、合同等领域主数据。

## 5. 模块组成
核心对象分三层：

- `identity_user`：全局账号。保存 username、password hash、realm、actorType、partyType、partyId、email、phone、avatar、status。
- `tenant_member`：账号在某个机构下的成员身份。登录必须落到启用成员，后续授权主体使用 `memberId`。
- `tenant_member_org`：成员和组织/岗位关系，支持按组织、岗位解析接收人。

外部登录身份单独保存在 `identity_external_binding`，通过 `tenant_id + provider + corp_id + external_user_id` 唯一定位绑定关系。

identity 管理接口按当前 `MangoContextHolder.tenantId()` 限定可管理成员。删除成员只移除当前机构成员身份、成员组织关系和当前机构下的成员角色绑定，不删除全局账号。

## 6. 接入方式
本地身份服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-starter</artifactId>
</dependency>
```

微服务远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-starter-remote</artifactId>
</dependency>
```

只使用契约对象和 Java API：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-api</artifactId>
</dependency>
```

本地服务需要基础设施提供：

- `PasswordEncoder`：创建用户、重置密码和登录校验使用同一编码器。
- `MangoContextHolder`：管理接口通过当前租户和用户上下文判断可管理范围。
- `SubjectRoleBindingMapper`：删除成员时清理当前机构成员角色绑定。

## 7. 配置说明
本模块未定义独立 `@ConfigurationProperties` 前缀。

但业务使用时要知道这些代码内默认值：

| 项 | 默认值 | 来源 | 影响 |
|----|--------|------|------|
| 登录域 | `INTERNAL` | `IdentityUserServiceImpl.DEFAULT_REALM` | 创建用户未传 `realm` 时使用；认证按 `realm + username` 查用户 |
| 操作者类型 | `INTERNAL_USER` | `IdentityUserServiceImpl.DEFAULT_ACTOR_TYPE` | 创建用户未传 `actorType` 时使用 |
| 归属主体类型 | `INTERNAL_ORG` | `IdentityUserServiceImpl.DEFAULT_PARTY_TYPE` | 创建用户未传 `partyType` 时使用 |
| 初始密码 | `admin123` | `IdentityUserServiceImpl.DEFAULT_INITIAL_PASSWORD` | 创建用户未传 `password` 时会被编码保存 |
| 外部身份默认绑定来源 | `SYNC` | `bindExternalIdentity` | `BindExternalIdentityCommand.bindSource` 为空时使用 |
| 外部身份绑定状态 | `BOUND` | `bindExternalIdentity` | 绑定成功后写入 `bind_status` |
| 租户初始化角色 | `ROLE_ADMIN` | `IdentityTenantProvisioner` | 新建租户时尝试给创建者成员绑定该角色 |
| 租户初始化 appCode | `internal-admin` | `IdentityTenantProvisioner` | 创建者默认授权上下文 |

数据库、Flyway、Web、安全和租户拦截配置来自 Mango 通用基础设施，不在 identity 内单独配置。

## 8. API 与扩展
HTTP 接口：

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/identity/users/page` | `system:user:list` | 分页查询当前机构可管理成员 |
| GET | `/identity/users/detail` | `system:user:query` | 查询成员详情 |
| POST | `/identity/users` | `system:user:add` | 新增当前机构成员账号 |
| PUT | `/identity/users` | `system:user:edit` | 修改成员资料和成员状态 |
| DELETE | `/identity/users` | `system:user:delete` | 移除当前机构成员身份 |
| POST | `/identity/users/delete-batch` | `system:user:delete` | 批量移除当前机构成员身份 |
| PUT | `/identity/users/status` | `system:user:status` | 启用或禁用当前机构成员身份 |
| PUT | `/identity/users/password/reset` | `system:user:reset-password` | 重置成员账号密码 |
| POST | `/identity/users/external-identities` | `system:user:edit` | 绑定第三方登录身份 |
| DELETE | `/identity/users/external-identities` | `system:user:edit` | 解绑第三方登录身份 |
| GET | `/identity/users/external-identity` | `system:user:query` | 按 provider/corpId/externalUserId/userId 查询绑定 |
| GET | `/identity/users/external-identities` | `system:user:query` | 查询用户已绑定外部身份 |
| GET | `/identity/auth/username` | INTERNAL | 认证链路按用户名查询认证事实 |
| GET | `/identity/auth/id` | INTERNAL | 认证链路按用户 ID 查询认证事实 |
| GET | `/identity/user/info/username` | 内部调用 | 按用户名查询身份资料 |
| GET | `/identity/user/info/id` | 内部调用 | 按用户 ID 查询身份资料 |
| GET | `/identity/user/info/targets` | 内部调用 | 按 USER、ORG、POST、ROLE 解析接收人 |

主要入参：

- `CreateIdentityUserCommand`：`username` 必填；可传 `password`、`nickname`、`realm`、`actorType`、`partyType`、`partyId`、`email`、`phone`、`avatar`、`status`、`remark`。
- `UpdateIdentityUserCommand`：`userId` 必填；可改昵称、归属主体、邮箱、手机号、头像、状态和备注。
- `UpdateIdentityUserStatusCommand`：`userId`、`status` 必填；不能修改当前登录用户自己的成员状态。
- `ResetIdentityUserPasswordCommand`：`userId`、`password` 必填，密码长度 6 到 200。
- `BindExternalIdentityCommand`：`userId`、`provider`、`corpId`、`externalUserId` 必填；`provider` 例如 `WECOM`。
- `IdentityUserPageQuery`：支持 `username`、`keyword`、`nickname`、`phone`、`email`、`status`、`realm`、`actorType`、`partyType`、`partyId`、`orgId`。

Java API 和扩展点：

- `IdentityUserApi`：用户管理和身份资料查询。
- `AuthIdentityApi`：认证链路内部身份事实查询。
- `AuthUserProvider`：给 `mango-auth` 使用的认证事实 Provider。
- `TenantMemberProvider`：按用户和租户查询启用成员事实。
- `TenantProvisioner` / `TenantDependencyChecker`：`IdentityTenantProvisioner` 参与租户初始化和删除前依赖检查。

## 9. 数据与初始化
Flyway 路径：`mango-identity-core/src/main/resources/db/migration/identity`。

核心表：

| 表 | 作用 | 关键约束 |
|----|------|----------|
| `identity_user` | 全局账号和认证资料 | `uk_identity_user_realm_username(realm, username)` |
| `tenant_member` | 账号在租户下的成员身份 | `uk_tenant_member_tenant_user(tenant_id, user_id)`、`uk_tenant_member_tenant_no(tenant_id, member_no)` |
| `tenant_member_org` | 成员组织岗位关系 | `uk_tenant_member_org_member_org(tenant_id, member_id, org_id)` |
| `identity_external_binding` | 第三方登录身份绑定 | `uk_external_binding_external(tenant_id, provider, corp_id, external_user_id)` |

初始化数据：

- `V1__init_identity.sql` 创建 `admin` 全局账号，密码为 BCrypt 哈希。
- `V1__init_identity.sql` 为初始化机构写入 `tenant_member`，成员号形如 `ADMIN-default`、`ADMIN-company_a`。
- `V3__external_identity_org_change_handover.sql` 创建 `identity_external_binding`。
- `IdentityTenantProvisioner` 是租户创建链路的启动初始化器，在新建机构时如果当前上下文有创建者用户，会补建成员号 `ADMIN-<tenantId>-<userId>` 的机构管理员成员。
- `IdentityTenantProvisioner` 会查找当前租户 `internal-admin + ROLE_ADMIN`，存在时绑定到创建者成员，绑定上下文为 `INTERNAL / INTERNAL_USER / INTERNAL_ORG / partyId=tenantId`。

## 10. 管理入口
identity 的用户管理接口使用这些权限码：

- `system:user:list`
- `system:user:query`
- `system:user:add`
- `system:user:edit`
- `system:user:delete`
- `system:user:status`
- `system:user:reset-password`

菜单和角色授权不在 identity 中初始化，归属 `mango-authorization`。用户管理页面能否看到、按钮能否点击，取决于菜单和角色是否绑定这些权限码。

租户边界：

- 管理接口使用当前 `MangoContextHolder.tenantId()` 查询 `tenant_member`。
- 创建用户会写入全局 `identity_user`，同时在当前租户创建 `tenant_member`，成员号为 `USER-<userId>`。
- 移除用户只删除当前租户成员、成员组织关系和当前租户成员角色绑定，不删除 `identity_user`。
- 外部身份绑定也按当前租户隔离。

## 11. 快速开始
业务方创建可登录用户时，按这个顺序做：

1. 调用 `/identity/users` 创建账号，传明确的 `realm`、`actorType`、`partyType` 和初始密码。
2. 确认当前租户下生成了 `tenant_member`，并且状态为 1。
3. 到 authorization 为该成员绑定角色或菜单权限。
4. 调用 `/auth/login-institutions` 能看到该机构。
5. 调用 `/auth/login` 能拿到含 `memberId` 的 token。
6. 调用 `/auth/info` 确认角色和权限正确。

第三方登录接入：

1. 先在 notice 配置企业微信登录渠道。
2. 调用 identity 外部身份绑定接口，写入 `provider=WECOM`、`corpId`、`externalUserId`。
3. 调用 `/auth/wecom/login-config` 获取扫码配置。
4. 通过 `/auth/wecom/login` 用企微 code 登录。

## 12. 问题排查
- 登录找不到用户：检查 `identity_user` 的 `realm + username`，不是只检查 username。
- 登录提示机构缺失：检查当前用户在 `tenant_member` 中是否有启用成员。
- 创建用户后无权限：identity 只创建账号和成员，还需要 authorization 绑定角色和菜单权限。
- 删除用户后账号还在：这是设计行为；删除的是当前机构成员身份，不删除全局账号。
- 企微登录提示未绑定：检查 `identity_external_binding` 的 `tenant_id`、`provider`、`corp_id`、`external_user_id` 是否和企微返回一致。
- 新租户创建者没有管理员权限：检查当前租户是否已存在 `internal-admin + ROLE_ADMIN` 角色。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [数据库规范](../../../mango-pmo/rules/backend/04-db.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
