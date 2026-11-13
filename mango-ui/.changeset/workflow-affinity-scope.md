---
'@mango/workflow': minor
---

节点亲和新增跨实例复用范围：审批节点可配置 `nodeAffinityScope`（`PROCESS_INSTANCE` 默认、`BUSINESS_KEY`、`AFFINITY_KEY`），运行时按“当前实例 → 同一业务键 → 同一亲和键”由近及远复用历史办理人，命中者必须仍在当前候选集，否则继续回溯（单层最多 200 条记录）或按 `nodeAffinityFallback` 回退。业务申请创建与业务发起新增可选 `affinityKey` 稳定聚合键并在申请 VO 返回，创建后不可修改。`AUTO_ASSIGN` 审计补充 `nodeAffinityScope` 与 `nodeAffinityHit`；设计器在 `AUTO + NODE_AFFINITY` 时展示“复用范围”。未配置时行为与 1.7.0 一致。
