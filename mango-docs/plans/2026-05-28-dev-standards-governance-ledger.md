# 前后端开发规范治理交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| STD-001 | 用户要求 | 前端拿到需求后先提取组件、类型、辅助数据，并优先形成支持单选、多选、输入检索的下拉选择能力 | 新增 Element Plus UI 规范，明确需求拆解和选择组件规则 | `mango-pmo/rules/frontend/02-element-plus-ui.md` | 人工检查规范内容覆盖组件提取、字典、辅助数据、单选、多选、输入检索 | DONE | `mango-pmo/rules/frontend/02-element-plus-ui.md` |
| STD-002 | 用户要求 | 统一前端列表布局、搜索区域、列表区域、功能区域和列表按钮样式 | 在 Element Plus UI 规范中定义列表页布局、搜索区、功能区、列表区、分页区和行按钮规则 | `mango-pmo/rules/frontend/02-element-plus-ui.md` | 人工检查规范内容覆盖列表页各区域和按钮顺序 | DONE | `mango-pmo/rules/frontend/02-element-plus-ui.md` |
| STD-003 | 用户要求 | 追加字体、字号、注释、标题、正文等规范 | 在 Element Plus UI 规范中定义文案层级和字号边界 | `mango-pmo/rules/frontend/02-element-plus-ui.md` | 人工检查规范内容覆盖页面标题、区块标题、正文、辅助说明、注释 | DONE | `mango-pmo/rules/frontend/02-element-plus-ui.md` |
| STD-004 | 用户要求 | 追加状态和 tag 样式规范 | 在 Element Plus UI 规范中统一 `ElTag` 状态语义映射 | `mango-pmo/rules/frontend/02-element-plus-ui.md` | 人工检查规范内容覆盖成功、处理中、草稿、失败和未知状态 | DONE | `mango-pmo/rules/frontend/02-element-plus-ui.md` |
| STD-005 | 用户要求 | 尽量通过 lint 等方式有效检查 | 将可自动检查项写入规范，并接入 preflight 与模板检查；视觉一致性作为 PR review 检查 | `mango-pmo/rules/frontend/02-element-plus-ui.md`、模板检查脚本 | 检查脚本能发现 baseline 漏带规范；preflight 能加载新规范 | DONE | `node mango-business-starter/scripts/check-template.mjs`、`node mango-ui/packages/create-mango-app/scripts/check-cli.mjs`、`node mango-pmo/tools/pmo-preflight.mjs ...` |
| STD-006 | 用户要求 | 后端同样先拆解需求，提取字典、枚举、数据模型、设计模式等 | 更新后端开发流程的开发前要求 | `mango-pmo/rules/backend/10-dev-flow.md` | 人工检查规范内容覆盖业务对象、字典枚举、数据模型、接口模型、业务规则、辅助数据、设计模式、复用能力 | DONE | `mango-pmo/rules/backend/10-dev-flow.md` |
| STD-007 | PMO 规范源规则 | 长期规范必须收口到 `mango-pmo`，业务模板只携带 baseline 快照 | 更新 `mango-pmo` 后同步业务 starter 和 create-mango-app baseline | 三处 baseline 规范文件和 `rules/index.json` | 检查三处 index 均包含 `frontend.elementPlusUi`，三处均有前端 UI 规范文件 | DONE | `mango-pmo/rules/index.json`、`mango-business-starter/.../rules/index.json`、`mango-ui/.../rules/index.json` |
| STD-008 | PMO 要求 | 本次治理必须有计划、台账和验证记录 | 新增治理计划和交付台账 | `mango-docs/plans/2026-05-28-dev-standards-governance-plan.md`、`mango-docs/plans/2026-05-28-dev-standards-governance-ledger.md` | `delivery-contract-check` verify 通过 | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-28-dev-standards-governance-plan.md --ledger mango-docs/plans/2026-05-28-dev-standards-governance-ledger.md --mode verify` |
