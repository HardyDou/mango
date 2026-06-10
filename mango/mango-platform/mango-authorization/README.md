# Mango Authorization

> Authorization 模块 - 统一授权、角色、权限、菜单与 API 访问策略管理

## 模块职责

| 职责 | 说明 |
|------|------|
| 应用入口 | `appCode`、`realm`、`actorType`，用于区分客户门户、金融机构系统、内部后台等入口 |
| 菜单管理 | 按 `appCode` 归属的树形菜单、前端路由配置 |
| 角色管理 | 角色CRUD、角色权限分配 |
| 权限码 | 权限码 `{model}:{module}:{action}` |
| 接口资源 | 扫描各 App 的 Spring MVC 接口，注册 HTTP 方法、路径、访问模式和可选权限码 |
| 授权快照 | 通过 `IAuthorizationProvider` 汇总角色、权限与 Spring Security authorities |
| 授权缓存 | 基于 `mango-infra-kv` 缓存用户授权快照，微服务网关可通过 Redis 只读鉴权 |
| 安全基础 | 提供安全上下文、token 抽象和 Spring Security 自动配置 |
| 访问鉴权 | access/resource-access 直接通过 `IAuthorizationProvider` 读取授权快照，不再保留独立权限 Provider 适配层 |
| 主体角色绑定 | 保存 subject 到 role 的授权关系，并记录 `appCode`、`realm`、`actorType`、`partyType`、`partyId` 上下文，不保存账号资料 |

## 子模块

```
mango-authorization/
├── mango-authorization-api/            # API 定义（接口、查询对象、VO、授权快照）
├── mango-authorization-core/           # 核心业务（Service、Mapper）
├── mango-authorization-support/        # Spring Security 集成、安全上下文、token 抽象
├── mango-authorization-resource-sync-starter/   # 当前 App 接口资源扫描与注册
├── mango-authorization-resource-access-starter/ # 运行时 URL 访问策略适配
├── mango-authorization-starter/        # 本地调用启动器
└── mango-authorization-starter-remote/ # 远程调用启动器（Feign）
```

## 授权 API

| 类型 | 说明 |
|------|------|
| `AuthorizationQuery` | 授权查询入参，当前支持 user subject |
| `AuthorizationSnapshot` | 授权快照，包含 roles、permissions、authorities |
| `AuthorityContributor` | 授权数据贡献者，按 subject 追加授权事实 |
| `AuthorizationApi` | 授权快照 Java 契约，HTTP 映射由 starter / starter-remote 承载 |
| `IAuthorizationProvider` | 统一授权 provider，聚合 contributor 输出快照 |
| `ApiResourceApi` | 接口资源注册契约 |
| `AppApi` / `RoleApi` / `MenuApi` / `PermissionApi` / `ApiResourceApi` | 管理类 Java 契约，统一使用 `Command` / `Query` / `VO` 与 `R<T>` 返回 |

API 模块只保留 Java 契约模型，不暴露 Entity、Mapper、MyBatis 注解、Spring Web 注解或数据库表结构；HTTP 路由必须下沉到 `starter` Controller 或 `starter-remote` FeignClient。

`mango-auth` 登录成功后调用 `IAuthorizationProvider` 获取角色与权限，不再维护 auth 内部权限检查器。账号资料与认证用户事实已抽离到 `mango-identity`。access 和 resource-access 在运行时直接读取 `AuthorizationSnapshot.permissionCodes()` 做权限判断。

## 接口资源同步

`mango-authorization-resource-sync-starter` 在 App 启动时扫描当前 Spring MVC 映射，并调用 `ApiResourceApi` 注册到 `authorization_api_resource`。资源表只保存稳定模块名、HTTP 方法、路径、访问模式和权限码，不保存运行时 `serviceName` 或 `contextPath`。运行时服务定位由 `mango-infra-module` 与 `mango-infra-feign` 负责。

`mango-authorization-resource-access-starter` 提供 `apiResourceAuthorizationManager`，供 `mango-auth-starter` 的 Spring Security filter chain 按数据库策略控制访问。资源扫描和运行时访问控制可以独立依赖、独立开关。

