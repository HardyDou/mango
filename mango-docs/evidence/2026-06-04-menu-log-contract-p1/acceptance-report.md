# P1 菜单契约与操作日志修复验收记录

## 验收范围

- 前端菜单契约：目录菜单跳转到首个可运行子菜单；可见页面菜单缺少 `component` 时不作为可运行菜单，不进入 404 掩盖配置错误。
- 后端操作日志：公开接口默认不写操作日志；显式 `@Log` 的接口仍写日志；请求/响应/文件流等不可序列化参数进入日志前被过滤。

## 真实应用验收

- 服务地址：`http://127.0.0.1:8707`
- 后端地址：`http://127.0.0.1:19017`
- 数据库：`mango_dev_359f37`
- 登录账号：`admin / admin123`

### 页面抽查

1. 首页：小喇叭、组织名 `芒果集团`、首页能力区正常显示，未见 404 或空白页。
   - 截图：`screenshots/01-home-confirm.png`
2. 系统管理 / 套餐管理：筛选栏、表格、操作列、TagView 正常显示。
   - 截图：`screenshots/02-system-menu-package.png`
3. 审批中心 / 发起流程：审批中心顶层菜单自动落到 `#/workflow/start-process`，未进入 404。
   - 截图：`screenshots/05-workflow-start-process.png`
4. 审批中心父路径：直接访问 `#/workflow/task` 自动重定向到 `#/workflow/start-process`。
   - 截图：`screenshots/06-workflow-task-redirect.png`
5. 平台能力 / 日历管理：左右分栏、年度表格、分页区域正常显示。
   - 截图：`screenshots/07-platform-calendar.png`
6. 通知中心 / 我的消息：筛选栏、空数据表格、分页、小喇叭入口正常显示。
   - 截图：`screenshots/08-notice-site-message.png`

### 功能操作

1. 系统管理 / 套餐管理关键词查询：输入 `平台` 后点击查询，表格收敛为平台管理套餐。
   - 截图：`screenshots/03-system-menu-package-query.png`
2. 系统管理 / 套餐管理重置：点击重置后关键词清空，列表恢复为两条套餐数据。
   - 截图：`screenshots/04-system-menu-package-reset.png`
3. TagView 关闭当前页签：关闭 `我的消息` 后自动切到相邻 `日历管理`，未出现系统错误或空白页。
   - 截图：`screenshots/09-tag-close-fallback-calendar.png`

## UI 截图分析

- 首页：顶部导航、小喇叭、组织名和内容区对齐正常。
- 系统套餐页：整体布局正常；套餐编码列在较窄宽度下出现换行，属于现有表格列宽表现，不属于本次菜单/日志改动范围。
- 日历管理页：左右分栏、表格固定操作区、分页控件未见遮挡。
- 通知中心页：空数据状态正常，未见元素重叠。

## 控制台与网络

- 最后一轮控制台采集：`0` error，`0` warning。
  - 日志：`.playwright-cli/console-2026-06-04T01-51-54-433Z.log`
- 最后一轮网络采集：未见 404；页面相关接口均为 200。
  - 日志：`.playwright-cli/network-2026-06-04T01-51-54-477Z.log`

## 未覆盖项与风险

- 浏览器未临时插入“可见页面菜单缺少 component”的脏数据库场景；该契约已通过 `menuHost.spec.ts` 单测覆盖，真实浏览器覆盖了现有目录菜单重定向。
- `pnpm install --frozen-lockfile` 在新 worktree 中因 main 现有 lockfile/package manifest 版本不一致失败，本次使用 `pnpm install --no-frozen-lockfile` 安装依赖并已还原 lockfile，不提交 lockfile 改动。
