# 系统基础功能闭环任务计划

更新时间：2026-05-09

## 执行规则

- 每次只推进一个任务。
- 当前任务未通过后端验证和前端 E2E 前，不开始下一个任务。
- E2E 必须使用真实后端接口，不使用 mock。
- 验证必须覆盖平台租户“芒果集团”和普通租户“A 公司”的权限差异。
- Flyway 必须执行，不允许禁用迁移。

## 任务清单

| 序号 | 任务 | 状态 | 后端验收 | 前端 E2E 验收 |
|---|---|---|---|---|
| T1 | 组织/岗位权限闭环 | 已完成 | 组织、岗位接口权限声明完整；普通租户只能访问租户内组织/岗位；未授权接口返回 403 | A 公司可进入组织架构、岗位管理；平台运营菜单不可见；岗位查询/新增/编辑/删除基础流程通过 |
| T2 | 角色授权闭环 | 已完成 | 角色列表、新增、编辑、删除、分配菜单、分配主体权限码完整；普通租户不能授权平台运营能力 | A 公司可维护本租户角色；不能看到或授权租户管理/应用管理/菜单管理 |
| T3 | 平台元数据隔离复核 | 已完成 | 应用、菜单、字典、行政区划、接口资源不被 tenant SQL 过滤误伤；普通租户无维护权限 | 芒果集团可见平台运营和基础数据；A 公司不可见平台运营和基础数据维护入口 |
| T4 | 系统管理菜单 E2E 更新 | 已完成 | `/authorization/menus/user?fmt=tree` 按登录租户上下文返回菜单 | 平台租户显示完整菜单；A 公司只显示账号权限、组织人事、审计日志 |
| T5 | 用户管理能力决策与落地 | 已完成 | 用户 CRUD、启停、重置密码、角色绑定接口可用；A 公司只能管理本租户创建或本租户已授权主体用户 | 用户管理不再使用静态数据；新增、查询、编辑、启停、重置密码、分配角色、删除流程通过 |
| T6 | 新租户初始化闭环 | 已完成 | 平台管理员新增租户后自动初始化根组织、默认岗位、租户管理员角色、菜单授权、创建者成员与角色绑定；新租户可直接登录并加载菜单 | 新增租户后，登录页可选择新租户，admin 可登录并进入系统，用户菜单接口返回 200 |
| T7 | 租户管理页面真实接口闭环 | 已完成 | 平台管理员可新增、查询、编辑、禁用、启用、删除租户；普通租户不能访问租户管理维护接口 | 平台租户在租户管理页面完成 CRUD 与状态流转；A 公司不可见租户管理入口，直连维护接口返回 403 |
| T8 | 应用管理页面真实接口闭环 | 已完成 | 平台管理员可查询、新增、编辑、删除应用入口；应用登录上下文随应用一并创建和更新；普通租户不能维护应用入口 | 平台租户在应用管理页面完成新增、图标选择、编辑、删除流程；编辑保存不丢登录态 |
| T9 | 菜单管理页面真实接口闭环 | 已完成 | 平台管理员可查询、新增、编辑、删除菜单；新增菜单补齐领域默认值；普通租户不能维护平台菜单 | 平台租户在菜单管理页面完成新增、编辑、删除流程；列表按树形展示；A 公司不可见菜单管理入口，直连维护接口返回 403 |
| T10 | 字典管理页面真实接口闭环 | 已完成 | 平台管理员可维护字典类型和字典数据；普通租户不能维护基础字典；字典选项接口作为基础选择能力可读 | 平台租户完成字典类型和字典数据新增、编辑、删除；A 公司不可见字典管理入口，维护接口 403，选项接口 200 |
| T11 | 行政区划管理页面真实接口闭环 | 已完成 | 行政区划选择器接口登录可读；平台管理员可维护自定义区划；普通租户不能维护行政区划 | 平台租户逐级加载行政区划并完成自定义区划编辑、删除；A 公司不可见行政区划入口，维护接口 403，选择器读取 200 |
| T12 | 系统配置页面真实接口闭环 | 已完成 | 系统配置作为平台基础元数据不参与租户过滤；平台管理员可维护系统参数/配置；普通租户不能维护参数配置 | 平台租户完成系统参数、系统配置新增、编辑、删除；A 公司不可见参数配置入口，维护接口 403 |
| T13 | 路由管理页面真实接口闭环 | 已完成 | 路由配置作为平台运行元数据不参与租户过滤；平台管理员可维护路由配置；普通租户不能维护路由配置 | 平台租户完成路由配置新增、编辑、删除；A 公司不可见路由管理入口，维护接口 403 |
| T14 | 审计日志真实写入与查询闭环 | 已完成 | 登录成功/失败写入登录日志；维护接口操作写入操作日志；日志查询按租户上下文隔离；清理权限补齐 | 登录日志、操作日志页面使用后端分页查询；E2E 覆盖登录日志、操作日志、租户隔离 |
| T15 | 组织架构管理闭环 | 已完成 | 组织新增、编辑、删除接口可用；普通租户只能维护本租户组织；根组织不可删除、不可禁用、不可移动 | A 公司在组织架构页面完成新增下级、编辑、删除；根组织保护和权限边界通过 |
| T16 | IP 地理位置库基础能力 | 已完成 | 新增独立 IP 归属地解析基础模块；登录日志、操作日志消费统一解析能力；解析失败不影响主流程 | 登录日志、操作日志展示 IP 归属地；页面刷新和详情展示一致 |
| T17 | 日期范围查询参数契约修复 | 待处理 | 所有列表查询的 `startTime/endTime` 参数支持前端日期选择器常见输入；后端不再因 `2026-05-07` 绑定到 `LocalDateTime` 失败 | Swagger/Knife4j 和前端页面用日期范围查询不再出现参数转换错误；日志、用户、机构等使用日期筛选的页面 E2E 覆盖 |
| T18 | 组织架构页面布局一致性修复 | 待处理 | 无后端改造 | `/system/org` 主内容区域与面包屑左侧间距需和角色、成员、岗位等系统页面一致；经典主题下截图/E2E 验证通过 |

