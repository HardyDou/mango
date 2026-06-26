# Mango Identity

## 1. 概览

`mango-identity` 是 Mango 的身份事实模块，负责全局账号、租户成员、成员组织关系、认证用户事实和第三方登录身份绑定。它回答“这个账号是谁、在哪个机构下是什么成员、账号状态如何”，不负责 token，也不负责菜单权限。

登录链路中，`mango-auth` 会从 identity 读取账号、密码哈希、状态和成员信息；`mango-authorization` 使用登录得到的 `memberId` 做角色和菜单授权。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 全局账号 | 保存 username、password hash、昵称、手机号、邮箱、头像、状态、登录域和主体信息 |
| 租户成员 | 保存账号在某个租户下的 `memberId`，登录和授权都依赖这个成员身份 |
| 成员组织关系 | 维护成员和组织、岗位关系，支持按 USER、ORG、POST、ROLE 解析接收人 |
| 认证事实查询 | 为 `mango-auth` 提供 `AuthUserProvider`，以及按用户名和用户 ID 查询认证事实的内部接口 |
| 第三方身份绑定 | 保存企业微信等外部身份和 Mango 用户的绑定关系 |
| 租户初始化 | 新建租户时为创建者补建管理员成员，并尝试绑定 `ROLE_ADMIN` |
| 用户管理接口 | 提供用户分页、详情、新增、编辑、状态、重置密码、批量移除等接口 |
| 资源声明 | 通过 Resource Registry 的 `IDENTITY_USER` 和 `ORG_MEMBER_BINDING` 注入 demo/bootstrap 用户和组织成员绑定 |

## 3. 后端接入

业务模块只使用身份契约时依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-api</artifactId>
</dependency>
```

部署身份服务或单体启用身份接口时依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-starter</artifactId>
</dependency>
```

