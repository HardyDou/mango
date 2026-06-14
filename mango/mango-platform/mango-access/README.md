# Mango Access

## 1. 能力定位

`mango-access` 提供边界入口访问控制能力，主要服务于网关应用、单体 Web 应用和需要在请求进入业务模块前完成认证初筛的 Mango 开发者。

代码事实：

- 聚合模块 `io.mango.platform.access:mango-access`。
- 子模块包括 `mango-access-core`、`mango-access-web-starter`、`mango-access-gateway-starter`。
- 核心配置类为 `AccessProperties`，配置前缀是 `mango.access`。
- 单体入口为 Servlet `AuthFilter`，网关入口为 `AuthGlobalFilter`。

## 2. 适用场景

- 微服务拓扑下，在 Spring Cloud Gateway 入口统一拦截外部 HTTP 请求。
- 单体应用中，在 Servlet Filter 阶段完成 token 校验、访问策略判断，并写入 request attributes 与 `MangoContextHolder`。
- 需要根据 `mango-authorization` 已同步的 API 资源策略判断 PUBLIC、LOGIN、PERMISSION、INTERNAL 访问模式。
- 需要把认证后的用户、租户、应用入口等上下文传递给下游服务。

## 3. 不适用场景

- 不负责登录、刷新 token、注销 token，这些能力属于 `mango-auth`。
- 不维护账号资料、机构成员或外部身份绑定，这些能力属于 `mango-identity`。
- 不维护角色、菜单、权限码和 API 资源表，这些能力属于 `mango-authorization`。
- 不替代业务服务内部的数据权限和领域校验。

## 4. 模块边界

`mango-access` 负责请求进入边界时的访问决策和上下文传递；认证事实来自 `mango-auth`，资源策略和权限快照来自 `mango-authorization`，请求上下文落地依赖 `mango-infra-web`。

访问模式边界：

- PUBLIC：匿名放行。
- LOGIN：要求 access token 有效。
- PERMISSION：要求 access token 有效且具备资源声明的权限码。
- INTERNAL：外部入口拒绝访问。

## 5. 接入方式

单体 Web 应用接入：

```xml
<dependency>
    <groupId>io.mango.platform.access</groupId>
    <artifactId>mango-access-web-starter</artifactId>
</dependency>
```

网关应用接入：

```xml
<dependency>
    <groupId>io.mango.platform.access</groupId>
    <artifactId>mango-access-gateway-starter</artifactId>
</dependency>
```

应用内应同时具备可调用的 token provider、`ApiResourceApi` 和授权快照提供者实现，具体由本地 starter 或 remote starter 装配。

## 6. 配置项

配置前缀：`mango.access`。

已发现配置入口：

- `AccessProperties`：访问控制总配置。
- `mango.access.auth-enabled`：访问控制总开关。
- `mango.access.require-permission-code`：PERMISSION 模式下权限码缺失时是否拒绝。
- token 入口覆盖 Authorization header、query token 和 cookie token。
- IP 白名单匹配由 `IpWhitelistMatcher` 承担。

具体字段以 `mango-access-core/src/main/java/io/mango/access/core/config/AccessProperties.java` 为准；README 不复制长期默认值规则。

## 7. 对外接口 / 扩展点

- `AccessService`：访问决策核心服务。
- `AccessContextValidator`：请求上下文校验。
- `AccessPrincipal`：访问主体模型。
- `AuthFilter`：Servlet 单体模式过滤器，写入 request attributes 和 `MangoContextHolder`。
- `AuthGlobalFilter`：Gateway 微服务模式过滤器，以 `X-Mango-*` 头传递用户、租户、登录域、主体类型和应用入口。

## 8. 数据库 / 初始化数据

本模块未发现独立 Flyway migration。API 资源、权限码、角色和菜单数据由 `mango-authorization` 维护。

## 9. 菜单 / 权限 / 租户

本模块不提供管理菜单。权限判断读取 `mango-authorization` 的 API 资源策略和授权快照；租户上下文来自 token claim 和 Mango 请求上下文。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-access -am test
```

当前代码中可见的单测主要覆盖 `mango-access-web-starter` 的 Servlet Filter 行为。Gateway starter 的 `AuthGlobalFilter` 需要在网关集成环境或专门测试中补充验收，不能只用 Servlet Filter 单测代表网关链路。

代表性验收：

- PUBLIC 接口无 token 可通过。
- LOGIN 接口无 token 被拒绝，有效 access token 可通过。
- PERMISSION 接口无权限码授权时被拒绝。
- INTERNAL 接口从外部入口访问被拒绝。
- IP 白名单、`auth-enabled=false`、缺权限码强校验、refresh token 拒绝访问、Servlet 与 Gateway 上下文传递差异需要分别覆盖。

## 11. 业务接入最小闭环

单体应用接入 `mango-access-web-starter`，网关接入 `mango-access-gateway-starter`，并确认 token provider、`ApiResourceApi` 和授权快照提供者已装配。业务接口通过 authorization 注解或资源清单声明 PUBLIC、LOGIN、PERMISSION、INTERNAL，access 只消费同步后的资源策略。

验收时至少准备四类接口分别验证匿名放行、登录态要求、权限码要求和外部拒绝。下游只信任入口写入的 request attributes、`MangoContextHolder` 或网关传递的 `X-Mango-*` 头，不信任客户端自带权限码或租户声明。

## 12. 常见问题

- 如果资源策略不符合预期，先检查业务接口是否通过 `mango-authorization-resource-sync-starter` 完成同步。
- 如果下游拿不到用户或租户上下文，检查边界入口是否接入对应 starter，并检查 `X-Mango-*` 头是否被代理层剥离。
- 如果单体模式和网关模式行为不一致，优先对照 `AuthFilter` 和 `AuthGlobalFilter` 的装配路径。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