## 待办任务：T18 组织架构页面布局一致性修复

### 问题现象

访问 `http://localhost:7777/#/system/org` 时，组织架构页面主内容区域与面包屑左侧距离和其它系统管理页面不同，视觉上不一致。

### 处理原则

- 优先排查页面自身容器样式，不在全局布局上做无差别调整。
- 与成员管理、角色管理、岗位管理等页面的主内容左边距、面包屑起点保持一致。
- 经典主题优先验证；如默认、横向、分栏主题受影响，需要一起回归。

### 验收标准

- `/system/org` 与 `/system/role`、`/system/user` 等页面截图对比，主内容左边距和面包屑左起点一致。
- E2E 覆盖经典主题下进入 `/system/org` 后无布局偏移、无横向滚动、无 401/403/加载失败。

## 待办任务：T17 日期范围查询参数契约修复

### 问题现象

当前部分查询对象使用 `LocalDateTime startTime/endTime`，但前端或 Swagger 调试时会传入日期字符串，例如：

- `startTime=2026-05-07`
- `endTime=2026-05-08`

Spring 绑定时会报错：

```text
Failed to convert property value of type 'java.lang.String' to required type 'java.time.LocalDateTime' for property 'startTime'
Failed to convert from type [java.lang.String] to type [java.time.LocalDateTime] for value [2026-05-07]
```

### 处理原则

- 不为了“能用”在单个接口里临时拼字符串。
- 统一查询参数契约：明确日期范围到底接受 `yyyy-MM-dd`、`yyyy-MM-dd HH:mm:ss`，还是 ISO-8601。
- 对“按日期筛选”的查询，前端传日期即可；后端转换为当天开始时间和结束时间。
- 对“精确时间筛选”的查询，前端传完整时间；后端使用统一格式注解或全局转换器。
- Swagger/Knife4j 文档必须能看出参数格式要求。

### 待排查范围

- `startTime/endTime` 类型为 `LocalDateTime` 的 Query/Command。
- 登录日志、操作日志等时间范围筛选接口。
- 其它列表页中使用日期范围筛选的接口。

### 验收标准

- 后端直连：
  - `startTime=2026-05-07&endTime=2026-05-08` 不再触发绑定异常。
  - 完整时间格式仍可用。
- 文档：
  - Swagger/Knife4j 参数说明展示日期或时间格式。
- 前端 E2E：
  - 至少覆盖登录日志和操作日志日期范围筛选。
  - 如成员、机构、角色等页面存在日期筛选，也要补对应用例。

## 已完成任务：T16 IP 地理位置库基础能力

详细模块规划见：

- [IP 地理位置库模块规划](./ip-location-module-plan.md)

### 模块规划

- 新增基础设施模块，建议命名为 `mango-infra-ip-location`：
  - `mango-infra-ip-location-api`：定义 `IpLocationResolver`、`IpLocation`、配置模型和空实现约定。
  - `mango-infra-ip-location-core`：实现离线库解析、缓存、资源加载和异常兜底。
  - `mango-infra-ip-location-starter`：提供 Spring Boot 自动配置，供单体和后续微服务按需引入。
- 默认解析引擎使用 `ip2region xdb`：
  - xdb 文件通过配置指定路径，支持 classpath 和外部文件两种加载方式。
  - 默认只做本地离线查询，不调用外部接口。
  - 解析失败返回空归属地，不阻断登录、操作日志写入。
- 日志模块只依赖 `IpLocationResolver` 抽象：
  - 登录日志写入 `location`。
  - 操作日志补齐 `location` 字段后写入归属地。
  - 不在日志切面中直接绑定具体 IP 库实现。

### 边界约束

- IP 归属地是基础能力，不属于 system 日志模块自身实现。
- 不把 xdb 数据文件硬编码进业务代码；数据文件更新应能通过替换文件完成。
- 当前先满足审计展示：国家、省、市、运营商合并为展示字段；后续如需统计，再拆分 `country/province/city/isp`。
- IPv4 优先完成；IPv6 支持跟随所选库能力单独验收。

### 验收

