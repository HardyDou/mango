# Mango Authorization

## 1. 概览

`mango-authorization` 是 Mango 的授权事实中心。它负责应用入口、登录上下文、API 资源访问策略、角色、菜单、按钮权限、菜单套餐、租户应用绑定、前端运行描述和授权快照。

业务开发最常用它解决三件事：

- 后端接口声明 PUBLIC、LOGIN、PERMISSION、INTERNAL 后，把 API 资源同步进库。
- 模块通过资源清单把菜单、按钮权限、前端页面 `component` key 注册进库。
- 给角色授权菜单、给成员绑定角色，让登录后 `/auth/info` 和管理端菜单返回正确权限。

## 2. 功能清单

| 能力 | 说明 |
|------|------|
| 应用入口 | 管理 `internal-admin` 等应用编码、登录上下文和前端运行配置 |
| API 资源 | 保存 method、path、access mode、permission code，供 `mango-access` 判断请求策略 |
| API 资源同步 | 通过 `mango-resource` 的 `API_RESOURCE` 资源类型扫描 Controller 注解、配置资源和 Gateway route，并同步到 `authorization_api_resource` |
| 菜单管理 | 保存目录、菜单、按钮权限、页面 key、路由、运行类型和可见状态 |
| 资源清单 | 从 `META-INF/mango/resource-manifest.json` 或 `resource-manifests/*.json` 批量注册模块菜单和权限 |
| 角色授权 | 管理角色、成员角色绑定、角色菜单授权 |
| 数据权限 | 按角色配置资源级数据范围，解析当前成员生效范围 |
| 用户菜单 | 按当前成员授权快照返回可见菜单树 |
| 授权快照 | 为 `mango-auth` 返回 roles 和 permissions，为 `mango-access` 做权限码匹配 |
| 角色数据权限 | 为 `mango-infra-persistence` 提供 `DataScopeProvider`，业务查询可显式应用数据范围 |
| 前端运行时 | 返回应用 runtime descriptor 和模块运行策略 |

## 3. 后端接入

业务代码只需要注解、命令对象或 Java API 时依赖 API 包：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-api</artifactId>
</dependency>
```

部署授权中心服务时依赖 starter：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-starter</artifactId>
</dependency>
```

应用需要在启动时同步 Controller API 资源时，按拓扑启用 `mango-resource-sync-starter`，并选择本地或远程 Resource Registry starter：

```xml
<dependency>
    <groupId>io.mango.platform.resource</groupId>
    <artifactId>mango-resource-sync-starter</artifactId>
</dependency>
```

应用需要同步 authorization manifest 菜单、按钮权限和前端运行配置时，部署入口依赖 authorization resource sync starter：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-resource-sync-starter</artifactId>
</dependency>
```

应用需要 Spring Security 按授权资源做运行时 URL 控制时依赖 resource access starter：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-resource-access-starter</artifactId>
</dependency>
```

微服务中远程调用授权中心时依赖 remote starter：

```xml
<dependency>
    <groupId>io.mango.platform.authorization</groupId>
    <artifactId>mango-authorization-starter-remote</artifactId>
</dependency>
```

## 4. 前端接入

`mango-authorization` 的菜单数据被 `@mango/admin-shell` 消费。Shell 登录后调用当前用户菜单接口，把菜单里的 `component` 字段匹配到已注册的前端页面 key。

常见关系：

| 后端字段 | 前端含义 |
|----------|----------|
| `appCode` | 当前管理应用，默认常见值是 `internal-admin` |
| `moduleCode` | 能力模块编码，例如 `mango-payment`、`mango-notice` |
| `path` | 前端路由路径 |
| `component` | 前端页面 key，必须和对应 npm 包注册的页面 key 一致 |
| `pageType` | `LOCAL_ROUTE`、`MICRO_ROUTE`、`IFRAME`、`EXTERNAL_LINK`、`BUTTON` |
| `permissions` | 页面级权限码，前端按钮权限和后端接口权限共同使用 |

