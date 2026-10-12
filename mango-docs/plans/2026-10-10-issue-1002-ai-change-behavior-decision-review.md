---
decisionId: ISSUE-1002-AI-CHANGE-BEHAVIOR
documentType: decision-review
status: REVIEWED
subject: Issue #1002 AI Agent 默认变更行为
decisionOwner: Mango PMO
reviewDate: 2026-10-10
---

# Issue #1002 AI Agent 默认变更行为 决策复核

> **先读：** 本决定把复用优先、最小充分实现和稳定术语写入 Rule、Agent 和 Skill；Review 只保留为后置兜底，不新增固定前置检查器。

## 1. 决定摘要

- **决定：** 扩展始终加载的 `mango-pmo/rules/03-ai-coding-redlines.md`，并同步 Tech Lead、Dev、PMO Agent 及相关 Skill；增加空白上下文边界评测。
- **不决定：** 不新增每个任务必跑的检查器、固定问卷、运行时 API、数据库变更或自动公共标识重命名。
- **读者下一步：** 在任务 worktree 中按 Issue #1002 治理计划实施并运行 PMO Skill 评测。
- **依据来源：** GitHub Issue #1002；`mango-pmo/rules/03-ai-coding-redlines.md`；`mango-pmo/rules/09-test-case-automation-flow.md`；`mango-pmo/rules/11-delivery-assurance.md`。
- **状态：** REVIEWED

## 2. 第一轮：事实与用户视角

| 参与者/角色 | 当前事实 | 用户场景与成功/失败边界 | 不确定项 | 意见 |
|---|---|---|---|---|
| Agent 角色视角：事实与用户 | Issue #1002 是 P2 能力优化建议，不是运行缺陷；用户明确指出前置应是规则、Skill 和 Agent 默认行为 | Agent 面对简单需求时应直接复用现有能力；成功是减少无必要实现和术语转换，失败是把所有任务变成问卷 | 不同模型对“已有能力”和“当前消费者”的识别可能不同 | 前置默认行为比后置 Review 更符合目标 |

## 3. 第二轮：技术与风险视角

| 参与者/角色 | 模块/接口/数据边界 | 权限/租户/兼容性风险 | 回退方式 | 意见 |
|---|---|---|---|---|
| Agent 角色视角：技术与风险 | 影响 `mango-pmo/rules`、`agents`、`skills` 和 Skill eval；不影响业务 API、数据库和运行态 | “最小充分实现”不得删除安全、权限、租户、数据一致性、错误、可访问性和测试边界；公共 API、权限码、页面 key 和 Resource identity 不自动重命名 | 按本任务提交回退 Rule、Agent、Skill 和评测文件 | 将长期约束放入现有始终加载 Rule，避免新增规则加载面 |

## 4. 第三轮：验证与交付视角

| 参与者/角色 | 可观察结果 | 验证命令或步骤 | 证据路径 | 意见 |
|---|---|---|---|---|
| Agent 角色视角：验证与交付 | Skill 结构和评测集合可解析；简单任务不触发固定问卷；公共术语不确定时返回 ASK；Review 能发现偏离 | `node mango-pmo/tests/skills/check-skill-evals.mjs`；相关 Node tests；空白上下文独立评测；人工治理验收 | `mango-pmo/tests/skills/evals.json`；`mango-docs/plans/2026-10-10-issue-1002-ai-change-behavior-governance-plan.md` | 结构检查不能替代独立 Agent 语义评测和人工验收 |

## 5. 方案整理

| 项目 | 内容 |
|---|---|
| 采用方案 | Rule 定义约束，Agent 定义默认判断，Skill 执行，Review 检查偏离，评测验证边界 |
| 选择依据 | 前置行为能减少返工；现有 Rule 始终加载；不新增固定问卷；保留安全和真实验证边界 |
| 被拒绝方案 | 只在 PR Review 检查；所有任务生成术语表；新增统一检查器；按代码行数判定过度设计 |
| 未解决冲突 | 无法仅凭文本评测完全证明模型在真实任务中减少 Review 时间；需要后续真实任务基线 |
| 影响范围 | PMO 长期规则、Agent、Skill、空白上下文评测和治理记录 |
| 剩余风险 | 模型可能仍然误判已有能力或消费者；Review 和真实任务基线继续承担纠偏责任 |
| 评审选择 | AGENT_ONLY |
| 人工确认 | 未指定外部同行评审；保留后续发起入口 |

## 6. 评审选择与记录

- **评审选择：** AGENT_ONLY
- **询问记录：** 用户未指定外部同行评审；按规范记录为 AGENT_ONLY，未将 Agent 分析称为人工评审或人工审批。
- **用户决定人：** 当前任务用户

| 评审方式 | 评审人/Agent 角色 | 检查范围 | 阻断问题 | 非阻断建议 | 结论 |
|---|---|---|---|---|---|
| AGENT_ONLY | Agent 角色视角分析：事实与用户、技术与风险、验证与交付 | Issue 范围、规则唯一源、Agent/Skill 责任、验证边界、回退 | 无 | 后续真实 Mango 任务补充收益、误报和剩余风险基线 | RECORDED |

## 7. 审批与后续动作

- **审批人：** 待用户或维护者确认
- **审批证据：** 当前任务会话和 Issue #1002 评论；未声明人工审批完成
- **下一动作：** 在独立任务 worktree 实施 Rule、Agent、Skill 和评测修改
- **责任人：** 当前开发 Agent
- **完成条件：** 相关文件修改完成，结构和评测检查通过，独立语义评测与人工验收结果已记录，Issue #1002 回写实施状态
