# mango-infra-security

> Security 基础设施模块，负责 Spring Security 集成、方法级权限授权、token 技术契约和当前安全上下文读取。

## 职责边界

- 负责：`@Perm` 方法级权限注解、Spring Security 方法授权适配、`ITokenProvider` token 技术接口、`ISecurityContextProvider` 当前安全上下文接口。
- 不负责：RBAC 角色/菜单/组织等业务规则、用户资料模型、平台业务权限存储、登录业务流程。
- 依赖方向：只依赖 `mango-infra-kv-api`、Spring Security 等技术依赖，不依赖平台层模块。

## 模块结构

```text
mango-infra-security/
├── mango-infra-security-api/      # Perm、ITokenProvider、IPermissionProvider、ISecurityContextProvider、SecurityContext、SecurityPrincipal
├── mango-infra-security-core/     # JWT token 默认实现、内存权限默认实现、TokenContextHolder
└── mango-infra-security-starter/  # Spring Security 自动配置与 @Perm 授权适配
```

## 对外接口

| 接口/类型 | 说明 |
|-----------|------|
| `@Perm` | 方法级权限码注解 |
| `IPermissionProvider` | 权限码查询技术接口，由 RBAC 等平台模块提供实现 |
| `ITokenProvider` | token 生成、校验、刷新和 claim 读取技术接口 |
| `ISecurityContextProvider` | 当前安全上下文 provider |
| `SecurityContext` | 不可变安全上下文快照，包含认证主体与身份上下文字段 |
| `SecurityPrincipal` | Spring Security `Authentication` 中存放的轻量 principal |

默认 JWT 实现会为 access token 和 refresh token 写入 `jti`。refresh token 刷新时依赖 `jti` 做黑名单防重放；业务侧登出后的 token 注销由 `mango-auth` 的 `TokenRevocationService` 通过 KV 存储完成。

JWT 密钥必须通过 `mango.security.jwt.secret` 配置提供，兼容读取历史配置 `mango.jwt.secret`。未配置密钥或密钥长度不足 256 bit 时启动失败，不再提供默认硬编码密钥。未装配 `IKvStore` 时 refresh token 重放防护会降级并输出启动告警。

