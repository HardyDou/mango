# Mango Job 任务调度专家预评审记录

## 1. 评审类型

AI 预评审结果，待人工评审会确认。

本记录不表示已经完成真实会议评审；用于在正式评审前给出可执行的初步结论、风险项和文档修订结果。

## 2. 评审输入

- `mango-docs/designs/mango-job-design.md`
- `mango-docs/plans/2026-06-05-mango-job-development-plan.md`
- `mango-docs/plans/2026-06-05-mango-job-delivery-ledger.md`
- `mango-docs/designs/mango-multi-datasource-foundation-design.md`
- `mango/mango-infra/mango-infra-persistence/README.md`

## 3. 评审角色

| 角色 | 关注点 |
|---|---|
| 平台架构专家 | 模块边界、依赖方向、PowerJob Adapter 隔离、微服务和单体部署。 |
| 数据架构专家 | `mango`、`mango_job`、`powerjob` 边界、索引、跨库一致性和 Flyway。 |
| 安全与权限专家 | 租户隔离、权限码、敏感参数、PowerJob Server 认证。 |
| 前端架构专家 | 统一 UI、菜单注册、页面能力、单体和 Shell 一致性。 |
| SRE/运维专家 | 部署形态、健康检查、日志、告警、发布和回滚。 |
| PMO 专家 | 交付契约、台账原子项、文档归属和 Sprint 可执行性。 |

## 4. 总体结论

预评审结论：有条件通过。

方向正确，可以作为 Mango Job 后续开发基线。用户已确认 PowerJob 版本策略、认证方式、部署拓扑、菜单入口、告警通知归属和 PowerJob 同库共置边界。进入开发前仍需从 `main` 创建独立 worktree 和任务分支，并复核 Maven 坐标、Server 镜像、安全公告和兼容 API。已在预评审中修订以下文档缺口：

- 补充业务处理器注册契约。
- 补充 `mango` 主库、`mango_job` Job 库、PowerJob 内部表同库共置或独立库/schema 边界。
- 补充 PowerJob 认证配置和同步失败补偿。
- 补充任务定义唯一约束、实例索引和引擎映射索引。
- 细化告警 API 和处理器页面。
- 补充外部资料复核日期和资料使用边界。
- 修正模块依赖方向，避免误导实现成 `api -> core`。
- 新增开发交付台账，覆盖 Sprint 1 到 Sprint 6。
- 固化用户确认项：PowerJob 当前基线 `5.1.2`；复用 Mango 内部调用安全机制；支持单体 `Job Center + Worker`、独立 Job Center、远程 Worker；菜单入口为 `平台能力 -> 任务管理`；告警接入 `mango-notice`；PowerJob 可与 `mango_job` 同库共置；模块独立库按 `mango_{module}` 命名。

## 5. 通过项

| 议题 | 结论 |
|---|---|
| 是否应该自研调度引擎 | 不自研。Mango 做契约、治理和 UI，底层优先 PowerJob。 |
| 是否直接集成 PowerJob Console | 不直接集成。正式 UI 由 Mango 统一承载。 |
| 数据库是否独立 | `mango` 主库与 `mango_job` Job 库隔离；`mango_job` 按 `mango_{module}` 规则命名；PowerJob 内部表可与 `mango_job` 同库共置，也可使用独立 `powerjob` 库或 schema。 |
| 是否支持灵活部署 | 支持单体 `Job Center + Worker`、独立 Job Center、远程 Worker、共享 PowerJob Server 和受控单库降级。 |
| 前后端是否同步 | 计划包含后端 API、菜单权限、前端页面和 E2E。 |
| PMO 台账是否可检查 | 台账 16 项，全部 DONE，0 EXCEPTION。 |
| PowerJob 版本 | 当前实现基线锁定最新稳定版 `5.1.2`，编码前复核 Maven 坐标和 Server 镜像。 |
| 认证方式 | Mango 内部接口复用 Mango 内部调用安全机制；PowerJob Server 保留访问令牌、内网 ACL 和健康检查。 |
| 菜单入口 | `平台能力 -> 任务管理`。 |
| 告警通知 | 直接接入 `mango-notice`，Job 只配置通知场景、模板编码、模板参数和启用策略。 |

