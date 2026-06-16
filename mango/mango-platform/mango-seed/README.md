# 初始化种子 Seed

## 1. 概览
`mango-seed` 提供 Mango 官方基础数据初始化入口。它在应用启动时按配置创建基础租户、管理员账号、租户成员、超级管理员角色、租户应用绑定，并把菜单包授权给管理员角色。

主要使用者是本地开发、演示环境、首次部署环境和自动化验收环境。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 新环境需要自动生成可登录的管理员账号和默认租户 | Maven 依赖 / HTTP API / Java API |
| Pages 或业务开发环境需要一键启动后能进入 internal-admin | Maven 依赖 / HTTP API / Java API |
| 测试环境需要幂等准备基础角色、菜单授权和应用绑定 | Maven 依赖 / HTTP API / Java API |
| 官方 starter 需要统一生成 Mango 平台初始化数据 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 新环境需要自动生成可登录的管理员账号和默认租户。
- Pages 或业务开发环境需要一键启动后能进入 `internal-admin`。
- 测试环境需要幂等准备基础角色、菜单授权和应用绑定。
- 官方 starter 需要统一生成 Mango 平台初始化数据。

## 4. 边界说明
- 不替代 Flyway migration；目标表必须先由各模块迁移创建。
- 不负责业务模块自定义初始化数据，业务数据应放在对应模块 migration 或专门 initializer。
- 不适合在生产环境用弱密码自动创建管理员。
- 不负责同步运行期业务数据。

## 5. 模块组成
- `mango-seed-starter`：`MangoSeedAutoConfiguration`、`MangoSeedProperties`、`MangoSeedRunner`。
- `MangoSeedRunner`：实现 `ApplicationRunner`，启动时执行幂等初始化。

依赖目标模块表：`sys_tenant`、`identity_user`、`tenant_member`、`authorization_role`、`authorization_subject_role`、`frontend_tenant_app_binding`、`authorization_menu_package`、`authorization_menu_package_item`、`authorization_role_menu`。

## 6. 接入方式
宿主应用引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.seed</groupId>
    <artifactId>mango-seed-starter</artifactId>
</dependency>
```

默认不执行 seed，必须显式开启并配置初始密码：

```yaml
mango:
  seed:
    enabled: true
    admin:
      initial-password: "change-me-to-a-strong-password"
```

## 7. 配置说明
配置前缀：`mango.seed`。

| 配置项 | 类型 | 默认值 | 含义 |
|--------|------|--------|------|
| `enabled` | boolean | `false` | 是否启用启动期 seed。 |
| `profile` | string | `official` | seed 配置档标识，当前 Runner 只读取配置值，不按该字段分支加载额外脚本。 |
| `admin.username` | string | `admin` | 初始管理员用户名，写入 `identity_user.username`。 |
| `admin.initial-password` | string | 空 | 新建管理员时的明文初始密码，写入前用 `PasswordEncoder` 加密。新建账号时必须配置。 |
| `admin.nickname` | string | `Administrator` | 管理员昵称，也用于租户联系人。 |
| `admin.email` | string | `admin@mango.local` | 管理员邮箱。 |
| `admin.phone` | string | 空 | 管理员手机号。 |
| `tenant.code` | string | `default` | 种子租户编码。 |
| `tenant.name` | string | `芒果集团` | 种子租户名称。 |
| `tenant.institution-type` | string | `PLATFORM` | 机构类型，写入 `sys_tenant.institution_type`。 |
| `tenant.capability-codes` | set | `PLATFORM_ADMIN,SYSTEM_ADMIN,AUTH_ADMIN,ORG_ADMIN,WORKFLOW` | 租户开通能力编码。 |
| `tenant.package-code` | string | `platform_admin` | 用于查找 `authorization_menu_package` 并同步菜单到管理员角色。 |
| `tenant.app-code` | string | `internal-admin` | 应用编码，写入角色和租户应用绑定。 |

生产或 `prod` profile 下，新建管理员时必须配置非弱口令。弱口令包括 `admin`、`admin123`、`123456`、`password`。

## 8. API 与扩展
本模块不提供 HTTP Controller。

启动期入口：

- `MangoSeedAutoConfiguration`：注册配置属性和 Runner。
- `MangoSeedRunner`：启动后执行 `seed()`。

初始化步骤：

1. `ensureTenant`：按 `tenant.code` 查找或创建 `sys_tenant`。
2. `ensureAdminUser`：按 `realm = INTERNAL` 和用户名查找或创建 `identity_user`。
3. `ensureTenantMember`：创建管理员租户成员。
4. `ensureAdminRole`：创建 `ROLE_ADMIN`。
5. `ensureSubjectRole`：给租户成员绑定管理员角色。
6. `ensureTenantAppBinding`：绑定租户和应用。
7. `syncRoleMenusFromPackage`：从租户 `1` 的菜单包复制菜单授权到管理员角色。

## 9. 数据与初始化
本模块不拥有独立业务表。它写入其他模块已有表，因此要求 system、identity、authorization、frontend 等 migration 已先执行。

幂等条件：

- 租户按 `sys_tenant.tenant_code` 查询。
- 用户按 `identity_user.realm + username` 查询。
- 租户成员按 `tenant_member.tenant_id + user_id` 查询。
- 角色按 `authorization_role.tenant_id + app_code + role_code` 查询。
- 角色菜单按 `authorization_role_menu.tenant_id + role_id + menu_id` 查询。

重复启动不会重复创建租户、账号、角色、应用绑定或菜单授权。

## 10. 管理入口
`MangoSeedRunner` 会查找租户 `1` 下启用的菜单包：

- `package_code = mango.seed.tenant.package-code`
- `app_code = mango.seed.tenant.app-code`

找到后，把 `authorization_menu_package_item` 中的菜单授权给新建或已存在的 `ROLE_ADMIN`。如果菜单包不存在，seed 不会报错，但管理员角色不会自动拥有菜单。

租户应用绑定写入 `frontend_tenant_app_binding`，用于前端应用入口识别该租户是否开通 `internal-admin`。

## 11. 快速开始
1. 在部署环境打开 `mango.seed.enabled`。
2. 配置强 `admin.initial-password`。
3. 确认目标模块 migration 先执行完成。
4. 启动应用并检查 seed 日志和目标表。
5. 使用管理员账号登录前端，验证菜单和权限可用。
6. 首次登录后按安全策略修改管理员密码或关闭 seed。

## 12. 问题排查
- seed 未执行：检查 `mango.seed.enabled` 是否为 `true`。
- 启动报初始密码错误：新建管理员时必须配置非弱口令，生产 profile 下更严格。
- 能登录但无菜单：检查 `authorization_menu_package` 是否存在 `platform_admin` 和 `internal-admin`，以及包内是否有菜单。
- 报目标表不存在：Flyway migration 顺序不对，先执行 system、identity、authorization 和 frontend 相关模块。
- 重复启动没有更新密码：已有管理员账号不会被覆盖，改密码走身份模块能力。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
