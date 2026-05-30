# Mango Frontend Productization Correction Ledger

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| CORR-001 | 用户要求 | 制定新的补救计划，并明确每个 Sprint 的人工验收内容 | 新增 correction plan，按 Sprint 列出目标、范围、自动验证、人工验收和退出标准 | `mango-docs/plans/2026-05-30-mango-frontend-productization-correction-plan.md` | 人工审阅计划内容；PMO 台账结构检查 | DONE | `mango-docs/plans/2026-05-30-mango-frontend-productization-correction-plan.md` |
| CORR-002 | 用户要求 | 默认必须复用完整原 Mango Admin，不允许仿写壳 | 设定 single-source admin shell 为不可协商设计决策，Sprint 2 专门验收 | correction plan Sprint 2 | E2E 截图对比；用户确认不是复制/仿写 shell | TODO |  |
| CORR-003 | 用户要求 | 每个 Sprint 完成后都必须回归测试、新特性测试和截图识别 | 在 verification contract 中设置每 Sprint 停止门禁 | correction plan section 5.5 | 每 Sprint evidence 目录包含截图、layout report、summary 和 manual acceptance | TODO |  |
| CORR-004 | 用户要求 | 菜单必须稳定，默认 full 模式不能随意增加菜单 | 将后端菜单设为 full/default source of truth，能力包只提供解析和资源声明 | correction plan Sprint 3 | 后端菜单 capture 与页面菜单截图一致；用户人工确认 | TODO |  |
| CORR-005 | 用户要求 | 登录页和主框架样式不能靠手写新 CSS 修 | 将 style contract 设为包出口问题，`@mango/admin/style.css` 聚合样式 | correction plan Sprint 4 | package consumption E2E；登录页截图与 baseline 对比 | TODO |  |
| CORR-006 | 用户要求 | 业务项目 init 后应得到可用 Mango，再按配置自定义 | 将 create-mango-app full preset 放到 Sprint 6，custom preset 放到 Sprint 7 | correction plan Sprint 6/7 | 生成项目真实启动、截图对比、用户体验验收 | TODO |  |
| CORR-007 | 用户要求 | 支持单体、微前端、混合部署，但先保证基础特性稳定 | 将 deployment mode matrix 放到 Sprint 8，禁止提前扩展远程治理能力 | correction plan Sprint 8 | local/micro/mixed E2E 截图和报告；用户确认无范围扩张 | TODO |  |
| CORR-008 | PMO 要求 | 计划必须明确不做什么，防止范围扩大 | 明确列出 gray release、remote registry、CDN governance 等 out of scope | correction plan section 3 | 人工审阅计划；后续 Sprint 按 out of scope 检查 | TODO |  |
| CORR-009 | PMO 要求 | 历史失败分支不能作为完成证明 | 将失败分支降级为 salvage input，Sprint 1 做 KEEP/REWORK/DROP 矩阵 | correction plan Sprint 1 | salvage report 由用户确认后才能进入实现 | TODO |  |
| CORR-010 | PMO 要求 | 每 Sprint 必须人工验收后才能进入下一阶段 | 每 Sprint 设置 manual acceptance 和 exit criteria | correction plan section 6 | 每 Sprint `manual-acceptance.md` 存档 | TODO |  |