如果新增的是 admin 页面插件，先在对应前端包 README 里确认页面 key，再把相同 key 写入 authorization 菜单清单。

## 5. 快速开始

新增一个业务模块的菜单和接口权限，按这个顺序做：

1. Controller 使用 `@PublicApi`、`@LoginApi`、`@PermissionAccess` 或 `@InternalApi` 声明访问模式。
2. PERMISSION 接口用稳定权限码，例如 `contract:archive:create`。
3. 部署应用启用 `mango-resource-sync-starter`，并按拓扑选择本地 `mango-resource-starter` 或远程 `mango-resource-starter-remote`。
4. 在模块资源目录放 `META-INF/mango/resource-manifest.json`，登记模块、菜单、页面 key 和按钮权限。
5. 启动服务后确认 `authorization_api_resource` 有接口资源，`authorization_menu` 有菜单和按钮权限。
6. 给角色授权菜单，或在 manifest 的 `roleCodes` 中写入已存在角色编码。
7. 给成员绑定角色。
8. 登录后检查 `/auth/info` 的 `permissions` 和 `/authorization/menus/user?fmt=tree&appCode=internal-admin` 的菜单树。
9. 访问受保护接口，确认无权限返回 403、授权后通过。

## 6. 配置说明

API 资源扫描配置示例：

```yaml
mango:
  authorization:
    resource-sync:
      enabled: true
      mode: write
      include-packages:
        - io.mango
      exclude-paths:
        - /error
        - /actuator/**
      default-access-mode: LOGIN
      resources:
        - module-name: mango-doc
          http-method: GET
          path-pattern: /swagger-ui/**
          access-mode: PUBLIC
          description: Swagger UI
```

manifest 同步示例：

```yaml
mango:
  authorization:
    resource-sync:
      manifest:
        enabled: true
        mode: write
        locations:
          - classpath*:META-INF/mango/resource-manifest.json
          - classpath*:META-INF/mango/resource-manifests/*.json
```

前端运行配置档：

```yaml
mango:
  frontend:
    deploy-profile: monolith
```

## 7. YAML 配置字段

| 配置项 | 默认值 | 含义 |
|--------|--------|------|
| `mango.authorization.resource-sync.enabled` | `true` | 是否启用 MVC / Gateway 资源同步自动配置 |
| `mango.authorization.resource-sync.module-name` | 空，兜底 `unknown-module` | 扫描资源无法解析模块时使用的模块名 |
| `mango.authorization.resource-sync.mode` | `write` | `write` 写入授权服务，`read` 只扫描并输出日志 |
| `mango.authorization.resource-sync.include-packages` | `io.mango` | 只扫描这些包前缀下的 Controller |
| `mango.authorization.resource-sync.exclude-paths` | `/error,/actuator/**` | 排除路径 |
| `mango.authorization.resource-sync.default-access-mode` | `LOGIN` | Controller 未声明访问注解时使用的访问模式 |
| `mango.authorization.resource-sync.resources[]` | 空列表 | 用 YAML 补充非 Controller 资源 |
| `mango.authorization.resource-sync.manifest.enabled` | `true` | 是否启用资源清单同步 |
| `mango.authorization.resource-sync.manifest.mode` | `write` | `write` 写入授权服务，`read` 只解析日志 |
| `mango.authorization.resource-sync.manifest.locations` | `classpath*:META-INF/mango/resource-manifest.json`、`classpath*:META-INF/mango/resource-manifests/*.json` | 资源清单位置 |
| `mango.authorization.resource-sync.gateway.enabled` | `true` | 是否同步 Gateway route Path 谓词 |
| `mango.authorization.resource-sync.gateway.mode` | `write` | Gateway 资源同步模式 |
| `mango.authorization.resource-sync.gateway.module-name` | `gateway` | Gateway 路由资源所属模块名 |
| `mango.authorization.resource-access.enabled` | `true` | 是否装配 `apiResourceAuthorizationManager` |
| `mango.frontend.deploy-profile` | `monolith` | 前端部署配置档：`monolith`、`hybrid`、`micro` |

