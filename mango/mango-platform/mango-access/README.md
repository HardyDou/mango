# Mango Access

## 1. 概览
`mango-access` 是 Mango 的边界入口访问控制模块。它不做登录，也不维护角色菜单；它在请求进入单体 Web 应用或 Spring Cloud Gateway 时读取 token、查询 `mango-authorization` 的 API 资源策略，并按 PUBLIC、LOGIN、PERMISSION、INTERNAL 四种模式决定是否放行。

代码事实：

- Maven 聚合模块：`io.mango.platform.access:mango-access`。
- 子模块：`mango-access-core`、`mango-access-web-starter`、`mango-access-gateway-starter`。
- 配置类：`AccessProperties`，前缀 `mango.access`。
- 单体入口：`AuthFilter`，写入 request attributes 和 `MangoContextHolder`。
- 网关入口：`AuthGlobalFilter`，写入 `MangoContextHeaders` 对应请求头给下游服务。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 单体应用需要在 Servlet Filter 阶段完成登录态和权限初筛 | Maven 依赖 / HTTP API / Java API |
| 微服务拓扑需要在 Gateway 统一拦截外部 HTTP 请求 | Maven 依赖 / HTTP API / Java API |
| 业务接口已经通过 mango-authorization 注解或资源同步注册了访问策略 | Maven 依赖 / HTTP API / Java API |
| 下游服务需要拿到 userId、memberId、tenantId、realm、actorType、partyType、partyId、appCode 等登录上下文 | Maven 依赖 / HTTP API / Java API |
| 需要在入口层拒绝 INTERNAL 接口从外部访问 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 单体应用需要在 Servlet Filter 阶段完成登录态和权限初筛。
- 微服务拓扑需要在 Gateway 统一拦截外部 HTTP 请求。
- 业务接口已经通过 `mango-authorization` 注解或资源同步注册了访问策略。
- 下游服务需要拿到 userId、memberId、tenantId、realm、actorType、partyType、partyId、appCode 等登录上下文。
- 需要在入口层拒绝 INTERNAL 接口从外部访问。

## 4. 边界说明
- 不签发、刷新或注销 token；这些属于 `mango-auth`。
- 不保存账号、成员或外部身份；这些属于 `mango-identity`。
- 不维护 API 资源、菜单、角色和权限码；这些属于 `mango-authorization`。
- 不替代业务模块自己的数据权限、状态流转和领域校验。

## 5. 模块组成
访问决策由 `AccessService` 完成：

1. `mango.access.auth-enabled=false` 时直接放行并返回 disabled 状态。
2. 命中 `mango.access.ip-whitelist` 时匿名放行。
3. 调用 `ApiResourceApi.resolveAccessDecision` 解析当前 `httpMethod + path` 的资源策略。
4. 未匹配资源时按 LOGIN 处理；如果路径带外部前缀，会按 `external-api-prefixes` 剥离后重试。
5. PUBLIC 匿名放行，INTERNAL 直接拒绝外部入口。
6. LOGIN 和 PERMISSION 要求 Bearer access token 有效，且 token type 必须是 access。
7. PERMISSION 继续调用 `IAuthorizationProvider.load` 加载成员授权快照，并匹配资源上的 `permissionCode`。

`mango-access` 只消费授权事实，不负责把菜单和权限初始化进库。业务模块要先通过 `mango-authorization-resource-sync-starter` 或资源清单完成资源注册。

## 6. 接入方式
单体 Web 应用接入：

```xml
<dependency>
    <groupId>io.mango.platform.access</groupId>
    <artifactId>mango-access-web-starter</artifactId>
</dependency>
```

Spring Cloud Gateway 接入：

```xml
<dependency>
    <groupId>io.mango.platform.access</groupId>
    <artifactId>mango-access-gateway-starter</artifactId>
</dependency>
```

接入应用还必须能装配以下 Bean：

- `ITokenProvider`：校验 access token，并读取 userId、memberId、tenantId 等 claim。
- `ApiResourceApi`：解析当前 HTTP 资源访问模式和权限码。
- `IAuthorizationProvider`：在 PERMISSION 模式下加载成员权限快照。
- 可选 `AccessContextValidator`：增加租户、成员状态、应用上下文等额外登录上下文校验。

## 7. 配置说明
配置前缀：`mango.access`。

