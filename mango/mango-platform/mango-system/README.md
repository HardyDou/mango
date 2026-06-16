# 系统 System

## 1. 概览
`mango-system` 提供 Mango 基础系统能力：字典、系统配置、租户、行政区划、国际化、登录日志、操作日志、个人配置，以及租户初始化和删除依赖检查扩展点。

主要使用者是平台管理端、业务后台、公共组件和需要读取基础字典或租户信息的业务模块。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 页面下拉框需要统一读取字典选项 | Maven 依赖 / HTTP API / Java API |
| 业务需要通过系统配置维护开关、阈值、文案或安全参数 | Maven 依赖 / HTTP API / Java API |
| 平台需要管理租户、机构类型、机构能力和机构状态 | Maven 依赖 / HTTP API / Java API |
| 登录、操作审计需要落日志并分页查询 | Maven 依赖 / HTTP API / Java API |
| 用户需要保存个人偏好，例如表格列、筛选条件、布局配置 | Maven 依赖 / HTTP API / Java API |
| 新租户创建后，需要编排各模块初始化默认数据 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 页面下拉框需要统一读取字典选项。
- 业务需要通过系统配置维护开关、阈值、文案或安全参数。
- 平台需要管理租户、机构类型、机构能力和机构状态。
- 登录、操作审计需要落日志并分页查询。
- 用户需要保存个人偏好，例如表格列、筛选条件、布局配置。
- 新租户创建后，需要编排各模块初始化默认数据。

## 4. 边界说明
- 不负责认证登录、token 签发和密码校验。
- 不负责菜单、角色、权限授权；这些归 `mango-authorization`。
- 不负责组织岗位；这些归 `mango-org`。
- 不负责事件管理页面；`/system/events` 归 `mango-infra-event`。
- 不适合承载业务主数据，例如客户、合同、订单、商品。

## 5. 模块组成
- `mango-system-api`：字典、配置、租户、日志、个人配置、行政区划、国际化 API 和 tenant 扩展点。
- `mango-system-core`：实体、Mapper、服务、租户过滤、操作日志切面、行政区划、国际化实现。
- `mango-system-starter`：注册 `SystemAutoConfiguration` 和系统管理 Controller。

其他模块通过 `TenantProvisioner` 初始化自己拥有的租户默认数据，通过 `TenantDependencyChecker` 阻止删除仍有业务数据的租户。

## 6. 接入方式
宿主应用引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.system</groupId>
    <artifactId>mango-system-starter</artifactId>
