# Mango Seed

## 1. 概览

`mango-seed` 是 Mango 官方基础数据初始化 starter。它在应用启动时按 `mango.seed` 配置幂等创建基础租户、管理员账号、租户成员、超级管理员角色、租户应用绑定，并把指定菜单包授权给管理员角色。

它适合本地开发、演示环境、首次部署环境和自动化验收环境。生产环境如果启用，必须配置强初始密码，并在首次登录后按安全策略处理管理员账号。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 租户初始化 | 按 `tenant.code` 查找或创建 `sys_tenant` |
| 管理员初始化 | 按 `INTERNAL + username` 查找或创建 `identity_user` |
| 租户成员初始化 | 给管理员创建 `tenant_member` |
| 角色初始化 | 在目标租户创建 `ROLE_ADMIN` |
| 授权初始化 | 给管理员成员绑定超级管理员角色 |
| 前端应用绑定 | 写入 `frontend_tenant_app_binding` |
| 菜单授权 | 从租户 `1` 的菜单包复制菜单授权到管理员角色 |
| 幂等执行 | 重复启动不会重复创建已有租户、账号、成员、角色、绑定或菜单授权 |

## 3. 接入方式

宿主应用引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.seed</groupId>
    <artifactId>mango-seed-starter</artifactId>
</dependency>
```

默认不执行 seed，必须显式开启：

```yaml
mango:
  seed:
    enabled: true
    admin:
      initial-password: "change-me-to-a-strong-password"
```

## 4. 配置说明

配置前缀是 `mango.seed`。

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `enabled` | `false` | 是否启用启动期 seed |
| `profile` | `official` | seed 配置档标识；当前 runner 只保存该值，不按它加载额外脚本 |
| `admin.username` | `admin` | 初始管理员用户名 |
| `admin.initial-password` | 空 | 新建管理员时的明文初始密码，保存前用 `PasswordEncoder` 加密 |
| `admin.nickname` | `Administrator` | 管理员昵称，也作为租户联系人 |
| `admin.email` | `admin@mango.local` | 管理员邮箱 |
| `admin.phone` | 空 | 管理员手机号 |
| `tenant.code` | `default` | 种子租户编码 |
| `tenant.name` | `芒果集团` | 种子租户名称 |
| `tenant.institution-type` | `PLATFORM` | 写入 `sys_tenant.institution_type` |
| `tenant.capability-codes` | `PLATFORM_ADMIN,SYSTEM_ADMIN,AUTH_ADMIN,ORG_ADMIN,WORKFLOW` | 写入 `sys_tenant.capability_codes` |
| `tenant.package-code` | `platform_admin` | 查找菜单包并同步菜单授权 |
| `tenant.app-code` | `internal-admin` | 写入角色和租户应用绑定；为空时使用 `internal-admin` |

新建管理员时必须配置 `admin.initial-password`。`prod` 或 `production` profile 下，不能使用 `admin`、`admin123`、`123456`、`password` 这类弱口令。

完整示例：

```yaml
mango:
  seed:
    enabled: true
    admin:
      username: admin
      initial-password: "change-me-to-a-strong-password"
      nickname: Administrator
      email: admin@mango.local
    tenant:
      code: default
      name: 芒果集团
      institution-type: PLATFORM
      capability-codes:
        - PLATFORM_ADMIN
        - SYSTEM_ADMIN
        - AUTH_ADMIN
        - ORG_ADMIN
        - WORKFLOW
      package-code: platform_admin
      app-code: internal-admin
```

## 5. API 与扩展

`mango-seed` 不提供 HTTP Controller，也没有前端页面。

启动期入口：

| 类 | 作用 |
|----|------|
| `MangoSeedAutoConfiguration` | 注册 `MangoSeedProperties` 和 `MangoSeedRunner` |
| `MangoSeedRunner` | 实现 `ApplicationRunner`，启动后执行 `seed()` |

执行步骤：

1. `ensureTenant`：按 `sys_tenant.tenant_code` 查找或创建租户。
2. `ensureAdminUser`：按 `identity_user.realm + username` 查找或创建管理员。
3. `ensureTenantMember`：创建管理员租户成员。
4. `ensureAdminRole`：创建 `ROLE_ADMIN`。
5. `ensureSubjectRole`：给租户成员绑定管理员角色。
6. `ensureTenantAppBinding`：绑定租户和前端应用。
7. `syncRoleMenusFromPackage`：从租户 `1` 的菜单包复制菜单授权。

## 6. 数据与初始化

本模块不拥有独立业务表。它写入其他模块已有表，因此要求相关 migration 已先执行。

写入表：

| 表 | 写入内容 |
|----|----------|
| `sys_tenant` | 租户名称、编码、机构类型和能力编码 |
| `identity_user` | 管理员账号、密码 hash、昵称、邮箱、手机号 |
| `tenant_member` | 管理员在目标租户下的成员身份 |
| `authorization_role` | `ROLE_ADMIN` 超级管理员角色 |
| `authorization_subject_role` | 管理员成员和角色绑定 |
| `frontend_tenant_app_binding` | 租户和 `internal-admin` 应用绑定 |
| `authorization_role_menu` | 管理员角色菜单授权 |

幂等条件：

| 数据 | 查找条件 |
|------|----------|
| 租户 | `sys_tenant.tenant_code` |
| 用户 | `identity_user.realm + username` |
| 租户成员 | `tenant_member.tenant_id + user_id` |
| 角色 | `authorization_role.tenant_id + app_code + role_code` |
| 应用绑定 | `frontend_tenant_app_binding.tenant_id + app_code` |
| 角色菜单 | `authorization_role_menu.tenant_id + role_id + menu_id` |

## 7. 管理入口

Seed 没有菜单。它会读取租户 `1` 下启用的菜单包：

| 条件 | 来源 |
|------|------|
| `package_code` | `mango.seed.tenant.package-code` |
| `app_code` | `mango.seed.tenant.app-code` |
| `status` | 固定要求启用 |
| `del_flag` | 固定要求未删除 |

找到菜单包后，会把 `authorization_menu_package_item.menu_id` 授权给目标租户的 `ROLE_ADMIN`。如果菜单包不存在，seed 不会报错，但管理员登录后不会自动拥有菜单。

## 8. 快速开始

1. 确认 system、identity、authorization 和 frontend 相关表已由 Flyway 创建。
2. 引入 `mango-seed-starter`。
3. 设置 `mango.seed.enabled=true`。
4. 设置强 `mango.seed.admin.initial-password`。
5. 启动应用，检查 seed 日志和目标表数据。
6. 使用管理员账号登录 `internal-admin`。
7. 首次登录后修改管理员密码，生产环境按安全策略关闭 seed 或保留幂等只读效果。

## 9. 问题排查

| 现象 | 排查点 |
|------|--------|
| seed 没执行 | 检查 `mango.seed.enabled` 是否为 `true`，以及 starter 是否在应用 classpath |
| 启动报初始密码错误 | 新建管理员时必须配置非空密码；生产 profile 下不能是弱口令 |
| 能登录但没有菜单 | 检查租户 `1` 是否存在 `platform_admin + internal-admin` 菜单包，以及包内是否有菜单 |
| 报目标表不存在 | 先执行 system、identity、authorization、frontend 相关 migration |
| 重启后密码没有变化 | 已存在管理员不会被覆盖，改密码走 identity 用户能力 |

## 10. 相关文档

- [Mango System](../mango-system/README.md)
- [Mango Identity](../mango-identity/README.md)
- [Mango Authorization](../mango-authorization/README.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
