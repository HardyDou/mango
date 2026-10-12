## 判定：STOP — 退回设计，不得进入实现

这是**边界不合格**，不是实现细节问题。设计阶段引入的"未来扩展"抽象在本交付范围内没有消费者，与已批准需求脱节，必须先退回技术设计重新决策。

### 事实对照

| 事实 | 影响 |
|---|---|
| `approvedSystemRequirements: true` | 需求边界已冻结，"未来扩展"不在其中 |
| `existingMangoCapability: true` | 复用顺序第 1 项已成立，第 5 项无从谈起 |
| `futureConsumerOnly: true` | 新增抽象/配置/依赖缺"当前消费者"，直接命中禁止条件 |

### 依据

**1. `REDLINE-REUSE-010`（`mango-pmo/rules/03-ai-coding-redlines.md:32-42`）**
复用顺序第 1 项"Mango 已有能力"已能覆盖已批准需求，因此不得跳到第 5 项"新增抽象、配置、接口、模块或依赖"。规则要求新增第 5 项时必须同时给出**被放弃的复用对象和放弃原因**——此处的唯一理由是"未来扩展"，而规则明确"

### 待办

需要我继续核对 `mango-design-technical` skill 中对该 skill 的自检清单，确认它是否已把 `REDLINE-OVERDESIGN-060` 列为出稿前必检项吗？