- 后端单测覆盖：
  - 私网、空 IP、非法 IP 返回空归属地。
  - 正常公网 IP 返回可展示归属地。
  - xdb 文件不存在时服务可启动，解析返回空。
- 后端直连：
  - 登录成功后登录日志有 IP 和归属地。
  - 触发一个 `@Log` 操作后操作日志有 IP 和归属地。
- 前端 E2E：
  - 登录日志列表和详情展示归属地。
  - 操作日志列表和详情展示归属地。

## 已完成任务：T16 IP 地理位置库基础能力

### 模块落地

- 新增基础设施模块 `mango-infra-ip-location`：
  - `mango-infra-ip-location-api`：提供 `IpLocationResolver`、`IpLocation` 抽象。
  - `mango-infra-ip-location-core`：提供 `NoopIpLocationResolver`、`IpAddressClassifier`、`CachingIpLocationResolver`、`Ip2RegionXdbLocationResolver`。
  - `mango-infra-ip-location-starter`：提供 `IpLocationAutoConfiguration`、`IpLocationProperties`。
- 单体应用引入 `mango-infra-ip-location-starter`。
- 默认配置指向外部文件 `./config/ip-location/ip2region_v4.xdb`。
- xdb 数据文件不提交仓库，仓库只保留 `config/ip-location/README.md`；缺失时 `fail-fast=false`，服务正常启动并返回“未知”。

### 日志接入

- 登录日志：
  - `AuthController` 只依赖 `IpLocationResolver` 抽象。
  - 登录成功/失败写入 `location`。
- 操作日志：
  - `OperationLogAspect` 只依赖 `IpLocationResolver` 抽象。
  - 新增迁移 `system.V13__add_operation_log_location.sql`，给 `sys_operation_log` 增加 `location` 字段。
  - 操作日志 PO、实体、转换逻辑补齐 `location`。

### 通过结果

- 后端构建：
  - `mvn -pl mango-infra/mango-infra-ip-location/mango-infra-ip-location-starter -am -DskipTests package` 通过。
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- Flyway：
  - `system.V13__add_operation_log_location.sql` 已执行成功。
- 后端直连：
  - 使用 `X-Forwarded-For: 8.8.8.8` 登录后，登录日志写入 `location=United States Google LLC US`。
  - 使用 `X-Forwarded-For: 8.8.4.4` 新增/删除系统路由后，操作日志写入 `location=United States Atlanta Google LLC US`。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/log-management.spec.ts --project=chromium`，3 个用例通过。
  - E2E 已覆盖登录日志和操作日志 `location` 字段非空断言。

## 已完成任务：T1 组织/岗位权限闭环

### 后端改造

- 组织查询接口补 `@ApiAccess(PERMISSION)`：
  - `/org/tree` -> `system:org:list`
  - `/org/children` -> `system:org:list`
  - `/org/detail` -> `system:org:query`
- 岗位接口补 `@ApiAccess(PERMISSION)`：
  - `/post/page` -> `system:post:list`
  - `/post/detail` -> `system:post:query`
  - `POST /post` -> `system:post:add`
  - `PUT /post` -> `system:post:edit`
  - `DELETE /post` -> `system:post:delete`
- 迁移补齐菜单按钮权限和默认租户授权。

### 验证

- 后端构建：`mvn -pl mango-app/monolith/mango-monolith-app -am package -DskipTests`
- 重启 `5555` 后检查 Flyway 迁移状态。
- 使用 A 公司 token：
  - `GET /org/tree` 返回 200
  - `GET /post/page` 返回 200
  - 无平台运营菜单
  - `POST /system/tenant` 返回 403
- 前端 E2E：
  - 登录 A 公司。
  - 访问 `/system/org`、`/system/post`。
  - 截图检查页面无 401/403/未授权/加载失败。
  - 岗位管理至少完成新增、编辑、删除一个临时岗位。
- 通过结果：
  - Flyway `authorization` schema 已执行到 `V14__add_org_post_list_permissions.sql`。
  - A 公司 `GET /org/tree`、`GET /post/page` 返回 200。
  - A 公司 `POST /system/tenant` 返回 403。
  - A 公司 `GET /system/dict/data/options?typeCode=sys_normal_disable` 返回 200，字典维护接口仍返回 403。
  - Playwright：`pnpm exec playwright test e2e/specs/org-post-permission.spec.ts --project=chromium` 通过。

## 已完成任务：T2 角色授权闭环

### 后端改造

- 新增 `GET /authorization/roles/assignable-menus`，返回当前用户可分配给角色的菜单权限树。
- `assignMenus` 增加可授权范围校验，普通租户不能把平台运营、租户管理、应用管理、菜单管理等菜单授权给角色。
- `assignRoles` 增加角色租户归属校验，普通租户不能把平台角色绑定给主体。
- 角色、主体角色、角色菜单写入不再手工设置 `tenantId`，由租户插件按上下文注入。
- Controller 不再固定返回成功，按 service 返回值透出无权授权结果。

### 验证

- 后端构建：`mvn -pl mango-app/monolith/mango-monolith-app -am package -DskipTests`
- 使用 A 公司 token：
  - `GET /authorization/roles` 返回 200。
  - `GET /authorization/roles/assignable-menus?appCode=internal-admin` 返回 200，范围只包含账号权限、组织人事、审计日志。
  - `GET /authorization/apps` 返回 403。
  - `GET /authorization/menus?fmt=tree` 返回 403。
  - `POST /authorization/roles/menus` 分配菜单 `12` 返回业务 `code=403`。
  - 分配合法菜单返回 200。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/role-permission.spec.ts --project=chromium` 通过。
  - 回归 `pnpm exec playwright test e2e/specs/org-post-permission.spec.ts --project=chromium` 通过。

