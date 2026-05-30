# Mango Admin Runtime 完整产品化升级计划交付台账

## 1. 说明

本台账用于验收本次“详细升级计划”设计交付，不代表产品化实现已经完成。实现交付必须在后续 Sprint 中按本台账继续拆分执行，并逐阶段保留回归测试、新特性测试、E2E 截图、布局报告和数据核对证据。

## 2. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| PLAN-001 | 用户要求 | 默认初始化后就是完整 Mango Admin，不再是仿写主框架 | 规划新增 `@mango/admin` 完整基座包，并把 `@mango/admin-shell` 下沉为底层运行时 | Sprint 1：`@mango/admin` 完整基座、full preset、原 Mango 主框架能力抽取 | 检查计划中 Sprint 1 是否包含完整顶栏、小铃铛、用户区、设置、主题、标签页、布局和 starter 接入要求 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-002 | 用户要求 | 业务项目默认 full preset，也可以按配置裁剪 | 规划 `full`、`standard`、`minimal`、`custom` 四类 preset，custom 通过能力清单组合 | Sprint 1 和 Sprint 6：preset 契约、CLI 参数、starter 入口改造 | 检查计划中是否定义 full 和 custom 示例、CLI `--preset` 参数、初始化验收标准 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-003 | 用户要求 | Mango 能力物料必须拆成 API SDK 和 Admin UI 两类包 | 规划每个内置能力拆为 `@mango/*-api` 与 `@mango/*-admin`，旧混合包只保留兼容出口 | Sprint 2：system、rbac、auth、file、notice、workflow、template、numgen、calendar 拆包 | 检查计划中是否列出拆包范围、API 包禁止项、Admin 包必需项和兼容策略 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-004 | 用户要求 | API SDK 可被非管理系统 UI 使用 | 规划 `*-api` 不依赖 Admin Shell、不包含页面、菜单、后台路由和 Admin store | Sprint 2 和 Sprint 8：非管理 UI API SDK 消费验证 | 检查计划中是否要求临时 Vue 或 TypeScript 消费项目 typecheck、build 和 E2E | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-005 | 用户要求 | Admin 能力包一依赖就集成页面、菜单、权限、局部样式和能力依赖 | 规划 `*-admin` 必须携带 capability manifest、页面 registry、菜单、权限、样式、后端能力要求和 E2E 清单 | Sprint 2、Sprint 3、Sprint 4：Admin 能力包集成协议 | 检查计划中是否把页面、菜单、权限、样式、后端能力和 E2E 全部列为 Admin 包必需项 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-006 | 用户要求 | custom 模式需要自动补齐必需依赖，不能静默生成残缺页面 | 规划 capability manifest 2.0 和能力依赖图解析，包含缺失依赖失败、冲突检测、循环依赖检查 | Sprint 3：依赖解析、冲突检测、CLI 和 runtime 共用逻辑 | 检查计划中是否包含自动补齐、明确失败、循环依赖识别和依赖解析报告 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-007 | 用户要求 | 菜单默认复用 Mango 后端菜单体系，并合并能力包菜单和业务菜单 | 规划后端菜单优先、能力菜单补充、业务菜单追加的合并策略，starter 不再手写 Mango 内置菜单 | Sprint 4：菜单、权限和资源自动集成 | 检查计划中是否包含菜单合并、冲突规则、权限过滤、后端初始化数据和 capability 对齐 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-008 | 用户要求 | 系统管理等基础能力存在必需依赖限制 | 规划基础依赖集合包含 system、auth、rbac、tenant、menu、permission，由依赖解析统一处理 | Sprint 3：基础依赖和不可自动补齐依赖失败机制 | 检查计划中是否明确基础依赖、自动补齐和失败机制 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-009 | 用户要求 | 保持 Mango 自由组合部署特性：单体、本地模块、微前端、混合部署 | 规划 local、micro、mixed 共用同一 capability runtime 协议和运行时配置 | Sprint 5：运行时部署模式统一 | 检查计划中是否覆盖 local、micro、mixed、远程入口、健康检查、失败诊断和标准错误面板 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-010 | 用户要求 | create-mango-app 和 starter 必须初始化出可用 Mango 项目 | 规划 CLI 支持 preset、features、frontend-mode，并生成业务 `*-api` 和 `*-admin` 包 | Sprint 6：create-mango-app 和 starter 完整改造 | 检查计划中是否要求 full 初始化完整 Mango、custom 初始化裁剪能力、业务 API 包独立消费、业务 Admin 包自动接入 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-011 | 用户要求 | 每个阶段完成后必须做回归测试和新特性测试，通过后进入下一阶段 | 规划 Sprint 0 至 Sprint 8 每阶段均包含新特性测试、回归测试和完成标准 | 阶段门禁设计：逐 Sprint 测试门禁 | 检查计划中每个 Sprint 是否包含新特性测试、回归测试和完成标准 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-012 | 用户要求 | 验收必须重视 E2E 截图、布局识别、样式、颜色、数据和功能 | 规划基准截图、full/custom E2E、DOM 布局报告、CSS 断链检查、API 数据核对、权限和菜单核对 | Sprint 0 和 Sprint 8：截图基准、最终验收和质量冻结 | 检查计划中是否要求截图保存、布局报告、CSS 检查、数据核对和权限菜单核对 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-013 | 用户要求 | 不允许骗人、不写假代码、不偷工减料、不改变范围和缩小目标 | 规划“不允许缩小的范围”和“不完成不得声明完成的事项”，明确禁止把简化 Shell、静态菜单和假数据当完成 | 范围红线和完成声明红线 | 检查计划中是否包含完整不可缩小范围、最终声明限制和真实验证要求 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-plan.md` |
| PLAN-014 | 同行评审 | 开始前必须确认计划无致命架构问题，并把评审门禁固化 | 记录同行评审结论：可从 Sprint 0 开始；manifest 2.0 类型和检查器骨架提前到 Sprint 0 | 同行评审记录和计划调整 | 检查评审记录是否包含结论、致命问题、开始前门禁、阶段顺序和失败防线 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-peer-review.md` |
| PLAN-015 | PMO 规范 | 升级计划要可追踪到交付台账 | 使用 PMO 交付契约列结构记录来源、要求、设计决策、交付物、验收方式、状态和证据文件 | 本台账文件 | 运行 `delivery-contract-check` verify 模式，确认台账字段完整且均为已完成设计交付项 | DONE | `mango-docs/plans/2026-05-29-mango-admin-runtime-full-productization-ledger.md` |
