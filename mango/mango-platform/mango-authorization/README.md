# Mango Authorization

## 1. 概览
`mango-authorization` 是 Mango 的授权事实中心，负责应用入口、登录上下文、API 资源访问策略、角色、菜单、按钮权限、菜单套餐、租户应用绑定、前端运行时描述和授权快照聚合。

业务开发者最常用它做三件事：

- 在接口上声明 PUBLIC、LOGIN、PERMISSION、INTERNAL，并把资源同步进库。
- 用资源清单把模块、菜单、按钮权限、页面 component key 注册进库。
- 给角色、菜单和租户应用建立绑定，让登录后能返回正确菜单和权限。

代码事实：

- Maven 聚合模块：`io.mango.platform.authorization:mango-authorization`。
- 子模块：`mango-authorization-api`、`mango-authorization-core`、`mango-authorization-support`、`mango-authorization-starter`、`mango-authorization-resource-sync-starter`、`mango-authorization-resource-access-starter`、`mango-authorization-starter-remote`。
- 本地 Controller 路径：`/authorization`、`/authorization/apps`、`/authorization/app-modules`、`/authorization/roles`、`/authorization/menus`、`/authorization/menu-packages`、`/authorization/tenant-app-bindings`。
- Remote starter Feign 服务名：`mango-authorization`。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 管理后台应用入口和登录上下文 | Maven 依赖 / HTTP API / Java API |
| 管理角色、角色成员、角色菜单授权 | Maven 依赖 / HTTP API / Java API |
| 管理菜单树、按钮权限和菜单套餐 | Maven 依赖 / HTTP API / Java API |
| 同步 Spring MVC Controller 暴露的 API 资源 | Maven 依赖 / HTTP API / Java API |
| 同步 Spring Cloud Gateway 路由暴露面 | Maven 依赖 / HTTP API / Java API |
| 注册业务模块资源清单，把菜单、按钮权限和前端运行时配置入库 | Maven 依赖 / HTTP API / Java API |
| 给 mango-auth 返回登录后的 roles 和 permissions | Maven 依赖 / HTTP API / Java API |
| 给 mango-access 返回 API 资源访问决策 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 管理后台应用入口和登录上下文。
- 管理角色、角色成员、角色菜单授权。
- 管理菜单树、按钮权限和菜单套餐。
- 同步 Spring MVC Controller 暴露的 API 资源。
- 同步 Spring Cloud Gateway 路由暴露面。
- 注册业务模块资源清单，把菜单、按钮权限和前端运行时配置入库。
- 给 `mango-auth` 返回登录后的 roles 和 permissions。
- 给 `mango-access` 返回 API 资源访问决策。
- 给前端返回当前用户可见菜单和应用运行描述。

## 4. 边界说明
- 不保存账号、密码、成员组织关系和外部身份绑定；这些属于 `mango-identity`。
- 不签发 token、不刷新 token、不注销 token；这些属于 `mango-auth`。
- 不在边界入口拦截请求；这些属于 `mango-access`。
- 不替代业务模块内部的数据权限和领域规则。

## 5. 模块组成
authorization 保存和聚合授权事实，消费方如下：

- `mango-auth` 登录成功后调用 `IAuthorizationProvider.load`，把角色码和权限码写入 `LoginVO`。
- `mango-access` 调用 `ApiResourceApi.resolveAccessDecision`，按 API 资源策略决定是否需要登录或权限码。
- 前端菜单运行时调用菜单和应用运行接口，获取当前用户可访问菜单。
- 业务模块通过注解、配置资源或 manifest 把自己的资源声明给 authorization。

授权快照由 `DefaultAuthorizationService` 聚合所有 `AuthorityContributor` 输出，默认角色权限贡献器会把成员角色和菜单权限聚合为 `AuthorizationSnapshot`。

## 6. 接入方式
本地授权服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-starter</artifactId>
</dependency>
```

业务模块同步 API 和资源清单：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-resource-sync-starter</artifactId>
</dependency>
```

Spring Security 运行时 URL 策略接入：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-resource-access-starter</artifactId>
</dependency>
```

微服务远程调用接入：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-starter-remote</artifactId>
</dependency>
```