</dependency>
```

业务代码按需注入：

- `DictApi`：读取字典类型、字典数据和选项。
- `SysConfigApi`：读取或维护系统配置。
- `SysTenantApi`：维护租户和登录租户选项。
- `PersonalConfigApi`：读写当前用户个人配置。
- `SysAreaApi`：读取行政区划树和详情。
- `SysI18nApi`：读取国际化文本。
- `SysLoginLogApi`、`SysOperationLogApi`：写入或查询日志。

## 7. 配置说明
当前模块没有专属 `@ConfigurationProperties`。运行配置主要来自数据库表 `sys_config`，默认初始化值包括：

| 配置键 | 默认值 | 含义 |
|--------|--------|------|
| `sys.index.skinName` | `skin-blue` | 默认皮肤名称。 |
| `sys.account.captchaEnabled` | `true` | 账号登录验证码开关。 |
| `sys.account.registerEnabled` | `false` | 注册开关。 |
| `sys.login.lockCount` | `5` | 登录锁定次数。 |

新增系统配置时，业务要明确配置类型、状态、默认值和读取方，不要把临时业务状态写入系统配置。

## 8. API 与扩展
主要 HTTP 根路径：

| 根路径 | 能力 |
|--------|------|
| `/system/dict` | 字典类型、字典数据、字典选项。 |
| `/system/config` | 系统配置列表、详情、新增、修改、删除、改值、配置类型和分组。 |
| `/system/tenant` | 租户列表、登录租户选项、详情、新增、修改、删除、启停。 |
| `/system/log` | 登录日志、操作日志、清理和统计。 |
| `/system/area` | 行政区划树、详情、adcode、子级、启用区划、新增、修改、删除。 |
| `/system/i18n` | 公共国际化文本、语言列表、名称查询和管理查询。 |
| `/system/personal-configs` | 当前用户个人配置查询、取值、保存、删除。 |

扩展点：

- `TenantProvisioner.provision(TenantProvisionContext)`：租户创建成功后，各模块初始化自己拥有的默认数据。
- `TenantDependencyChecker.check(Long tenantId)`：租户删除前，各模块返回是否存在阻断依赖。

## 9. 数据与初始化
Flyway 路径：`mango-system-core/src/main/resources/db/migration/system`。

核心表：

| 表 | 用途 |
|----|------|
| `sys_dict_type` | 字典类型。 |
| `sys_dict_data` | 字典数据。 |
| `sys_config` | 系统配置。 |
| `sys_login_log` | 登录日志。 |
| `sys_operation_log` | 操作日志。 |
| `sys_tenant` | 租户和机构空间。 |
| `sys_route_conf` | 历史路由配置表，`V6` 已退休路由管理。 |
| `sys_area` | 行政区划。 |
| `sys_personal_config` | 用户个人配置。 |

初始化数据：

- 字典类型：用户性别、系统开关、登录类型、授权角色类型、菜单类型、系统参数类型、配置类型、登录状态、操作状态、组织类型、行政区划、认证域、操作者类型、机构类型、机构能力、机构状态等。
- 字典数据：启用/禁用、账号密码/手机号登录、内部/租户/客户/合作方/金融机构认证域、机构能力 `PLATFORM_ADMIN`、`SYSTEM_ADMIN`、`AUTH_ADMIN`、`ORG_ADMIN`、`WORKFLOW` 等。
- 租户：默认租户 `default`、A 公司、B 公司、C 公司。
- 行政区划：基础省市区数据。
- 个人配置：由 `V5__personal_config.sql` 创建表，运行期按用户保存。
- 业务域：`V7`、`V8`、`V9` 把字典和配置接入业务域分类。

## 10. 管理入口
权限码按能力拆分：

- 字典：`system:dict:type:*`、`system:dict:data:*`。
- 配置：`system:config:*`。
- 租户：`system:tenant:*`，其中 `/system/tenant/login-options` 是公共接口。
- 日志：`system:log:login:*`、`system:log:operation:*`。
- 行政区划：查询树、adcode、子级、启用区划登录可访问；新增、修改、删除和详情使用 `system:area:*`。
- 个人配置：登录可访问，只能读写当前用户自己的配置。

系统表里既有平台级数据，也有租户隔离数据。业务读取时必须明确使用当前租户、默认租户还是公共数据，避免把 `default` 租户配置误当成所有租户配置。

## 11. 快速开始
1. 引入 `mango-system-starter`。
2. 通过 migration 或管理页面维护业务需要的字典类型和数据。
3. 业务读取字典选项并在前端展示。
4. 需要运行期开关时写入 `sys_config`，业务按配置键读取。
5. 创建租户后确认相关模块默认数据完成初始化。
6. 如果业务模块有租户内默认数据，实现 `TenantProvisioner`；如果租户删除前要保护数据，实现 `TenantDependencyChecker`。

## 12. 问题排查
- 字典选项为空：检查字典类型、状态、租户上下文和 migration。
- 配置更新后业务不生效：确认业务读取的是数据库配置而不是 YAML 或缓存旧值。
- 新租户没有组织或角色：检查对应模块是否实现并注册 `TenantProvisioner`。
- 不能删除租户：查看各模块 `TenantDependencyChecker` 返回的阻断原因。
- 登录页租户列表为空：检查租户状态和 `/system/tenant/login-options` 是否可访问。

## 13. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
