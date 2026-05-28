# Mango 业务项目开发模式评审交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| BPM-001 | 用户要求 | 将业务项目开发模式形成文档 | 在 `mango-docs/plans` 新增评审方案，覆盖框架业务分离、业务单仓、模板初始化、PMO 继承、前后端协作和 AI 实现责任 | `mango-docs/plans/2026-05-27-business-project-development-model-review.md` | 文档存在且包含目标、范围、设计输入、总体决策、落地阶段和风险 | DONE | `mango-docs/plans/2026-05-27-business-project-development-model-review.md` |
| BPM-002 | 用户要求 | 组织评审这个方案 | 在评审方案中定义评审目标、参会角色、评审议题、评审前准备和评审输出记录路径 | `mango-docs/plans/2026-05-27-business-project-development-model-review.md` | 文档包含“评审组织方案”章节和评审议题表 | DONE | `mango-docs/plans/2026-05-27-business-project-development-model-review.md` |
| BPM-003 | PMO 要求 | 实质设计任务必须建立交付台账 | 为本次方案文档化和评审组织建立独立交付台账 | `mango-docs/plans/2026-05-27-business-project-development-model-review-ledger.md` | 交付台账检查通过 | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-27-business-project-development-model-review.md --ledger mango-docs/plans/2026-05-27-business-project-development-model-review-ledger.md --mode verify` |
| BPM-004 | 用户追问 | 补充评审结果 | 新增 AI 预评审结果记录，明确总体结论、议题结论、关键决策、必须补充验证和待人工确认问题 | `mango-docs/plans/2026-05-27-business-project-development-model-review-record.md` | 评审记录存在且明确不是已完成真实会议评审 | DONE | `mango-docs/plans/2026-05-27-business-project-development-model-review-record.md` |