`resources[]` 会被 `ApiAccessResourceProvider` 转换为 `mango-resource` 的 `API_RESOURCE` 资源声明，再由资源注册中心调用授权模块的 `ApiResourceHandler` 写入 `authorization_api_resource`。

`resources[]` 字段：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `module-name` | 同模块名兜底逻辑 | 稳定 Mango 模块名 |
| `http-method` | `ALL` | HTTP 方法 |
| `path-pattern` | 无 | 路径模式 |
| `resource-code` | `METHOD:path` 或权限码 | 稳定资源编码 |
| `permission-code` | 空 | PERMISSION 模式需要的权限码 |
| `access-mode` | `default-access-mode` | PUBLIC、LOGIN、PERMISSION、INTERNAL |
| `description` | `Configured API resource` | 描述 |

## 8. 接口/API 使用

注解使用：

| 注解 | 访问模式 | 说明 |
|------|----------|------|
| `@PublicApi` | PUBLIC | 匿名访问 |
| `@LoginApi` | LOGIN | 登录后访问，不要求权限码 |
| `@PermissionAccess("code")` | PERMISSION | 登录后且具备指定权限码 |
| `@InternalApi` | INTERNAL | 内部接口，外部 access 入口拒绝 |
| `@ApiAccess` | 自定义 | 直接指定模式、权限码和描述 |

主要 HTTP 接口：

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/authorization/api-resources/register` | 批量注册 API 资源 |
| GET | `/authorization/api-resources/access-decision` | 查询 method + path 的访问模式和权限码 |
| POST | `/authorization/api-resources/cache/refresh` | 刷新 API 资源运行时缓存 |
| GET | `/authorization/menus/user` | 查询当前用户可见菜单 |
| GET | `/authorization/menus` | 查询菜单列表或树 |
| POST/PUT/DELETE | `/authorization/menus` | 创建、更新、删除菜单 |
| GET/POST/PUT/DELETE | `/authorization/roles` | 角色管理 |
| POST | `/authorization/roles/subjects` | 给主体分配角色 |
| POST | `/authorization/roles/menus` | 给角色分配菜单 |
| GET/POST/DELETE | `/authorization/data-scopes/roles` | 查询、保存、删除角色数据权限 |
| GET | `/authorization/data-scopes/effective` | 查询当前成员在资源上的生效数据权限 |
| GET/POST/PUT/DELETE | `/authorization/apps` | 应用入口管理 |
| GET/POST/DELETE | `/authorization/app-modules` | 应用模块绑定管理 |
| POST | `/authorization/app-modules/resource-manifests/register` | 手动注册模块资源清单 |
| GET/POST | `/authorization/app-modules/runtime-strategies` | 前端模块运行策略 |
| GET/POST/DELETE | `/authorization/tenant-app-bindings` | 租户应用开通 |
| GET | `/authorization/subjects/user` | 查询主体授权快照 |

常用 Java API：

| API | 用途 |
|-----|------|
| `ApiResourceApi` | 注册 API 资源、查询访问决策、刷新缓存 |
| `AppModuleApi` | 注册模块资源清单、模块绑定和运行策略 |
| `DataScopeApi` | 管理角色数据权限、查询当前成员生效数据范围 |
| `MenuApi` | 菜单树、用户菜单和菜单管理 |
| `RoleApi` | 角色、主体角色和角色菜单授权 |
| `AuthorizationApi` | 查询授权快照 |
| `IAuthorizationProvider` | 运行时加载角色码和权限码 |
| `AuthorityContributor` | 扩展授权快照贡献来源 |

### 8.1 角色数据权限

数据权限只按角色配置，不提供个人数据权限，也不提供岗位数据权限。用户侧通过“成员主部门 + 角色绑定”动态生效，避免为每个部门复制一套角色。

范围模式：

| 模式 | 含义 | 典型用途 |
|------|------|----------|
| `ALL` | 当前租户内全部数据 | 租户管理员、平台运营角色 |
| `SELF` | 当前登录用户创建的数据 | 普通员工只看本人数据 |
| `SELF_ORG` | 当前成员主部门数据 | 部门管理员看本部门数据 |
| `SELF_ORG_AND_CHILDREN` | 当前成员主部门及下级部门数据 | 大部门负责人看部门树数据 |
| `ORG` | 指定组织集合数据 | 固定组织授权，不随用户主部门变化 |

管理端配置路径：

1. 角色管理：进入 `/#/system/role`，在角色行点击“数据权限”。
2. 数据资源：在表格新增行里从树形选择器选择资源。页面只展示 list 类资源，通常和业务列表查询权限码一致，例如 `workflow:definition:list`。
3. 数据范围：选择“全部 / 本人 / 本人部门 / 本人部门及下级 / 指定组织”，保存后立即按角色生效。
4. 用户配置：进入成员管理，把用户加入部门并设置“主部门”，再在“分配成员角色”里选择角色。

