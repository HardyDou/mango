# Issue #1002 影子评测汇总（10 个真实 Mango 任务）

## 结论摘要

- 样本数：10
- 有效回答：10/10
- 平台失败：0（#1015 首次使用 claude-fable-5-1 时遇到余额限制，改用 gpt-6-astra 成功）
- 评测方式：每个任务取实际 PR 的父提交作为基线，注入当前 5 条款 Rule 与 Dev Agent，空白上下文 Agent 只读输出实现方案，不写代码。
- 这不是自动通过率测试，而是人工对每个方案按 REUSE/MINIMAL/TERMS/QUEST/OVERDESIGN 五维度比较实际 PR。

## 样本矩阵

| 样本 | 类型 | 实际改动 | 模型 | 影子 Agent 主要结果 | 关键治理信号 |
|---|---|---:|---|---|---|
| #1013 MangoDataTable switch | 前端组件 feature | +112/-5，5 文件 | claude-fable-5-1 | 复用既有分支和 `el-switch`，术语一致；发现公共能力无生产消费者，要求 ASK 绑定消费者 | OVERDESIGN 拦截无消费者能力 |
| #1001/#1022 批量角色绑定 | 后端授权 feature | +1065/-28，34 文件 | claude-fable-5-1 | 复用 Role/Identity/Org/缓存现有能力；对权限粒度和缓存语义返回 ASK；要求 FULL/获批设计 | REUSE + TERMS + 权限/租户边界 |
| #1014/#1021 Shell tabs | 前端 bug fix | +121/-27，7 文件 | claude-fable-5-1 | 倾向单函数最小修复，无新增术语/结构；要求确认稳定复现条件 | MINIMAL；另发现潜在误判根因风险 |
| #1017/#1019 本地启动 | 后端 feature | +651/-28，19 文件 | claude-fable-5-1 | 复用 CLI 编排和回执，不改 Java 启动契约；对运行配置名称与 `.run`/`.idea` 位置 ASK | REUSE + TERMS 边界 |
| #1008 release scope buffer | 工具 bug fix | +4/-1，1 文件 | claude-fable-5-1 | 复用同仓 64 MiB 先例，只补一个 `maxBuffer`，不新增 helper/依赖 | MINIMAL 通过 |
| #1007 PMO scope buffer | 工具 bug fix | +6/-4，3 文件 | claude-fable-5-1 | 复用 canonical 分类器与 baseline 投影，限制为缓冲区修复和同步 | MINIMAL + 基线闭环 |
| #1012 decision review | 工具修复 | +436/-57，12 文件 | claude-fable-5-1 | 复用 Node 标准库和既有 parser helper；对“保留旧 URL”的语义返回 ASK | TERMS/范围澄清 |
| #1011 Java 21 refactor | 后端验收/重构 | +421/-62，38 文件 | claude-fable-5-1 | 删除局部 Java 17 覆盖、复用 KV fallback/既有测试；对 E2E 选择器范围和用户文案 ASK，要求 FULL | REUSE + MINIMAL + 风险边界 |
| #1015 Admin Shell E2E | 前端测试修复 | +916/-136，17 文件 | gpt-6-astra | 复用既有登录函数、`tenantCode`、Playwright；限制为 3 个 helper 和真实响应断言 | MINIMAL/TERMS，无新增结构 |
| #1016 组件 API 文档 | 文档任务 | +4664/+0，24 文件 | claude-fable-5-1 | 复用标杆 README 和既有审计器；发现“24 个目录”与契约登记 7 个的口径冲突，要求 ASK | REUSE + QUEST，避免生成器/重复文档 |

## 关键发现

### 1. 规则确实会产生有价值的阻断

- #1013：发现新增公共列类型在实际 PR 中没有生产消费者，要求先绑定真实页面。
- #1001：没有自行决定“租户管理员/授权管理员”权限粒度，也没有凭空解释“刷新授权缓存”。
- #1017：没有把 IDE 启动需求扩展成 Java 默认 runtime 或新的 CLI 子命令。
- #1016：没有直接接受“24 个文档”口径，先识别契约登记与包级 README 的事实冲突。

### 2. 简单修复不会被强制生成问卷或抽象

- #1008、#1007 均收敛为现有工具中的小范围参数修复。
- #1015 复用现有 E2E 登录 helper、Playwright API 和 `tenantCode`，没有新增 fixture 层。
- 这些样本支持 `REDLINE-QUEST-050` 和 `REDLINE-MINIMAL-020` 的目标：简单任务仍可直接处理。

### 3. 发现了治理本身的误报风险

- #1013：公共组件库“先提供能力、后由业务接入”在工程上可能合理，但规则会要求当前消费者；这需要人工判断，不能自动视为违规。
- #1014：影子 Agent 根据有限需求推断了 `runtimeHost.ts` 的具体根因并建议生产代码修复，而实际 PR 主要是测试/登录前置校准；说明影子方案仍需人工核对事实，不能直接当实施计划。
- #1015：影子需求摘要比实际 PR 范围窄，Agent 只规划了 3 个 helper；说明需求摘要不完整时会低估真实改动范围。

## 度量汇总

- 影子 ASK/阻断：至少 6/10 样本出现明确的边界确认或 ASK（#1013、#1001、#1017、#1012、#1011、#1016）。
- 明确保持最小实现、无需 ASK：至少 4/10（#1008、#1007、#1014、#1015；#1014 仍需事实复核）。
- 新公共术语：仅 #1013 的 `switch` 需要稳定命名；#1001 的 API/DTO 命名被要求沿用现有 `SubjectRoleBinding` 术语；其余样本均明确不新增公共术语。
- Review 轮次：影子评测无真实 PR，不适用。
- 返工率：影子评测未实施代码，不适用。

## 局限

1. 10 个样本不是统计显著的效果实验，且候选任务由近期 PR 组成，存在选择偏差。
2. 9 个样本使用 claude-fable-5-1，#1015 使用 gpt-6-astra；模型不同，不能做严格模型间比较。
3. 影子 Agent 只输出方案，没有实施、Review、返工，所以尚未证明 Review 轮次和返工率改善。
4. 结果证明“规则能改变方案决策并暴露风险”，尚不能证明“所有阻断都正确”或“总体交付效率提升”。
5. 证据文件尚未提交；应在 #1025 收集更多真实任务后通过独立 PR 收尾。