## 已完成任务：T3 平台元数据隔离复核

### Pigx 租户逻辑参考结论

- Pigx 通过请求头/参数写入租户上下文，租户插件基于上下文自动追加 `tenant_id`。
- Pigx 的 `sys_tenant` 自身不带租户列，租户列表公开读取用于登录选择，租户维护受权限控制。
- Pigx 的租户插件不是“所有表一律过滤”，而是通过租户表集合、实体标记和 `TenantBroker.noneAs/runAs` 控制过滤边界。
- Pigx 新租户会从默认租户复制菜单、字典、客户端、参数等数据；Mango 当前正式模型不采用复制菜单/字典的方式，应用、菜单、接口资源、基础字典、行政区划作为平台元数据全局共享，租户侧只通过角色授权决定可见范围。

### 后端边界

- 应用、菜单、接口资源、基础字典、行政区划保持平台元数据，不复制到租户。
- 普通租户通过角色菜单授权获得“当前用户菜单”，但不能访问平台元数据维护接口。
- 登录租户选项公开读取，租户维护接口仍受权限控制。
- 字典选项与行政区划分级树属于前端基础选择能力，登录后可读；字典维护仍受权限控制。

### 当前验证结果

- `GET /system/tenant/login-options` 公开可访问，返回芒果集团、A 公司、B 公司、C 公司。
- 芒果集团 token：
  - `GET /authorization/apps` 返回 200。
  - `GET /authorization/menus?fmt=tree` 返回 200。
  - `GET /system/dict/type/list` 返回 200。
  - `GET /system/dict/data/list` 返回 200。
  - `GET /system/dict/data/options?typeCode=sys_normal_disable` 返回 200。
  - `GET /system/area/tree?level=1` 返回 200。
- A 公司 token：
  - `GET /authorization/apps` 返回 403。
  - `GET /authorization/menus?fmt=tree` 返回 403。
  - `GET /system/dict/type/list` 返回 403。
  - `GET /system/dict/data/list` 返回 403。
  - `GET /system/dict/data/options?typeCode=sys_normal_disable` 返回 200。
  - `GET /system/area/tree?level=1` 返回 200。
  - `GET /authorization/menus/user?fmt=tree` 返回 200，菜单只包含账号权限、组织人事、审计日志。

### 前端修复

- 动态菜单按用户、租户、应用、登录域、身份上下文缓存。
- 同一 SPA 会话内切换租户重新登录时，重置后端菜单缓存与路由列表，避免沿用上一租户菜单。

### 验证

- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/platform-metadata-isolation.spec.ts --project=chromium` 通过。
  - 回归 `pnpm exec playwright test e2e/specs/org-post-permission.spec.ts e2e/specs/role-permission.spec.ts --project=chromium` 通过。

## 已完成任务：T4 系统管理菜单 E2E 更新

### 验证

- `/authorization/menus/user?fmt=tree` 在芒果集团返回账号权限、组织人事、平台运营、基础数据、审计日志。
- `/authorization/menus/user?fmt=tree` 在 A 公司只返回账号权限、组织人事、审计日志。
- 前端导航按后端用户菜单树渲染：
  - 芒果集团可见平台运营、基础数据、菜单管理、字典管理、行政区划。
  - A 公司不可见平台运营和基础数据。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/menu-navigation.spec.ts --project=chromium` 通过。

## 已完成任务：T5 用户管理能力决策与落地

### 后端改造

- 新增身份用户管理接口：
  - `GET /identity/users/page` -> `system:user:list`
  - `GET /identity/users/detail` -> `system:user:query`
  - `POST /identity/users` -> `system:user:add`
  - `PUT /identity/users` -> `system:user:edit`
  - `DELETE /identity/users` -> `system:user:delete`
  - `PUT /identity/users/status` -> `system:user:status`
  - `PUT /identity/users/password/reset` -> `system:user:reset-password`
- 用户管理范围按当前租户上下文收敛：
  - 当前租户创建的身份用户。
  - 当前租户已有角色绑定的身份用户。
- 新增用户管理菜单与按钮权限迁移 `V15__add_identity_user_management_menu.sql`。
- 用户角色分配复用现有 `POST /authorization/roles/subjects`，权限码为 `authorization:role:assign`。

### 前端改造

- `packages/rbac/src/views/user/index.vue` 从静态数据改为真实接口。
- 支持用户查询、分页、新增、编辑、删除、启停、重置密码、分配角色。
- 用户接口适配后端 `records/current/size` 分页结构。
- 新增/编辑提交对象按后端命令模型拆分，不再把页面 VO 原样提交给更新接口。

