# Mango Access

## 1. 概览

`mango-access` 是 Mango 的边界入口访问控制模块。它放在 HTTP 请求进入应用的最前面，负责读取 access token、查询 `mango-authorization` 中登记的 API 资源访问策略，并决定本次请求匿名放行、登录放行、权限放行、返回 401 或返回 403。

这个模块不负责登录发 token，也不维护菜单、角色和权限数据。登录由 `mango-auth` 提供，资源、菜单、角色和权限由 `mango-authorization` 提供。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 单体入口鉴权 | `mango-access-web-starter` 注册 Servlet `AuthFilter`，适合单体 Web 应用 |
| 网关入口鉴权 | `mango-access-gateway-starter` 注册 Spring Cloud Gateway `AuthGlobalFilter`，适合微服务网关 |
| API 访问模式判断 | 调用 `ApiResourceApi.resolveAccessDecision` 按 HTTP method 和 path 查询 PUBLIC、LOGIN、PERMISSION、INTERNAL |
| token 校验 | 通过 `ITokenProvider` 校验 Bearer access token，并读取 userId、memberId、tenantId、realm、actorType、partyType、partyId、appCode |
| 权限码校验 | PERMISSION 资源会调用 `IAuthorizationProvider.load`，判断成员授权快照是否包含资源权限码 |
| 上下文透传 | 单体写入 `MangoContextHolder`，网关写入 Mango 上下文请求头 |
| IP 白名单 | 可按路径、HTTP 方法和 IP/CIDR 配置匿名放行 |
| 登录上下文扩展校验 | 可提供 `AccessContextValidator` Bean，对成员状态、租户状态或应用上下文做额外校验 |

## 3. 后端接入

业务代码如果只需要声明接口访问模式，通常依赖 `mango-authorization-api` 中的注解，不需要直接依赖 `mango-access`。部署入口应用时才接入 access starter。

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

接入应用必须能装配这些 Bean：

| Bean | 来源 | 用途 |
|------|------|------|
| `ITokenProvider` | `mango-authorization-support` 或业务自定义实现 | 校验 access token，读取 token claim |
| `ApiResourceApi` | `mango-authorization-starter` 或 `mango-authorization-starter-remote` | 查询当前接口访问模式和权限码 |
| `IAuthorizationProvider` | `mango-authorization-starter` 或业务授权实现 | 加载当前成员授权快照 |
| `AccessContextValidator` | 可选，业务自定义 Bean | 对登录上下文做额外运行时校验 |

## 4. 前端调用方式

`mango-access` 没有前端包和管理页面。前端只需要按登录模块约定携带 token：

| 携带位置 | 格式 |
|----------|------|
| HTTP Header | `Authorization: Bearer <accessToken>` |
| Query 参数 | `token=<accessToken>` 或 `token=Bearer <accessToken>` |
| Cookie | `MANGO_TOKEN=<accessToken>` 或 `MANGO_TOKEN=Bearer <accessToken>` |

入口会优先读取 `Authorization`，其次读取 query 参数 `token`，最后读取 Cookie `MANGO_TOKEN`。query 和 Cookie 里缺少 `Bearer ` 前缀时，入口会自动补齐。

## 5. 快速开始

1. 在业务 Controller 上使用 `mango-authorization-api` 提供的 `@PublicApi`、`@LoginApi`、`@PermissionAccess` 或 `@InternalApi` 声明访问模式。
2. 接入 `mango-authorization-resource-sync-starter`，让接口资源同步到 `authorization_api_resource`。
3. PERMISSION 接口需要把资源权限码授权给角色，并让当前登录成员拥有该角色。
4. 单体部署接入 `mango-access-web-starter`，微服务网关部署接入 `mango-access-gateway-starter`。
5. 使用真实 access token 请求接口，按接口类型确认匿名、401、403 和放行结果。

## 6. 配置说明

