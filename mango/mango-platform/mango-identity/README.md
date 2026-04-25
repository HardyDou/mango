# Mango Identity

> Identity 模块 - 账号、身份资料与认证用户事实。

## 模块职责

| 职责 | 说明 |
|------|------|
| 账号资料 | `IdentityUser`、用户名、昵称、邮箱、手机号、头像、状态 |
| 认证用户事实 | `IAuthUserProvider` 为 `mango-auth` 提供密码哈希、状态等认证所需字段 |
| 身份 API | `IdentityUserApi` 提供身份资料查询 |
| 本地/远程装配 | `mango-identity-starter` 本地实现，`mango-identity-starter-remote` Feign 远程实现 |

## 边界

- identity 不计算角色、权限、菜单。
- identity 不依赖 authorization。
- auth 依赖 `mango-identity-api` 获取认证用户事实。
- authorization 只保存主体到角色的授权绑定，不保存账号资料。

## 子模块

```text
mango-identity/
├── mango-identity-api
├── mango-identity-core
├── mango-identity-starter
└── mango-identity-starter-remote
```

## 本地开发数据库

本地开发和测试使用 H2 内存数据库即可，不要求外部 MySQL。

当前 `IdentityUser` 仍映射既有 `sys_user` 表；表名物理迁移属于后续数据库 Phase，避免本轮扩大到未验证的数据迁移。

## 使用

本地单体：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-identity-starter</artifactId>
</dependency>
```

远程服务：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-identity-starter-remote</artifactId>
</dependency>
```
