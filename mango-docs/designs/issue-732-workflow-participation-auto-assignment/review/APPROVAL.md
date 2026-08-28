# Issue #732 生命周期审批记录

- 审批人：HardyDou
- 审批时间：2026-08-28
- 需求来源：[GitHub Issue #732](https://github.com/HardyDou/mango/issues/732)
- 审批证据：用户在完成需求目标和技术方案评审后明确回复“继续吧”。
- 审批结论：批准采用租户级工作流参与关系投影、稳定用户身份、严格 ROUND_ROBIN 自动派单和空候选事务回滚方案，进入 FULL 生命周期文档与实施。
- 范围约束：历史参与关系只授予只读事实，不扩大任务处理权限；第一版不实现 LEAST_TASKS、AFFINITY，也不改变业务状态机所有权。