配置前缀是 `mango.access`。

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

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.access.auth-enabled` | `true` | 是否启用入口鉴权。设为 `false` 时 `AccessService` 直接放行 |
| `mango.access.require-permission-code` | `false` | PERMISSION 接口是否必须登记权限码。设为 `true` 后，资源没有 `permissionCode` 会返回 403 |
| `mango.access.external-api-prefixes` | `["/api"]` | 外部代理或网关暴露的 API 前缀。资源匹配失败时会剥离这些前缀后重试 |
| `mango.access.ip-whitelist.enabled` | `false` | 是否启用 IP 白名单 |
| `mango.access.ip-whitelist.rules[].path-pattern` | 无 | 白名单路径，支持 Ant 风格通配符 |
| `mango.access.ip-whitelist.rules[].methods` | 空列表 | HTTP 方法。为空或包含 `ALL` 表示匹配全部方法 |
| `mango.access.ip-whitelist.rules[].cidrs` | 空列表 | 允许访问的 IP 或 CIDR |

## 8. 返回字段

入口拦截失败时直接返回 JSON：

| 场景 | HTTP 状态 | 返回示例 |
|------|-----------|----------|
| 缺少 token、token 无效、token 不是 access token、上下文校验失败 | 401 | `{"code":401,"message":"Token 无效或已过期"}` |
| INTERNAL 接口从外部入口访问、权限不足、资源缺少权限码 | 403 | `{"code":403,"message":"权限不足"}` |

放行后不会改写业务接口返回值。

## 9. 运行上下文

单体模式由 `AuthFilter` 写入：

| 位置 | 字段 |
|------|------|
| request attribute | `userId`、`memberId`、`username`、`tenantId` |
| `MangoContextHolder` | userId、memberId、tenantId、username、realm、actorType、partyType、partyId、appCode |

网关模式由 `AuthGlobalFilter` 写入下游请求头，字段来自 `MangoContextHeaders`：

| 上下文 | 来源 token claim |
|--------|------------------|
| 用户 | `userId`、`username` |
| 成员 | `memberId` |
| 租户 | `tenantId` |
| 登录域 | `realm` |
| 主体类型 | `actorType`、`partyType`、`partyId` |
| 应用 | `appCode` |

## 10. 管理入口

`mango-access` 不提供管理菜单，也没有前端管理页面。它消费 `mango-authorization` 中已经登记好的 API 资源、菜单和角色授权。

入口权限是否生效，主要到这些位置确认：

| 目标 | 确认位置 |
|------|----------|
| API 资源访问模式 | `authorization_api_resource` 或 `/authorization/api-resources/access-decision` |
| 菜单和按钮权限 | `mango-authorization` 菜单、角色授权接口和 RBAC 管理页 |
| 登录 token claim | `/auth/info` 返回值和 access 入口解析出的上下文 |

## 11. 数据与初始化

`mango-access` 没有独立 migration，也不会初始化菜单和权限。

运行所需数据来自 `mango-authorization`：

| 数据 | 表或入口 | 作用 |
|------|----------|------|
| API 资源 | `authorization_api_resource` | 保存 method、path、access mode 和 permission code |
| 菜单和按钮权限 | authorization migration 或资源清单 | 给管理端和角色授权使用 |
| 角色授权 | authorization 角色、菜单、主体绑定接口 | PERMISSION 模式下生成成员授权快照 |

接口资源同步通常由 `mango-authorization-resource-sync-starter` 完成。菜单、角色和默认授权由对应业务模块的 authorization migration 或初始化逻辑负责。

## 12. 问题排查

| 现象 | 排查点 |
|------|--------|
| 匿名接口返回 401 | 查 `authorization_api_resource` 中对应 path/method 是否登记为 PUBLIC；如果外部路径带 `/api`，确认 `mango.access.external-api-prefixes` 是否包含该前缀 |
| PERMISSION 接口返回 403 | 查资源是否有 `permission_code`，当前成员是否绑定包含该权限的角色，token 是否带 `memberId` 和正确 `appCode` |
| INTERNAL 接口无法访问 | 这是入口默认行为。INTERNAL 资源不允许从 access 外部入口访问 |
| 网关后面的服务拿不到租户 | 查 Gateway 是否接入 `mango-access-gateway-starter`，下游是否读取 Mango 上下文请求头 |
| 单体服务拿不到上下文 | 查应用是否接入 `mango-access-web-starter`，请求是否已经通过 `AuthFilter` |
| 白名单没有生效 | 查 `path-pattern`、HTTP method、客户端 IP 和 CIDR 是否匹配 |

## 13. 相关文档

- [Mango Authorization](../mango-authorization/README.md)
- [Mango Auth](../mango-auth/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
