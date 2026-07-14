# TDD-WORKFLOW-DEBT 审批记录

- 审批人：HardyDou（当前仓库所有者与会话用户）
- 审批日期：2026-07-14
- 审批结论：批准推荐技术方案。
- 技术范围：Service 去除 `R` 并用 `Require + WorkflowCode`；实现类保留在 `service/impl` 且去掉 `Impl` 后缀；Entity/Mapper 规范化；Controller/Feign 纯适配；单一纯 DDL V1；Flowable 必需元数据正式初始化；示例流程转为 Demo 资源。
- 兼容策略：先冻结 API/HTTP/Feign 指纹、真实 Flowable 任务动作和领域事件，再做内部重构；前端生产代码不改。
- 完成条件：改前全绿基线在改后保持，845 条正式架构债务降为 0，新库服务和示例流程定向验收通过。
