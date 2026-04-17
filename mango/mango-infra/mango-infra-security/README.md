# mango-infra-security

## 职责

安全基础设施模块，提供 `@Perm` 注解权限检查的 AOP 切面和 `IPermissionService` 接口，用于方法级别的权限校验。

## 技术实现

- 核心框架：Spring Boot 3.x + Spring AOP
- 数据存储：取决于 `IPermissionService` 实现（内存 / 数据库 / Redis）
- 通信方式：`@Autowired` 依赖注入

## 模块结构（4 层）

```
mango-infra-security/
├── mango-infra-security-api/              ← IPermissionService 接口定义
├── mango-infra-security-core/             ← DefaultPermissionServiceImpl（内存默认实现）
├── mango-infra-security-starter/          ← PermAspect + SecurityAutoConfiguration
└── mango-infra-security-starter-remote/   ← 预留（微服务时跨进程调用）
```

## 核心接口

### IPermissionService

| 方法 | 说明 |
|------|------|
| `listUserPermissions(userId)` | 返回用户拥有的权限码列表 |

## 依赖关系

```
本模块依赖：
├── mango-common                    ← @Perm 注解、BizException
├── spring-boot-starter-aop        ← @Aspect
└── spring-boot-starter-web        ← HttpServletRequest

本模块被依赖：
├── mango-*-starter               ← 业务模块的本地 starter
└── mango-rbac-starter            ← 提供 IPermissionService 生产实现
```

## IPermissionService 实现优先级

| 优先级 | 实现 | 来源 | 条件 |
|--------|------|------|------|
| 1 | `mango-rbac-starter` | `SysUserService` | `ISysUserService` 存在于 classpath |
| 2 | `DefaultPermissionServiceImpl` | 内存存储 | `@ConditionalOnMissingBean` 回退 |

> 业务系统应使用 `mango-rbac-starter` 的实现，`-core` 的默认实现仅用于测试或无 RBAC 模块的简单场景。

## 使用方式

### Backend

```java
// 1. 在方法上使用 @Perm 注解
@Perm("user:user:add")
@PostMapping
public R<Void> addUser(@RequestBody SysUserPo po) {
    // ...
}

// 2. IPermissionService 由 Spring 自动注入
@Autowired
private IPermissionService permissionService;

List<String> perms = permissionService.listUserPermissions(userId);
```

### 前端

```bash
# 带有 @Perm 注解的接口会自动权限校验
POST /api/user
```

## 配置项

本模块零配置。`IPermissionService` 的实现由 Spring Bean 注入决定。

如需自定义 `IPermissionService` 实现：

```java
@Configuration
public class MySecurityConfig {
    @Bean
    public IPermissionService customPermissionService() {
        return new CustomPermissionServiceImpl();
    }
}
```

## 约束（强制）

- ✅ `@Perm` 注解放在 `mango-infra-security-api`
- ✅ `IPermissionService` 接口放在 `mango-infra-security-api`（Layer 1）
- ✅ `PermAspect` 必须放在 `mango-infra-security-starter`（依赖 Spring AOP）
- ✅ `IPermissionService` 禁止直接实现，应通过 `@Bean` 注入
- ✅ 权限码格式：`{model}:{module}:{action}`（参见 `security-rules.md`）
