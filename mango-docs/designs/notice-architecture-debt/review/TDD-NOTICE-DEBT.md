# TDD-NOTICE-DEBT 审批记录

- 审批人：HardyDou（当前仓库所有者与会话用户）
- 审批日期：2026-07-14
- 审批结论：批准 Notice 推荐技术方案。
- 技术范围：Service 按真实事务边界拆分，CRUD 聚合使用 MangoCrudService；实现类统一放入 `service/impl`；Entity/Mapper/协议/SPI 规范化；固定 HTTP 路径；单一纯 DDL V1；正式资源、Demo 与运行态数据分层。
- 兼容策略：先冻结业务方法、字段、权限、发送/重试/动作副作用；仓内远程和前端同批升级到唯一固定路径，不保留历史双协议。
- 消费者修复：完整启动前等价修复 Workflow 新 DTO 与 Payment 旧 Map 调用不匹配，并只跑 Payment 定向编译/测试。
- 完成条件：增强 before 在 after 保持，实际 663 条正式架构债务降为 0，新库结构等价且完整服务健康。
