# mango-infra-security

> Security 基础设施模块，负责 Spring Security 集成、`@Perm` 方法授权、token 技术契约和当前安全上下文读取。

## 职责边界

- 负责：`@Perm` 注解、Spring Security 方法授权适配、`ITokenService` token 技术接口、`ISecurityContextProvider` 当前安全上下文接口。
- 不负责：RBAC 角色/菜单/组织等业务规则、用户资料模型、平台业务权限存储、登录业务流程。
- 依赖方向：只依赖 `mango-infra-kv-api`、Spring Security 等技术依赖，不依赖平台层模块。

## 模块结构

```text
mango-infra-security/
├── mango-infra-security-api/      # Perm、ITokenService、IPermissionService、ISecurityContextProvider、SecurityContext、SecurityPrincipal
├── mango-infra-security-core/     # JWT token 默认实现、内存权限默认实现、TokenContextHolder
└── mango-infra-security-starter/  # Spring Security 自动配置与 @Perm 授权适配
```

## 对外接口

| 接口/类型 | 说明 |
|-----------|------|
| `@Perm` | 方法级权限码注解 |
| `IPermissionService` | 权限码查询技术接口，由 RBAC 等平台模块提供实现 |
| `ITokenService` | token 生成、校验、刷新和 claim 读取技术接口 |
| `ISecurityContextProvider` | 当前安全上下文 provider |
| `SecurityContext` | 不可变安全上下文快照，包含 `userId`、`tenantId`、`authenticated`、`principalName` |
| `SecurityPrincipal` | Spring Security `Authentication` 中存放的轻量 principal |

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

默认实现 `SpringSecurityContextProvider` 只读取 `SecurityContextHolder`，不解析 JWT、不读取平台业务实体、不访问 RBAC 存储。

## 自动配置

`SecurityAutoConfiguration` 自动注册：

- `ISecurityContextProvider` 默认实现 `SpringSecurityContextProvider`
- `IPermissionService` 默认内存实现 `DefaultPermissionServiceImpl`
- `@Perm` 的 Spring Method Security 拦截器
- 默认 API `AuthenticationEntryPoint` / `AccessDeniedHandler`
- 默认兜底 `SecurityFilterChain`（未接入业务认证时放行 HTTP，供上层 starter 继续扩展）

`TokenAutoConfiguration` 自动注册 `ITokenService` 默认 JWT 实现。

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
- 安全上下文契约冻结为 `userId`、`tenantId`、`authenticated`、`principalName`。
- `mango-auth-starter` 通过 JWT 过滤器把 `SecurityPrincipal` 放入 `SecurityContextHolder`。

## 测试基线

- 单元/切片测试：
  - `PermMethodSecurityTest`，覆盖 `@Perm` 通过 authority 直接授权、`IPermissionService` fallback、未认证和无权限场景。
- 性能基线测试：
  - `JjwtTokenServicePerformanceBaselineTest`，覆盖 JWT 校验与 claim 读取的基础性能阈值。

## 验证命令

```bash
cd mango
mvn -q -DskipTests compile -pl mango-infra/mango-infra-security -am
mvn -q test -pl mango-infra/mango-infra-security/mango-infra-security-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=PermMethodSecurityTest
mvn -q test -pl mango-infra/mango-infra-security/mango-infra-security-core -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=JjwtTokenServicePerformanceBaselineTest
rg -n "<platform package patterns>" mango/mango-infra/mango-infra-security
```
