# Mango 文档索引

这里只放设计和计划文档。

规范入口在：

- [mango-pmo](../mango-pmo/)

## 顶层设计

| 文档 | 说明 |
|------|------|
| [mango-architecture-design.md](./mango-architecture-design.md) | 顶层设计 |
| [mango-backend-architecture-boundary-refactor-master-plan.md](./mango-backend-architecture-boundary-refactor-master-plan.md) | 后端边界收敛总计划 |

## Sprint 计划

| 计划 | 状态 | 说明 |
|------|------|------|
| [sprint-00](./plans/2026-04-07-sprint-00-mango-module-architecture-plan.md) | 已完成 | 模块架构总纲 |
| [sprint-03](./plans/2026-04-08-sprint-03-mango-infra-kv-iucase-refactor.md) | 已完成 | mango-infra-kv IUCASE 重构 |
| [sprint-04](./plans/2026-04-08-sprint-04-mango-infra-kv-memoryxistore-fix.md) | 已完成 | kv MemoryXistore 修复 |
| [sprint-04b](./plans/2026-04-08-sprint-04b-dal-naming-fix.md) | 已完成 | kv 命名修复 |
| [sprint-05](./plans/2026-04-08-sprint-05-mango-infra-crypto.md) | 已完成 | 国密算法实现 |
| [sprint-06](./plans/2026-04-08-sprint-06-mango-infra-security.md) | 已完成 | 权限注解、AOP 切面 |
| [sprint-07](./plans/2026-04-08-sprint-07-mango-rbac-refactor.md) | 已完成 | auth × rbac 重构 |
| [sprint-09](./plans/2026-04-14-sprint-09-mango-common-kernel-contract-refactor.md) | 已完成 | mango-common 收敛 |
| [sprint-09-delivery](./plans/2026-04-15-sprint-09-delivery-record.md) | 已完成 | Sprint 09 交付记录 |
| [sprint-10](./plans/2026-04-14-sprint-10-infra-web-security-boundary-decoupling.md) | 已完成 | infra-web / infra-security 去业务依赖 |
| [sprint-11](./plans/2026-04-14-sprint-11-platform-rbac-system-boundary-phase1.md) | 进行中 | rbac / system 第一阶段边界收敛 |
| [sprint-12](./plans/2026-04-14-sprint-12-auth-admin-app-boundary-assembly-cleanup.md) | 待执行 | auth / admin-app 边界收口 |
| [sprint-13](./plans/2026-04-14-sprint-13-frontend-monorepo-migration.md) | 已完成 | 前端 Monorepo 迁移 |
| [sprint-14](./plans/2026-04-17-sprint-14-pmo-backend-rules-engineering.md) | 进行中 | PMO 后端规则工程化 |
| [sprint-15](./plans/2026-04-17-sprint-15-capability-registry-remote-adapter.md) | 待执行 | 能力自动注册与 Remote Adapter 重构 |

## 后端待执行顺序

| 顺序 | Sprint | 原因 |
|------|--------|------|
| 1 | [sprint-14](./plans/2026-04-17-sprint-14-pmo-backend-rules-engineering.md) | 先固化规则分类和自动检查落点 |
| 2 | [sprint-15](./plans/2026-04-17-sprint-15-capability-registry-remote-adapter.md) | 再实现能力自动注册和 remote adapter 重构 |
| 3 | [sprint-12](./plans/2026-04-14-sprint-12-auth-admin-app-boundary-assembly-cleanup.md) | 最后基于新机制清理 auth / admin-app 装配边界 |