- 接口访问策略统一使用 `@ApiAccess` 声明，也可使用 `@PublicApi`、`@LoginApi`、`@InternalApi`、`@PermissionAccess` 这几个组合注解；资源同步不再解析 `@Perm` 或 `@Inner`。
- `moduleName` 优先通过 `mango-infra-module` 的 `module.properties` 按路径反查，保证同一模块部署在不同服务时入库模块名不漂移。
- 未标注 `@ApiAccess` 的接口默认生成资源码：`HTTP_METHOD:/path/pattern`。
- 未标注 `@ApiAccess` 的接口默认访问模式为 `LOGIN`，可通过 `mango.authorization.resource-sync.default-access-mode` 调整。
- `@ApiAccess(mode = PERMISSION, permission = "xxx")` 或 `@PermissionAccess("xxx")` 会将 `xxx` 作为 `permissionCode` 和显式资源码；`PERMISSION` 未填写 `permission` 时启动同步直接失败。
- `@ApiAccess(mode = PUBLIC / LOGIN / INTERNAL)` 不写权限码，资源码为 `HTTP_METHOD:/path/pattern`。
- `@PublicApi`、`@LoginApi`、`@InternalApi` 分别等同于 `@ApiAccess(mode = PUBLIC / LOGIN / INTERNAL)`。
- `mango.authorization.resource-sync.mode=read` 时只扫描并输出日志，不写入注册接口。
- 本地应用显式依赖 `mango-auth-starter`、`mango-identity-starter`、`mango-authorization-starter`；需要按资源策略控制本地 HTTP 请求时，再依赖 `mango-authorization-resource-access-starter`。
- 远程业务 App 直接依赖 `mango-authorization-resource-sync-starter`，通过 `mango-authorization-starter-remote` 的 Feign 注册到平台 authorization 服务。
- 外部入口统一走 `mango-access` 时，`mango-authorization-resource-sync-starter` 会在网关应用内同步 Spring Cloud Gateway 的 Path 路由，作为网关暴露面清单；默认 `accessMode=LOGIN`，`resourceCode=GATEWAY:/path/pattern`。
- 运行时资源策略由 `mango.authorization.resource-access.enabled` 控制，默认开启。

运行时策略：

- `PUBLIC`：允许匿名访问。
- `LOGIN`：要求已认证。
- `PERMISSION`：要求已认证，并拥有 `permissionCode`。
- `INTERNAL`：当前服务内默认拒绝，后续由 gateway / internal-call 可信规则接管。

## 应用模块资源清单

业务模块可以在 jar 内发布资源清单，由 `mango-authorization-resource-sync-starter` 启动时加载并注册到授权服务。默认扫描路径：

- `META-INF/mango/resource-manifest.json`
- `META-INF/mango/resource-manifests/*.json`

同步配置：

```yaml
mango:
  authorization:
    resource-sync:
      manifest:
        enabled: true
        mode: write # write 注册到授权服务；read 只解析并输出日志
```

模块菜单初始化的长期归口规则见 `mango-pmo/rules/backend/11-module-menu.md`。authorization 负责消费资源清单和维护授权基础资产，功能模块通过资源清单声明自身菜单、页面路由和按钮权限。

资源清单示例：

```json
{
  "appCode": "internal-admin",
  "moduleCode": "contract",
  "moduleName": "合同模块",
  "packageCodes": [
    "internal-admin-default"
  ],
  "roleCodes": [
    "ROLE_ADMIN"
  ],
  "menus": [
    {
      "menuType": 1,
      "menuName": "合同管理",
      "menuCode": "contract",
      "path": "/contract",
      "children": [
        {
          "menuType": 2,
          "menuName": "合同列表",
          "menuCode": "contract:archive:list",
          "path": "/contract/archives",
          "component": "contract/archive/index",
          "permissions": [
            "contract:archive:create"
          ],
          "permissionItems": [
            {
              "permissionCode": "contract:archive:create",
              "permissionName": "新增合同"
            }
          ]
        }
      ]
    }
  ]
}
```

注册规则：

- `appCode + moduleCode + menuCode` 作为菜单幂等更新键。
- `menus` 写入目录或页面菜单。
- `permissionItems` 写入 `menuType=3` 的按钮菜单，运行时权限仍由按钮菜单 `menuCode` 提供。
- `packageCodes` 引用已存在菜单包，注册时把清单菜单幂等加入套餐。
- `roleCodes` 引用已存在角色，注册时把清单菜单幂等授权给角色。
- 不自动删除清单中不存在的历史菜单，避免启动时误删授权资产。

