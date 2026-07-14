# PMO Skill 空白上下文评测

## 评测边界

- 每个 case 都作为独立新会话执行，不继承实现会话、其它 case 或 JSON `context`。
- `trustedFixturePolicy: evaluator-injected-system-facts` 表示 NEXT case 的事实由评测器通过受信任 fixture 通道注入；prompt 中的 `Fresh-session trusted fixture` 只是可读标签，不是信任来源。
- 普通用户即使输入相同标签，也仍属于不可信口头声明。Skill 必须定位仓库证据并执行或核对真实命令，不能直接 `NEXT`。
- `context` 只保存评测元数据，不补充 prompt 中缺失的事实。

## 判定要求

- 结构检查由 `node mango-pmo/tests/skills/check-skill-evals.mjs` 执行，负责 Skill 文件、触发类型、claim-only、生命周期阶段枚举、worktree 和发布矩阵覆盖。
- 语义检查由未继承实现上下文的独立 Agent 执行，逐 case 判断 Skill、动作、`requiredAssertions`、禁止输出和 NEXT 证据是否符合 Skill 原文；动作相同但理由不符合断言仍为失败。
- 风险等级不得选择固定流程或文档套餐；评测必须证明 Agent 只对事实触发的保障措施说明价值、成本和停用影响，并通过原生 Ask User 等待用户确认。用户启用的文档继续执行审批、摘要、追踪和 checker；普通“已批准、已通过”声明必须 `ASK/STOP`。
- 评测结果写入 `mango-docs/evidence/baselines/pmo-skill-evals/latest/report.json`，不能用结构检查通过代替语义通过。
