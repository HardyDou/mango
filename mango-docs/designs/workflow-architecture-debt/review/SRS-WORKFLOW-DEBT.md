# SRS-WORKFLOW-DEBT 审批记录

- 审批人：HardyDou（当前仓库所有者与会话用户）
- 审批日期：2026-07-14
- 审批结论：批准 Workflow 系统需求按 Payment 政策一次性实施。
- 审批范围：先使现有测试基础设施形成全绿 before，补 API/任务/事件/初始化保护，再规范 API、Core、Starter、Remote、Flyway 和 Resource Registry。
- 不变量：Java 方法与字段、HTTP/Feign 路径和 binding、权限码、租户/数据权限、Flowable 状态、任务动作、快照与事件顺序不得发生未批准变化。
- 验收：同一 Maven 测试入口、定向架构门禁、新 MySQL 启动、正式/Demo 两种初始化结果。
