# Mango Job 任务调度开发交付台账

设计文档：`mango-docs/designs/mango-job-design.md`

开发计划：`mango-docs/plans/2026-06-05-mango-job-development-plan.md`

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| JOB-DEV-001 | Sprint 1 | 新增 `mango-platform/mango-job` 聚合模块 | 按 api/core/starter/starter-remote 拆分 | Maven 模块和 POM | `mvn -pl mango-platform/mango-job/mango-job-core -am test`、starter 和 starter-remote 测试 | DONE | `mango/mango-platform/pom.xml`、`mango/mango-platform/mango-job/pom.xml`、`mango/mango-platform/mango-job/*/pom.xml` |
| JOB-DEV-002 | Sprint 1 | 声明模块信息和默认数据源 | 仅 `mango-job-starter` 提供 `module.properties`，默认 `persistence-datasource=job` | `META-INF/mango/module.properties` | 检查 module-name、module-path、persistence-datasource | DONE | `mango/mango-platform/mango-job/mango-job-starter/src/main/resources/META-INF/mango/module.properties` |
| JOB-DEV-003 | Sprint 1 | 建立 `mango_job` 治理表 | Flyway 路径 `db/migration/mango-job`，满足表结构准入 | migration SQL | `mvn mango:check -Drule=persistence-schema` | DONE | `mango-job-core` 局部 schema check 通过；全仓 schema check 仍被 workflow/authorization/calendar/numgen 既有脚本阻断，需专项治理 |
| JOB-DEV-004 | Sprint 1 | 多数据源迁移到 job 库 | 使用 `mango.persistence.modules.mango-job.datasource=job` | Flyway 集成测试 | MyBatis-Plus/Flyway 真实路由测试 | DONE | `MangoJobMultiDataSourceIntegrationTest` 验证 Flyway 进入 job 库，MyBatis-Plus Service 插入/查询走 job 数据源 |
| JOB-DEV-005 | Sprint 2 | 定义 Job API 契约 | `api` 只放 Command、Query、VO、枚举、`MangoJobApi` | `mango-job-api` | `mvn -pl mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter,mango-platform/mango-job/mango-job-starter-remote -am test` | DONE | `MangoJobApi`、`command`、`query`、`vo` |
| JOB-DEV-006 | Sprint 2 | 定义业务处理器契约 | `MangoJobHandler`、Context、Result 和处理器元数据不依赖 PowerJob | API 契约、core 注册表 | 集成测试覆盖处理器注册和查询 | DONE | `MangoJobHandlerRegistry`、`MangoJobMultiDataSourceIntegrationTest` |
| JOB-DEV-007 | Sprint 2 | 实现任务定义 CRUD 和状态流转 | `core` 实现业务服务，不暴露 Entity | Service、Controller、Feign | 集成测试覆盖创建、分页、详情、状态、触发 | DONE | `MangoJobDefinitionService`、`MangoJobController`、`MangoJobFeignClient` |
| JOB-DEV-008 | Sprint 2 | 实现租户和权限控制 | Job 服务按当前上下文隔离；tenantId 无感知治理拆到 #102 | 租户隔离测试 | 跨租户查询、详情、实例、日志、Worker 不可见 | DONE | `MangoJobMultiDataSourceIntegrationTest`；后续全局租户无感知治理见 GitHub Issue #102 |
| JOB-DEV-009 | Sprint 2 | 提供实例、日志、Worker、处理器和引擎状态 API | API 前缀 `/job`，返回 Mango VO | Controller 和 Feign API | 编译和集成测试覆盖查询服务 | DONE | `MangoJobController`、`MangoJobQueryService`、`MangoJobFeignClient` |
| JOB-DEV-010 | Sprint 3 | 定义 `MangoJobEngine` SPI | `core` 只依赖 SPI，不依赖 PowerJob 类型 | SPI 接口、注册表、同步服务 | 编译依赖检查和 core 集成测试 | DONE | `IMangoJobEngine`、`MangoJobEngineRegistry`、`MangoJobEngineSyncService` |
| JOB-DEV-011 | Sprint 3 | 实现 PowerJob Adapter | PowerJob 依赖只在 starter Adapter 包；Mango 前端/后端不直连 PowerJob Console | Adapter 实现、PowerJob 5.1.2 SDK 依赖 | Adapter 单测和 starter 编译测试 | DONE | `PowerJobEngineAdapter`、`PowerJobClientOperations`、`PowerJobEngineAdapterTest` |
| JOB-DEV-012 | Sprint 3 | 实现引擎映射和同步状态 | `sync_status`、失败摘要、映射表；失败可查，重试入口后续迭代 | 映射写入和状态同步 | core 集成测试覆盖同步、触发和映射 | DONE | `MangoJobEngineSyncService`、`MangoJobMultiDataSourceIntegrationTest` |
| JOB-DEV-013 | Sprint 3 | 实现 PowerJob Server 认证和健康检查 | 显式开启才接入 PowerJob；配置缺失启动失败；SDK client 懒加载，Job Center 不可用时健康检查返回失败 | 配置、懒加载 SDK 操作、健康检查 | 自动配置和健康失败测试 | DONE | `PowerJobAutoConfiguration`、`PowerJobAutoConfigurationTest`、`PowerJobEngineAdapterTest` |
| JOB-DEV-014 | Sprint 4 | 初始化后台菜单和权限 | 菜单和权限入库，不用前端临时菜单；菜单路径为 `平台能力 / 任务管理` | 菜单/权限种子、Controller 权限标记、单体和平台微服务装配 | 登录后菜单和权限按钮验证 | DONE | `V39__job_menu.sql`、`MangoJobController`、`mango-admin-starter/pom.xml`、`mango-platform-app/pom.xml` |
| JOB-DEV-015 | Sprint 4 | 实现前端任务定义页面 | 真实 API、加载、空、错误态；前端包独立提供 `@mango/job/style.css` | 前端页面和 API client | 前端构建、登录后页面走查和 CRUD 联调 | DONE | `mango-ui/packages/job/src/views/definition/index.vue`、`mango-ui/packages/job/src/api/job.ts` |
| JOB-DEV-016 | Sprint 4 | 实现实例、日志、执行器、引擎状态和处理器页面 | 统一 UI，不嵌入 PowerJob Console；告警规则和通知归入 JOB-DEV-017 | 前端页面 | 前端构建、登录后页面走查和真实接口联调 | DONE | `mango-ui/packages/job/src/views/instance/index.vue`、`log/index.vue`、`worker/index.vue`、`handler/index.vue`、`engine/index.vue` |
| JOB-DEV-017 | Sprint 5 | 接入告警通知 | 使用 Mango Notice，敏感参数脱敏 | 告警规则和通知服务 | 告警触发测试 | TODO | 待开发 |
| JOB-DEV-018 | Sprint 5 | 提供运行态和可观测性 | 任务统计、Worker、失败率、耗时、同步失败 | 指标和状态接口 | 指标读取测试 | TODO | 待开发 |
| JOB-DEV-019 | Sprint 6 | 提供部署配置和 PowerJob Compose 样例 | 覆盖三库、单库降级、Job Center 和 Worker | 部署文档和配置样例 | 启动验证 | TODO | 待开发 |
| JOB-DEV-020 | Sprint 6 | 完成后端回归和质量检查 | Maven test/verify/mango:check | 验证记录 | 命令通过 | TODO | 待开发 |
| JOB-DEV-021 | Sprint 6 | 完成前端回归和构建 | pnpm test/build/E2E | 验证记录 | 命令通过 | DONE | `pnpm --dir mango-ui admin:styles:check`、`pnpm -F @mango/job build`、`pnpm -F mango-admin build`、`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm --dir mango-ui/apps/mango-admin exec playwright test e2e/specs/job-management.spec.ts --project=chromium --reporter=line`、`mango-docs/evidence/2026-06-05-mango-job-ui-e2e/acceptance-evidence.md` |
| JOB-DEV-022 | Sprint 6 | 发布 Maven 模块和前端包 | 发布后消费项目可解析 | 发布记录 | 仓库和消费验证 | TODO | 待开发 |

