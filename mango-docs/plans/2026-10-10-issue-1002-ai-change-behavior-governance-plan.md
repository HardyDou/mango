# Issue #1002 AI Agent 默认变更行为治理计划

## 先读

- **一句话目标：** 让 Mango Agent 默认复用已有能力、选择最小充分实现并保持稳定术语，而不是通过新增前置检查表治理。
- **真实场景：** Agent 处理一个已有 Mango 能力可以满足的需求时，应复用现有能力；只有确认当前消费者、边界和验证入口后，才新增抽象、依赖或公共术语。
- **关键输入/输出：** 输入为 Issue #1002、当前 PMO Rule/Agent/Skill；输出为规则、角色约束、Skill 行为和空白上下文评测。
- **成功/失败边界：** 成功是默认行为在设计、开发和 Review Skill 中一致；失败是 Agent 继续创建无消费者能力、固定问卷、重复术语，或以减少代码为由删除安全和验证边界。
- **明确不做：** 不新增所有任务必跑的前置检查器，不改变业务运行时 API、数据库、权限和发布制品，不自动重命名公共标识。
- **阅读顺序：** 先看本计划的决定和范围，再看 `mango-pmo/rules/03-ai-coding-redlines.md`、Agent、Skill 和评测用例。

## 1. 任务基线

| 项目 | 决定 |
|---|---|
| 来源 | GitHub Issue #1002 |
| 需求影响 | L3：改变 PMO 和 AI Agent 的长期开发行为 |
| 方案风险 | L3：修改始终加载的 Rule、多个 Agent/Skill 和空白上下文评测 |
| 最终风险 | L3 = max(L3, L3) |
| 交付模式 | FULL 治理任务 |
| 工作区 | `M01=CREATE`，仓库外任务 worktree |
| 启用措施 | M07、M09、M10、M14、M16 |
| 不启用措施 | 产品 BRD/SRS/TDD/Plan、API、数据库、UI、运行态验证 |

## 2. 采用方案

### 2.1 规则源

扩展始终加载的 `mango-pmo/rules/03-ai-coding-redlines.md` 第 2.1 节，新增**五个带条款 ID** 的默认变更行为，使 Agent、Skill 和评测可以稳定引用同一份定义，不在入口文件复制正文：

| 条款 ID | 约束 |
|---|---|
| `REDLINE-REUSE-010` | 按 Mango 能力、当前模块实现、标准库、已安装依赖、新增结构的顺序取舍；历史代码仅作为事实证据 |
| `REDLINE-MINIMAL-020` | 新增抽象、配置、接口、模块或依赖必须具备当前消费者、责任边界和验证入口 |
| `REDLINE-TERMS-030` | 先复用主术语；术语冲突时 `ASK`，禁止造同义词或直接重命名公共标识 |
| `REDLINE-QUEST-050` | 简单任务不生成固定问卷、无消费者文档或无观察目标的测试 |
| `REDLINE-OVERDESIGN-060` | 过度设计判定清单，供设计和 PR Review 逐条检查 |

不在 Rule 之外新增第二个规则文件，也不新增每个任务必跑的检查器。

### 2.2 Agent 行为

- `02-tech-lead-agent.md`：设计阶段按 `REDLINE-REUSE-010` 至 `REDLINE-OVERDESIGN-060` 取舍。
- `03-dev-agent.md`：编码前确认复用顺序、当前消费者和验证入口。
- `05-pmo-agent.md`：保证条款只在 `mango-pmo` 维护，入口只引用 ID。

### 2.3 Skill 行为

六个 Skill 只引用条款 ID，不复制正文：

| Skill | 引用 |
|---|---|
| `mango-design-technical` | `REDLINE-REUSE-010`、`REDLINE-MINIMAL-020`、`REDLINE-TERMS-030` |
| `mango-engineering` | `REDLINE-REUSE-010` 至 `REDLINE-OVERDESIGN-060` |
| `mango-defect-fix` | `REDLINE-REUSE-010`（优先共享根因） |
| `mango-module-development` | `REDLINE-REUSE-010`（canonical 模块结构与生成入口） |
| `mango-plan-implementation` | `REDLINE-REUSE-010`、`REDLINE-MINIMAL-020` |
| `mango-review-pr` | `REDLINE-OVERDESIGN-060`（逐条输出带行号发现） |

### 2.4 评测

在 `mango-pmo/tests/skills/evals.json` 增加正例、反例和边界用例，覆盖：

