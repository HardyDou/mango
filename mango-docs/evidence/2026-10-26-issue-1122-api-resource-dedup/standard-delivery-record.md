# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1122
- 交付模式：STANDARD
- 需求影响：L2 - 固定 API 资源声明与 Controller 扫描影响授权资源唯一性、权限标识页面和数据库字段
- 方案风险：L2 - 涉及跨模块扫描、资源唯一归属、数据库 migration 和菜单权限
- 最终风险：L2
- 工作区决策：REUSE

## 2. 目标与范围

- 目标：消除固定 `API_RESOURCE` 声明与 Controller 自动扫描的重复登记，提供显式忽略入口，并新增“权限标识”管理页面。
- 成功条件：同一 HTTP 方法和路径只保留一个有效 API 资源；固定声明优先；页面可按模块、领域和来源查看登记结果。
- 处理范围：`mango-authorization` API/核心/Starter/Resource Sync Starter、`@mango/rbac`、`admin-pages`、授权菜单、数据库 migration、模块 README 和能力地图。
- 不处理范围：不关闭 Resource Registry 冲突校验；不删除固定声明或业务目标资源；不处理与本 Issue 无关的 `AuthorizationDemoResourceContractTest` 基线失败；不提交、Push、创建 PR 或发布。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | Resource Sync / 授权模块 | 固定声明和 Controller 扫描命中同一方法、路径 | 固定声明保留为唯一有效记录，扫描记录停用 | 不同方法或路径冲突继续阻断 | 数据库仅一条启用记录，固定模块和来源保留 |
| AC-2 | Controller 开发者 | 类或方法标注 `@ApiResourceIgnore` | 该类型或方法不参与扫描 | 不改变接口访问和固定声明 | 扫描结果缺少目标路由 |
| AC-3 | 权限管理员 | 打开“系统管理 / 权限管理 / 权限标识” | 展示模块、领域、来源、方法、路径、权限标识、访问模式和状态 | 接口失败显示错误提示 | 分页、筛选和来源标识按真实接口返回 |
| AC-4 | 授权管理员 | 分配“权限标识”菜单 | 菜单携带 `system:api-resource:list` 接口权限 | 未授权用户不可访问 | 菜单资源契约和接口权限一致 |
| AC-5 | 数据库升级 | 已有 `authorization_api_resource` 数据 | 新增 `source`、`domain` 字段和索引 | migration 失败时启动失败 | 已有行使用默认值，查询正常 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1 | 固定声明优先于人工登记和 Controller 扫描；同一方法、路径只保留一个启用行，并停用被覆盖行 | `ApiResourceService`、`ApiAccessResourceDiscoverer` | 回退扫描过滤和路由归属逻辑 |
| D-2 | AC-2 | 新增 `@ApiResourceIgnore`，只影响扫描发现 | `ApiAccessResourceDiscoverer` | 回退注解和扫描判断 |
| D-3 | AC-1/AC-3/AC-5 | `authorization_api_resource` 新增 `source`、`domain` 和索引；`domain` 取一级稳定路径段 | `V4__add_api_resource_source_and_domain.sql`、实体和查询 | 已执行 migration 不回滚，旧代码忽略新增列 |
| D-4 | AC-3 | 新增管理员分页和筛选维度接口，使用 `system:api-resource:list` 权限码 | `ApiResourceAdminApi`、`ApiResourceAdminController`、`apiResourceApi` | 回退接口和页面 |
| D-5 | AC-4 | 菜单通过 `AUTH_MENU` 声明，页面 key 为 `system/api-resource/index` | `authorization-common-menu.json`、`admin-pages/src/defaults.ts` | 回退菜单声明和页面注册 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1/D-2 | 1 | `mango-authorization-resource-sync-starter` | 扫描来源、固定覆盖、忽略行为测试通过 |
| I-2 | D-1 | 2 | `mango-authorization-core` | 路由唯一归属、来源和领域写入测试通过 |
| I-3 | D-3 | 3 | `authorization/V4__add_api_resource_source_and_domain.sql` | 增加字段和索引，默认值兼容旧数据 |
| I-4 | D-4 | 4 | `ApiResourceAdminApi`、Controller、Service | 分页和筛选接口测试通过 |
| I-5 | D-5 | 5 | 菜单 JSON、`@mango/rbac`、`admin-pages` | RBAC 构建和 lint 通过 |
| I-6 | AC-1 ~ AC-5 | 6 | 模块 README、Views README、能力地图 | 能力说明与代码一致 |
| I-7 | AC-1 | 7 | `AuthorizationDemoResourceContractTest` | demo 资源契约与当前 `SYNC_ONCE` 基线一致 |
| I-8 | AC-1 | 8 | `ApiAccessResourceProvider` | 固定文件声明覆盖同模块、同方法、同路径的 Controller 扫描结果 |
| I-9 | AC-3 | 9 | `api-resource/index.vue` | 合并重复字段，同时展示接口说明、资源 ID 和创建/更新时间 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1 | 单元测试 | `mvn -o -pl :mango-authorization-resource-sync-starter -am -Dtest=ApiAccessResourceDiscovererTest ... test` | PASS | 本记录第 7 节 |
| AC-1 | 固定文件去重测试 | `ApiAccessResourceProviderTest` | PASS | 固定 YAML 声明的同模块、同方法、同路径不再输出扫描结果 |
| AC-1 | 集成测试 | `mvn -o -pl :mango-authorization-core -am test` | PASS | `ApiResourceServiceImplIntegrationTest` |
| AC-2 | 单元测试 | 同上 Discoverer 测试 | PASS | 类级和方法级忽略用例 |
| AC-3 | 集成测试 | `ApiResourceServiceImplIntegrationTest#pageQueryShouldFilterAndExposeDomainAndSource` | PASS | 模块、领域、来源和关键词筛选 |
| AC-4 | 契约测试 | `mvn -o -pl :mango-authorization-starter -am -Dtest=ModuleDiagnosticPermissionResourceContractTest ... test` | PASS | 菜单和 `apiCodes` 契约 |
| AC-1 ~ AC-5 | 后端模块全量测试 | `mvn -o -pl :mango-authorization-starter -am test`、`mvn -o -pl :mango-authorization-resource-sync-starter -am test` | PASS | 两个模块全量测试通过 |
| AC-5 | 模块校验 | `mvn -o -pl :mango-authorization-api,:mango-authorization-core,:mango-authorization-starter,:mango-authorization-resource-sync-starter -am -DskipTests verify` | PASS | Maven 输出 |
| AC-3 | 前端构建 | `pnpm --filter @mango/rbac... build` | PASS | `@mango/rbac` dist 构建完成 |
| AC-3 | 前端 API 测试 | `pnpm -F @mango/rbac test` | PASS | `apiResource.spec.ts` 覆盖查询参数和分页字段映射 |
| AC-3 | 前端 lint | `pnpm exec eslint packages/rbac/src/views/api-resource/index.vue packages/rbac/src/api/apiResource.ts packages/rbac/src/index.ts packages/admin-pages/src/defaults.ts` | PASS | 无 lint 错误 |
| AC-3 | 浏览器验证 | Playwright 登录后打开 `/#/system/api-resource` | PASS | 页面加载真实接口，显示 Controller 扫描和固定登记来源；表头为接口、接口说明、所属模块、路径领域、权限标识 / 资源编码、访问模式、登记来源、处理入口、状态、创建 / 更新时间、资源 ID；console 无错误 |

## 7. 例外与剩余风险

- 已在本地启动服务验证：后端 `18006`、前端 `30006`、数据库 `mango_dev_mango_issue_1122_006`。
- 已通过浏览器确认“权限标识”菜单可展开、页面可打开、真实接口返回 Controller 扫描和固定登记两类来源。
- 真实 MySQL migration 已在本地工作区数据库执行；未在业务生产副本回放。
- 未提交、Push、创建 PR 或发布。
