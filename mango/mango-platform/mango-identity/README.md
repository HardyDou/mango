# Mango Identity

## 1. 能力定位

`mango-identity` 提供账号、身份资料、认证用户事实、租户成员和外部身份绑定能力。主要使用者是 `mango-auth`、业务用户管理功能和需要读取身份资料的业务模块。

代码事实：

- 聚合模块 `io.mango.platform.identity:mango-identity`。
- 子模块包括 `mango-identity-api`、`mango-identity-core`、`mango-identity-starter`、`mango-identity-starter-remote`。
- 本地 Controller 路径为 `/identity`。
- 远程 Feign Client 服务名为 `mango-identity`，路径为 `/identity`。

## 2. 适用场景

- 管理内部账号、昵称、手机号、邮箱、头像、状态等身份资料。
- 为 `mango-auth` 提供认证所需的用户名、密码哈希、登录域和状态。
- 维护租户成员、成员组织关系和外部身份绑定。
- 微服务模块通过 remote starter 查询身份用户。

## 3. 不适用场景

- 不计算角色、权限和菜单，授权归属 `mango-authorization`。
- 不负责登录、token 签发和 token 注销，认证归属 `mango-auth`。
- 不负责边界入口访问控制，入口控制归属 `mango-access`。
- 不承载业务客户、合同、订单等领域主数据。

## 4. 模块边界

`mango-identity` 只保存身份事实和成员关系。授权关系只引用 subject，不把账号资料复制到 authorization；认证流程只读取身份事实，不在 identity 内生成 token。

## 5. 接入方式

本地身份服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-starter</artifactId>
</dependency>
```

远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.identity</groupId>
    <artifactId>mango-identity-starter-remote</artifactId>
</dependency>
```

只使用契约模型时依赖 `mango-identity-api`。

## 6. 配置项

未发现本模块独立 `@ConfigurationProperties` 配置前缀。数据库、Flyway、Web 和安全装配使用 Mango 通用基础设施配置。

## 7. 对外接口 / 扩展点

- `AuthIdentityApi`：认证侧身份事实查询。
- `IdentityUserApi`：身份用户查询和维护 API。
- `AuthUserProvider`：供 `mango-auth` 查询认证用户事实。
- `TenantMemberProvider`：租户成员事实扩展点。
- 命令对象覆盖创建、修改、删除、重置密码、状态变更、绑定和解绑外部身份。
- Controller：`AuthIdentityController`、`IdentityUserController`。
- Feign：`AuthIdentityFeignClient`、`IdentityUserFeignClient`。
- HTTP 路径覆盖 `/identity/users/page`、`/identity/users/detail`、`/identity/users/status`、`/identity/users/password/reset`、`/identity/auth/username`、`/identity/auth/id`、外部身份绑定与查询接口。

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-identity-core/src/main/resources/db/migration/identity`。

已发现表：

- `identity_user`
- `tenant_member`
- `tenant_member_org`
- `identity_external_binding`

迁移文件包括 `V1__init_identity.sql`、`V2__update_admin_contact.sql`、`V3__external_identity_org_change_handover.sql`。

## 9. 菜单 / 权限 / 租户

本模块保存租户成员关系，并承载用户管理接口的权限入口。当前用户管理接口使用 `system:user:*` 权限码，菜单和角色授权事实由 `mango-authorization` 保存。`AuthIdentityController` 面向认证内部查询，应按 INTERNAL 访问边界处理。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-identity -am test
```

代表性验收：

- 创建用户后可通过 `IdentityUserApi` 查询。
- `AuthIdentityApi` 能按登录域和用户名返回认证用户事实。
- 批量删除、状态变更、重置密码、租户成员和组织关系维护应覆盖。
- 外部身份绑定、解绑，以及按 provider / corpId / userid 查询后，关联关系与用户状态一致。

## 11. 业务接入最小闭环

业务方创建可登录用户时，先通过 identity 写入用户资料、登录域、密码哈希和状态，再按业务需要维护 `tenant_member`、`tenant_member_org` 或外部身份绑定。auth 只读取认证事实，不在登录链路中补建用户。

最小验收链路：创建用户后可分页和详情查询；状态禁用后 auth 查询仍能返回用户但登录应被拒；重置密码后新密码生效旧密码失效；外部身份绑定后可按 provider / corpId / userid 定位用户。跨租户数据通过成员关系和后续授权链路校验，不在 identity 中伪造角色或菜单。

## 12. 常见问题

- 登录找不到用户时先检查 `realm + username` 是否匹配。
- 权限、菜单为空不是 identity 问题，应检查 authorization 的角色和权限绑定。
- 租户成员异常时检查 `tenant_member` 和 `tenant_member_org` 的关系数据。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