## 6. 修订项

| ID | 问题 | 处理 |
|---|---|---|
| REVIEW-001 | 缺少业务任务处理器注册契约 | 已补充 `MangoJobHandler`、处理器元数据、远程注册和处理器页面。 |
| REVIEW-002 | 单库降级与数据库独立边界不够硬 | 已明确 `mango` 与 `mango_job` 隔离，PowerJob 同库共置时仍区分表前缀、schema、账号权限和 migration 所有权。 |
| REVIEW-003 | Mango 与 PowerJob 跨库状态一致性未描述 | 已补充 `sync_status`、失败记录、重试和删除保护流程。 |
| REVIEW-004 | PowerJob Server 认证和敏感参数处理不够明确 | 已补充 `access-token` 配置、认证校验和脱敏要求。 |
| REVIEW-005 | 数据模型缺少索引和唯一约束 | 已补充任务定义唯一约束、实例索引和引擎映射索引。 |
| REVIEW-006 | 告警 API 表达过粗 | 已拆分告警分页、新建、修改和删除接口。 |
| REVIEW-007 | 外部调研资料缺少复核日期和使用边界 | 已补充资料复核日期和资料使用边界。 |
| REVIEW-008 | 依赖方向图容易误导实现成 `api -> core` | 已按 Mango 模块规范重写依赖图，明确 `business -> api`、`core -> api`、`starter -> api + core`、`starter-remote -> api + infra-feign-starter`。 |
| REVIEW-009 | 微服务和单体部署契约不够细 | 已补充部署矩阵，明确 app 依赖、调用路径、module.properties 和 Feign adapter 规则。 |
| REVIEW-010 | 设计台账无法承接开发验收 | 已新增 `mango-docs/plans/2026-06-05-mango-job-implementation-ledger.md`，覆盖模块、数据、API、Adapter、UI、测试和发布项。 |

## 7. 用户确认项和开发前复核项

| 问题 | 结论 |
|---|---|
| PowerJob 目标版本 | 用户确认采用最新稳定版本；当前锁定 `5.1.2`，编码前复核 Maven 坐标、Server 镜像、安全公告和兼容 API。 |
| PowerJob Server 认证方式 | 用户确认优先复用 Mango 内部调用安全机制；PowerJob Server 保留 token、内网 ACL 或部署网络隔离。 |
| 部署拓扑 | 用户确认支持单体 `Job Center + Worker`、独立 Job Center、远程 Worker。 |
| HTTP/SCRIPT 任务是否首轮开放 | 用户确认按建议执行：HTTP 任务仅白名单受控开放，SCRIPT 首轮默认禁用。 |
| Handler 契约 | 用户确认使用 Mango 原生 `MangoJobHandler`，业务不直接写 PowerJob Processor。 |
| PowerJob 是否可并入 `mango_job` | 用户提出可归属于 Job 模块；设计结论为支持同一物理数据库共置，但 Mango Job 表和 PowerJob 内部表必须保持逻辑所有权隔离。 |
| 模块数据库命名 | 用户确认 Mango 数据库统一加前缀 `mango_{moduleName}`；PMO 规范落地为主库 `mango`，模块独立库 `mango_{module}`，`mango-job` / `job` 对应 `mango_job`。 |
| 菜单入口 | 用户确认 `平台能力 -> 任务管理`。 |
| 告警通知 | 用户确认直接接入 `mango-notice`，具体通信平台由 Notice 负责，Job 配好消息模板并按需启用。 |

## 8. 评审状态

当前状态：`AI_PRE_REVIEW_DONE`。

建议下一步：人工确认待确认项后，按 Sprint 1 创建独立 worktree 和任务分支进入开发。

## 9. 后续修正

研发验收中确认：`MangoJobHandler` 是任务定义的执行动作契约，不是任务管理下的独立运营菜单。PowerJob Processor 概念保留在 Adapter 和统一 processor 桥接内，业务用户只看到任务定义、执行实例、执行日志、Worker 和引擎状态。上文“处理器页面”相关表述作为预评审历史记录保留，当前实现以设计文档和交付台账的修正版本为准。