部门管理员的推荐配置是：只维护一个可复用角色，例如 `部门管理员`；该角色的数据权限选择 `SELF_ORG` 或 `SELF_ORG_AND_CHILDREN`。不同部门的管理员分配同一个角色，系统按每个成员自己的主部门解析数据范围。

业务查询生效链路：

1. 业务列表或查询资源使用稳定权限码作为 `resourceCode`。
2. 业务 Service 调用 `DataScopeApplier`，传入表名和 `created_by`、`org_id`、`tenant_id` 映射。
3. 授权中心按当前成员角色解析 `EffectiveDataScopeVO`。
4. persistence 应用器追加本人或组织条件；缺少必要字段时 fail-fast。

## 9. 资源清单

资源清单放在模块 classpath：

```text
src/main/resources/META-INF/mango/resource-manifest.json
src/main/resources/META-INF/mango/resource-manifests/*.json
```

示例：

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

资源清单字段：

| 字段 | 含义 |
|------|------|
| `appCode` | 应用编码 |
| `moduleCode` | 能力模块编码，通常来自 `module.properties` 的 `module-name` |
| `moduleName` | 能力模块名称 |
| `status` | 模块状态，空时保存为 1 |
| `sort` | 模块排序，空时保存为 0 |
| `packageCodes` | 菜单自动加入的套餐编码；套餐必须已存在 |
| `roleCodes` | 菜单自动授权的角色编码；角色必须已存在 |
| `menus[].menuType` | 1 目录、2 菜单、3 按钮 |
| `menus[].menuName` | 菜单名称 |
| `menus[].menuCode` | 菜单编码或权限码 |
| `menus[].parentCode` | 指定父菜单编码；为空时按清单树结构入库 |
| `menus[].path` | 前端路由路径 |
| `menus[].pageType` | `LOCAL_ROUTE`、`MICRO_ROUTE`、`IFRAME`、`EXTERNAL_LINK`、`BUTTON` |
| `menus[].component` | 前端页面 key |
| `menus[].externalUrl` | iframe 或外链地址 |
| `menus[].permissions` | 页面携带的权限编码列表 |
| `menus[].permissionItems` | 页面下按钮权限，会生成隐藏按钮菜单 |

清单同步行为：

| 行为 | 说明 |
|------|------|
| 模块绑定 | 写入或更新 `authorization_app_module` |
| 菜单入库 | 按 `appCode + moduleCode + menuCode` upsert 到 `authorization_menu` |
| 按钮权限 | `permissionItems` 生成 `menuType=3`、`visible=0` 的按钮菜单 |
| 前端运行配置 | 每个菜单写入 `frontend_menu_runtime_config` |
| 套餐绑定 | `packageCodes` 命中已有菜单套餐时写入 `authorization_menu_package_item` |
| 角色授权 | `roleCodes` 命中已有角色时写入 `authorization_role_menu` |

