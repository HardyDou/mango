# Mango Access

> 外部流量入口、Token 初筛、运行时上下文头注入、网关暴露面同步。

## 模块职责

| 职责 | 说明 |
|------|------|
| 外部入口 | 微服务拓扑下统一承接外部 HTTP 请求 |
| Token 初筛 | 校验 `Authorization` 中的 access token，拒绝无效 token 和 refresh token |
| API 访问策略 | 通过 `ApiResourceApi` 读取已同步的 `@ApiAccess` 资源策略，支持 PUBLIC / LOGIN / PERMISSION / INTERNAL |
| 上下文注入 | 校验通过后向下游注入 `X-Mango-*` 运行时上下文头 |
| 暴露面同步 | 边界入口应用可通过 `mango-authorization-resource-sync-starter` 同步 Spring Cloud Gateway route Path 到 authorization |

认证业务、权限模型、资源存储由 `mango-auth`、`mango-authorization` 承担。边界入口不维护公共路径白名单表。

## 子模块

```text
mango-access/
├── mango-access-core            # 访问决策核心
├── mango-access-web-starter     # 单体模式 Servlet Filter 自动配置
└── mango-access-gateway-starter # 微服务模式 Spring Cloud Gateway 自动配置
```

## 访问策略

| 访问模式 | 边界入口行为 |
|----------|----------|
| PUBLIC | 直接放行 |
| LOGIN | 必须校验 access token，校验通过后转发 |
| PERMISSION | 必须校验 access token 和权限码，校验通过后转发 |
| INTERNAL | 直接返回 403，不允许外部访问 |

访问策略来自 `mango-authorization` 的 `ApiResourceApi`。业务接口通过 `@ApiAccess` 声明策略，并由 `mango-authorization-resource-sync-starter` 同步到 `authorization_api_resource`。

## 上下文头

边界入口校验 token 通过后，会向下游服务注入 Mango 运行时上下文头：

| Header | 来源 |
|--------|------|
| `X-Mango-User-Id` | token userId |
| `X-Mango-Principal-Name` | token username |
| `X-Mango-Tenant-Id` | token `tenantId` claim |
| `X-Mango-Realm` | token `realm` claim |
| `X-Mango-Actor-Type` | token `actorType` claim |
| `X-Mango-Party-Type` | token `partyType` claim |
| `X-Mango-Party-Id` | token `partyId` claim |
| `X-Mango-App-Code` | token `appCode` claim |

下游服务由 `mango-infra-web` 初始化 `MangoContext`，由 `mango-auth-starter` / `mango-authorization-security-starter` 写入 Spring Security 上下文。

## 单体模式

单体模式使用 `mango-access-web-starter`，注册 Servlet `AuthFilter`。

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-access-web-starter</artifactId>
</dependency>
```

单体应用内应存在 `ApiResourceApi` Bean。通常由 `mango-authorization-starter` 的 `ApiResourceController` 提供。

## 微服务模式

微服务网关模式使用 `mango-access-gateway-starter`，注册 Spring Cloud Gateway `AuthGlobalFilter`。

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-access-gateway-starter</artifactId>
</dependency>
```

`mango-access-gateway-starter` 依赖远程安全聚合能力，通过 Feign 读取 API 访问策略和用户权限。该模块不是 remote starter，不暴露 Feign 客户端。

## 配置

```yaml
mango:
  access:
    auth-enabled: true
  security:
    jwt:
      secret: ${JWT_SECRET:mango-secret-key-change-in-production-must-be-at-least-32-chars}
```

`mango-access-gateway-starter` 不注册业务路由。网关路由统一使用 Spring Cloud Gateway 原生配置 `spring.cloud.gateway.routes`，可写在本地 `application.yml`，也可由 Nacos 配置中心下发。

## Gateway 路由资源同步

`mango-authorization-resource-sync-starter` 会在检测到 `RouteDefinitionLocator` 和 `ApiResourceApi` 时同步 `spring.cloud.gateway.routes` 中的 `Path` 路由：

- 每个 Path pattern 注册为一条资源。
- `httpMethod=ALL`。
- `resourceCode=GATEWAY:/path/pattern`。
- 默认 `accessMode=LOGIN`。
- 可通过 route metadata `apiAccessMode` 覆盖为 `PUBLIC / LOGIN / INTERNAL`。

该能力只用于外部入口暴露面治理和审计，不替代业务服务内的 `@ApiAccess` 资源同步。业务接口是否公开、是否登录、是否需要权限，仍以服务内接口同步到 `authorization_api_resource` 的访问策略为准。
