# Sprint 4 Layout Review Report

Status: `PASS`

Review time: 2026-05-31 01:28 Asia/Shanghai

## Reviewed Screenshots

- `screenshots/s4-login-current-1440x960.png`
- `screenshots/menu-contract-home-1440x960.png`
- `screenshots/s4-sample-系统管理-参数配置.png`
- `screenshots/s4-sample-系统管理-机构管理.png`
- `screenshots/s4-sample-系统管理-登录日志.png`
- `screenshots/s4-sample-审批中心-我的待办.png`
- `screenshots/s4-sample-审批中心-流程模板.png`
- `screenshots/s4-sample-审批中心-发起流程.png`
- `screenshots/s4-sample-平台能力-编号规则.png`
- `screenshots/s4-sample-平台能力-日历管理.png`
- `screenshots/s4-sample-平台能力-模板列表.png`
- `screenshots/s4-sample-通知中心-渠道配置.png`
- `screenshots/s4-sample-通知中心-发送记录.png`
- `screenshots/s4-sample-通知中心-消息配置.png`
- `screenshots/s4-dev-center-文件上传.png`
- `screenshots/s4-dev-center-组织架构选择器.png`
- `screenshots/s4-dev-center-省市区选择器.png`
- `screenshots/s4-dev-center-AI-对话.png`
- `screenshots/s4-dev-center-实时通信.png`
- `screenshots/s4-dev-center-验证码.png`
- `screenshots/sprint-4-layout-contact-sheet.png`

## Visual Findings

- 登录页：蓝紫背景、左右分栏登录卡、租户选择、用户名、密码和登录按钮显示正常。
- 主框架：首页使用 Mango 原蓝色顶栏、白色左侧菜单、tags view、内容卡片和原 dashboard 布局。
- 顶栏工具：搜索、全屏、小铃铛、设置、用户和租户区域可见，位置和颜色符合 Mango baseline。
- 菜单：后端一级菜单 `系统管理`、`审批中心`、`平台能力`、`通知中心` 均可见；shell 菜单仅包含允许的 `首页` 和开发环境 `开发中心`。
- 页面抽查：每个后端一级菜单抽查 3 个子页面，均未出现 404、路由加载失败、页面样式缺失或黑块图标。
- 开发中心：`文件上传`、`组织架构选择器`、`省市区选择器`、`AI 对话`、`实时通信`、`验证码` 均在原 Mango shell 内渲染，未自造主框架。
- 数据/API：菜单、登录、页面访问均使用真实本地后端；抽查页面空状态来自真实接口结果或页面自身空数据状态，不作为失败。

## Non Blocking Notes

- 实时通信页面在切页或浏览器关闭时会产生 `/api/realtime/transports/probe/sse` 的 `ERR_ABORTED`。脚本已将该项单独记录为 `ignoredNetworkFailures`；其他网络失败仍会导致 E2E 失败。

## Evidence

- Contact sheet: `screenshots/sprint-4-layout-contact-sheet.png`
- Menu contract report: `menu-contract-report.json`
- Menu sampling report: `menu-sampling-report.json`
- Dev center report: `dev-center-pages-report.json`