## 10. 返回字段

`ApiResourceAccessDecisionVO`：

| 字段 | 含义 |
|------|------|
| `matched` | 是否命中已注册资源 |
| `accessMode` | PUBLIC、LOGIN、PERMISSION、INTERNAL |
| `permissionCode` | PERMISSION 模式需要的权限码 |

`ApiResourceRegisterResultVO`：

| 字段 | 含义 |
|------|------|
| `scanned` | 扫描到的资源数量 |
| `created` | 新增资源数量 |
| `updated` | 更新资源数量 |

`MenuVO` 关键字段：

| 字段 | 含义 |
|------|------|
| `menuId` | 菜单 ID |
| `appCode` | 应用编码 |
| `moduleCode` | 模块编码 |
| `parentId` | 父菜单 ID |
| `menuType` | 1 目录、2 菜单、3 按钮 |
| `menuName` | 菜单名称 |
| `menuCode` | 菜单编码或权限码 |
| `path` | 路由路径 |
| `pageType` | 页面运行类型 |
| `component` | 前端页面 key |
| `permissions` | 权限编码，多个值以逗号保存 |
| `children` | 子菜单 |

## 11. 管理入口

后端管理接口集中在 `/authorization/**`。前端管理页面通常来自 `@mango/rbac` 和 `@mango/admin-pages` 注册的 RBAC 页面。

当前用户菜单接口：

```http
GET /authorization/menus/user?fmt=tree&appCode=internal-admin
```

过滤逻辑：

| 条件 | 行为 |
|------|------|
| 当前主体没有 `memberId` | 无法加载成员授权菜单 |
| 指定 `appCode` | 只返回该应用下菜单 |
| 应用模块存在启用绑定 | 只返回启用模块下菜单 |
| 授权快照包含 `*:*` | 返回全部启用菜单 |
| 普通角色授权 | 只返回角色授权到的菜单 |

## 12. 资源注入

授权模块作为 `mango-resource` 的资源消费者公开 `API_RESOURCE`。该类型主要由扫描型 Provider 生成，不要求业务把接口权限写成 YAML。

资源链路：

```text
Controller / @ApiAccess / YAML resources / Gateway route
    -> ApiAccessResourceProvider
    -> ResourceDeclaration(API_RESOURCE)
    -> ResourceRegistryApi
    -> ApiResourceHandler
    -> authorization_api_resource
```

`API_RESOURCE` 保持原授权注册逻辑，底层仍调用授权模块的 API 资源注册服务；新链路只把“扫描和注册过程”纳入 `mango-resource` 的统一声明、hash、同步日志和覆盖控制。

字段契约：

| 字段 | 类型 | 必填 | 含义 |
|------|------|------|------|
| `id` | `STRING` | 是 | 资源稳定 ID，扫描型资源由 Provider 生成稳定 ID。 |
| `version` | `INT` | 是 | 资源版本，扫描策略变化时递增。 |
| `biz-key` | `STRING` | 是 | 接口资源业务键，通常由模块、方法和路径生成。 |
| `target-module` | `STRING` | 是 | 固定为 `authorization`。 |
| `moduleName` | `STRING` | 否 | 来源模块名。 |
| `resourceCode` | `STRING` | 是 | 接口资源编码。 |
| `httpMethod` | `STRING` | 是 | HTTP 方法，支持 `GET`、`POST`、`PUT`、`DELETE`、`ALL` 等。 |
| `pathPattern` | `STRING` | 是 | 路径模式。 |
| `accessMode` | `STRING` | 是 | `PUBLIC`、`LOGIN`、`PERMISSION`、`INTERNAL`。 |
| `permissionCode` | `STRING` | `PERMISSION` 时必填 | 权限码。 |
| `description` | `STRING` | 否 | 资源说明。 |

