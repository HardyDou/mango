# Mango Job 任务调度交付台账

设计文档：`mango-docs/designs/mango-job-design.md`

开发计划：`mango-docs/plans/2026-06-05-mango-job-development-plan.md`

## 1. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| JOB-DES-001 | 用户要求 | 补全 Job 必须研发资料 | 新增设计文档、开发计划和交付台账 | 设计文档、开发计划、台账 | 文件存在且结构完整 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-002 | 用户要求 | 调研主流 Job 组件 | 覆盖 PowerJob、XXL-JOB、Quartz、ElasticJob、JobRunr、Spring Batch、Temporal、Airflow | 设计文档组件调研章节 | 人工检查调研表 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-003 | 用户要求 | 说明为何不自研 | 采用 Mango 契约和 UI，底层 PowerJob Adapter，不自研调度引擎 | 选型结论 | 人工检查设计结论 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-004 | 用户要求 | 说明是否可直接集成 XXL-JOB 或 PowerJob | PowerJob 优先，XXL-JOB 保留备选 Adapter；不直接暴露第三方 UI | 组件调研和 Adapter 设计 | 人工检查集成边界 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-005 | 用户要求 | 说明是否统一 UI | Mango 提供统一后台菜单、权限、页面和 API | 前端统一 UI 章节 | 人工检查页面清单 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-006 | 用户要求 | 支持灵活部署 | 支持单体 `Job Center + Worker`、独立 Job Center、远程 Worker、共享 PowerJob Server 和单库降级 | 部署形态章节 | 人工检查部署矩阵 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-007 | 用户要求 | 数据库独立 | `mango` 与 `mango_job` 隔离；PowerJob 内部表支持同库共置或独立 `powerjob` 库/schema | 数据库设计章节 | 人工检查数据库边界 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-008 | 用户要求 | 结合 Mango 特性集成 | 接入租户、权限、菜单、审计、Notice、多数据源和模块机制 | 模块边界、权限租户、告警章节 | 人工检查 Mango 能力清单 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-009 | 用户要求 | 前后端同步集成 | 后端 API、菜单权限和前端页面同步拆分 Sprint | API 设计、UI 设计、开发计划 | 人工检查 Sprint 4 | DONE | `mango-docs/plans/2026-06-05-mango-job-development-plan.md` |
| JOB-DES-010 | 设计说明 | 明确模块边界 | 新增 `mango-platform/mango-job`，拆分 api/core/starter/starter-remote | 模块边界章节 | 人工检查依赖方向 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-011 | 设计说明 | 明确接口变化 | 设计 `/job` API 和 Command、Query、VO 命名 | 后端 API 章节 | 人工检查 API 表 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-012 | 设计说明 | 明确数据变化 | 设计 Job 定义、实例、日志、Worker、告警、映射表 | 数据库设计章节 | 人工检查表清单 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-013 | 设计说明 | 明确菜单、页面、权限变化 | 设计任务定义、实例、日志、执行器、告警、引擎状态页面和权限码 | UI 和权限章节 | 人工检查菜单权限清单 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-014 | 设计说明 | 明确测试范围 | 覆盖后端、前端、集成、E2E、发布验证 | 测试范围和测试矩阵 | 人工检查测试矩阵 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-015 | PMO 要求 | 计划可执行 | 按 Sprint 0 到 Sprint 6 拆分，每个 Sprint 有目标、改动、验收和验证命令 | 开发计划 | 人工检查 Sprint 结构 | DONE | `mango-docs/plans/2026-06-05-mango-job-development-plan.md` |
| JOB-DES-016 | PMO 要求 | 交付台账原子化 | 将用户要求和设计要求拆为独立台账项 | 交付台账 | 运行 delivery-contract-check | DONE | `mango-docs/plans/2026-06-05-mango-job-delivery-ledger.md` |
| JOB-DES-017 | 用户要求 | 组织专家评审 | 新增 AI 专家预评审记录，覆盖架构、数据、安全、前端、运维和 PMO 视角 | 评审记录 | 文档存在且列出结论、修订项、用户确认项和开发前复核项 | DONE | `mango-docs/plans/2026-06-05-mango-job-review-record.md` |
| JOB-DES-018 | 专家评审 | 修正依赖方向和部署契约 | 按 Mango 模块规范补充依赖图、Adapter 边界、starter/starter-remote 和 Feign 规则 | 设计文档 | 人工检查依赖方向和部署矩阵 | DONE | `mango-docs/designs/mango-job-design.md` |
| JOB-DES-019 | 专家评审 | 建立开发交付台账 | 将 Sprint 1 到 Sprint 6 拆为开发验收原子项，进入开发前状态为待开发 | 开发交付台账 | plan 模式台账检查 | DONE | `mango-docs/plans/2026-06-05-mango-job-implementation-ledger.md` |
| JOB-DES-020 | 用户确认 | 固化 Job 待确认决策 | 版本、安全、部署、HTTP/SCRIPT、Handler、PowerJob 同库共置、菜单、Notice、数据库命名均写入设计与计划 | 设计文档、开发计划、评审记录 | 人工检查用户确认项 | DONE | `mango-docs/plans/2026-06-05-mango-job-review-record.md` |