只使用契约对象、注解和 Java API：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-api</artifactId>
</dependency>
```

## 7. 配置说明
### 6.1 API 资源同步

配置前缀：`mango.authorization.resource-sync`。

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.authorization.resource-sync.enabled` | `true` | 是否启用 MVC / Gateway 资源同步自动配置 |
| `mango.authorization.resource-sync.module-name` | 空，兜底 `unknown-module` | 扫描资源无法通过 `ModuleInfoRegistry` 解析模块时使用 |
| `mango.authorization.resource-sync.mode` | `write` | `write` 写入授权服务；`read` 只扫描和输出日志 |
| `mango.authorization.resource-sync.include-packages` | `io.mango` | 只扫描类名以这些包前缀开头的 Controller |
| `mango.authorization.resource-sync.exclude-paths` | `/error,/actuator/**` | 排除路径，支持精确值和 `/**` 前缀 |
| `mango.authorization.resource-sync.default-access-mode` | `LOGIN` | Controller 未声明注解时使用的访问模式 |
| `mango.authorization.resource-sync.resources[]` | 空列表 | 用配置补充非 Controller 资源 |

`resources[]` 字段：

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `module-name` | 同 `module-name` 兜底逻辑 | 稳定模块名 |
| `http-method` | `ALL` | HTTP 方法，如 GET、POST、ALL |
| `path-pattern` | 无，必填 | 路径模式，支持 `/**` 和 Spring 风格路径变量 |
| `resource-code` | `METHOD:path` 或权限码 | 稳定资源编码 |
| `permission-code` | 空 | PERMISSION 访问模式需要的权限码 |
| `access-mode` | `default-access-mode` | PUBLIC、LOGIN、PERMISSION、INTERNAL |
| `description` | `Configured API resource` | 描述 |

示例：

```yaml
mango:
  authorization:
    resource-sync:
      enabled: true
      mode: write
      include-packages: io.mango
      exclude-paths: /error,/actuator/**
      default-access-mode: LOGIN
      resources:
        - module-name: mango-doc
          http-method: GET
          path-pattern: /swagger-ui/**
          access-mode: PUBLIC
          description: Swagger UI
```

### 6.2 Manifest 资源清单同步