### 验证

- 后端直连：
  - A 公司 token `GET /identity/users/page?page=1&size=10` 返回 200。
  - A 公司 token `GET /authorization/menus/user?fmt=tree` 包含“用户管理”。
  - `authorization_api_resource` 已同步 `/identity/users/*` 管理接口。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/user-management.spec.ts --project=chromium` 通过。
  - 回归 `pnpm exec playwright test e2e/specs/org-post-permission.spec.ts e2e/specs/role-permission.spec.ts e2e/specs/platform-metadata-isolation.spec.ts e2e/specs/menu-navigation.spec.ts e2e/specs/user-management.spec.ts --project=chromium`，7 个用例通过。

### 说明

- 本轮先完成“账号权限下的身份用户管理”和“用户角色绑定”闭环。
- 组织/岗位查询与岗位管理已在 T1 完成；用户与组织/岗位的强绑定关系后续需要结合正式人员模型继续设计，不在 T5 中伪造关系。

## 已完成任务：T6 新租户初始化闭环

### 后端改造

- `sys_org.id` 使用 MyBatis-Plus 雪花主键生成，避免新增租户初始化根组织时主键为空。
- 组织编码、岗位编码调整为租户内唯一：
  - `sys_org(tenant_id, org_code)`
  - `org_post(tenant_id, post_code)`
- 内部组织登录上下文默认以当前租户作为 `partyId`，不再由前端固定传 `partyId=1`。
- 存量 `TENANT_MEMBER + INTERNAL_ORG` 角色绑定的 `party_id` 归一为对应 `tenant_id`。

### 验证

- 后端构建：`mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package`
- Flyway：
  - `org.V4__scope_org_post_code_by_tenant.sql`
  - `authorization.V16__normalize_internal_org_party_scope.sql`
- 后端直连：
  - `POST /system/tenant` 返回 200。
  - 新租户生成根组织 1 条、默认岗位 3 条、管理员角色 1 条、角色菜单授权、创建者成员 1 条、成员角色绑定 1 条。
  - 使用新租户编码登录 `admin/admin123` 返回 `ROLE_ADMIN` 和权限列表。
  - 新租户 token 调用 `/authorization/menus?appCode=internal-admin&fmt=tree` 返回 200。

### 前端改造

- 前端登录页、E2E helper 和用户管理页面移除 `partyId=1` 硬编码。
- 用户管理分配角色使用当前登录上下文的 `partyId`，不再兜底到平台租户 `1`。
- 增加新租户初始化 E2E，用真实后端验证新增租户、登录选项、新租户登录和菜单加载。

### 通过结果

- `pnpm exec playwright test e2e/specs/tenant-provisioning.spec.ts --project=chromium` 通过。
- 回归 `pnpm exec playwright test e2e/specs/platform-metadata-isolation.spec.ts e2e/specs/role-permission.spec.ts e2e/specs/user-management.spec.ts --project=chromium`，4 个用例通过。
- `pnpm --filter mango-admin build` 通过。

## 已完成任务：T7 租户管理页面真实接口闭环

### 范围

- 平台管理员维护租户基础资料：
  - 列表：`GET /system/tenant/list`
  - 详情：`GET /system/tenant/detail?id={id}`
  - 新增：`POST /system/tenant`
  - 修改：`PUT /system/tenant`
  - 状态：`PUT /system/tenant/status?id={id}&status={status}`
  - 删除：`DELETE /system/tenant?id={id}`
- 普通租户只能作为登录租户使用，不能维护租户清单。

### 验收

- 平台租户登录后进入 `/system/tenant`，使用真实接口新增临时租户。
- 新增租户展示在列表中，联系人、手机号、邮箱字段与后端 `contact/mobile/email` 正确转换。
- 编辑联系人信息后列表立即刷新并展示新值。
- 禁用后登录选项不再出现该租户；启用后登录选项恢复。
- 删除后列表不再出现该租户。
- A 公司登录后看不到“平台运营/租户管理”入口。
- A 公司 token 直连 `GET /system/tenant/list`、`POST /system/tenant` 返回 403。

### 通过结果

- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/tenant-management.spec.ts --project=chromium` 通过。
  - 回归 `pnpm exec playwright test e2e/specs/tenant-provisioning.spec.ts e2e/specs/platform-metadata-isolation.spec.ts e2e/specs/tenant-management.spec.ts --project=chromium`，5 个用例通过。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。

## 已完成任务：T8 应用管理页面真实接口闭环

### 范围

- 应用入口维护：
  - 列表：`GET /authorization/apps`
  - 详情：`GET /authorization/apps/detail?appId={appId}`
  - 新增：`POST /authorization/apps`
  - 修改：`PUT /authorization/apps`
  - 删除：`DELETE /authorization/apps?appId={appId}`
- 应用登录上下文作为应用入口的一部分维护，不再把登录域、操作者类型写死在前端。
- 应用图标使用图标选择组件，不再手工输入不可控图标名。

### 通过结果

- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/app-management-save.spec.ts --project=chromium` 通过。
- 覆盖点：
  - 编辑已有应用并保存，真实接口返回 200，未出现 401/登录过期。
  - 新增临时应用，选择图标，提交登录上下文。
  - 编辑临时应用名称。
  - 删除临时应用并从列表消失。

## 已完成任务：T9 菜单管理页面真实接口闭环

### 范围

- 菜单资源维护：
  - 列表：`GET /authorization/menus?fmt=list`
  - 新增：`POST /authorization/menus`
  - 修改：`PUT /authorization/menus`
  - 删除：`DELETE /authorization/menus?menuId={menuId}`
- 用户菜单仍使用 `GET /authorization/menus/user?fmt=tree`，不和菜单管理入口混用。

### 后端修复

- 菜单新增时在服务层补齐领域默认值：
  - `appCode` 默认 `internal-admin`
  - `tenantId` 默认平台租户 `1`
  - `parentId` 默认 `0`
  - `menuType` 默认菜单 `2`
  - `sort/status/visible/keepAlive/embedded/delFlag` 按表结构默认语义补齐
- 保持 `menuName`、`menuCode` 为必填，不绕过后端契约。

### 前端修复

- 菜单管理列表使用 `fmt=list` 拉取菜单资源，再由页面本地组树，避免后端树形数据被二次组树破坏。
- 菜单 ID 统一按字符串归一化，适配后端 Long 序列化。
- 菜单新增/编辑表单补充“菜单编码”必填项。
- 菜单 API 提交改为后端命令字段白名单，不再把 `children/meta/groupCode` 等页面字段提交给后端。

### 通过结果

- 后端单测：
  - `mvn -pl mango-platform/mango-authorization/mango-authorization-core -am -Dtest=MenuServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
- 后端构建：
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- 后端直连：
  - `POST /authorization/menus` 返回 200。
  - `DELETE /authorization/menus?menuId=...` 返回 200。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/menu-management.spec.ts --project=chromium` 通过。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。

## 已完成任务：T10 字典管理页面真实接口闭环

### 范围

- 字典类型维护：
  - 列表：`GET /system/dict/type/list`
  - 新增：`POST /system/dict/type`
  - 修改：`PUT /system/dict/type`
  - 删除：`DELETE /system/dict/type?id={id}`
- 字典数据维护：
  - 列表：`GET /system/dict/data/list?typeId={typeId}`
  - 新增：`POST /system/dict/data`
  - 修改：`PUT /system/dict/data`
  - 删除：`DELETE /system/dict/data?id={id}`
- 基础选项读取：
  - `GET /system/dict/data/options?typeCode={typeCode}`

### 后端修复

- 新增字典类型默认 `status=1`，避免页面新增时因未显式传状态导致不可用。
- 新增字典数据默认 `sort=0`、`status=1`，保持基础字段语义稳定。
- 删除字典类型前校验该类型下是否还有字典数据；存在数据时返回失败，避免产生孤儿字典数据。

### 前端修复

- 字典类型列表补充编辑、删除操作。
- 删除当前选中的字典类型后，清空右侧字典数据并重新加载类型列表。
- 字典 API 注释统一到真实路径 `/system/dict`。
- E2E 中消息断言改为定位最新可见消息，避免连续操作时多个 Element Plus toast 导致 strict mode 误判。

### 通过结果

- 后端编译：
  - `mvn -pl mango-platform/mango-system/mango-system-core -am -DskipTests compile` 通过。
- 后端构建：
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/dict-management.spec.ts --project=chromium` 通过。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。

## 已完成任务：T11 行政区划管理页面真实接口闭环

### 范围

- 行政区划选择器读取：
  - `GET /system/area/tree?type={type}`
  - `GET /system/area/children?parentId={parentId}`
  - `GET /system/area/adcode?adcode={adcode}`
- 行政区划维护：
  - 详情：`GET /system/area/detail?id={id}`
  - 新增：`POST /system/area`
  - 修改：`PUT /system/area`
  - 删除：`DELETE /system/area?id={id}`

### 后端修复

- 行政区划接口补齐访问声明：
  - `tree`、`children`、`adcode`、`active` 为登录接口，服务前端选择器和基础读取。
  - `detail`、`create`、`update`、`delete` 为权限接口。
- 新增迁移 `authorization.V17__add_area_button_permissions.sql`，给平台管理员分配行政区划查询、新增、修改、删除按钮权限。
- 保持标准行政区划保护规则：标准区划不能删除，标准区划编码不能修改；E2E 只维护自定义区划。

### 前端修复

- 行政区划管理页根列表改为 `GET /system/area/children?parentId=0`，使用完整 `SysArea` 实体逐级加载。
- `/system/area/tree` 继续保留给选择器，避免管理页把选择器节点当完整管理实体使用。
- 前端区划 ID 类型支持字符串或数字，适配后端 Long 序列化。

### 通过结果

- 后端编译：
  - `mvn -pl mango-platform/mango-system/mango-system-core -am -DskipTests compile` 通过。
