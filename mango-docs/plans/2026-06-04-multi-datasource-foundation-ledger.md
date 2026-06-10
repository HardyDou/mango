# Mango 多数据源底座交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| MDS-001 | 用户要求 | 先解决多数据源底层能力 | 能力归属 `mango-infra-persistence-starter` | 设计说明、开发计划 | 文档存在且写清模块归属 | DONE | `mango-docs/designs/mango-multi-datasource-foundation-design.md` |
| MDS-002 | 用户要求 | 支持 Job 独立数据库 | 通过模块到数据源映射支持 `mango-job -> job` | 设计说明 | 配置示例存在 | DONE | `mango-docs/designs/mango-multi-datasource-foundation-design.md` |
| MDS-003 | 用户要求 | 支持 PowerJob 独立数据库 | PowerJob 引擎库独立，Mango 不维护其内部表 | 设计说明 | 数据库独立策略存在 | DONE | `mango-docs/designs/mango-multi-datasource-foundation-design.md` |
| MDS-004 | 用户要求 | 支持灵活部署 | 单体、微服务、共享 Job Center 均通过配置组合 | 设计说明 | 文档写清部署不绑定业务代码分支 | DONE | `mango-docs/designs/mango-multi-datasource-foundation-design.md` |
| MDS-005 | PMO 规范 | 开发前明确范围、接口、数据、测试 | 设计说明和计划分离 | 设计说明、计划 | 结构完整 | DONE | `mango-docs/plans/2026-06-04-multi-datasource-foundation-plan.md` |

## 验收证据记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| MDS-001 | 文档 | 多数据源底座目标 | 不适用 | 设计说明和计划已创建 | 不涉及页面 | 不涉及前端网络 | 本台账 | DONE |
| MDS-002 | 文档 | Job 独立数据库 | 不适用 | 文档包含 `mango-job -> job` 映射 | 不涉及页面 | 不涉及前端网络 | 设计说明 | DONE |
| MDS-003 | 文档 | PowerJob 独立数据库 | 不适用 | 文档声明 PowerJob 数据库由 PowerJob 管理 | 不涉及页面 | 不涉及前端网络 | 设计说明 | DONE |
| MDS-004 | 文档 | 灵活部署 | 不适用 | 文档声明部署形态通过配置组合 | 不涉及页面 | 不涉及前端网络 | 设计说明 | DONE |
| MDS-005 | 文档 | PMO 结构 | 不适用 | 文档覆盖范围、接口、数据、测试 | 不涉及页面 | 不涉及前端网络 | 计划文档 | DONE |
