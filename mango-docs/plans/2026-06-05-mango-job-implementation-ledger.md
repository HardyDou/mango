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
| JOB-DEV-005 | Sprint 2 | 定义 Job API 契约 | `api` 只放 Command、Query、VO、枚举、`MangoJobApi` | `mango-job-api` | 编译和模块边界检查 | TODO | 待开发 |
| JOB-DEV-006 | Sprint 2 | 定义业务处理器契约 | `MangoJobHandler`、Context、Result 和处理器元数据不依赖 PowerJob | API 或 core 契约类 | 单测覆盖注册和参数校验 | TODO | 待开发 |
| JOB-DEV-007 | Sprint 2 | 实现任务定义 CRUD 和状态流转 | `core` 实现业务服务，不暴露 Entity | Service、Mapper、Controller | 单元和集成测试 | TODO | 待开发 |
| JOB-DEV-008 | Sprint 2 | 实现租户和权限控制 | 查询、操作和触发按租户和权限隔离 | 权限校验、查询条件 | 权限/租户测试 | TODO | 待开发 |
| JOB-DEV-009 | Sprint 2 | 提供实例、日志、Worker、处理器和引擎状态 API | API 前缀 `/{module-path}`，返回 Mango VO | Controller 和 API | API 测试 | TODO | 待开发 |
| JOB-DEV-010 | Sprint 3 | 定义 `MangoJobEngine` SPI | `core` 只依赖 SPI，不依赖 PowerJob 类型 | SPI 接口 | 编译依赖检查 | TODO | 待开发 |
| JOB-DEV-011 | Sprint 3 | 实现 PowerJob Adapter | PowerJob 依赖只在 starter Adapter 包或独立 adapter 模块 | Adapter 实现 | Adapter 单测和集成测试 | TODO | 待开发 |
| JOB-DEV-012 | Sprint 3 | 实现引擎映射和同步状态 | `sync_status`、失败摘要、重试和删除保护 | 映射服务和表字段 | 同步失败补偿测试 | TODO | 待开发 |
| JOB-DEV-013 | Sprint 3 | 实现 PowerJob Server 认证和健康检查 | token、连接状态、版本兼容检查 | 配置和健康接口 | 认证失败和健康检查测试 | TODO | 待开发 |
| JOB-DEV-014 | Sprint 4 | 初始化后台菜单和权限 | 菜单和权限入库，不用前端临时菜单 | 菜单/权限种子 | 登录后菜单和权限按钮验证 | TODO | 待开发 |
| JOB-DEV-015 | Sprint 4 | 实现前端任务定义页面 | 真实 API、加载、空、错误态 | 前端页面和 API client | 前端测试和 E2E | TODO | 待开发 |
| JOB-DEV-016 | Sprint 4 | 实现实例、日志、执行器、告警、引擎状态和处理器页面 | 统一 UI，不嵌入 PowerJob Console | 前端页面 | E2E 和页面走查 | TODO | 待开发 |
| JOB-DEV-017 | Sprint 5 | 接入告警通知 | 使用 Mango Notice，敏感参数脱敏 | 告警规则和通知服务 | 告警触发测试 | TODO | 待开发 |
| JOB-DEV-018 | Sprint 5 | 提供运行态和可观测性 | 任务统计、Worker、失败率、耗时、同步失败 | 指标和状态接口 | 指标读取测试 | TODO | 待开发 |
| JOB-DEV-019 | Sprint 6 | 提供部署配置和 PowerJob Compose 样例 | 覆盖三库、单库降级、Job Center 和 Worker | 部署文档和配置样例 | 启动验证 | TODO | 待开发 |
| JOB-DEV-020 | Sprint 6 | 完成后端回归和质量检查 | Maven test/verify/mango:check | 验证记录 | 命令通过 | TODO | 待开发 |
| JOB-DEV-021 | Sprint 6 | 完成前端回归和构建 | pnpm test/build/E2E | 验证记录 | 命令通过 | TODO | 待开发 |
| JOB-DEV-022 | Sprint 6 | 发布 Maven 模块和前端包 | 发布后消费项目可解析 | 发布记录 | 仓库和消费验证 | TODO | 待开发 |