## 2. 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| JOB-DES-001 | 文档 | 必须资料补全 | 不适用 | 三个文件已创建 | 不涉及页面 | 不涉及前端网络 | 文件路径 | PASS |
| JOB-DES-002 | 文档 | 组件调研 | 不适用 | 调研表覆盖 8 类组件 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-003 | 文档 | 自研边界 | 不适用 | 结论明确不自研调度引擎 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-004 | 文档 | 第三方集成边界 | 不适用 | PowerJob 优先，XXL-JOB 备选 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-005 | 文档 | 统一 UI | 不适用 | 页面清单完整 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-006 | 文档 | 灵活部署 | 不适用 | 部署矩阵存在 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-007 | 文档 | 数据库独立 | 不适用 | 主库与 Job 库隔离，PowerJob 同库共置边界明确 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-008 | 文档 | Mango 特性集成 | 不适用 | 租户、权限、菜单、审计、Notice、多数据源均覆盖 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-009 | 文档 | 前后端同步 | 不适用 | 后端和前端 Sprint 均存在 | 不涉及页面 | 不涉及前端网络 | 开发计划 | PASS |
| JOB-DES-010 | 文档 | 模块边界 | 不适用 | api/core/starter/starter-remote 边界明确 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-011 | 文档 | API 变化 | 不适用 | `/job` API 清单存在 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-012 | 文档 | 数据变化 | 不适用 | 表清单存在 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-013 | 文档 | 菜单权限 | 不适用 | 菜单、页面、权限码存在 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-014 | 文档 | 测试范围 | 不适用 | 测试矩阵存在 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-015 | 文档 | Sprint 计划 | 不适用 | Sprint 0 到 6 均有验收和验证命令 | 不涉及页面 | 不涉及前端网络 | 开发计划 | PASS |
| JOB-DES-016 | 文档 | 台账检查 | 不适用 | 台账满足 PMO 检查 | 不涉及页面 | 不涉及前端网络 | 检查命令输出 | PASS |
| JOB-DES-017 | 文档 | 专家预评审 | 不适用 | 评审记录包含通过项、修订项、用户确认项和开发前复核项 | 不涉及页面 | 不涉及前端网络 | 评审记录 | PASS |
| JOB-DES-018 | 文档 | 依赖方向修订 | 不适用 | 设计文档符合 Mango 模块依赖规则 | 不涉及页面 | 不涉及前端网络 | 设计文档 | PASS |
| JOB-DES-019 | 文档 | 开发台账 | 不适用 | 开发台账包含 Sprint 1 到 6 原子项 | 不涉及页面 | 不涉及前端网络 | 台账检查输出 | PASS |
| JOB-DES-020 | 文档 | 用户确认决策 | 不适用 | 版本、安全、部署、HTTP/SCRIPT、Handler、PowerJob 同库共置、菜单、Notice、数据库命名均写入研发资料 | 不涉及页面 | 不涉及前端网络 | 评审记录 | PASS |