## POM 依赖

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-security-starter</artifactId>
</dependency>
```

子模块依赖规则：

- `api` 不依赖平台业务模块。
- `core` 依赖 `api`、`mango-infra-kv-api` 和 JWT 技术库。
- `starter` 依赖 `api`、`core` 和 Spring Security。

## 安全上下文契约

| 字段 | 来源 | 说明 |
|------|------|------|
| `userId` | `Authentication.principal` 中的 `SecurityPrincipal.userId` | 当前认证主体 ID，不是用户业务模型 |
| `tenantId` | `Authentication.principal` 中的 `SecurityPrincipal.tenantId` | 当前租户标识，infra 不解释租户业务规则 |
| `authenticated` | `Authentication.isAuthenticated()` | 是否已认证 |
| `principalName` | `Authentication.principal` 中的 `SecurityPrincipal.principalName` | 当前主体名称，仅作技术标识 |
| `realm` | `SecurityPrincipal.realm` | 登录域，如 INTERNAL、CUSTOMER、FINANCIAL |
| `actorType` | `SecurityPrincipal.actorType` | 操作者类型，如 INTERNAL_USER、CUSTOMER_USER |
| `partyType` | `SecurityPrincipal.partyType` | 归属主体类型，如 COMPANY、FINANCIAL_INSTITUTION |
| `partyId` | `SecurityPrincipal.partyId` | 归属主体 ID |
| `appCode` | `SecurityPrincipal.appCode` | 当前应用入口编码 |

默认实现 `SpringSecurityContextProvider` 只读取 `SecurityContextHolder`，不解析 JWT、不读取平台业务实体、不访问 RBAC 存储。

## 与 Web / Context / Feign 的关系

| 能力 | 所属模块 | 承载内容 | 与 `SecurityContext` 的关系 |
|------|----------|----------|-----------------------------|
| `SecurityContext` | `mango-infra-security` | 当前认证主体与身份上下文：`userId`、`tenantId`、`realm`、`actorType`、`partyType`、`partyId`、`appCode` 等 | 是认证视角的主体快照 |
| `RequestContextSnapshot` | `mango-infra-web` | 当前 HTTP 请求事实：请求 ID、traceId、clientIp、headers、cookies、request | 不是认证来源，不应替代 `SecurityContext` |
| `MangoContextHolder` | `mango-infra-context` | 当前线程/异步链路中的请求、租户、主体、应用与参与方上下文 | 是运行时传播载体，不负责认证判定 |
| `TokenContextHolder` | `mango-infra-security-core` | 当前线程中的原始 Authorization token | 仅用于 Feign token 透传，不代表已授权 |
| Feign header 拦截器 | `mango-infra-feign` | 出站调用 header 传播 | 消费 MangoContext 与 TokenContext，不直接读取 `SecurityContext` |

关系边界：

- `SecurityContext` 来自 Spring Security 的 `SecurityContextHolder`，由认证过滤器写入 `SecurityPrincipal` 后再读取。
- `realm`、`actorType`、`partyType`、`partyId`、`appCode` 来自 token claim，由认证过滤器写入 `SecurityPrincipal`；infra-security 只透传这些事实，不解释业务含义。
- `RequestContextSnapshot` 来自当前 Servlet 请求，可读取 header 和 cookie，但不能把 header 当成已认证身份。
- `MangoContextHolder` 是跨线程和跨服务传播用的统一上下文 holder，应由 Web 过滤器、认证过滤器或任务入口明确写入并在执行结束清理。
- `FeignTokenFilter` 从入站 `Authorization` header 中提取 token，写入 `TokenContextHolder`；`FeignRequestInterceptor` 再把它写入出站 `Authorization` header。
- `FeignRequestInterceptor` 从 `MangoContextHolder` 写出 `X-Mango-*` 上下文 header。
- `InternalCallFeignInterceptor` 负责生成 `X-Internal-*` 内部调用签名 header，由 `mango-infra-web` 的内部调用过滤器校验，和用户认证 token 是两条独立链路。

注意事项：

- 认证过滤器会把 token 中的主体事实补齐到 `MangoContextHolder`，Feign 透传时以该快照为准。
- `TokenContextHolder` 当前使用普通 `ThreadLocal`，只保证同线程请求链路中的 token 透传；`MangoContextHolder` 使用 `TransmittableThreadLocal`，可跨受支持的线程池边界传播。

## 自动配置

`SecurityAutoConfiguration` 自动注册：

- `ISecurityContextProvider` 默认实现 `SpringSecurityContextProvider`
- `IPermissionProvider` 默认内存实现 `DefaultPermissionServiceImpl`
- `@Perm` 的 Spring Method Security 拦截器
- 默认 API `AuthenticationEntryPoint` / `AccessDeniedHandler`
- 可选兜底 `SecurityFilterChain`。只有显式配置 `mango.security.default-permit-all-filter-chain.enabled=true` 时才注册放行链路，避免未接入认证 starter 时默认放行 HTTP。

`TokenAutoConfiguration` 自动注册 `ITokenProvider` 默认 JWT 实现。

## 使用示例

```java
@Perm("system:config:view")
public R<Void> viewConfig() {
    return R.ok();
}
```

```java
@Autowired
private ISecurityContextProvider securityContextProvider;

public Long currentUserId() {
    return securityContextProvider.currentContext().userId();
}
```

## 禁止事项

- 禁止依赖 `rbac-api`、`auth-core`、`system-core` 等平台模块。
- 禁止在 `infra-security` 中实现 RBAC 角色、菜单、组织等业务规则。
- 禁止继续维护自定义权限切面去绕开 Spring Security。
- 禁止把平台业务实体作为安全上下文类型。

## 当前基线

- `mango-infra-security` 未发现平台包依赖。
- `@Perm` 已切到 Spring Security method security 机制。
- 安全上下文契约包含 `userId`、`tenantId`、`authenticated`、`principalName`、`realm`、`actorType`、`partyType`、`partyId`、`appCode`。
- `mango-auth-starter` 通过 JWT 过滤器把 `SecurityPrincipal` 放入 `SecurityContextHolder`。
- JWT access token / refresh token 均包含 `jti`，refresh token 黑名单校验使用标准 JWT ID。
- JWT 密钥缺失时启动失败；默认放行的 `SecurityFilterChain` 需要显式开启。

## 测试基线

- 单元/切片测试覆盖 token 生成、刷新、claim 读取、黑名单与安全过滤链。
- 性能基线测试：
  - `JjwtTokenServicePerformanceBaselineTest`，覆盖 JWT 校验与 claim 读取的基础性能阈值。

## 验证命令

```bash
cd mango
mvn -q -DskipTests compile -pl mango-infra/mango-infra-security -am
mvn -q test -pl mango-platform/mango-auth/mango-auth-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AuthSecurityConfigTest,AuthSecurityIntegrationTest
mvn -q test -pl mango-infra/mango-infra-security/mango-infra-security-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=JjwtTokenServicePerformanceBaselineTest
rg -n "<platform package patterns>" mango/mango-infra/mango-infra-security
```