## 2. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| JOB-DEV-001 | Maven | 模块骨架 | api/core/starter/starter-remote 四模块 | reactor 识别并完成 core/starter/starter-remote 测试 | 不涉及页面 | 不涉及前端网络 | `mvn -pl mango-platform/mango-job/mango-job-core -am test`、`mvn -pl mango-platform/mango-job/mango-job-starter -am test`、`mvn -pl mango-platform/mango-job/mango-job-starter-remote -am test` | DONE |
| JOB-DEV-002 | 模块信息 | 默认数据源 | `module-name=mango-job`、`module-path=/job`、`persistence-datasource=job` | module.properties 只在 starter | 不涉及页面 | 不涉及前端网络 | `META-INF/mango/module.properties` | DONE |
| JOB-DEV-003 | 数据库 | 治理表 | `mango_job_definition` 等 7 张治理表 | 新表包含 `id`、`tenant_id`、`created_by`、`created_at`、`updated_by`、`updated_at` | 不涉及页面 | 不涉及前端网络 | 在 `mango-platform/mango-job/mango-job-core` 执行 `mvn mango:check -Drule=persistence-schema` 通过；全仓同规则被既有模块 276 个问题阻断 | DONE |
| JOB-DEV-004 | 数据库 | 多数据源 | H2 primary + job 双数据源 | resolver 映射 `mango-job -> job`；`mango_job_definition` 只在 job 库；MyBatis-Plus Service 写入 job 库 | 不涉及页面 | 不涉及前端网络 | `MangoJobMultiDataSourceIntegrationTest` | DONE |
| JOB-DEV-005 | API | 契约 | 待开发 | api 不依赖 core/PowerJob | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-006 | API | 处理器契约 | 待开发 | 不暴露 PowerJob 类型 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-007 | API | CRUD/状态 | 待开发 | 状态流转正确 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-008 | API | 租户权限 | 待开发 | 跨租户不可见 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-009 | API | 查询接口 | 待开发 | 返回 Mango VO | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-010 | SPI | 引擎 SPI | 待开发 | core 只依赖 SPI | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-011 | Adapter | PowerJob 集成 | 待开发 | Adapter 能创建/触发任务 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-012 | Adapter | 同步补偿 | 待开发 | 失败可查可重试 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-013 | 运维 | 认证健康 | 待开发 | 认证失败有明确异常 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-014 | 页面/权限 | 菜单权限 | 待开发 | 后端菜单可见，按钮受控 | 待走查 | 待验证 | 待开发 | TODO |
| JOB-DEV-015 | 页面 | 任务定义 | 待开发 | 真实数据增删改查 | 待走查 | 待验证 | 待开发 | TODO |
| JOB-DEV-016 | 页面 | 运行态页面 | 待开发 | 实例/日志/Worker/告警/处理器可查 | 待走查 | 待验证 | 待开发 | TODO |
| JOB-DEV-017 | 通知 | 告警 | 待开发 | 失败触发通知 | 不涉及页面 | 待验证 | 待开发 | TODO |
| JOB-DEV-018 | 运维 | 可观测性 | 待开发 | 指标可读 | 待走查 | 待验证 | 待开发 | TODO |
| JOB-DEV-019 | 部署 | 配置样例 | 待开发 | 三库和降级可启动 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-020 | 后端验证 | 质量检查 | 待开发 | Maven 检查通过 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
| JOB-DEV-021 | 前端验证 | 构建/E2E | 待开发 | pnpm 检查通过 | 待走查 | 待验证 | 待开发 | TODO |
| JOB-DEV-022 | 发布 | 发布消费 | 待开发 | 仓库和消费项目可解析 | 不涉及页面 | 不涉及前端网络 | 待开发 | TODO |