批量同步语义：

| 操作 | 行为 |
|------|------|
| `upsertBatch` | 按当前扫描批次调用授权原注册逻辑，避免逐条 upsert 改变原数据行为。 |
| `disable` | 禁用对应 API 资源。 |
| `delete` | 删除对应 API 资源。 |

## 13. 数据与初始化

Flyway 路径：

```text
mango-authorization-core/src/main/resources/db/migration/authorization
```

核心表：

| 表 | 作用 |
|----|------|
| `authorization_api_resource` | API 方法、路径、访问模式、权限码 |
| `authorization_app` | 应用入口 |
| `authorization_app_login_context` | 应用登录上下文 |
| `authorization_app_module` | 应用集成能力模块 |
| `authorization_menu` | 菜单、页面、按钮权限 |
| `authorization_role` | 角色 |
| `authorization_role_menu` | 角色菜单授权 |
| `authorization_subject_role` | 成员或主体绑定角色 |
| `authorization_role_data_scope` | 角色数据权限 |
| `authorization_menu_package` | 菜单套餐 |
| `authorization_menu_package_item` | 套餐包含菜单 |
| `frontend_app_registry` | 前端运行单元注册 |
| `frontend_menu_runtime_config` | 菜单页面运行类型和外链地址 |
| `frontend_module_runtime_strategy` | 前端模块运行策略 |
| `frontend_tenant_app_binding` | 租户应用绑定 |

`V1__init_authorization.sql` 初始化 `internal-admin` 应用、`INTERNAL / INTERNAL_USER` 登录上下文、多个租户下的 `ROLE_ADMIN` 和 admin 成员角色绑定。后续 migration 按模块补充 notice、job、payment、domain、system event 等菜单和权限。

启动同步入口：

| Runner | 作用 |
|--------|------|
| `ResourceSyncRunner` | 汇总 `ApiAccessResourceProvider` 生成的 `API_RESOURCE` 声明并调用资源注册中心 |
| `AppModuleResourceManifestSyncRunner` | 读取资源清单，注册模块、菜单、按钮权限和前端运行配置 |
| `GatewayRouteResourceSyncRunner` | 扫描 Gateway route Path 谓词，注册网关资源 |

## 14. 问题排查

| 现象 | 排查点 |
|------|--------|
| 接口权限不生效 | 查 `authorization_api_resource` 是否有正确的 `http_method + path_pattern + access_mode + permission_code` |
| PERMISSION 同步失败 | `@PermissionAccess` 或 `@ApiAccess(mode=PERMISSION)` 必须填写权限码；同时检查 `resource_registry` 和 `resource_sync_log` |
| 菜单不显示 | 查 `authorization_app_module.status`、菜单 `status/visible`、角色菜单绑定、当前 `appCode` |
| manifest 写了 `roleCodes` 但没授权 | 只有角色已存在时才会写 `authorization_role_menu` |
| manifest 写了 `packageCodes` 但套餐没变化 | 只有菜单套餐已存在时才会写套餐明细 |
| 改了资源策略但 access 仍按旧策略 | 调 `/authorization/api-resources/cache/refresh` 或重新注册资源清空缓存 |
| 前端页面打不开 | 查菜单 `component` 是否等于前端包注册的页面 key，`pageType` 和 `externalUrl` 是否匹配 |
| 登录后 `/auth/info` 权限为空 | 查成员角色绑定的 `subjectId` 是否等于登录 `memberId`，`appCode`、`realm`、`actorType` 是否一致 |

## 15. 相关文档

- [Mango Access](../mango-access/README.md)
- [Mango Auth](../mango-auth/README.md)
- [Mango Resource](../mango-resource/README.md)
- [@mango/rbac](../../../mango-ui/packages/rbac/README.md)
- [@mango/admin-shell](../../../mango-ui/packages/admin-shell/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
