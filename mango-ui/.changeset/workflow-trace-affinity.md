---
'@mango/workflow': minor
---

新增审批流转记录读取与节点亲和派单：设计器支持 `autoAssignmentStrategy=NODE_AFFINITY` 并暴露 `nodeAffinityFallback`（`CLAIM`、`ROUND_ROBIN`、`RANDOM`、`LEAST_TASKS`），审批详情可调用 `trace-by-business-key` 展示发起、认领、通过、退回、驳回、撤回等多实例流转记录。既有 `ROUND_ROBIN`、`LEAST_TASKS`、`AFFINITY` 策略、`CLAIM` 模式和已保存节点配置保持兼容。