微服务远程调用身份服务时依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-starter-remote</artifactId>
</dependency>
```

本地服务需要这些基础能力：

| 依赖 | 用途 |
|------|------|
| `PasswordEncoder` | 创建用户、重置密码和登录校验使用同一密码编码器 |
| `MangoContextHolder` | 管理接口按当前租户和当前用户上下文处理成员数据 |
| `SubjectRoleBindingMapper` | 删除租户成员时清理当前租户下的成员角色绑定 |

## 4. 前端接入

identity 没有独立前端包。用户管理页面通常由 RBAC/系统管理页面调用 identity 后端接口。

前端需要关注两类关系：

| 场景 | 说明 |
|------|------|
| 用户管理页面 | 调用 `/identity/users/**`，按钮权限使用 `system:user:*` |
| 登录页 | 登录前机构来自 `mango-auth` 调用的 `LoginTenantProvider`，底层依赖 `tenant_member` |
| 企业微信登录 | 先通过 identity 外部身份接口绑定 `WECOM + corpId + externalUserId`，再走 `mango-auth` 企微登录 |

## 5. 快速开始

创建一个可登录管理后台用户：

1. 使用 `/identity/users` 创建账号，传入 `username`、密码、昵称、手机号或邮箱，并明确 `realm`、`actorType`、`partyType`。
2. 确认当前租户生成了启用状态的 `tenant_member`，并拿到 `memberId`。
3. 在 authorization 中给该 `memberId` 绑定角色。
4. 调用 `/auth/login-institutions`，确认能返回该租户。
5. 调用 `/auth/login`，请求体传 `tenantId` 或 `tenantCode`。
6. 调用 `/auth/info`，确认返回的 `memberId`、roles 和 permissions 正确。

Resource Registry 基线注入可用于 demo、样例租户和初始化数据：

| 资源类型 | 关键字段 |
|----------|----------|
| `IDENTITY_USER` | `tenantId`、`username`，可声明 `password`、`memberNo`、`displayName`、联系方式和状态；初始密码由 handler 使用现有 `PasswordEncoder` 加密保存。 |
| `ORG_MEMBER_BINDING` | `tenantId`、`orgCode`，并通过 `memberId`、`memberNo` 或 `username` 解析成员；可声明 `postCode`、`primaryOrg`、`leader`。 |

首次登录改密、密码复杂度、登录失败锁定和锁定时长不在资源声明 handler 中处理，统一由独立身份安全策略能力承接。

绑定企业微信登录身份：

1. 在 notice 中配置当前租户的企业微信登录渠道。
2. 调用 `/identity/users/external-identities` 绑定 `provider=WECOM`、`corpId`、`externalUserId` 和 Mango `userId`。
3. 前端调用 `/auth/wecom/login-config` 获取企微扫码配置。
4. 前端拿到企微 code 后调用 `/auth/wecom/login`。

## 6. 配置说明

`mango-identity` 当前没有独立 `@ConfigurationProperties` 前缀。数据库、Flyway、Web、安全和租户上下文配置来自 Mango 通用基础设施。

代码内默认值：

| 项 | 默认值 | 影响 |
|----|--------|------|
| 登录域 | `INTERNAL` | 创建用户未传 `realm` 时使用；认证按 `realm + username` 查用户 |
| 操作者类型 | `INTERNAL_USER` | 创建用户未传 `actorType` 时使用 |
| 归属主体类型 | `INTERNAL_ORG` | 创建用户未传 `partyType` 时使用 |
| 初始密码 | `admin123` | 创建用户未传 `password` 时会被编码保存 |
| 外部身份默认绑定来源 | `SYNC` | `BindExternalIdentityCommand.bindSource` 为空时使用 |
| 外部身份绑定状态 | `BOUND` | 绑定成功后写入 `bind_status` |
| 租户初始化角色 | `ROLE_ADMIN` | 新建租户时尝试给创建者成员绑定该角色 |
| 租户初始化 appCode | `internal-admin` | 创建者默认授权上下文 |

## 7. 接口/API 使用

HTTP 接口前缀是 `/identity`。

| 方法 | 路径 | 权限 | 用途 |
|------|------|------|------|
| GET | `/identity/users/page` | `system:user:list` | 分页查询当前租户可管理成员 |
| GET | `/identity/users/detail` | `system:user:query` | 查询成员详情 |
| POST | `/identity/users` | `system:user:add` | 新增当前租户成员账号 |
| PUT | `/identity/users` | `system:user:edit` | 修改成员资料和成员状态 |
| DELETE | `/identity/users` | `system:user:delete` | 移除当前租户成员身份 |
| POST | `/identity/users/delete-batch` | `system:user:delete` | 批量移除当前租户成员身份 |
| PUT | `/identity/users/status` | `system:user:status` | 启用或禁用当前租户成员身份 |
| PUT | `/identity/users/password/reset` | `system:user:reset-password` | 重置成员账号密码 |
| POST | `/identity/users/external-identities` | `system:user:edit` | 绑定第三方登录身份 |
| DELETE | `/identity/users/external-identities` | `system:user:edit` | 解绑第三方登录身份 |
| GET | `/identity/users/external-identity` | `system:user:query` | 按 provider、corpId、externalUserId 或 userId 查询绑定 |
| GET | `/identity/users/external-identities` | `system:user:query` | 查询用户已绑定外部身份 |
| GET | `/identity/auth/username` | INTERNAL | 认证链路按用户名查询认证事实 |
| GET | `/identity/auth/id` | INTERNAL | 认证链路按用户 ID 查询认证事实 |
| GET | `/identity/user/info/username` | 内部调用 | 按用户名查询身份资料 |
| GET | `/identity/user/info/id` | 内部调用 | 按用户 ID 查询身份资料 |
| GET | `/identity/user/info/targets` | 内部调用 | 按 USER、ORG、POST、ROLE 解析接收人 |

主要命令对象：

| 对象 | 关键字段 |
|------|----------|
| `CreateIdentityUserCommand` | `username` 必填；可传 `password`、`nickname`、`realm`、`actorType`、`partyType`、`partyId`、`email`、`phone`、`avatar`、`status`、`remark` |
| `UpdateIdentityUserCommand` | `userId` 必填；可改昵称、归属主体、邮箱、手机号、头像、状态和备注 |
| `UpdateIdentityUserStatusCommand` | `userId`、`status` 必填；不能修改当前登录用户自己的成员状态 |
| `ResetIdentityUserPasswordCommand` | `userId`、`password` 必填，密码长度 6 到 200 |
| `BindExternalIdentityCommand` | `userId`、`provider`、`corpId`、`externalUserId` 必填 |
| `IdentityUserPageQuery` | 支持 `username`、`keyword`、`nickname`、`phone`、`email`、`status`、`realm`、`actorType`、`partyType`、`partyId`、`orgId` |

Java API：

| API | 用途 |
|-----|------|
| `IdentityUserApi` | 用户管理、身份资料、接收人解析和外部身份绑定 |
| `AuthIdentityApi` | 认证链路内部身份事实查询 |
| `AuthUserProvider` | 给 `mango-auth` 使用的认证事实 Provider |
| `TenantMemberProvider` | 按用户和租户查询启用成员事实 |

## 8. 返回字段

`AuthUserInfo` 是登录校验使用的认证事实，包含 userId、username、password hash、nickname、realm、actorType、partyType、partyId、status。

`IdentityUserVO` 是用户管理页面使用的成员视图，包含用户资料、成员状态、租户成员信息、组织岗位关系和外部身份信息。

`LoginTenantProvider` 返回的 `LoginTenantVO` 会被 `mango-auth` 用于登录机构选择，关键字段是 `tenantId`、`tenantCode`、`tenantName`、`memberId`、`memberName`、`memberType`。

`ExternalIdentityBindingVO` 用于第三方登录绑定，关键字段是 `tenantId`、`userId`、`provider`、`corpId`、`externalUserId`、`bindStatus`、`bindSource`。

## 9. 管理入口

identity 的用户管理接口使用这些权限码：

| 权限码 | 用途 |
|--------|------|
| `system:user:list` | 用户列表 |
| `system:user:query` | 用户详情和外部身份查询 |
| `system:user:add` | 新增用户 |
| `system:user:edit` | 编辑用户和外部身份绑定 |
| `system:user:delete` | 删除或批量删除当前租户成员 |
| `system:user:status` | 启停用户成员状态 |
| `system:user:reset-password` | 重置密码 |

菜单和角色授权由 `mango-authorization` 初始化和维护。用户管理页面能否看到、按钮能否点击，取决于当前角色是否拥有这些权限码。

## 10. 数据与初始化

Flyway 路径：

```text
mango-identity-core/src/main/resources/db/migration/identity
```

核心表：

| 表 | 作用 | 关键约束 |
|----|------|----------|
| `identity_user` | 全局账号和认证资料 | `uk_identity_user_realm_username(realm, username)` |
| `tenant_member` | 账号在租户下的成员身份 | `uk_tenant_member_tenant_user(tenant_id, user_id)`、`uk_tenant_member_tenant_no(tenant_id, member_no)` |
| `tenant_member_org` | 成员组织岗位关系 | `uk_tenant_member_org_member_org(tenant_id, member_id, org_id)` |
| `identity_external_binding` | 第三方登录身份绑定 | `uk_external_binding_external(tenant_id, provider, corp_id, external_user_id)` |

初始化入口：

| 入口 | 内容 |
|------|------|
| `V1__init_identity.sql` | 创建 `admin` 全局账号和初始化机构成员 |
| `V2__update_admin_contact.sql` | 更新 admin 联系方式 |
| `V3__external_identity_org_change_handover.sql` | 创建外部身份绑定表 |
| `IdentityTenantProvisioner` | 新建租户时，如果当前上下文有创建者用户，则补建成员号 `ADMIN-<tenantId>-<userId>` 的机构管理员成员 |

`IdentityTenantProvisioner` 还会查找当前租户 `internal-admin + ROLE_ADMIN` 角色。角色存在时，会把创建者成员绑定到该角色，授权上下文为 `INTERNAL / INTERNAL_USER / INTERNAL_ORG / partyId=tenantId`。

资源注入：

| 资源类型 | 目标模块 | 声明入口 | 内容 |
|----------|----------|----------|------|
| `MESSAGE_TEMPLATE` | `notice` | `IdentityMessageTemplateResourceProvider` | `identity.user.created`、`identity.password.reset`、`auth.wecom.login.bound`、`auth.wecom.login.unbound` |

通知模板通过 Java `ResourceProvider` 声明，字段契约以 `mango-notice` 的 `MESSAGE_TEMPLATE` 说明为准。创建账号、重置密码和企业微信绑定变更只发布 `NoticeSendEvent`，由 notice 本地或远程 starter 在事务提交后发送，通知失败只记录日志，不阻断 identity 主流程。

## 11. 租户边界

| 操作 | 租户行为 |
|------|----------|
| 用户分页和详情 | 按当前 `MangoContextHolder.tenantId()` 查询 `tenant_member` |
| 新增用户 | 写入全局 `identity_user`，同时在当前租户创建 `tenant_member`，成员号为 `USER-<userId>` |
| 删除用户 | 删除当前租户成员、成员组织关系和当前租户成员角色绑定，不删除全局账号 |
| 外部身份绑定 | 按当前租户隔离 |
| 登录 | `mango-auth` 必须把用户解析到启用的 `tenant_member`，后续权限主体使用 `memberId` |

## 12. 问题排查

| 现象 | 排查点 |
|------|--------|
| 登录找不到用户 | 检查 `identity_user` 的 `realm + username`，不是只检查 username |
| 登录提示机构缺失 | 检查该用户在 `tenant_member` 中是否有启用成员 |
| 创建用户后无权限 | identity 只创建账号和成员，还需要 authorization 绑定角色和菜单权限 |
| 删除用户后账号还在 | 删除的是当前租户成员身份，不删除全局 `identity_user` |
| 企微登录提示未绑定 | 检查 `identity_external_binding` 的 `tenant_id`、`provider`、`corp_id`、`external_user_id` |
| 新租户创建者没有管理员权限 | 检查当前租户是否已存在 `internal-admin + ROLE_ADMIN` 角色 |

## 13. 相关文档

- [Mango Auth](../mango-auth/README.md)
- [Mango Authorization](../mango-authorization/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
