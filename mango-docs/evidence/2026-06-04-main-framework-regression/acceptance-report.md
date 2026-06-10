# Mango 主框架主链路回归报告

## 1. 结论

通过。

本次使用真实单体后端和真实单体前端执行浏览器回归。登录、首页、系统管理、审批中心、平台能力、通知中心、菜单跳转、页签关闭、小喇叭、流程发起、文件上传、日历能力和根路径跳转均通过。

## 2. 环境

- 后端：`http://127.0.0.1:18554`
- 前端：`http://127.0.0.1:8244`
- 启动方式：`scripts/dev-workspace.sh start`
- 前端模式：`source`
- 数据库：`mango_dev_3ab00a`
- 账号：`admin / admin123`
- 租户：`芒果集团`

## 3. 覆盖范围

| 模块 | 页面/能力 | 验证内容 | 结果 | 证据 |
|------|-----------|----------|------|------|
| 登录 | 登录页 | 输入账号密码、选择租户、进入首页、菜单接口成功 | 通过 | `screenshots/01-home.png` |
| 首页 | `/home` | 三个能力入口、顶部导航、左侧首页菜单 | 通过 | `screenshots/01-home.png` |
| 系统管理 | 成员管理 | 菜单跳转、列表展示、按 `admin` 查询、表格回显 | 通过 | `screenshots/02-system-user.png`, `screenshots/03-system-user-search.png` |
| 审批中心 | 发起流程 | 菜单跳转、已发布流程展示、请假申请弹窗、动态表单字段展示 | 通过 | `screenshots/04-workflow-start.png`, `screenshots/05-workflow-leave-dialog.png` |
| 平台能力 | 日历管理 | 菜单跳转、日历页展示、日期明细 Tab、日期查询接口和真实日期行 | 通过 | `screenshots/06-calendar-page.png`, `screenshots/07-calendar-date-query.png` |
| 平台能力 | 文件管理 | 菜单可见、页面打开、目录树接口、真实 txt 文件上传、按文件名查询后表格回显 | 通过 | `screenshots/08-file-page.png`, `screenshots/09-file-upload.png` |
| 通知中心 | 我的消息 | 菜单可见、消息列表接口、页面按钮和列表展示 | 通过 | `screenshots/10-notice-messages.png` |
| 通知中心 | 小喇叭 | 右上角小喇叭可见、可点击、弹层显示消息和入口 | 通过 | `screenshots/11-notice-bell.png` |
| 框架 | TagsView | 关闭当前页签后切换到有效页签，无 404、无系统错误 | 通过 | `screenshots/12-tags-before-close.png`, `screenshots/13-tags-after-close.png` |
| 框架 | 根路径 | `/` 自动跳转 `/home`，首页内容显示 | 通过 | `screenshots/14-root-redirect.png` |

## 4. Console / Network

- Console error/warning：无。
- Failed request：无。
- HTTP 4xx/5xx response：无。
- 详情见 `browser-report.json`。

## 5. UI 走查

- 首页：卡片、导航、页签和常用能力区域无明显重叠、错位或文字溢出。
- 成员管理：左侧组织树和右侧表格布局稳定，查询后表格回显正常。
- 审批中心：流程卡片、弹窗和动态表单字段展示完整，无遮挡。
- 日历管理：左右分栏、Tab、查询区、表格和固定操作列显示稳定。
- 文件管理：左侧目录、上传按钮、搜索区、表格和上传后回显正常。表格操作列为固定列，覆盖横向滚动区域属于既有预期。
- 通知中心：消息列表、小喇叭弹层、顶部导航位置正常。
- TagsView：关闭页签后未出现 404 或系统错误。

## 6. 遗留和风险

- 本次未做流程表单最终提交，只验证发起流程入口和动态表单弹窗。
- 本次未清理上传的验收 txt 文件，文件中心会保留一条 `mango-main-regression-*` 测试文件记录。
- 日历年度选择器没有作为本轮重点验证，日历能力以日期明细 Tab、查询接口和真实日期行作为验收依据。
