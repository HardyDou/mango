# Mango 企业业务开发框架正式推广交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| ER-001 | 用户要求 | 制定详细计划，说明目的和验收方式 | 新增推广计划，按推广门槛和 Sprint 拆分 | `mango-docs/plans/2026-06-02-enterprise-readiness-plan.md` | 人工审阅计划；台账检查 | DONE | `mango-docs/plans/2026-06-02-enterprise-readiness-plan.md` |
| ER-002 | 用户要求 | 先让 Mango 达到稳定企业业务开发框架推广状态 | 将推广门槛拆成阻塞修复、业务仿真、规范固化、发布回归四阶段 | 本台账和后续 Sprint 交付记录 | 每阶段台账项 DONE 或 EXCEPTION | IN_PROGRESS | 本台账；后续 Sprint 证据待补齐 |
| ER-003 | 用户验收 | 单体文件上传组件 404 需要修复 | 定位单体菜单、路由、组件导出或资源路径问题，不用静默兜底掩盖缺失组件 | 回归截图；验收报告 | 单体浏览器打开文件上传组件页；验证主内容、上传入口、console/network | DONE | `mango-docs/evidence/2026-06-02-enterprise-readiness/screenshots/upload-components.png` |
| ER-004 | 用户验收 | 单体工作流组件 404 需要修复 | 定位单体菜单、路由、组件导出或资源路径问题，不用静默兜底掩盖缺失组件 | 回归截图；验收报告 | 单体浏览器打开工作流组件页；验证主内容、关键示例、console/network | DONE | `mango-docs/evidence/2026-06-02-enterprise-readiness/screenshots/workflow-components.png` |
| ER-005 | 用户验收 | 表单设计器首个组件拖拽异常需要修复 | 定位 form-create/designer 配置与组件 schema，避免吞异常或假成功 | 回归截图；验收报告 | 进入审批中心流程定义表单信息内置设计器，拖入首个组件，断言无 Vue error 且画布出现组件 | DONE | `mango-docs/evidence/2026-06-02-enterprise-readiness/screenshots/workflow-designer-drag.png` |
| ER-006 | 用户验收 | 控制台 CSP/X-Frame-Options meta 配置错误提示需要处理 | 判断是否配置位置错误或浏览器不支持 meta，移到服务端 header 或删除无效 meta | 单体和微前端验收均未复现 CSP/X-Frame 提示；微前端另有事件参数 warning 待治理 | 浏览器验收 console 无该错误，或记录非阻塞证据 | IN_PROGRESS | `mango-docs/evidence/2026-06-02-enterprise-readiness/micro-readiness-report.json` |
| ER-007 | 用户验收 | `/system/user` 分页 total String/Number warning 需要处理 | 统一分页 total 数值类型，优先修正 API 适配层 | 回归截图；验收报告 | 打开用户页，分页显示正常，无 Vue prop type warning | DONE | `mango-docs/evidence/2026-06-02-enterprise-readiness/screenshots/system-user.png` |
| ER-008 | 用户验收 | Element Plus 分页 deprecated warning 需要处理 | 替换废弃用法，遵循 Element Plus 当前 API | 本轮用户页未复现；更多分页页待抽查 | 涉及分页页面无 deprecated warning | IN_PROGRESS | `mango-docs/evidence/2026-06-02-enterprise-readiness/monolith-readiness-report.json` |
| ER-009 | GitHub Issue #31 | E2E API baseURL 去硬编码治理 | 评估现有分支，统一 E2E baseURL 配置 | 代码修复或 PR 合并 | Playwright 配置和测试引用检查；关键 E2E 可运行 | TODO | 待 issue #31 专项评审 |
| ER-010 | GitHub Issue #32 | `@mango/admin-shell` 产品级 API 与文档打磨 | 判断是否为正式推广阻塞项；阻塞部分优先处理 | 评审结论或代码/文档 | API 文档和消费示例可用于业务项目 | TODO | 待 issue #32 专项评审 |
| ER-011 | GitHub Issue #28 | 初始化种子数据能力 | 判断业务项目独立启动是否依赖种子数据；必要项纳入推广前置 | 评审结论或代码 | 业务项目初始化后菜单、角色、租户基础数据可用 | TODO | 待 issue #28 专项评审 |
| ER-012 | GitHub Issue #30 | Mango Initializr 服务化与 Web UI | 不作为正式推广前置，CLI 先满足企业使用 | 计划说明 | 台账标记 EXCEPTION 或后续项 | TODO | 待正式推广范围确认 |
| ER-013 | 用户要求 | 企业业务项目全流程真实验证 | 用 `mango-cli` 生成独立业务项目并新增真实 CRUD 模块 | 验收报告和命令证据 | CLI 初始化、模块新增、后端构建和 PMO 激活通过；前端 build 失败项修复后才能置为 DONE | IN_PROGRESS | `mango-docs/evidence/2026-06-02-enterprise-readiness/enterprise-business-flow-report.md` |
| ER-014 | 用户要求 | 所有模块抽查至少 3 个菜单页面、2 个真实功能 | 建立浏览器验收清单，记录截图和 UI 分析 | 验收证据 | 每模块记录页面、功能点、UI、console/network 和截图 | DONE | `mango-docs/evidence/2026-06-02-enterprise-readiness/acceptance-evidence.md` |
| ER-015 | 用户要求 | 全仓后端测试 | 推广前执行全仓后端测试 | 测试日志 | `mvn test` 或等价全仓测试通过 | TODO | Sprint 4 待执行 |
| ER-016 | PMO 规则 | 交付前必须检查台账 | 使用 delivery-contract-check 验证计划和台账 | 检查结果 | `node mango-pmo/tools/delivery-contract-check.mjs --design ... --ledger ... --mode verify` | IN_PROGRESS | 本轮执行 plan 模式；verify 模式待所有项收口 |