配置前缀：`mango.authorization.resource-sync.manifest`。

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.authorization.resource-sync.manifest.enabled` | `true` | 是否启用资源清单同步 |
| `mango.authorization.resource-sync.manifest.mode` | `write` | `write` 写入授权服务；`read` 只解析日志 |
| `mango.authorization.resource-sync.manifest.locations` | `classpath*:META-INF/mango/resource-manifest.json` 和 `classpath*:META-INF/mango/resource-manifests/*.json` | classpath 清单位置 |

### 6.3 Gateway 资源同步

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.authorization.resource-sync.gateway.enabled` | `true` | 是否同步 Gateway route Path 谓词 |
| `mango.authorization.resource-sync.gateway.mode` | `write` | `write` 写入，`read` 只扫描 |
| `mango.authorization.resource-sync.gateway.module-name` | `gateway` | Gateway 路由资源所属模块名 |

Gateway route metadata 中可配置 `apiAccessMode`，未配置时按 LOGIN。

### 6.4 运行时访问控制和前端运行时

| 配置项 | 默认值 | 作用 |
|--------|--------|------|
| `mango.authorization.resource-access.enabled` | `true` | 是否装配 `apiResourceAuthorizationManager` |
| `mango.frontend.deploy-profile` | `monolith` | 前端部署配置档，支持 monolith、hybrid、micro |
| `mango.security.debug-permit-all-filter-chain.enabled` | 未启用 | 没有其它 `SecurityFilterChain` 时提供全放行调试链 |

`mango.security.debug-permit-all-filter-chain.enabled` 只用于本地问题定位。真实业务验收必须走 token、资源策略和权限链路。

## 8. API 与扩展
API 资源和运行时决策：

- `POST /authorization/api-resources/register`：批量注册 API 资源。
- `GET /authorization/api-resources/access-decision`：按 `httpMethod` 和 `path` 返回是否命中、访问模式和权限码。
- `POST /authorization/api-resources/cache/refresh`：刷新 API 资源运行时缓存。

应用、模块和前端运行：

- `/authorization/apps`：应用入口增删改查，运行时 descriptor 查询。
- `/authorization/app-modules`：应用模块绑定、菜单同步、manifest 注册、runtime strategies。
- `/authorization/tenant-app-bindings`：租户应用绑定。

角色和菜单：

- `/authorization/roles`：角色增删改查、成员角色、角色菜单授权、可分配菜单。
- `/authorization/menus`：菜单树、当前用户菜单、菜单增删改查。
- `/authorization/menu-packages`：菜单套餐增删改查。
- `/authorization/subjects/user`：查询主体授权信息。

注解：

| 注解 | 等价模式 | 用法 |
|------|----------|------|
| `@PublicApi` | PUBLIC | 匿名访问 |
| `@LoginApi` | LOGIN | 登录后访问，不要求权限码 |
| `@PermissionAccess(value = "...")` | PERMISSION | 登录后且具备指定权限码 |
| `@InternalApi` | INTERNAL | 内部可信调用，外部入口拒绝 |
| `@ApiAccess` | 自定义 | 直接指定模式、权限码和描述 |

`ApiResourceSyncRunner` 扫描注解时：

- 方法注解优先于类注解。
- PERMISSION 必须有权限码，否则启动同步会抛错。
- PERMISSION 资源的 `resourceCode` 默认等于权限码。
- 其它模式资源的 `resourceCode` 默认是 `METHOD:path`。

Java API：

- `AuthorizationApi`
- `ApiResourceApi`
- `AppApi`
- `AppModuleApi`
- `MenuApi`
- `PermissionApi`
- `RoleApi`
- `TenantAppBindingApi`
- `IAuthorizationProvider`
- `AuthorityContributor`
- `ITokenProvider`

## 9. 数据与初始化
Flyway 路径：`mango-authorization-core/src/main/resources/db/migration/authorization`。

核心表：

| 表 | 作用 |
|----|------|
| `authorization_api_resource` | API 方法、路径、访问模式、权限码 |
| `authorization_role` | 角色 |
| `authorization_subject_role` | 成员或主体绑定角色 |
| `authorization_role_permission` | 历史权限定义绑定 |
| `authorization_subject_permission` | 主体直授权限 |
| `authorization_app` | 应用入口 |
| `authorization_app_login_context` | 应用登录域和操作者类型 |
| `authorization_menu` | 菜单、页面、按钮权限 |
| `authorization_role_menu` | 角色菜单授权 |
| `authorization_menu_package` | 菜单套餐 |
| `authorization_menu_package_item` | 套餐包含菜单 |
| `frontend_app_registry` | 前端运行单元注册 |
| `frontend_menu_runtime_config` | 菜单页面运行类型和外链地址 |
| `frontend_tenant_app_binding` | 租户应用绑定 |
| `authorization_app_module` | 应用集成能力模块 |
| `frontend_module_runtime_strategy` | 前端模块运行策略 |

初始化数据：

- `V1__init_authorization.sql` 初始化 `internal-admin` 应用入口。
- `V1__init_authorization.sql` 初始化 `internal-admin / INTERNAL / INTERNAL_USER` 登录上下文。
- `V1__init_authorization.sql` 初始化多个租户下的 `ROLE_ADMIN`。
- `V1__init_authorization.sql` 初始化 admin 成员的 `authorization_subject_role`。
- 后续 migration 包含 notice、job、payment、domain、system event 等模块菜单和权限调整。

启动初始化入口：

- `ApiResourceSyncRunner` 是应用启动时的 API 资源同步 Runner，负责扫描 Spring MVC Controller 和配置资源。
- `AppModuleResourceManifestSyncRunner` 是应用启动时的资源清单同步 Runner，负责加载 `META-INF/mango/resource-manifest.json` 和 `META-INF/mango/resource-manifests/*.json`。
- `GatewayRouteResourceSyncRunner` 是 Gateway 应用启动时的路由资源同步 Runner，负责把 Path 谓词注册为 API 资源。

API 资源注册行为：

- 唯一键是 `module_name + http_method + path_pattern`。
- 同步时会 upsert 当前扫描资源。
- 对自动扫描资源，如果同一 handler class 下旧资源本次没扫描到，会把旧资源 `status` 置为 0。
- 运行时决策缓存最多 10000 个 key，注册或手动刷新会清空缓存。

Manifest 入库行为：

- 写入或更新 `authorization_app_module`。
- 菜单按 `appCode + moduleCode + menuCode` upsert 到 `authorization_menu`。
- `permissionItems` 会生成 `menuType=3`、`visible=0` 的按钮菜单，`permissions` 等于权限码。
- 每个菜单都会写入 `frontend_menu_runtime_config`；按钮默认 pageType 为 BUTTON，embedded=1 默认 IFRAME，其它默认 LOCAL_ROUTE。
- `packageCodes` 命中已有菜单套餐时，会写入 `authorization_menu_package_item`。
- `roleCodes` 命中已有角色时，会写入 `authorization_role_menu`。

## 10. 管理入口
菜单清单字段：

| 字段 | 作用 |
|------|------|
| `appCode` | 应用入口编码，例如 `internal-admin` |
| `moduleCode` | 能力模块编码，通常来自模块 `module.properties` 的 `module-name` |
| `moduleName` | 能力模块名称 |
| `status` | 模块状态，空时保存为 1 |
| `sort` | 模块排序，空时保存为 0 |
| `packageCodes` | 菜单自动加入的套餐编码；为空不自动加入套餐 |
| `roleCodes` | 菜单自动授权的角色编码；为空不自动授权角色 |
| `menus[].menuType` | 1 目录、2 菜单、3 按钮；空时普通菜单保存为 2 |
| `menus[].menuName` | 菜单名称，必填 |
| `menus[].menuCode` | 菜单编码或权限码，必填 |
| `menus[].parentCode` | 指向已存在目录菜单编码；为空时使用树形父节点 |
| `menus[].path` | 前端路由路径 |
| `menus[].pageType` | LOCAL_ROUTE、MICRO_ROUTE、IFRAME、EXTERNAL_LINK、BUTTON |
| `menus[].externalUrl` | iframe 或外链地址 |
| `menus[].component` | 前端页面 key，例如某个 package 注册的页面 key |
| `menus[].permissions` | 页面携带的权限编码列表，保存为逗号分隔字符串 |
| `menus[].permissionItems` | 页面下按钮权限，会生成隐藏按钮菜单 |

资源清单放置位置：

```text
src/main/resources/META-INF/mango/resource-manifest.json
src/main/resources/META-INF/mango/resource-manifests/*.json
```

示例结构：

```json
{
  "appCode": "internal-admin",
  "moduleCode": "contract",
  "moduleName": "合同模块",
  "packageCodes": ["internal-admin-default"],
  "roleCodes": ["ROLE_ADMIN"],
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

当前用户菜单接口会按以下条件过滤：

- 当前主体必须有 `memberId`。
- 只返回指定 `appCode` 下启用菜单。
- 如果应用模块有启用绑定，只返回启用模块下的菜单。
- 如果授权快照包含 `*:*`，返回全部启用菜单。
- 否则只返回角色授权到的菜单。

## 11. 快速开始
新增一个业务模块菜单和接口权限时，按这个顺序做：

1. 后端 Controller 用 `@PermissionAccess(value = "module:resource:action")` 标注需要权限的接口；匿名接口用 `@PublicApi`，只登录用 `@LoginApi`，内部接口用 `@InternalApi`。
2. 模块引入 `mango-authorization-resource-sync-starter`。
3. 在 `META-INF/mango/resource-manifest.json` 或 `resource-manifests/*.json` 写模块、菜单、页面 component key 和按钮权限。
4. 启动服务，确认 API 资源和菜单已入库。
5. 给目标角色授权菜单，或在 manifest 中使用 `roleCodes` 自动绑定已有角色。
6. 给用户成员绑定角色。
7. 登录后检查 `/auth/info` 的 permissions 和 `/authorization/menus/user` 的菜单树。
8. 通过 `mango-access` 访问受保护接口，确认无权限 403、授权后通过。

## 12. 问题排查
- 接口权限不生效：先查 `authorization_api_resource` 是否有正确 `http_method + path_pattern + access_mode + permission_code`。
- PERMISSION 接口启动同步失败：检查 `@PermissionAccess` 或 `@ApiAccess(mode=PERMISSION)` 是否填写权限码。
- 菜单不显示：检查 `authorization_app_module.status`、菜单 `status/visible`、角色菜单绑定、租户应用绑定和当前 appCode。
- manifest 写了 `roleCodes` 但没授权：只有角色已存在时才会写 `authorization_role_menu`，不存在的 roleCode 会跳过。
- manifest 写了 `packageCodes` 但套餐没变化：只有菜单套餐已存在时才会写套餐明细。
- 改了资源策略但 access 仍按旧策略：调用 `/authorization/api-resources/cache/refresh` 或重新注册资源清空缓存。
- 前端页面打不开：检查菜单 `component` 是否对应前端包已注册页面 key，`pageType` 和 `externalUrl` 是否匹配。

## 13. 相关文档
- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [模块菜单规范](../../../mango-pmo/rules/backend/11-module-menu.md)
- [数据库规范](../../../mango-pmo/rules/backend/04-db.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