- 已有能力复用；
- 简单任务不生成固定问卷；
- 公共术语不确定时 `ASK`；
- 缺陷修复优先共享根因；
- Review 识别无消费者抽象和未映射术语；
- Plan 不得静默新增设计。

## 3. 不采用的方案

| 方案 | 不采用原因 |
|---|---|
| 每个任务运行“最小变更检查器” | 把默认行为变成后置或表单流程，增加无必要成本 |
| 每个任务强制生成上下文卡片和术语表 | 与 SIMPLE 任务和“禁止固定问卷”冲突 |
| 按代码行数判断过度设计 | 无法识别安全、权限、租户和数据边界 |
| 自动重命名公共 API、权限码和数据库字段 | 可能破坏兼容性、发布和业务消费者 |
| 只扩展 PR Review | 发现问题太晚，不能塑造 Agent 的默认行为 |

## 4. 影响范围

- PMO 规则：`mango-pmo/rules/03-ai-coding-redlines.md`、`mango-pmo/rules/index.json`
- Agent：`mango-pmo/agents/02-tech-lead-agent.md`、`mango-pmo/agents/03-dev-agent.md`、`mango-pmo/agents/05-pmo-agent.md`
- Skill：技术设计、工程、缺陷修复、新模块、实施计划、PR Review
- 测试：`mango-pmo/tests/skills/evals.json`
- 无 HTTP API、数据库、菜单、页面、权限数据和发布物料变化

## 5. 执行顺序

1. 修改 canonical Rule 和规则索引。
2. 修改 Tech Lead、Dev、PMO Agent 约束。
3. 修改六个相关 Skill 的默认行为。
4. 增加空白上下文评测。
5. 执行静态检查、JSON/Skill 检查和独立 Agent 评测。
6. 完成人工治理验收，记录误报、剩余风险和 Issue 回写。

## 6. 验收标准

- `AC-1002-01`：Rule 用条款 ID 定义复用优先、最小充分实现和稳定术语。
- `AC-1002-02`：Tech Lead、Dev、PMO Agent 只引用条款 ID，不复制正文。
- `AC-1002-03`：六个 Skill 引用条款 ID，Review 按 `REDLINE-OVERDESIGN-060` 输出带行号发现。
- `AC-1002-04`：空白上下文评测覆盖 STOP、ASK、SELECT_MINIMUM_SUFFICIENT、不触发和 claim-only 场景。
- `AC-1002-05`：Rule、Agent、Skill、评测引用有效，JSON、Skill 结构和 190 项 PMO Node 测试通过。
- `AC-1002-06`：业务 baseline 与主仓同步，`sync-pmo-baseline.mjs --check` 和 `check-template.mjs` 通过。
- `AC-1002-07`：没有业务运行时、数据库、权限、租户或发布行为变化。

## 6.1 单独跟踪的未完成项

- ~~6 个新增用例的独立空白上下文语义评测~~ 已完成：`mango-pmo/tests/skills/run-issue-1002-blank-context.mjs` 逐例在一次性仓库副本中执行，6/6 通过，证据在 `mango-docs/evidence/issue-1002/eval-packet/ac-1002-06-semantic-report.json`。`latest/report.json` 仍保持原基线不变（该基线已滞后于当前 20 skills/174 cases，reconcile 属独立治理任务）。
- 真实 Mango 任务度量：代码量、Review 轮次、澄清次数、返工率和新增术语数，作为独立后续任务跟踪，本任务不伪造结果。

## 7. 决策复核

- **事实与用户视角：** Issue 目标是降低 AI 过度实现和术语认知负担；前置默认行为比后置 Review 更符合目标。
- **技术与风险视角：** Rule 只约束取舍，不允许删除安全、权限、租户、数据和验证边界；公共标识不自动重命名。
- **验证与交付视角：** 通过空白上下文评测和 Review 反例验证行为；不以 Agent 自报通过代替证据。
- **评审选择：** `AGENT_ONLY`。当前未指定外部同行评审；该记录不表示人工审批。

## 8. 回退方式和剩余风险

- 回退方式：按文件回退本任务分支的 Rule、Agent、Skill 和评测修改，不涉及数据库或运行时数据恢复。
- 剩余风险：不同模型对“已有能力”和“当前消费者”的理解可能不一致；通过空白上下文反例、后置 Review 和后续真实任务基线持续观察。
- 本计划不批准发布、合并或部署。
