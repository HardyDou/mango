# Sprint 14 人工检查规则

## 1. PR 必查项

| Rule ID | Check |
|---|---|
| BE-CODE-001 | 代码是否可读、可测、可维护 |
| BE-CODE-003 | 是否存在无意义包装层 |
| BE-CODE-004 | 是否把临时方案当正式方案提交 |
| BE-CODE-006 | 类职责是否单一 |
| BE-CODE-007 | 一个类是否只解决一类问题 |
| BE-CODE-008 | 是否有逻辑越过当前抽象层 |
| BE-API-009 | 简单路径参数和查询参数是否直接放方法签名 |
| BE-API-016 | Controller 是否只做协议适配 |
| BE-API-019 | `XxxApi` 是否只定义能力契约 |
| BE-DB-002 | 表名是否体现模块边界 |
| BE-DB-004 | 一个表是否只服务一个领域 |
| BE-DB-007 | 多租户表是否按需要增加 `tenant_id` |
| BE-DB-012 | 是否修改了历史 migration |
| BE-PER-002 | 同一业务动作是否只有一个主事务边界 |
| BE-MOD-014 | 服务能力维护是否放在 `mango-infra` |
| BE-TEST-001 | 代码改动是否有对应验证 |
| BE-TEST-002 | 新增业务逻辑是否补测试 |
| BE-TEST-003 | 修复缺陷是否补回归测试 |
| BE-FLOW-001 | 是否只改本次需求相关代码 |
| BE-FLOW-003 | 是否执行对应检查 |

## 2. Sprint 验收必查项

| Area | Check |
|---|---|
| API | 新增接口不暴露 `Entity`、`PO`、默认 `DTO` |
| API | 新增接口参数有 Bean Validation |
| API | 新增返回对象统一 `XxxVO` |
| Module | 新增模块符合 `api`、`core`、`starter`、`starter-remote` 职责 |
| Remote | 远程调用不硬编码服务发现名 |
| Remote | `starter` 提供对外能力时有能力注册设计 |
| Require | 业务入口使用 `Require` 做前置条件校验 |
| Security | 外部输入有校验，敏感信息不进入日志 |
| DB | DDL 使用 Flyway migration |
| Test | 关键路径、参数校验、异常场景有验证 |

## 3. 人工结论格式

PR 或 Sprint 验收必须写明：

| Field | Requirement |
|---|---|
| Scope | 本次检查覆盖的模块 |
| Passed | 通过的规则 |
| Failed | 未通过的规则 |
| Deferred | 延后处理的规则 |
| Risk | 遗留风险 |
| Verification | 已执行命令或人工验证说明 |

