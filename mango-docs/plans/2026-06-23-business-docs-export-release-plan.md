# Business Docs Export Release Plan

## 1. 背景

当前 Mango 已在 `mango-pmo` 维护 PRD 模板规范、详细设计模板规范、交付契约规则和对应模板。业务开发者主要通过 `mango-docs` 文档站消费 Mango 文档，因此发布前需要保证这些产品文档输出资产进入公开文档白名单、文档站导航和文档首页入口。

## 2. 目标

- 让业务开发者能从 Mango 文档站直接找到 PRD 模板、详细设计模板、交付契约模板和对应规范。
- 让本次发布说明写清发布对象、业务升级步骤和验证结果。
- 不改变 Mango 后端、前端运行时代码、接口、数据库、权限、租户和菜单行为。

## 3. 范围

- `mango-docs/README.md`
- `mango-docs/.vitepress/stage-public-docs.mjs`
- `CHANGELOG.md`

## 4. 不处理范围

- 不发布新的 Maven artifact。
- 不发布新的 npm package。
- 不改变 `mango-pmo` 长期规范正文。
- 不调整业务 starter 模板和 CLI 生成逻辑。
- 不启动后端、前端或数据库验收。

## 5. 改动项

- 文档站公开白名单增加：
  - `mango-pmo/rules/product/03-detailed-design-template.md`
  - `mango-pmo/templates/prd.md`
  - `mango-pmo/templates/detailed-design.md`
- 文档站侧栏增加“产品文档输出”分组。
- 文档首页增加 PRD、详细设计、交付契约和规范入口。
- 平台级 `CHANGELOG.md` 增加本次发布段。

## 6. 验证方式

- `git diff --check`
- `npm --prefix mango-docs run docs:stage`
- `npm --prefix mango-docs run docs:build`
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-06-23-business-docs-export-release-plan.md --ledger mango-docs/plans/2026-06-23-business-docs-export-release-ledger.md --mode verify`

## 7. 完成标准

- 文档站 staging 输出包含 PRD 模板、详细设计模板、交付契约模板和对应规范。
- VitePress 静态构建通过。
- 交付台账全部为 `DONE` 或 `EXCEPTION`，且 `EXCEPTION=0`。
- `CHANGELOG.md` 最新发布段写清发布对象、升级步骤和验证命令。

## 8. 风险与限制

- 本次只发布文档输出入口，不验证业务项目实际使用这些模板生成 PRD 或详细设计的质量。
- GitHub Release 创建依赖 `gh` 登录状态和远端 tag 推送结果。
