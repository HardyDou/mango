# 影子评测报告：Issue #1013（MangoDataTable switch 列类型）

## 目的

验证 Issue #1002 治理生效后的实际行为差异。选取一个治理生效前已合并的真实任务
（#1013，2026-10-08 合入），在「加入 switch 之前」的代码基线（`7769add9f`）上，
注入新治理规则（5 条款），由空白上下文 Agent 重新给出实现方案，再与实际实现对比。

## 方法

- 基线：`7769add9f`（#1013 的父提交，`MangoTableColumnType` 尚无 `switch`）
- 规则：`mango-pmo/rules/03-ai-coding-redlines.md` 当前 5 条款 + `03-dev-agent.md`
- 会话：`pi -p --no-session --no-builtin-tools -t read,bash --model claude-fable-5-1`，
  只读，附加系统提示禁止写文件
- 需求：与 #1013 原文一致（新增 switch 列类型，boolean 与 0/1，写回保持原类型）

## 五维度对比

| 维度 | 实际 #1013 | 影子方案（治理生效） | 差异 |
|---|---|---|---|
| REUSE-010 复用 | 复用 el-switch + 既有分支链 | 同左，且明确排除 select/custom 替代、拒绝新建 MangoSwitch | 一致 |
| TERMS-030 术语 | `switch`，符合既有小写单词惯例 | `switch`，并明确禁止 isSwitch/switchColumn/中文别名（开关/切换/是否） | 一致，影子更显式 |
| MINIMAL-020 最小实现 | 5 文件 112 行（含 77 行测试） | 3 文件 + 4 条单测 + 迁移消费者 | 影子强制绑定消费者 |
| QUEST-050 问卷 | 无固定问卷 | 无，仅 PR 描述一句术语说明 | 一致 |
| OVERDESIGN-060 过度设计 | **未自查**：新增公共列类型无生产消费者 | **主动拦截**：识别 3 处真实 el-switch 表格场景，要求绑定消费者后才开工，否则「本任务在红线口径下不成立」 | **关键差异** |

## 关键发现

实际 #1013 以「纯增量能力，既有调用方行为不变」交付，`switch` 列类型在 PR 内
**零生产消费者**（仅单测），5 个改动文件全部在公共组件内，未迁移任何真实页面。

治理规则下的 Agent 独立发现了同样的真实消费者候选：

- `packages/rbac/src/views/app/index.vue:266`（`row.status`，1/0）
- `packages/system/src/views/public-path/index.vue:46/84`
- `packages/payment/src/views/methods/PaymentMethodRoutePanel.vue:149`

并在「A：迁移真实页面」与「B：以 MangoCollapseDetailPage 为消费者」之间返回 ASK，
拒绝在无消费者状态下实施——这正是 `REDLINE-OVERDESIGN-060` 第 1 条与
`REDLINE-MINIMAL-020`「当前消费者」要求的直接体现。

## 度量记录（AC-1002-07 首个样本）

- 任务：#1013，SIMPLE，公共组件 feature
- 实际代码量：+112/-5（5 文件）
- 影子方案：3 文件 + 4 单测 + 消费者迁移（规模相当，但含消费者绑定）
- 澄清/ASK 次数：实际 0 次；影子 1 次（A/B 消费者选择）
- 新增术语：1 个（`switch`），双方均与既有术语一致
- 返工率：不适用（影子未产出 PR）

## 局限

- 单一样本，不能代表全部任务类型；结论为「治理在消费者缺失场景确实生效」，非统计显著。
- 影子 Agent 与实现 Agent 同源模型，存在同源偏差；真实度量仍需不同真实任务补充。
- 本报告不伪造 Review 轮次、返工率等需要真实 PR 往返才能度量的指标。