- 后端构建：
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- Flyway：
  - `authorization.V17__add_area_button_permissions.sql` 已执行。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/area-management.spec.ts --project=chromium` 通过。
  - 回归 `pnpm exec playwright test e2e/specs/platform-metadata-isolation.spec.ts e2e/specs/dict-management.spec.ts e2e/specs/area-management.spec.ts --project=chromium` 通过。

## 已完成任务：T12 系统配置页面真实接口闭环

### 范围

- 系统配置维护：
  - 列表：`GET /system/config/list`
  - 详情：`GET /system/config/detail?id={id}`
  - 新增：`POST /system/config`
  - 修改：`PUT /system/config`
  - 删除：`DELETE /system/config?id={id}`
  - 快速修改值：`PUT /system/config/value?id={id}&value={value}`
  - 类型读取：`GET /system/config/type?type={type}`、`GET /system/config/groups`

### 后端修复

- 系统配置纳入平台基础元数据边界，`sys_config` 不参与租户行级过滤，避免平台配置被租户上下文误隔离。
- 新增迁移 `authorization.V18__add_config_button_permissions.sql`，给平台管理员分配系统配置查询、新增、修改、删除按钮权限。

### 前端修复

- 参数配置页新增、编辑、删除真正调用 `paramApi/configApi`，不再只显示成功提示。
- 系统参数列表按页面查询条件调用 `/system/config/list`，不再固定只查 `BUSINESS`。
- 系统配置提交改为后端命令字段白名单，避免把 `configGroup/description` 等页面字段提交给后端导致 400。
- E2E 断言限定当前可见 tab，避免 Element Plus 隐藏 tab 的表格 DOM 干扰。

### 通过结果

- 后端编译：
  - `mvn -pl mango-platform/mango-system/mango-system-core -am -DskipTests compile` 通过。
- 后端构建：
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- Flyway：
  - `authorization.V18__add_config_button_permissions.sql` 已执行。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/config-management.spec.ts --project=chromium` 通过。

## 已完成任务：T13 路由管理页面真实接口闭环

### 范围

- 路由配置维护：
  - 列表：`GET /system/route/list`
  - 详情：`GET /system/route/detail?id={id}`
  - 新增：`POST /system/route`
  - 修改：`PUT /system/route`
  - 删除：`DELETE /system/route?id={id}`
  - 排序：`PUT /system/route/sort`

### 后端修复

- `sys_route_conf` 纳入平台运行元数据边界，不参与租户行级过滤。
- `GET /system/route/list` 支持 `@ParameterObject SysRoutePo` 查询对象，按名称、路径、类型、状态过滤。
- `PUT /system/route/sort` 改为按命令对象 `ids` 更新排序。
- 新增迁移 `authorization.V20__fix_route_management_menu_scope.sql`：
  - 纠正 `V19` 复用菜单 ID `20` 的问题，将 `20` 恢复为“用户管理”。
  - 路由管理改为菜单 ID `21`，按钮权限改为 `21001..21004`。
  - 清理普通租户错误获得的路由管理授权。
- 新租户授权初始化改为租户默认菜单白名单，避免把平台运行配置菜单授予普通租户。

### 前端修复

- 路由管理页收敛为后端真实模型：`routeName/routePath/routeType/routeDesc/sort/status`。
- 去除前端原有父级路由、组件路径、图标、权限标识、树结构等后端不支持字段。
- 路由类型、状态使用字典渲染，不再硬编码 label。
- 路由 API 提交改为字段白名单，避免页面字段污染后端命令。

### 通过结果

- 后端构建：
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- Flyway：
  - `authorization.V19__add_route_management_menu.sql` 已执行。
  - `authorization.V20__fix_route_management_menu_scope.sql` 已执行。