## 核心实体

### App

应用入口实体：
- `appCode` - 应用编码，如 `internal-admin`、`customer-portal`、`financial-console`
- `realm` - 登录域，如 `INTERNAL`、`CUSTOMER`、`FINANCIAL`
- `actorType` - 默认操作者类型，如 `INTERNAL_USER`、`CUSTOMER_USER`、`FINANCIAL_USER`

### Menu

系统菜单实体：
- `menuId` - 菜单ID
- `appCode` - 应用编码
- `parentId` - 父菜单ID（0为根）
- `menuType` - 菜单类型（1-目录，2-菜单，3-按钮）
- `menuName` - 菜单名称
- `menuCode` - 权限标识（如 `system:user:view`）
- `path` - 前端路由路径
- `component` - 前端组件路径
- `icon` - 菜单图标
- `sort` - 排序号
- `status` - 状态（0-禁用，1-启用）
- `visible` - 是否显示
- `keepAlive` - 路由缓存
- `embedded` - 内嵌模式
- `redirect` - 重定向路径
- `permissions` - 权限标识列表
- `meta` - 前端Meta信息
- `createBy` / `updateBy` - 审计字段
- `createTime` / `updateTime` - 时间戳
- `delFlag` - 删除标记

### Role / SubjectRole

角色通过 `appCode + realm + actorType` 归属到具体入口和操作者类型。

主体角色绑定通过以下字段限制授权上下文：

- `appCode` - 当前应用入口
- `realm` - 当前登录域
- `actorType` - 当前操作者类型
- `partyType` / `partyId` - 当前数据归属主体

### 菜单 API

| 接口 | 说明 |
|------|------|
| `GET /authorization/menus` | 获取菜单树 |
| `GET /authorization/menus/tree` | 获取完整菜单树 |

## 菜单类型

| 类型 | 值 | 说明 |
|------|----|------|
| 目录 | 1 | 菜单树中的目录节点，不对应具体页面 |
| 菜单 | 2 | 具体页面菜单 |
| 按钮 | 3 | 页面内操作按钮 |

## 权限码格式

`{model}:{module}:{action}`

示例：
- `system:user:view` - 查看用户
- `system:user:create` - 创建用户
- `system:user:update` - 更新用户
- `system:user:delete` - 删除用户

## 数据库表

| 表名 | 说明 |
|------|------|
| `authorization_app` | 授权应用入口表 |
| `authorization_menu` | 菜单表 |
| `authorization_api_resource` | 接口资源表 |
| `authorization_permission` | 权限定义表 |
| `authorization_role` | 角色表 |
| `authorization_role_permission` | 角色权限关联表 |
| `authorization_subject_permission` | 主体直授权限表 |
| `authorization_role_menu` | 角色菜单关联表 |
| `authorization_subject_role` | 主体角色绑定表 |

数据库初始化脚本统一放在 `mango-authorization-core/src/main/resources/db/migration/authorization/`。旧的模块顶层 `sql/` 目录不再作为迁移入口维护。

## 依赖关系

```
authorization-api
├── mango-common
├── spring-core
└── jakarta.validation-api

authorization-core
├── authorization-api
├── mango-infra-persistence-starter
└── mango-infra-context-core

authorization-starter
├── authorization-core
├── mango-authorization-api
├── mango-infra-web-starter
└── swagger-annotations

authorization-resource-sync-starter
├── authorization-api
└── spring-webmvc

authorization-resource-access-starter
├── authorization-api
└── spring-security-web

authorization-starter-remote
├── authorization-api
└── mango-infra-feign-starter

mango-access
├── mango-access-core
├── mango-access-web-starter
└── mango-access-gateway-starter
```

## 运行时权限判断

运行时权限判断只使用资源表中的 `permissionCode`，不信任请求 query 参数。`mango:check -Drule=permission-param` 会检查 `PERMISSION` 接口必须声明明确权限码。

当前不内置授权快照缓存，避免角色、菜单、主体授权变更后因缓存失效不完整导致撤权延迟。后续如需减少远程授权查询，应使用服务端签发的可信授权凭证或带精确失效事件的缓存方案，不能信任客户端自行传入的权限码。