| 配置项 | 默认值 | 作用 | 影响 |
|--------|--------|------|------|
| `mango.access.auth-enabled` | `true` | 是否启用边界入口认证 | `false` 时 `AccessService` 直接放行，适合本地问题定位，不适合作为真实验收口径 |
| `mango.access.require-permission-code` | `false` | PERMISSION 接口是否强制声明权限码 | `true` 且资源是 PERMISSION 但 `permissionCode` 为空时返回 403 |
| `mango.access.external-api-prefixes` | `["/api"]` | 外部代理或网关暴露的 API 前缀 | 以 `/api/业务路径` 进入但未命中资源时，会剥离 `/api` 后按应用内路径再匹配一次 |
| `mango.access.ip-whitelist.enabled` | `false` | 是否启用来源 IP 白名单 | 命中白名单时匿名放行，不再走 token 和权限判断 |
| `mango.access.ip-whitelist.rules[].path-pattern` | 无 | 白名单路径，Ant 风格 | 例如 `/actuator/**` |
| `mango.access.ip-whitelist.rules[].methods` | 空列表 | HTTP 方法列表 | 空或包含 `ALL` 表示所有方法 |
| `mango.access.ip-whitelist.rules[].cidrs` | 空列表 | 允许访问的 IP 或 CIDR | 例如 `127.0.0.1/32`、`10.0.0.0/8` |

YAML 示例：

```yaml
mango:
  access:
    auth-enabled: true
    require-permission-code: true
    external-api-prefixes:
      - /api
    ip-whitelist:
      enabled: true
      rules:
        - path-pattern: /actuator/**
          methods: [GET]
          cidrs:
            - 127.0.0.1/32
```

token 读取顺序：

- `Authorization: Bearer <accessToken>`。
- query 参数 `token=<accessToken>`，缺少 Bearer 前缀时入口会补齐。
- Cookie `MANGO_TOKEN`，缺少 Bearer 前缀时入口会补齐。

## 8. API 与扩展
- `AccessService.check(httpMethod, path, authHeader, clientIp)`：核心访问决策入口。
- `AccessContextValidator`：额外上下文校验扩展点，返回不允许时会中断请求。
- `AccessPrincipal`：入口解析出的用户、成员、租户、登录域和应用上下文。
- `AuthFilter`：Servlet 模式过滤器，成功后写入 request attributes：`userId`、`memberId`、`username`、`tenantId`，并更新 `MangoContextHolder`。
- `AuthGlobalFilter`：Gateway 模式全局过滤器，成功后把主体上下文写入下游请求头。

访问模式来自 `mango-authorization`：

| 模式 | 行为 |
|------|------|
| PUBLIC | 匿名放行 |
| LOGIN | 要求合法 access token |
| PERMISSION | 要求合法 access token，并且当前成员授权快照包含资源权限码 |
| INTERNAL | 边界入口返回 403 |

## 9. 数据与初始化
本模块没有独立 Flyway migration。

运行依赖的数据来自 `mango-authorization`：

- `authorization_api_resource` 保存 `http_method`、`path_pattern`、`access_mode`、`permission_code`。
- 角色、菜单、权限、成员角色关系由 authorization 相关表维护。
- 业务接口资源应通过注解扫描、配置资源或 Gateway 路由同步写入 authorization。

## 10. 管理入口
`mango-access` 不提供管理菜单。

权限判断使用 token claim 中的登录上下文构造授权查询：

- `memberId`：权限主体，缺失时 PERMISSION 必然失败。
- `tenantId`：当前机构边界。
- `appCode`：当前应用入口，例如 `internal-admin`。
- `realm`、`actorType`、`partyType`、`partyId`：细分登录域和归属主体。

权限码不是客户端传入的。权限码来自 `authorization_api_resource.permission_code`，再和 `IAuthorizationProvider.load(...).permissionCodes()` 做匹配；`*:*` 可以匹配任意权限码。

## 11. 快速开始
业务模块接入时按这个顺序验证：

1. Controller 用 `@PublicApi`、`@LoginApi`、`@PermissionAccess` 或 `@InternalApi` 声明访问模式。
2. 接入 `mango-authorization-resource-sync-starter`，启动后确认 `authorization_api_resource` 有对应资源。
3. 给角色绑定菜单或权限，确认当前登录成员授权快照包含权限码。
4. 单体应用接入 `mango-access-web-starter`，网关接入 `mango-access-gateway-starter`。
5. 用真实登录 token 访问接口，分别断言 401、403 和通过场景。

## 12. 问题排查
- 接口本该匿名却要登录：检查 `authorization_api_resource` 是否同步成 PUBLIC，以及路径是否被外部前缀影响。
- PERMISSION 接口一直 403：检查资源是否有 `permission_code`，角色是否绑定该权限，token 是否有 `memberId` 和正确 `appCode`。
- 网关下游拿不到租户：检查 Gateway 是否接入 `mango-access-gateway-starter`，以及代理链路是否保留 Mango 上下文请求头。
- 单体模式和网关模式表现不同：分别对照 `AuthFilter` 和 `AuthGlobalFilter`，两者写入上下文的位置不同。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