- 后端数据核验：
  - 菜单 `20` 为“用户管理”。
  - 菜单 `21` 为“路由管理”。
  - 普通租户只保留用户管理授权，没有路由管理授权。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/route-management.spec.ts --project=chromium` 通过。
  - 回归 `pnpm exec playwright test e2e/specs/platform-metadata-isolation.spec.ts e2e/specs/config-management.spec.ts e2e/specs/dict-management.spec.ts e2e/specs/area-management.spec.ts e2e/specs/route-management.spec.ts --project=chromium`，10 个用例通过。

## 已完成任务：T14 审计日志真实写入与查询闭环

### 范围

- 登录日志：
  - 列表：`GET /system/log/login/list`
  - 详情：`GET /system/log/login/detail?id={id}`
  - 统计：`GET /system/log/login/statistics`
  - 清理：`DELETE /system/log/login/clean?retentionDays={days}`
- 操作日志：
  - 列表：`GET /system/log/operation/list`
  - 详情：`GET /system/log/operation/detail?id={id}`
  - 清理：`DELETE /system/log/operation/clean?retentionDays={days}`

### 后端修复

- 登录接口写入真实登录日志，成功和失败都记录。
- 操作日志切面按 `@Log` 注解写入真实操作日志，覆盖系统配置、字典、路由、租户等维护接口。
- 日志查询改为后端分页，支持关键字、状态、用户、时间范围过滤。
- 日志按租户归属隔离：普通租户只看本租户日志；平台租户用于平台审计，可看全量日志。
- 清理接口改为真实删除，支持 `retentionDays` 保留天数。
- 修正操作日志字段语义：
  - `method` 表示 HTTP Method，例如 `GET/POST/PUT/DELETE`。
  - `handler_method` 表示 Java 处理器方法，例如 `io.mango.system.starter.controller.SysRouteController.create`。
  - 存量误写入 `method` 的 Java 处理器方法迁移到 `handler_method`，旧 `method` 置空，避免误导。
- 新增迁移：
  - `system.V12__add_operation_log_handler_method.sql`：新增 `handler_method`，修正存量字段语义。
  - `authorization.V21__add_log_clean_permissions.sql`：补齐日志清理按钮权限。
  - `authorization.V22__fix_log_page_menu_permissions.sql`：修正日志页面菜单权限码，与后端接口权限保持一致。

### 前端修复

- 登录日志、操作日志 API 去掉前端本地分页和过滤，改为调用后端分页接口。
- 兼容后端分页结构 `{list,total,page,size}`。
- 兼容后端 `LocalDateTime` 数组格式，页面统一显示为 `YYYY-MM-DD HH:mm:ss`。
- 清理日志弹窗传递 `retentionDays` 参数，不再使用旧的 `days` 参数。
- 操作日志详情新增“处理器方法”，列表“请求方法”只展示 HTTP Method。

### 通过结果

- 后端构建：
  - `mvn -pl mango-app/monolith/mango-monolith-app -am -DskipTests package` 通过。
- Flyway：
  - `authorization.V21__add_log_clean_permissions.sql` 已执行。
  - `authorization.V22__fix_log_page_menu_permissions.sql` 已执行。
- 后端直连：
  - `GET /system/log/login/list?page=1&size=5` 返回真实登录日志。
  - `GET /system/log/login/statistics` 返回真实统计。
  - 通过新增/删除系统路由触发 `新增系统路由`、`删除系统路由` 操作日志。
  - 新增系统路由操作日志 `method=POST`，`handler_method=io.mango.system.starter.controller.SysRouteController.create`。
  - 删除系统路由操作日志 `method=DELETE`，`handler_method=io.mango.system.starter.controller.SysRouteController.delete`。
  - A 公司查询登录日志和操作日志返回 200，且看不到平台租户日志。
- 前端构建：
  - `pnpm --filter mango-admin build` 通过。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/log-management.spec.ts --project=chromium`，3 个用例通过。
  - 操作日志 E2E 已覆盖 `method` 与 `handlerMethod` 字段语义断言。
  - 回归 `pnpm exec playwright test e2e/specs/platform-metadata-isolation.spec.ts e2e/specs/config-management.spec.ts e2e/specs/dict-management.spec.ts e2e/specs/area-management.spec.ts e2e/specs/route-management.spec.ts e2e/specs/log-management.spec.ts --project=chromium`，13 个用例通过。

## 已完成任务：T15 组织架构管理闭环

### 范围

- 组织查询：
  - 树：`GET /org/tree?parentId={parentId}&includeDisabled=true`
  - 直属下级：`GET /org/children?parentId={parentId}`
  - 详情：`GET /org/detail?id={id}`
- 组织维护：
  - 新增：`POST /org`
  - 修改：`PUT /org`
  - 删除：`DELETE /org?id={id}`

### 后端修复

- 新增迁移 `org.V6__repair_builtin_tenant_org_roots.sql`，补齐内置普通租户 A 公司、B 公司、C 公司根组织。
- 修复内置租户成员 `primary_org_id`、`primary_post_id` 为空的问题。
- 后端禁止通过 `POST /org` 手工新增 `pid=0` 根组织，根组织只能由租户初始化/迁移创建。
- `GlobalExceptionHandler` 通过 Web starter 自动注册，`BizException` 标准返回业务错误，不再变成 HTTP 500。

### 前端修复

- 组织管理页支持真实组织新增、编辑、删除。
- 根组织删除按钮禁用，组织表格行同样按根组织规则禁用删除入口。
- 根组织判断兼容后端 Long/字符串序列化后的 `pid`，避免 `"0"` 被误判为非根组织。

### 通过结果

- Flyway：
  - `org.V6__repair_builtin_tenant_org_roots.sql` 已执行成功。
- 后端直连：
  - A 公司 `GET /org/tree?parentId=0&includeDisabled=true` 返回根组织。
  - A 公司 `POST /org` 新增下级组织成功。
  - A 公司 `PUT /org` 修改成功。
  - A 公司 `GET /org/detail` 查询成功。
  - A 公司 `DELETE /org` 删除成功。
  - A 公司 `POST /org` 手工新增 `pid=0` 根组织返回业务 `code=400`，提示“根组织由租户初始化创建，不能手工新增”。
- 前端 E2E：
  - `pnpm exec playwright test e2e/specs/org-management.spec.ts --project=chromium` 通过。

## 阻塞与注意事项

- 普通租户不能拿到 `*:*`，不能通过角色授权拿到租户、应用、菜单维护权限。
