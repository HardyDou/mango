# Mango Security

> Security 聚合入口。它不定义新的安全业务规则，只把已拆分的安全相关 starter 收敛成更简单的使用入口。

## 定位

| 模块 | 职责 |
|------|------|
| `mango-infra-security` | Spring Security 集成、`@Perm`、安全上下文、token 抽象 |
| `mango-auth` | 登录、登出、刷新 token、认证流程 |
| `mango-identity` | 账号、身份资料、认证用户事实 |
| `mango-authorization` | 角色、权限、菜单、主体授权绑定 |
| `mango-security` | starter 聚合入口 |

## 子模块

```text
mango-security/
├── mango-security-starter
└── mango-security-starter-remote
```

## 本地单体使用

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-security-starter</artifactId>
</dependency>
```

等价于聚合：

```text
mango-infra-security-starter
mango-auth-starter
mango-identity-starter
mango-authorization-starter
mango-authorization-resource-sync-starter
```

本地聚合会启用接口资源扫描，把当前应用内的 Spring MVC 接口注册到 `authorization_api_resource`，并提供 URL 级授权策略给 `mango-auth-starter`。

## 微服务调用方使用

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-security-starter-remote</artifactId>
</dependency>
```

等价于聚合：

```text
mango-infra-security-starter
mango-auth-starter-remote
mango-identity-starter-remote
mango-authorization-starter-remote
```

远程聚合 starter 负责把 `mango-authorization-starter-remote` 提供的 `IAuthorizationProvider`
适配成 `mango-infra-security` 所需的 `IPermissionProvider`，业务调用方不需要单独装配权限适配器。

微服务业务 App 仍需要直接依赖 `mango-authorization-resource-sync-starter`，用于扫描本服务接口并通过远程 authorization 服务注册资源。远程聚合 starter 不内置该 starter，避免在纯调用方场景产生错误扫描。

## 约束

- 不在 `mango-security` 中新增业务 service、controller、entity。
- 不让 infra 反向依赖 platform。
- 业务应用优先依赖聚合 starter，领域模块内部仍保持清晰边界。
