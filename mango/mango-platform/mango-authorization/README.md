# Mango Authorization

## 1. 能力定位

`mango-authorization` 提供授权、角色、权限、菜单、应用入口、租户应用绑定、API 资源同步和运行时访问策略能力。主要使用者是平台权限管理、业务模块资源注册、前端菜单运行时和边界访问控制。

代码事实：

- 聚合模块 `io.mango.platform.authorization:mango-authorization`。
- 子模块包括 `mango-authorization-api`、`mango-authorization-core`、`mango-authorization-support`、`mango-authorization-starter`、`mango-authorization-resource-sync-starter`、`mango-authorization-resource-access-starter`、`mango-authorization-starter-remote`。
- 本地 Controller 路径覆盖 `/authorization`、`/authorization/apps`、`/authorization/app-modules`、`/authorization/roles`、`/authorization/menus`、`/authorization/menu-packages`、`/authorization/tenant-app-bindings`。
- 远程 Feign Client 服务名为 `mango-authorization`。

## 2. 适用场景

- 管理角色、权限码、菜单树和角色菜单授权。
- 注册和查询 Spring MVC / Gateway 暴露的 API 资源。
- 按 `appCode`、登录域和主体上下文组织前端应用入口。
- 为 `mango-auth`、`mango-access` 和业务模块提供授权快照。
- 通过资源清单同步模块菜单、权限、前端运行时策略。

## 3. 不适用场景

- 不保存账号资料，账号归属 `mango-identity`。
- 不签发或刷新 token，认证归属 `mango-auth`。
- 不做文件、任务、流程等业务域数据管理。
- 不替代业务模块内部的数据权限规则和领域授权判断。

## 4. 模块边界

`mango-authorization` 是权限事实和资源策略的归口。业务模块负责声明资源和权限，authorization 负责保存、查询和聚合授权快照；边界入口和认证过滤链负责消费这些策略。

## 5. 接入方式

本地授权服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-starter</artifactId>
</dependency>
```

业务应用同步资源：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-resource-sync-starter</artifactId>
</dependency>
```

业务应用运行时 URL 策略接入：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-resource-access-starter</artifactId>
</dependency>
```

远程调用接入 `mango-authorization-starter-remote`。

## 6. 配置项

已发现配置前缀：

- `mango.authorization.resource-sync`：API 资源同步配置。
- `mango.authorization.resource-sync.manifest`：资源清单同步配置。
- `mango.authorization.resource-sync.enabled`：资源同步开关。
- `mango.authorization.resource-access.enabled`：运行时 URL 策略接入开关。
- `mango.frontend`：前端运行时配置。
- `mango.security.debug-permit-all-filter-chain`：安全调试过滤链开关。

`mango.security.debug-permit-all-filter-chain` 只用于本地调试和问题定位，业务交付验收不能依赖该开关绕过真实认证授权链路。

字段以对应 `@ConfigurationProperties` 类为准。

## 7. 对外接口 / 扩展点

- Java API 契约：`AuthorizationApi`、`ApiResourceApi`、`AppApi`、`AppModuleApi`、`MenuApi`、`PermissionApi`、`RoleApi`、`TenantAppBindingApi`。
- 注解：`@ApiAccess`、`@PermissionAccess`、`@PublicApi`、`@LoginApi`、`@InternalApi`。
- Feign 适配：`AuthorizationFeignClient`、`ApiResourceFeignClient`、`AppModuleFeignClient`；并非每个 Java API 都直接暴露 Feign 或 Controller。
- 资源清单默认扫描 `META-INF/mango/resource-manifest.json` 和 `META-INF/mango/resource-manifests/*.json`。

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-authorization-core/src/main/resources/db/migration/authorization`。

核心表：

- `authorization_api_resource`
- `authorization_permission`
- `authorization_role`
- `authorization_subject_role`
- `authorization_role_permission`
- `authorization_subject_permission`
- `authorization_app`
- `authorization_app_login_context`
- `authorization_menu`
- `authorization_role_menu`
- `authorization_menu_package`
- `authorization_menu_package_item`
- `frontend_app_registry`
- `frontend_menu_runtime_config`
- `frontend_tenant_app_binding`
- `authorization_app_module`
- `frontend_module_runtime_strategy`

当前迁移目录包含 `V1__init_authorization.sql`，以及 notice、job、business domain、system event 等历史菜单和权限资源调整迁移。新增功能模块的菜单和权限资产应归属功能模块自身的资源清单或 migration，authorization 负责保存、查询和聚合。

## 9. 菜单 / 权限 / 租户

本模块是菜单、权限和租户应用绑定的归口。菜单按应用入口和模块资源组织，权限码通过 API 注解或资源清单注册，租户入口绑定由 `frontend_tenant_app_binding` 和 `TenantAppBindingApi` 管理。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-authorization -am test
```

代表性验收：

- 资源同步后 `authorization_api_resource` 存在对应 HTTP 方法、路径和访问模式。
- 创建角色并绑定权限后，`AuthorizationApi` 返回的授权快照包含权限码。
- 用户菜单接口返回当前应用入口可访问菜单。
- 租户应用绑定后，前端运行时只暴露已绑定应用。
- resource-access `AuthorizationManager`、manifest 读写同步、缓存刷新、app runtime descriptor 和 runtime strategies 应纳入集成验收。

## 11. 业务接入最小闭环

业务模块先在 Controller 上使用 `@PublicApi`、`@LoginApi`、`@PermissionAccess` 或 `@InternalApi` 声明接口访问模式，再通过 resource manifest 声明菜单、按钮权限、页面 component key 和应用模块信息。服务启动后由 resource-sync starter 把 API 资源和 manifest 写入 authorization。

最小验收链路：资源同步后查询 `authorization_api_resource` 和菜单接口能看到业务资源；角色绑定权限后授权快照包含对应权限码；租户应用绑定后当前租户只看到已绑定应用；缓存刷新后权限变更立即反映到 access 决策。

## 12. 常见问题

- 接口权限不生效时先检查资源同步 starter 是否接入，以及注解访问模式是否正确。
- 菜单不显示时检查应用入口、菜单包、角色菜单绑定和租户应用绑定。
- 权限码变更后按功能模块归属更新资源清单或 migration，并同步验证历史角色绑定。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
