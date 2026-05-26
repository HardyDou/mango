# 业务运行期模块分库设计交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RMDS-001 | 用户要求 | 给出业务运行期 Mapper 模块分库方案 | 使用模块路由数据源，默认单库兼容 | 设计说明 | 文档检查 | DONE | `mango-docs/plans/2026-05-26-runtime-module-datasource-design.md` |
| RMDS-002 | 用户要求 | 考虑事务和多数据源 | 本地事务只允许单物理数据源，跨库写默认拒绝 | 设计说明 | 文档检查 | DONE | `mango-docs/plans/2026-05-26-runtime-module-datasource-design.md` |
| RMDS-003 | 用户要求 | 考虑跨模块一致性 | 默认事件/Outbox/Saga，XA/Seata 作为显式增强 | 设计说明 | 文档检查 | DONE | `mango-docs/plans/2026-05-26-runtime-module-datasource-design.md` |
