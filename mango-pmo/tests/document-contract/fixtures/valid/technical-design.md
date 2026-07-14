---
documentId: TDD-ANN-001
documentType: technical-design
pmoVersion: 1.2.1
schemaRevision: 1
riskLevel: L2
riskAssessmentEvidence: SRS-ANN-001 risk assessment
status: APPROVED
action: NEXT
owner: Tech Lead
approver: 架构负责人
approvalEvidence: review/TDD-ANN-001.md
upstreamDocumentId: SRS-ANN-001
upstreamDocumentHash: 0000000000000000000000000000000000000000000000000000000000000000
---

# 公告审核技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 审核记录是否独立保存 | 覆盖主记录或保存不可变历史 | 保存不可变历史 | 满足审核追溯和审计要求 | DR-001, SAC-001 | 否 | 增加审核历史模型 | 数据量增加 | 设计评审否决审计要求时重新评估 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | notice api, core, starter and starter-remote | api 提供契约，core 实现业务，starter 组装，starter-remote 提供远程消费 | 新增 | 消费层依赖契约层，组装层依赖实现层 | 公告提交和查询契约 | SC-001, FR-001, IR-001 | rules/backend/05-module.md | 模块架构检查与编译 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 | 保存公告当前状态和不可变审核历史 | 企业内公告标识 | 公告一对多关联审核历史 | EDITABLE, PENDING, APPROVED, REJECTED | 审核历史不可覆盖 | tenantId 由上下文注入 | 最终决定只能存在一个有效版本 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | EDITABLE | 提交审核 | PENDING | 必需信息完整 | 写入提交时间和审核历史 | 保持 EDITABLE 并返回缺项 | FR-001, UC-001, SAC-001 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, UC-001, SAC-001 | 公告提交入口 | MOD-001 | 校验、锁定当前状态、写入历史、更新状态、提交后通知 | 历史和状态更新在单一事务 | EDITABLE 到 PENDING | 企业标识加公告标识加提交版本 | 乐观版本冲突返回稳定错误 | 通知失败形成重试记录且不回滚决定 | 成功显示待审核，冲突显示刷新提示 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, UC-001, SAC-001 | 管理端和远程消费者 | MOD-001 | Controller and Feign | POST /notices/submit | SubmitNoticeCommand and NoticeVO | R<NoticeVO> | Require 校验必填和状态 | 登录用户、当前租户和公告归属三重校验 | requestId 幂等，不适用分页 | NOTICE_STATE_CONFLICT | 新增入口不改变既有查询 | rules/backend/03-api.md | 契约测试和负面参数测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DR-001, FR-001, DM-001 | notice and notice_audit, NoticeEntity and NoticeAuditEntity | 增加状态版本和审核历史字段 | 企业内 requestId 唯一 | tenant_id, notice_id and created_time | 租户与审计字段自动处理 | Mapper 仅持久化，MangoCrudService 承载业务 CRUD | 用户提交和审核决定 | migration 新建历史表，存量无需回填 | 发布前备份，失败时删除空历史表并恢复应用 | rules/backend/04-db.md, rules/backend/07-persistence.md | migration 测试和租户隔离测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001, FR-001, DR-001, SAC-001 | 提交公告 | notice:submit | 运营岗位套餐 | API-001 对应服务入口 | 从登录上下文取得 tenantId | 公告 tenantId 必须等于上下文 tenantId | 无权或跨租户时显示不可操作原因 | 记录操作人、租户和公告标识 | rules/backend/06-security.md | 无权限和跨租户负面测试 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001, UC-001, SAC-001 | 状态冲突 | 并发提交导致版本变化 | NOTICE_STATE_CONFLICT | BusinessException | 公告状态已变化，请刷新后重试 | requestId, tenantId, noticeId and oldState | 状态冲突计数 | 用户确认后重新提交 | 不记录公告正文和凭证 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-001, PG-001, BT-001, SAC-001 | 公告工作区和提交审核 | notice.workspace and /notice/workspace | 信息表单、状态区和动作区 | 查询结果与本地编辑状态 | API-001 | SEC-001 决定可操作状态 | 五种状态均映射 SRS 文案 | data-page notice.workspace and data-action notice.submit | 使用现有表单模式，不新增公共组件 | rules/frontend/07-admin-ui-common.md |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | 完整提交、缺项、状态冲突和跨租户 | P1 | API and E2E | AUTO | 每用例创建独立企业公告 | 普通运营账号和第二租户负例 | API 契约和语义锚点 | tagged contract and browser suites | 测试报告、trace 和截图 | 任一核心断言失败即阻断 | rules/09-test-case-automation-flow.md |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-001, MOD-001, API-001, DB-001, UI-001 | 模块消费者和业务项目 | 没有公告审核能力 | 提供可追踪提交和审核 | 只新增能力且不改变既有契约 | 先发布兼容数据变更再发布应用，可按批次回滚 | 更新模块 README 和能力地图 | api, core, starter then frontend | 消费项目契约和端到端验证 | 发布负责人 |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, UC-001, PG-001, BT-001, DR-001, IR-001, NFR-001, SAC-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | TC-001 | 所有系统要求均有设计和测试承接 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | check-technical-design 输出 |
| 生命周期 handoff | PASS | SRS 摘要和追踪检查输出 |
| 专项规范检查计划 | PASS | 模块、API、数据、安全、前端和测试门禁清单 |
| 未关闭阻断数量 | 0 | 无开放阻断 |
| Tech Lead 审批 | APPROVED | review/TDD-ANN-001 |
