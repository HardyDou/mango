# Issue #732 生命周期审批记录

- 审批人：HardyDou
- 审批时间：2026-08-28
- 需求来源：[GitHub Issue #732](https://github.com/HardyDou/mango/issues/732)
- 审批证据：用户在完成需求目标和技术方案评审后明确回复“继续吧”。
- 审批结论：批准采用租户级工作流参与关系投影、稳定用户身份、严格 ROUND_ROBIN 自动派单和空候选事务回滚方案，进入 FULL 生命周期文档与实施。
- 范围约束：历史参与关系只授予只读事实，不扩大任务处理权限；第一版不实现 LEAST_TASKS、AFFINITY，也不改变业务状态机所有权。

## 2026-08-29 设计器候选接口补充审批

- 审批证据：用户明确要求“工作流自定义接口也行，记住 provider 提供”，并确认通过 Provider 解耦。
- 审批结论：Workflow 新增自有设计器候选接口；默认实现通过平台公共 Java API 提供，承载应用可注册自定义 `WorkflowDesignerOptionProvider` 替换。
- 权限与租户约束：前端仅使用 `workflow:definition:query`，不得给 Workflow 菜单追加 `system:*` 或 `authorization:*`；租户只取当前可信上下文，客户端不传 `tenantId`。
- 失败约束：Provider 缺失或下游加载失败必须返回明确错误，不得吞错后返回空候选数据。
