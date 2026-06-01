# Sprint 4 Acceptance

Status: `AUTO_SCREENSHOT_ACCEPTED`

## Acceptance Basis

用户要求：

```text
接下来几个任务 像时光那次截图验收一样进行验收就行，必须留下截图。不用等人工确认，除非有必须人工确认。继续吧
```

因此 Sprint 4 不等待人工逐项确认，按自动 E2E + 截图留存 + 目视检查执行验收。

## Accepted Scope

- 登录页样式恢复并通过截图检查。
- 首页和 Mango 原主框架通过截图检查。
- 后端一级菜单和 UI 菜单契约通过。
- 每个后端一级菜单抽查 3 个子页面并保存截图。
- 开发中心页面抽查并保存截图。
- `@mango/admin` 和 `@mango/admin/style.css` 包消费契约通过。
- packed consumer app 构建通过。

## Evidence

- `layout-review-report.md`
- `screenshots/sprint-4-layout-contact-sheet.png`
- `menu-contract-report.json`
- `menu-sampling-report.json`
- `dev-center-pages-report.json`
- `package-export-report.json`
- `packed-consumer-report.json`
