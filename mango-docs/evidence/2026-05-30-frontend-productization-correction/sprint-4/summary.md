# Sprint 4 Summary: Style And Package Export Contract

## Status

`VERIFIED`

Sprint 4 完成了 `@mango/admin` 管理端消费入口、聚合样式入口、包导出契约、独立消费构建验证，以及真实微前端截图验收。

## Implemented

- 新增 `@mango/admin` 包，作为业务 admin app 的公开消费入口。
- 新增 `@mango/admin/style.css`，聚合 Element Plus、admin-shell、auth 和 Mango theme 样式。
- 补齐 `@mango/admin-shell/style.css`，确保发布包能够带出 shell 组件样式。
- 补齐 admin-shell、auth、common、admin-pages、notice 及内置能力包的导出、类型和构建配置。
- 更新 create-mango-app business starter，使生成项目依赖 `@mango/admin` 和 `@mango/admin/style.css`，不直接引入 shell 内部样式。
- 修复微前端开发脚本，读取 worktree backend 端口并显式设置真实后端代理和开发环境配置。
- 修复 dev center 在包构建态下菜单可见但页面 loader 未注册的问题。
- 修复 host app 对 package style 子路径的 Vite alias 解析问题。

## Verification

- Package export contract: passed.
- Packed consumer app install/build: passed.
- Real backend menu contract E2E: passed.
- Every backend first-level menu sampled with 3 child pages: passed.
- Development center pages sampled: passed.
- Screenshot review: passed.
- Delivery ledger: all `7` rows are `DONE`.

## Evidence

- `commands.log`
- `delivery-ledger.md`
- `package-export-report.json`
- `packed-consumer-report.json`
- `menu-contract-report.json`
- `menu-sampling-report.json`
- `dev-center-pages-report.json`
- `layout-review-report.md`
- `screenshots/sprint-4-layout-contact-sheet.png`

## Notes

- 本 Sprint 不包含灰度、远程 registry、缓存治理、回滚平台、发布平台、监控告警或性能专项。
- 本 Sprint 未改变后端菜单来源。full mode 仍以真实后端菜单接口为准。
