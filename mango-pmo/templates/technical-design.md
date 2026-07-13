---
documentId: {{DOCUMENT_ID}}
documentType: technical-design
pmoVersion: {{PMO_VERSION}}
schemaRevision: 1
riskLevel: {{RISK_LEVEL}}
riskAssessmentEvidence: {{RISK_ASSESSMENT_EVIDENCE}}
status: DRAFT
action: WRITE
owner: {{OWNER}}
approver: {{APPROVER}}
approvalEvidence: {{APPROVAL_EVIDENCE}}
upstreamDocumentId: {{SRS_DOCUMENT_ID}}
upstreamDocumentHash: {{SRS_SHA256}}
---

# {{REQUIREMENT_NAME}} 技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| {{DEC_ID}} | {{PROBLEM}} | {{OPTIONS}} | {{DECISION}} | {{RATIONALE}} | {{SOURCE}} | 否 | {{IMPACT}} | {{RISK}} | {{FALLBACK_CONDITION}} |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| {{MOD_ID}} | {{MODULE_OR_PACKAGE}} | {{RESPONSIBILITY}} | {{CHANGE_TYPE}} | {{DEPENDENCY_DIRECTION}} | {{PUBLIC_CAPABILITY}} | {{SRS_IDS}} | {{RULE_IDS}} | {{VALIDATION}} |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| {{DM_ID}} | {{BO_DR_OR_FR_ID}} | {{MODEL_RESPONSIBILITY}} | {{IDENTITY}} | {{RELATIONSHIPS}} | {{STATE_ENCODING}} | {{AUDIT_OR_HISTORY}} | {{OWNERSHIP_OR_TENANT}} | {{CONSISTENCY}} |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| {{DM_ID}} | {{CURRENT_STATE}} | {{TRIGGER}} | {{TARGET_STATE}} | {{PRECONDITION}} | {{SIDE_EFFECT}} | {{FAILURE_HANDLING}} | {{SRS_IDS}} |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| {{FLOW_ID}} | {{SRS_IDS}} | {{ENTRY}} | {{MODULES}} | {{PROCESS_SEQUENCE}} | {{TRANSACTION_BOUNDARY}} | {{STATE_CHANGE}} | {{IDEMPOTENCY_KEY}} | {{CONCURRENCY_STRATEGY}} | {{FAILURE_AND_COMPENSATION}} | {{VISIBLE_RESULT}} |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| {{API_ID}} | {{SRS_IDS}} | {{CALLER}} | {{MODULE}} | {{ENTRY_TYPE}} | {{METHOD_AND_PATH}} | {{REQUEST_OR_VIEW_CONTRACT}} | {{RETURN_CONTRACT}} | {{VALIDATION_RULE}} | {{SECURITY_BOUNDARY}} | {{PROTOCOL_BEHAVIOR}} | {{ERROR_CODES}} | {{COMPATIBILITY}} | {{API_RULE_IDS}} | {{VALIDATION}} |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| {{DB_ID}} | {{DM_DR_OR_FR_ID}} | {{TABLE_OR_ENTITY}} | {{FIELD_CHANGES}} | {{CONSTRAINTS}} | {{INDEXES}} | {{TENANT_AND_AUDIT}} | {{MAPPER_BOUNDARY}} | {{DATA_SOURCE}} | {{MIGRATION_OR_BACKFILL}} | {{ROLLBACK_OR_COMPENSATION}} | {{DB_RULE_IDS}} | {{VALIDATION}} |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{SEC_ID}} | {{SRS_IDS}} | {{CAPABILITY}} | {{PERMISSION_RESOURCE}} | {{DEFAULT_GRANT}} | {{BACKEND_GUARD}} | {{TENANT_BOUNDARY}} | {{OWNERSHIP_ASSERTION}} | {{FRONTEND_FEEDBACK}} | {{AUDIT}} | {{SECURITY_RULE_IDS}} | {{VALIDATION}} |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| {{ERR_ID}} | {{SRS_IDS}} | {{FAILURE_SCENARIO}} | {{TRIGGER}} | {{ERROR_CODE}} | {{EXCEPTION_TYPE}} | {{USER_FEEDBACK}} | {{LOG_CONTEXT}} | {{METRIC_OR_ALERT}} | {{RETRY_OR_COMPENSATION}} | {{SENSITIVE_DATA_HANDLING}} |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| {{UI_ID}} | {{PG_BT_OR_FR_ID}} | {{PAGE_OR_ACTION}} | {{PAGE_KEY_OR_ROUTE}} | {{SURFACE_AND_COMPONENT}} | {{STATE_SOURCE}} | {{API_ID}} | {{PERMISSION_OR_DISABLED}} | {{EMPTY_LOADING_ERROR}} | {{SEMANTIC_TEST_ANCHOR}} | {{REUSE_DECISION}} | {{FRONTEND_RULE_IDS}} |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| {{TC_ID}} | {{SAC_ID}} | {{DESIGN_IDS}} | {{SCENARIO}} | P1 | {{TEST_LEVEL}} | {{AUTO_MANUAL_OR_EXCEPTION}} | {{TEST_DATA}} | {{SECURITY_BOUNDARY}} | {{STABLE_CONTRACT}} | {{EXECUTION_ENTRY}} | {{EVIDENCE}} | {{FAILURE_HANDLING}} | {{TEST_RULE_IDS}} |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| {{IMP_ID}} | {{DESIGN_IDS}} | {{AFFECTED_ASSET}} | {{CURRENT_BEHAVIOR}} | {{TARGET_BEHAVIOR}} | {{COMPATIBILITY}} | {{UPGRADE_OR_ROLLBACK}} | {{DOC_IMPACT}} | {{RELEASE_BATCH}} | {{VALIDATION}} | {{OWNER}} |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| {{SRS_ID}} | {{DESIGN_IDS}} | {{TC_ID}} | {{COVERAGE}} |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | {{PASS_OR_FAIL}} | {{CHECK_COMMAND_AND_OUTPUT}} |
| 生命周期 handoff | {{PASS_OR_FAIL}} | {{HANDOFF_EVIDENCE}} |
| 专项规范检查计划 | {{PASS_OR_FAIL}} | {{SPECIALIZED_CHECKS}} |
| 未关闭阻断数量 | {{BLOCKER_COUNT}} | {{BLOCKER_EVIDENCE}} |
| Tech Lead 审批 | {{APPROVED_OR_REJECTED}} | {{APPROVAL_EVIDENCE}} |
