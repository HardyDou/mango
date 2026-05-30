# Mango 前端产品化 Sprint 5：运行时部署模式统一

## 1. 背景

总计划 Sprint 5 要求单体、本地模块、微前端、混合部署共享同一能力运行时协议。现有 mode matrix E2E 已覆盖 local 真实业务页，但 micro/mixed 只验证远程失败态，不能证明远程模块真实可访问，也不能证明版本、健康检查和诊断协议可用。

## 2. 目标

- local、micro、mixed 使用同一 runtime decision、菜单、权限、样式和主题协议。
- micro 和 mixed E2E 启动真实远程模块并验证页面内容、runtime props、主题、权限和菜单。
- runtime config 支持模块级 `version`、`entry`、`healthCheckUrl`、失败诊断。
- 远程加载失败和健康检查失败都使用 Mango 标准错误面板，并保留截图证据。

## 3. 范围

- `@mango/app-runtime`：补齐模块运行配置类型、规范化、健康检查和诊断日志。
- `@mango/admin-shell`：把 version/healthCheck 传入 runtime app，错误面板展示版本、健康检查和入口诊断。
- `frontend-mode:matrix-e2e`：local 验证真实业务页面；micro/mixed 启动真实远程业务模块；追加远程失败负向验证。
- `create-mango-app` 模板输出 runtime config 的版本和健康检查字段。

## 4. 不做范围

- 不改造生产微前端发布系统。
- 不新增后端接口。
- 不把业务页面数据替身作为验收完成依据；本 Sprint 只验证前端运行时部署协议，业务 API 失败必须以页面错误态明确呈现。

## 5. 改动项

- `MangoModuleRuntimeConfig` 增加 `version`、`healthCheckUrl`。
- 微应用 mount 前执行健康检查；失败时抛出 `MangoRuntimeError`，并输出可识别诊断。
- runtime marker 增加 version 和 healthCheck，便于 E2E 和排障读取。
- mode matrix E2E 生成真实远程 Vue 应用，通过 Wujie 接收 `mangoRuntime`、`mangoConfig`，渲染菜单、权限、主题、版本和健康检查信息。
- micro/mixed 截图必须显示真实远程页面，不再把失败面板作为通过条件。
- 额外负向场景验证远程入口不可用时标准错误面板样式、文案和诊断字段。

## 6. 验证方式

- `pnpm --filter @mango/app-runtime test`
- `pnpm --filter @mango/admin-shell test`
- `node --check mango-ui/scripts/frontend-mode-matrix-e2e.mjs`
- `pnpm frontend-mode:matrix-e2e -- --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-5/mode-matrix`
- `pnpm package:check`
- `pnpm package:build`
- `pnpm admin:full-preset-e2e -- --backend-url <url> --frontend-port <port> --evidence-dir ../mango-docs/evidence/2026-05-30-sprint-5/full-preset --skip-package-build`
- `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-30-runtime-deployment-mode-unification-sprint-5.md --ledger mango-docs/plans/2026-05-30-runtime-deployment-mode-unification-sprint-5-ledger.md --mode verify`

## 7. 完成标准

- local、micro、mixed 三种模式都有截图和布局报告。
- micro/mixed 截图显示真实远程模块页面、Mango Shell 侧栏/顶栏/标签页布局和远程 runtime 诊断。
- 失败态截图显示 Mango 标准错误面板，不出现原生未样式化按钮。
- 交付台账全部为 `DONE` 或明确 `EXCEPTION`。

## 8. 验证结果

- `@mango/app-runtime` 单测通过，覆盖 runtime config version、healthCheckUrl 和失败诊断。
- `@mango/admin-shell` 单测通过，覆盖菜单加载失败时的能力包回退和无回退失败。
- mode matrix E2E 通过，local、micro、mixed 均生成截图和布局报告；micro/mixed 使用真实远程 Vue 应用验证 version、health、permissions、theme 和业务数据。
- full preset E2E 通过，使用 staged `@mango/admin` 启动完整 Mango Admin；菜单抽样覆盖系统管理、审批中心、平台能力、通知中心，每个一级菜单抽 1-3 个子页面。
- 组织架构截图已重新生成并人工识别，左侧组织树、右侧详情和下级表格均可见；报告记录 `hasVisibleLoading: false`、`MANGO_TECH` 可见、相关接口无新增失败响应。

## 9. 遗留问题

- 本 Sprint 不覆盖生产灰度、缓存失效和远程模块版本回滚策略，进入发布升级 Sprint。
