# Mango Workflow Skill 术语路由治理记录

## 1. 元数据

- 任务 ID：WORKFLOW-SKILL-ROUTING-20260718
- 状态：APPROVED
- 交付模式：FULL（PMO Skill 与规则修改自身，使用单份治理记录，不生成无关产品文档）
- 需求影响：L2，影响 Agent 对 Mango 审批能力、前端规范、CI 和研发流程的任务分类。
- 方案风险：L3，修改官方 Skill、隐式触发边界、分发投影和治理测试。
- 最终风险：L3
- 工作区决策：CREATE，分支 `fix/workflow-skill-routing`，独立 worktree `mango-workflow-skill-routing`。
- 非降级事实：PMO Skill 与治理门禁修改自身。

## 2. 目的与范围

- 目的：消除 “workflow/工作流” 多义词导致的 Agent 误路由，确保只有明确的 Mango 审批流证据才进入 `mango-workflow` 能力处理。
- 成功条件：`@mango/workflow`、流程定义、审批任务等明确场景能触发；前端规范、GitHub Actions、PMO 研发流程、普通状态机和仅有多义词的场景不触发或只澄清；官方包、业务基线和本机 Skill 口径一致。
- 处理范围：`mango-workflow` 官方 Skill、能力说明规则、规则索引、Skill 正反例、`@mango/pmo` Skill 投影、业务 starter 基线和当前用户级 Skill。
- 不处理范围：Mango Workflow 运行时能力、后端 API、前端包实现、流程定义数据、CI workflow 文件命名和普通业务状态机设计。

## 3. 术语决定

| 术语 | 确切含义 | 是否触发 `mango-workflow` |
|---|---|---|
| `@mango/workflow` | Mango 前端审批能力 npm 包，包含管理页面、审批组件、任务入口、API 类型和样式 | 是 |
| `mango-workflow-api` / `mango-workflow-starter` | Mango 后端审批能力 API 与运行时装配 | 是 |
| 流程定义、`designerJson`、发起审批、办理任务 | Mango 审批能力的领域对象或动作 | 是 |
| GitHub Actions / CI workflow | 自动化流水线 | 否 |
| PMO/研发/发布流程 | 交付治理过程 | 否 |
| Vue 代码规范、lint、typecheck、目录规范 | 前端工程治理 | 否 |
| 状态机、编排、数据处理 workflow | 通用软件概念 | 否，除非另有 Mango 审批证据 |
| 单独的 “workflow/工作流” | 多义词，证据不足 | 否；必要时先澄清 |

## 4. 可观察要求

| ID | 入口 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|
| WF-SKILL-001 | 明确出现 `@mango/workflow` 或 Mango 审批对象/API | 进入 `mango-workflow`，先读当前事实源 | 未触发或猜测模型均失败 | trigger 与 source gate 用例通过 |
| WF-SKILL-002 | Vue 规范、CI、PMO 流程、普通状态机 | 不进入 `mango-workflow` | 仅因 workflow 一词进入即失败 | 每类 not-trigger 用例通过 |
| WF-SKILL-003 | 仅有多义词，无包名、API、路径或审批对象 | 不假定为 Mango 审批；需要时只问一个分类问题 | 直接加载审批资料或提出集成方案即失败 | bare-term not-trigger 用例通过 |
| WF-SKILL-004 | 官方 Skill 分发 | `@mango/pmo` 与业务基线包含相同 Skill | 任一投影缺失或内容漂移即失败 | package check 与 baseline check 通过 |
| WF-SKILL-005 | 当前用户级 Skill | 与官方触发边界同步，立即对后续会话生效 | 仍保留宽泛触发描述即失败 | 文件哈希或内容比较一致 |
| WF-SKILL-006 | 治理回归 | 关键正反例不可静默删除 | 删除关键用例但总门禁仍通过即失败 | checker 对关键 ID 和断言做硬校验 |

## 5. ADR：用证据分类，不禁用多义术语

### 决定

保留 workflow 在 CI、PMO 和通用架构中的正常含义。Agent 只有在包坐标、模块坐标、`/workflow` API、流程定义模型、审批任务动作或 Mango 审批页面等明确证据出现时，才隐式调用 `mango-workflow`。直接点名 Skill 但缺少目标时允许进入并 `ASK`；单独出现多义词不作为触发证据。

官方 Skill 由 `mango-pmo/skills/mango-workflow` 维护，经 `@mango/pmo` 构建投影到发布包并同步业务 starter。用户级 Skill 只作为当前环境安装副本，不再作为规范源。

### 备选与取舍

- 不采用全仓替换 “workflow”：会破坏 GitHub Actions、发布流程和通用工程术语，且不能解决模型的上下文分类问题。
- 不只修用户级 Skill：无法进入版本控制、测试和后续发布，其他 Agent 环境仍会复发。
- 不把 `@mango/workflow` 从文档中删除：它是合法公开包坐标，应登记清楚，但不应成为前端通用规范的依赖或主线主题。

### 失败模式与恢复

- 触发过窄时，明确包坐标和审批 API 正例会失败。
- 触发过宽时，前端规范、CI、PMO、状态机或裸词反例会失败。
- 分发漂移时，package/baseline manifest 哈希检查失败。
- 回滚时整体回滚 Skill、规则、索引、用例和投影；不涉及运行时数据回滚。

## 6. 实施清单

| ID | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|
| WF-TASK-001 | 1 | `mango-pmo/skills/mango-workflow/**` | 官方 Skill 明确正向证据、非触发边界和事实源 |
| WF-TASK-002 | 2 | `rules/08-capability-docs.md`、`rules/index.json` | 规范与索引只登记精确 Mango 审批关键词 |
| WF-TASK-003 | 3 | `tests/skills/**` | 正例、反例、空上下文、边界、gate、next 齐全且关键用例被硬校验 |
| WF-TASK-004 | 4 | `@mango/pmo` 与 business starter 投影 | 构建、包检查和 baseline 检查通过 |
| WF-TASK-005 | 5 | `~/.agents/skills/mango-workflow/**` | 当前安装副本同步并验证 |
| WF-TASK-006 | 6 | 治理记录与全量定向门禁 | 结果、证据和剩余风险如实登记 |

## 7. 验收映射与结果

| 要求 ID | 措施 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| WF-SKILL-001..003 | M09/M10 | Skill eval 结构与关键语义断言 | PASS | 16 Skills、131 个 eval 用例通过；关键 workflow 用例由 checker 硬校验 |
| WF-SKILL-004 | M08/M09 | PMO package build/check 与 starter baseline check | PASS | `@mango/pmo@1.2.6` 137 个文件构建、包检查和业务 baseline 检查通过 |
| WF-SKILL-005 | M09 | 官方源与用户级副本比较 | PASS | `SKILL.md`、`source-map.md`、`openai.yaml` SHA-256 分别一致 |
| WF-SKILL-006 | M09/M14 | 删除保护断言、自审和独立复核 | PASS | 独立 PMO/AI Skill 治理评审：无阻断，结论 PASS；采纳历史上下文污染反例和中文审批正例 |

## 8. 例外与剩余风险

- 本任务不改变运行时公开 API，因此不修改 Workflow 模块 README 和能力地图；只修改 Agent 能力路由规范。
- 不执行 npm 发布和版本升级；进入正式版本由独立发布任务决定。
- M14 独立专家复核已完成，无阻断项；建议的历史上下文污染用例和中文正例已纳入自动化。
