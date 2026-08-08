---
documentType: delivery-l3
deliveryLevel: L3
pageBudget: 3
---

# Worktree 交付完整性门禁

## 业务与用户故事

多个任务并行时，开发者可能遗留旧 worktree、只提交部分文件，或把新需求放进旧 worktree。目标是让遗漏在提交、PR 和清理前被机器阻断；范围仅包含 Git worktree 与提交完整性，不自动暂存、删除或修改其它任务文件。

1. BR-001 防止任务代码因 worktree 切换、部分暂存或错误复用而漏交付。
2. US-001 -> BR-001：前置条件是开发者开始、提交或结束任务；开发者执行对应检查后，应看到当前任务分支、文件状态和远端差异；状态完整时通过，存在遗漏时停止并列出具体文件或 worktree。

## 系统满足方式

1. SR-001 -> US-001：PMO 工具提供 `start`、`commit`、`deliver`、`cleanup` 四种检查；读取 Git 状态但不修改文件，失败返回非零退出码，成功输出当前 worktree、分支、变更计数和远端差异。
2. SR-002 -> BR-001：任务复用必须提供既有任务、Issue 或 PR 记录的期望分支；其它脏 worktree 默认阻断，只有用户按精确路径确认的未合并并行任务可保留。

## 名称术语与适用图

- 系统/模块/功能：Mango PMO / Worktree 交付完整性 / 任务开始、提交、交付、清理门禁。
- 业务术语：任务 worktree 指绑定一个任务或 PR 的非 main Git worktree；脏 worktree 指存在已暂存、未暂存、未跟踪或冲突文件的 worktree。

```mermaid
flowchart LR
  A[任务开始] --> B[start 全局巡检]
  B --> C[commit 完整暂存检查]
  C --> D[deliver 本地与远端检查]
  D --> E[cleanup 合并与清理检查]
```

## 参考规范与代码

- 规范：`mango-pmo/rules/00-dev-flow.md@65072d33`；采用：工作区创建、提交、PR 与清理流程。
- 代码：`mango-pmo/tools/pmo-preflight.mjs@65072d33`；采用：扩展现有 Node.js Git 状态检查方式，保持机器 JSON 与中文文本输出分离。

## 技术设计与变更字典

1. TD-001 -> SR-001：新增只读检查工具，分别查询 staged、unstaged、untracked、conflict、upstream 和 base 合并状态；任一阶段条件不满足即退出 1。
2. TD-002 -> SR-002：`REUSE` 校验已记录期望分支；新任务遇到脏 worktree 时停止。`--allow-dirty-worktree <path>` 只允许明确保留未合并并行任务，不能豁免已合并 worktree 的残留修改。
- 变更字典：新增模式 `start|commit|deliver|cleanup`；新增参数 `--expected-branch`、`--reuse-current-task`、`--require-upstream`、`--allow-dirty-worktree`；机器结果新增 `current`、`worktrees`、`warnings`、`errors`、`ok`。

## 实施与验证

1. TASK-001 -> TD-001：新增检查工具和隔离 Git fixture，覆盖当前/其它 worktree、部分暂存、Push 和合并状态，全部断言通过即完成。
2. TASK-002 -> TD-002：接入工程 Skill、交付保障 Skill、PMO 规则、业务 baseline 和 CI，投影检查无漂移即完成。
3. VAL-001 -> TASK-001：执行 `node --test mango-pmo/tests/worktree-delivery-integrity.test.mjs`，断言所有正反场景通过；失败时停止提交。
4. VAL-002 -> TASK-002：执行 Skill、治理意图、baseline、模板、PMO 包和 CLI 回归，断言退出码为 0；任一失败时停止 PR。
- 回滚与风险：回滚本次提交即可移除门禁；剩余风险是任务身份仍依赖既有任务、Issue 或 PR 记录，工具不会从需求文本自动推断任务相同。
