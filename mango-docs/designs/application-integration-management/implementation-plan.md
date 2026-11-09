---
documentId: PLAN-APPLICATION-INTEGRATION-001
documentType: implementation-plan
pmoVersion: 1.4.9
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: "实施含真实数据库迁移、入站密钥与出站凭证加密、出站连接器、权限资源、后端 API、前端页面和当前部署上下文验证；本期不实现多租户运行时和接口级授权。"
status: DRAFT
action: WRITE
owner: Mango Tech Lead
approver: 待技术负责人确认
approvalEvidence: 待补充实施计划审批记录
upstreamDocumentId: TDD-APPLICATION-INTEGRATION-001
upstreamDocumentHash: ea613a1283636cdc78b999af3df931eeabe46b82084f7ee059440c5c2c235d66
---

# 逻辑应用与集成应用管理实施计划

## 0. 使用合同与最小填写方式

本计划把技术设计拆解为可执行工作包、验证门禁和资产同步动作。每个工作包都有路径、完成标准和验证证据；不以 mock 或固定返回替代真实链路。

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | MOD-001、DB-001 | 集成应用 API 契约和 migration | authorization api/core | 编译、迁移和真实回读通过 | SAC-002~007 | OAuth2 授权服务器、MTLS、SFTP |
| DEL-002 | MOD-002、SEC-002、SEC-003 | Service、双向凭证加密、当前部署上下文和事务 | authorization core | 集成测试通过 | SAC-002~007 | 接口级授权和调用审计 |
| DEL-003 | MOD-002、SEC-004 | 出站 HTTP 连接器和认证方式 | authorization core support | 单元和集成测试通过 | SAC-005 | 连接池、熔断和调度 |
| DEL-004 | MOD-003、SEC-001 | Controller、菜单和权限资源 | authorization starter | 资源同步和权限测试通过 | SAC-001、SAC-005 | 新用户体系 |
| DEL-005 | UI-001~UI-004 | RBAC 管理页面 | `mango-ui/packages/rbac` | 真实接口、状态和一次性提示通过 | SAC-002~005 | 浏览器凭证长期存储 |
| DEL-006 | IMP-001~IMP-004 | README 和能力地图 | authorization、rbac、mango-docs | 公开能力与未实现边界一致 | 交付门禁 | 未实现能力宣称 |
| DEL-007 | MOD-005、UI-005 | 微应用运行单元总览页面 | `mango-ui/packages/rbac` | 只读总览真实接口可用 | SAC-008 | 微应用登记写入口 |
| DEL-008 | API-010、SEC-004 | 运行单元健康检查接口 | authorization api/core/starter | 探测接口和受限目标校验通过 | SAC-009 | 任意地址探测 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | MOD-001、DEC-002 | DEL-001 | Backend | authorization-api | 无 | 添加命令、查询、结果、枚举和接口权限码 | 编译和校验通过 | VAL-001 | B1 | PLANNED |
| TASK-002 | DB-001~DB-004 | DEL-001 | Backend/DB | authorization-core migration | TASK-001 | 新表、唯一键、实体和 Mapper | 真实 migration 和回读通过 | VAL-002 | B1 | PLANNED |
| TASK-003 | FLOW-001~FLOW-005、SEC-002/003/005 | DEL-002 | Backend | authorization-core service | TASK-002 | 创建、分页、详情、编辑、启停、双向凭证和事务 | 集成测试通过 | VAL-003 | B2 | PLANNED |
| TASK-004 | FLOW-006、SEC-004、DEC-009、DEC-010 | DEL-003 | Backend | authorization-core support | TASK-003 | HTTP 连接器、认证方式和地址限制 | 连接测试单元用例通过 | VAL-004 | B2 | PLANNED |
| TASK-005 | API-001~008、SEC-001 | DEL-004 | Backend | authorization-starter | TASK-004 | Controller、权限和菜单资源 | API/resource 检查通过 | VAL-005 | B3 | PLANNED |
| TASK-006 | UI-001~UI-004 | DEL-005 | Frontend | `@mango/rbac` | TASK-005 | API、页面、分区表单、凭证提示和连接测试 | UI 真实链路通过 | VAL-006 | B3 | PLANNED |
| TASK-007 | TC-001~008、IMP-001~004 | DEL-006 | QA/PMO | tests/docs | TASK-006 | 测试、README、能力地图和 checker | 全部门禁通过 | VAL-007 | B4 | PLANNED |
| TASK-008 | MOD-005、UI-005、DEC-012 | DEL-007 | Frontend | `@mango/rbac` | TASK-006 | 微应用总览页面、引用关系和健康检查入口 | UI 真实链路通过 | VAL-008 | B4 | PLANNED |
| TASK-009 | API-010、SEC-004 | DEL-008 | Backend | authorization api/core/starter | TASK-003 | 运行单元健康检查接口与探测执行器复用 | 单元和集成测试通过 | VAL-009 | B4 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | TDD 契约确认 | API 契约编译通过 | TDD | 无 | Tech Lead | Backend |
| MS-002 | TASK-002、TASK-003 | MS-001 | 真实 DB、事务、当前部署上下文和脱敏通过 | MS-001 | 文档草稿 | Tech Lead | Backend |
| MS-003 | TASK-004 | MS-002 | 连接测试和地址限制通过 | MS-002 | UI 原型 | Tech Lead | Backend |
| MS-004 | TASK-005 | MS-003 | 菜单、apiCodes 和后端权限一致 | MS-003 | 文档更新 | Tech Lead | Backend |
| MS-005 | TASK-006 | MS-004 | 页面无 mock，状态齐全 | MS-004 | 文档更新 | Frontend | Frontend |
| MS-006 | TASK-009 | MS-005 | 健康检查接口通过单元和集成测试 | MS-005 | UI 联调 | Backend | Backend |
| MS-007 | TASK-008 | MS-006 | 微应用总览页面无 console error、无 4xx/5xx | MS-006 | 文档更新 | Frontend | Frontend |
| MS-008 | TASK-007 | MS-007 | 测试、checker、资产同步和 diff 通过 | MS-007 | 无 | PMO | QA |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-002 | TASK-001 | 编译 | `mvn -f mango/pom.xml -pl :mango-authorization-starter -am -DskipTests compile` | 开发环境 | 合法/非法命令 | 有/无权限 | 编译通过 | target reports | Backend | 修复后重跑 |
| VAL-002 | TC-002、TC-003 | TASK-002 | DB/集成 | 执行 migration，回读表结构和唯一键 | 测试数据库 | 当前部署 | 有效上下文 | 表和索引正确 | migration log | Backend | 回滚并修复 |
| VAL-003 | TC-001、TC-002、TC-003、TC-004、TC-007 | TASK-003 | 后端集成 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=IntegrationApplicationServiceTest test` | 测试数据库 | 当前部署、多版本 | 当前部署上下文 | 事务和脱敏通过 | surefire 报告 | Backend | 阻断交付 |
| VAL-004 | TC-007 | TASK-004 | 单元 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=OutboundHttpInvokerTest test` | 开发环境 | 回环地址、五种认证方式 | 当前部署 | 默认拒绝回环目标、请求头和签名正确 | surefire 报告 | Backend | 修复后重跑 |
| VAL-005 | TC-006、TC-008 | TASK-005 | API/资源 | 调用接口和资源 checker | 集成应用 | 有/无权限 | 缺少上下文拒绝 | 401/403 和业务错误一致 | API/resource output | Backend | 修复后重跑 |
| VAL-006 | TC-005 | TASK-006 | UI | `pnpm --filter mango-admin test:e2e -- e2e/specs/integration-application-management.spec.ts` | 集成前端 | 当前部署 | 权限角色 | 真实接口、凭证生命周期和连接测试正确 | UI report/screens | Frontend | 修复后重跑 |
| VAL-007 | TC-006 | TASK-007 | 质量/文档 | 四类 checker、diff、README/能力地图检查 | CI/开发环境 | 全量变更 | 无 | 全部通过 | CI output | QA/PMO | 关闭阻断 |
| VAL-008 | TC-009 | TASK-008 | UI | `pnpm --filter mango-admin test:e2e -- e2e/specs/micro-application.spec.ts` | 集成前端 | 当前部署运行单元 | 权限角色 | 总览真实数据、健康检查与引用关系正常、无 4xx/5xx | UI report/screens | Frontend | 修复后重跑 |
| VAL-009 | TC-010 | TASK-009 | 集成 | `mvn -f mango/pom.xml -pl :mango-authorization-core -Dtest=FrontendAppHealthServiceTest test` | 测试数据库与本地探测目标 | 已登记运行单元 | 当前部署 | 成功、失败、受限目标可区分 | surefire 报告 | Backend | 修复后重跑 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DB-001 | 测试库 | migration 版本和连接 | 新增表和唯一键 | 1 | 测试库备份 | 表结构回读 | migration 失败 | 回滚版本 | Backend |
| DATA-002 | DB-002 | 测试库 | Crypto 配置可用 | 创建当前部署测试数据 | 2 | 不导入真实密钥 | 只见密文 | 出现明文 | 删除测试数据、修复代码 | Security |
| DATA-003 | DB-003、DB-004 | 测试库 | Service 测试通过 | 保存出站凭证并测试连接 | 3 | 保留旧版本断言 | 旧版本失败时保持 | 事务不一致 | 恢复旧密文和版本 | QA |
| DATA-004 | DB-001 | 生产预发布 | 安全确认和备份 | 执行正式 migration | 4 | 正式备份 | migration/健康检查 | 备份或密钥未确认 | 停止发布 | Release |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | DEL-001~004 | `mango-authorization/README.md` | 增加集成应用 API、表、菜单、入站与出站边界 | Backend | README 与代码一致 | git diff/文档检查 | 无 |
| DOC-002 | DEL-005 | rbac README | 增加页面和权限入口 | Frontend | 页面可定位 | rg 页面 key | 无 |
| DOC-003 | DEL-006 | `mango-docs/capabilities/README.md` | 标记真实已启用和未实现运行时边界 | PMO | 能力地图准确 | capability check | 无 |
| DOC-004 | DEL-004、DEL-007 | `authorization-common-menu.json` | AUTH_MENU 目录结构、微应用入口和 apiCodes | Backend | 资源同步通过 | resource checker | 无 |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | 入站密钥或出站凭证明文落库或日志 | 凭证泄露 | Crypto codec、脱敏测试 | 停止发布、轮换和清理 | Security | 发布前 | OPEN | NONE | NONE |
| RISK-002 | L1 | RISK | 当前部署上下文缺失 | 请求无法归属当前部署 | Service 上下文校验 | 拒绝请求并保留数据不变 | Tech Lead | 开发验证前 | CLOSED | NONE | NONE |
| RISK-003 | L3 | BLOCKER | 生产密钥托管策略未确认 | 无法安全发布 | 安全/运维评审 | 阻断发布 | Security | 发布前 | OPEN | NONE | NONE |
| RISK-004 | L2 | RISK | 出站连接被用于内网探测 | 内网暴露 | 默认拒绝回环和私网、显式配置放开 | 关闭配置并审计 | Security | 发布前 | CLOSED | NONE | NONE |
| RISK-005 | L1 | EXCEPTION | 本期不做接口级授权和调用审计 | 外部调用范围暂不承诺 | 文档明确范围 | 后续独立需求 | Product | 后续需求 | CLOSED | future-scope | 待批准 |
| RISK-006 | L1 | EXCEPTION | 本期不做集成应用行级数据权限 | 组织隔离需求暂不承诺 | 文档明确范围 | 后续独立需求 | Product | 后续需求 | CLOSED | future-scope | 待批准 |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| MOD-001、API-001~008 | DEL-001、DEL-004 | TASK-001、TASK-005 | VAL-001、VAL-005 | MS-001、MS-002、MS-004 | API 和管理入口 |
| DB-001~004、FLOW-001~005 | DEL-001、DEL-002 | TASK-002、TASK-003 | VAL-002、VAL-003 | DATA-001、DATA-002、DATA-003、DATA-004、RISK-001、RISK-002、RISK-003 | 持久化和事务 |
| FLOW-006、SEC-004、DEC-009、DEC-010 | DEL-003 | TASK-004 | VAL-004 | MS-003、RISK-004 | 出站连接能力 |
| UI-001~004 | DEL-005 | TASK-006 | VAL-006 | MS-005、MS-006 | 页面真实链路 |
| IMP-001~004 | DEL-006 | TASK-007 | VAL-007 | DOC-001、DOC-002、DOC-003、DOC-004、RISK-005 | 资产同步 |
| DEC-001、DEC-005、DEC-006、DEC-007、DEC-008、DEC-011 | DEL-001、DEL-005 | TASK-001、TASK-006 | VAL-005、VAL-006 | MS-001、MS-006、RISK-006 | 设计决策覆盖 |
| MOD-005、UI-005、DEC-012 | DEL-007 | TASK-008 | VAL-008 | MS-007 | 微应用总览 |
| API-010、SEC-004 | DEL-008 | TASK-009 | VAL-009 | MS-006、MS-008 | 运行单元健康检查 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | 待执行 | `node mango-pmo/tools/check-implementation-plan.mjs --document ...` |
| 依赖图 | 已列出 | MS-001→MS-006 和任务依赖字段。 |
| 生命周期 handoff | 待完成 | TDD 审批后生成 SHA-256 并回填。 |
| 未关闭阻断数量 | 2 | RISK-001、RISK-003。 |
| 例外 | 2 | RISK-005、RISK-006，需后续范围批准。 |
| 实施审批 | 待确认 | 当前为研发草案。 |