## 2. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| JOB-DEV-001 | Maven | 模块骨架 | api/core/starter/starter-remote 四模块 | reactor 识别并完成 core/starter/starter-remote 测试 | 不涉及页面 | 不涉及前端网络 | `mvn -pl mango-platform/mango-job/mango-job-core -am test`、`mvn -pl mango-platform/mango-job/mango-job-starter -am test`、`mvn -pl mango-platform/mango-job/mango-job-starter-remote -am test` | DONE |
| JOB-DEV-002 | 模块信息 | 默认数据源 | `module-name=mango-job`、`module-path=/job`、`persistence-datasource=job` | module.properties 只在 starter | 不涉及页面 | 不涉及前端网络 | `META-INF/mango/module.properties` | DONE |
| JOB-DEV-003 | 数据库 | 治理表 | `mango_job_definition` 等 7 张治理表 | 新表包含 `id`、`tenant_id`、`created_by`、`created_at`、`updated_by`、`updated_at` | 不涉及页面 | 不涉及前端网络 | 在 `mango-platform/mango-job/mango-job-core` 执行 `mvn mango:check -Drule=persistence-schema` 通过；全仓同规则被既有模块 276 个问题阻断 | DONE |
| JOB-DEV-004 | 数据库 | 多数据源 | H2 primary + job 双数据源 | resolver 映射 `mango-job -> job`；`mango_job_definition` 只在 job 库；MyBatis-Plus Service 写入 job 库 | 不涉及页面 | 不涉及前端网络 | `MangoJobMultiDataSourceIntegrationTest` | DONE |
| JOB-DEV-005 | API | 契约 | `MangoJobApi`、Command、Query、VO | api 不依赖 core/PowerJob；Controller/Feign 复用契约 | 不涉及页面 | 不涉及前端网络 | `mvn -pl mango-platform/mango-job/mango-job-api,mango-platform/mango-job/mango-job-core,mango-platform/mango-job/mango-job-starter,mango-platform/mango-job/mango-job-starter-remote -am test` | DONE |
| JOB-DEV-006 | API | 处理器契约 | `syncOrderHandler` 测试 Bean | Handler 注册表返回 `syncOrderHandler`；契约不暴露 PowerJob 类型 | 不涉及页面 | 不涉及前端网络 | `MangoJobMultiDataSourceIntegrationTest.definitionService_shouldCreateUpdateTriggerWithMybatisPlusOnJobDatasource` | DONE |
| JOB-DEV-007 | API | CRUD/状态 | `sync-order` | 创建默认为 DRAFT；分页可查；DRAFT -> ENABLED；手动触发生成实例和操作日志 | 不涉及页面 | 不涉及前端网络 | `MangoJobMultiDataSourceIntegrationTest.definitionService_shouldCreateUpdateTriggerWithMybatisPlusOnJobDatasource` | DONE |
| JOB-DEV-008 | API | 租户权限 | tenant-b 创建，tenant-c 查询 | tenant-c 分页为 0；详情返回任务不存在；实例、日志、Worker 均不可见。当前为 Job 级隔离验证，tenantId 无感知统一治理已拆 Issue #102 | 不涉及页面 | 不涉及前端网络 | `MangoJobMultiDataSourceIntegrationTest.queryService_shouldReadLogsWorkersAndKeepTenantIsolationOnJobDatasource` | DONE |
| JOB-DEV-009 | API | 查询接口 | 实例、日志索引、Worker 快照、处理器、引擎状态 | 查询服务返回 Mango VO；POWERJOB PENDING 计数为 1；Controller/Feign 编译通过 | 不涉及页面 | 不涉及前端网络 | `MangoJobQueryService`、`MangoJobController`、`MangoJobFeignClient` | DONE |
| JOB-DEV-010 | SPI | 引擎 SPI | `IMangoJobEngine`、`MangoJobEngineRequest`、`MangoJobEngineResult`、`MangoJobTriggerRequest` | core 层只依赖 Mango SPI，不出现 PowerJob 类型；无引擎时定义保持 `PENDING` | 不涉及页面 | 不涉及前端网络 | `mvn -pl mango-platform/mango-job/mango-job-core -am test` | DONE |
| JOB-DEV-011 | Adapter | PowerJob 集成 | PowerJob 5.1.2；Cron/FIXED_RATE/MANUAL、BUILTIN/SCRIPT、启停和手动触发 | SaveJobInfoRequest 映射 appId/jobName/cron/processor/concurrency/timeout；RunJobRequest 映射 appId/jobId/outerKey | 不涉及页面 | 不涉及前端网络 | `PowerJobEngineAdapterTest`、`mvn -pl mango-platform/mango-job/mango-job-starter -am test` | DONE |
| JOB-DEV-012 | Adapter | 同步补偿 | 测试引擎返回 appId=10001、jobId=90001、instanceId=80001 | 创建后 `sync_status=SYNCED`、写入 definition engine 字段和 engine mapping；触发后写入 instance engine id 和实例映射；失败路径返回 `FAILED` 摘要 | 不涉及页面 | 不涉及前端网络 | `MangoJobEngineSyncService`、`MangoJobMultiDataSourceIntegrationTest` | DONE |
| JOB-DEV-013 | 运维 | 认证健康 | `mango.job.powerjob.enabled=true`、完整 app/server/password/appId 配置；Job Center 不可用模拟异常 | 默认不注册 PowerJob Adapter；显式开启缺配置启动失败；完整配置注册懒加载 operations；健康检查异常返回失败摘要 | 不涉及页面 | 不涉及前端网络 | `PowerJobAutoConfigurationTest`、`PowerJobEngineAdapterTest.healthShouldExposePowerJobException` | DONE |
| JOB-DEV-014 | 页面/权限 | 菜单权限 | `mango-job` 菜单、`job:*` 权限点 | 后端种子写入 `平台能力 / 任务管理`；接口使用 `@ApiAccess(PERMISSION)`；按钮使用同名 `v-auth`；单体后端装配 Job Controller | 登录后 `平台能力 / 任务管理` 菜单可见 | `/authorization/menus/user?appCode=internal-admin&fmt=tree` 返回 `任务管理`；页面无 401/403 | `V39__job_menu.sql`、`MangoJobController`、`mvn -pl mango-platform/mango-job/mango-job-starter -am mango:check -Drule=permission-param`、临时 Playwright 走查 1 passed | DONE |
| JOB-DEV-015 | 页面 | 任务定义 | 真实 `/job/definitions/**` API | 列表、搜索、新增、编辑、状态流转、删除、手动触发均调用真实接口；具备加载、空、错误和重试态 | 登录后访问 `/#/job/definition`，页面标题、工具栏、表格、空态正常 | `/api/job/definitions/page` 返回 200；页面无 401/403/加载失败 | `pnpm -F @mango/job build`、`pnpm -F mango-admin build`、临时 Playwright 走查 1 passed | DONE |
| JOB-DEV-016 | 页面 | 运行态页面 | 真实 `/job/instances/page`、`/job/logs/page`、`/job/workers/page`、`/job/handlers`、`/job/engines/status` API | 实例、日志、Worker、处理器、引擎状态页面可查；告警通知不在本项，归 JOB-DEV-017 | 登录后访问 5 个运行态页面，标题、筛选、表格、空态正常 | 五组 `/api/job/**` 查询接口返回 200；页面无 401/403/加载失败 | `pnpm -F @mango/job build`、`pnpm -F @mango/admin build`、`pnpm -F mango-admin build`、临时 Playwright 走查 1 passed | DONE |
| JOB-DEV-017 | 通知 | 告警 | 待开发 | 失败触发通知 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-018 | 运维 | 可观测性 | 待开发 | 指标可读 | 待走查 | 待验证 | 待开发 | TODO |
| JOB-DEV-019 | 部署 | 配置样例 | 待开发 | 三库和降级可启动 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-020 | 后端验证 | 质量检查 | 待开发 | Maven 检查通过 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-021 | 前端验证 | 构建/E2E | `mango_job_example_` 三条示例任务和 `mango_job_e2e_tmp_<timestamp>` 临时任务 | 样式聚合清单包含 `@mango/job/style.css`；Job 包和 mango-admin 构建通过；任务定义 UI 完成新增、查询、编辑、状态流转、触发、删除；各 Job 列表页调用真实 API | 任务定义标题区按钮在右侧，搜索区横向紧凑，表格首屏可见；实例/日志/Worker 搜索区高度受控 | Job API 无 4xx/5xx，E2E 收集到的 console error 为空 | `mango-docs/evidence/2026-06-05-mango-job-ui-e2e/acceptance-evidence.md`、`job-definition.png`、`job-instance.png`、`job-engine.png` | DONE |
| JOB-DEV-022 | 发布 | 发布消费 | 待开发 | 仓库和消费项目可解析 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
