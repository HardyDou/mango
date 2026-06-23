# 工作台快捷入口小组件再次验证验收证据

## 1. 验收范围

- 页面：后台管理系统工作台 `/#/home`
- 接口：`/api/actuator/health`、登录租户接口、登录接口、工作台个人布局接口
- 权限：使用本地开发环境 `admin` 账号可见菜单验证快捷入口菜单来源
- 数据：快捷入口选择暂存浏览器 `localStorage`，工作台布局仍走 `@mango/grid-layout` 个人布局接口
- 部署形态：本次验证单体前端开发服务接真实后端；微前端子应用跳转需后续在微前端集成场景补测

## 2. 执行环境

- 前端地址：`http://127.0.0.1:7777`
- 后端地址：`http://127.0.0.1:18848`
- 数据库或租户：本地开发库，租户 `芒果集团`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| GWT-001 | `/api/actuator/health` | 后端和前端代理健康 | 本地 `18848` 后端、`7777` 前端代理 | 后端健康接口 JSON 中 `status=UP`，前端代理健康接口 JSON 中 `status=UP` | 该项为接口健康验证，UI 检查由 GWT-003 覆盖 | 两个请求均解析到健康 JSON，未记录代理连接拒绝信息 | `Invoke-RestMethod http://127.0.0.1:18848/actuator/health`；`Invoke-RestMethod http://127.0.0.1:7777/api/actuator/health` | PASS |
| GWT-002 | `/#/login` | 登录页租户接口 | 用户名 `admin`、密码 `admin123` | 租户接口响应 `success=true`，数据中包含 `tenantName=芒果集团` | 登录页账号输入框、密码输入框、租户选择器和登录按钮在 E2E 登录步骤中完成交互 | `/api/system/tenant/login-options` 返回业务成功结构，未出现 4xx/5xx 响应 | `Invoke-RestMethod http://127.0.0.1:7777/api/system/tenant/login-options` | PASS |
| GWT-003 | `/#/home` | 工作台默认布局加载 | 默认布局 `system.quick-entry` | E2E 断言“工作台”“快捷入口”和 `.mango-grid-widget-quick-entry__setting` 可见，默认入口数量大于 0 | 快捷入口标题、设置按钮、默认入口按钮均由页面真实 DOM 断言 | E2E 中收集 `console error=[]`、异常响应列表为空 | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true PLAYWRIGHT_BASE_URL=http://127.0.0.1:7777 PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18848 pnpm.cmd -F mango-admin exec playwright test e2e/specs/grid-widgets-quick-entry.spec.ts --project=chromium --reporter=list`，2 passed | PASS |
| GWT-004 | 快捷入口设置弹框 | 弹框打开、菜单搜索和列表展示 | 搜索关键字 `菜单管理` | E2E 断言 `.mango-dialog` 可见，弹框内存在“设置快捷入口”“可选菜单”“已选快捷入口”，搜索后存在 `菜单管理` 选项 | 弹框由 `MangoDialog` 渲染，左右配置面板和搜索输入框完成交互 | E2E 中收集 `console error=[]`、异常响应列表为空 | 同 GWT-003 命令，2 passed | PASS |
| GWT-005 | 快捷入口设置弹框 | 清空和空状态 | 点击“清空”后保存 | E2E 断言保存后卡片展示“暂无快捷入口”，读取快捷入口 `localStorage` 值为 `[]` | 空状态展示“去设置”按钮，并可再次打开设置弹框 | E2E 中收集 `console error=[]`、异常响应列表为空 | 同 GWT-003 命令，2 passed | PASS |
| GWT-006 | 快捷入口设置弹框 | 选择保存和刷新保留 | 选择 `菜单管理` | E2E 断言保存后卡片展示 `菜单管理`，刷新页面后 `菜单管理` 仍可见，缓存值不是空数组 | 快捷入口按钮保存后从空状态切换为真实菜单入口 | E2E 中收集 `console error=[]`、异常响应列表为空 | 同 GWT-003 命令，2 passed | PASS |
| GWT-007 | `/#/home` | 快捷入口跳转 | 点击 `菜单管理` 快捷入口 | E2E 断言点击后 URL 不再匹配 `/#/home`，证明宿主注入跳转方法被调用 | 点击快捷入口按钮触发页面路由切换 | E2E 中收集 `console error=[]`、异常响应列表为空 | 同 GWT-003 命令，2 passed | PASS |
| GWT-008 | `@mango/grid-widgets` | 包构建与类型导出 | 包源码 | Vite 构建输出 `index.js`、`quick-entry.js`、`style.css`，类型生成脚本执行完成 | 该项为包构建验证，UI 检查由 GWT-003 至 GWT-007 覆盖 | 构建过程无错误退出码 | `pnpm.cmd -F @mango/grid-widgets build` | PASS |
| GWT-009 | `@mango/admin-shell` | 工作台集成构建 | admin-shell 源码 | Vite 构建生成 `home.js` 等产物，类型生成脚本执行完成 | 该项为集成构建验证，页面交互由 GWT-003 至 GWT-007 覆盖 | 构建过程无错误退出码 | `pnpm.cmd -F @mango/admin-shell build` | PASS |
| GWT-010 | admin 样式聚合 | 系统小组件样式发布与聚合 | `@mango/grid-widgets/style.css` | `generated-package-styles.css` 检查为 up to date，官方模块样式治理检查通过 9 个模块 | 快捷入口样式由 `@mango/grid-widgets/style.css` 纳入 admin 聚合，页面交互验证中卡片标题和弹框可定位 | 样式检查脚本无错误退出码 | `pnpm.cmd admin:styles:check`；`pnpm.cmd admin:module-styles:check` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 工作台 | `/#/home` | 默认布局加载 | 编辑布局入口仍可见 | 顶部欢迎语、编辑布局按钮、快捷入口卡片结构由 E2E DOM 断言覆盖 | E2E 命令 `2 passed` | PASS |
| 快捷入口小组件 | `/#/home` | 设置弹框 | 保存后本地持久化 | 标题行、设置图标、空状态、弹框左右面板由 E2E DOM 断言覆盖 | E2E 命令 `2 passed` | PASS |
| 小组件聚合包 | 不涉及页面 | `mergeGridWidgets` 注入 runtime | 样式入口聚合 | 包构建和 admin 样式聚合检查覆盖公开出口 | 构建与样式检查命令通过 | PASS |

## 5. 生成的测试用例

| 用例 ID | 类型 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|---|
| TC-GWT-001 | API/环境 | 服务代理健康检查 | 后端和前端服务已启动 | 请求后端 `/actuator/health`，再请求前端代理 `/api/actuator/health` | 两个接口均返回 `UP`，无代理连接失败 |
| TC-GWT-002 | E2E | 登录并进入工作台 | `admin/admin123` 可登录，租户 `芒果集团` 可选 | 打开登录页，填写账号密码，选择租户并登录 | 跳转到 `/#/home`，框架和工作台主内容可见 |
| TC-GWT-003 | E2E | 默认快捷入口展示 | 清理快捷入口 localStorage | 进入工作台 | 展示快捷入口标题、设置按钮和默认入口，不出现空白/404/loading 卡死 |
| TC-GWT-004 | E2E | 快捷入口设置弹框打开 | 已登录工作台 | 点击快捷入口右上角设置按钮 | 打开“设置快捷入口”弹框，展示可选菜单和已选快捷入口区域 |
| TC-GWT-005 | E2E | 可选菜单搜索 | 设置弹框已打开 | 输入 `菜单管理` | 可选菜单过滤出 `菜单管理`，目录、按钮、隐藏菜单不作为快捷入口展示 |
| TC-GWT-006 | E2E | 清空快捷入口 | 设置弹框已打开 | 点击清空并保存 | 卡片展示“暂无快捷入口”，本地缓存保存为空数组 |
| TC-GWT-007 | E2E | 保存快捷入口并刷新保留 | 设置弹框已打开 | 选择 `菜单管理`，保存后刷新页面 | 卡片展示 `菜单管理`，刷新后仍展示 |
| TC-GWT-008 | E2E | 快捷入口跳转 | 卡片中已有 `菜单管理` | 点击 `菜单管理` | URL 离开 `/#/home`，进入对应模块 |
| TC-GWT-009 | 构建 | `@mango/grid-widgets` 构建 | 依赖已安装 | 执行 `pnpm.cmd -F @mango/grid-widgets build` | 构建成功，生成 JS、CSS、类型声明 |
| TC-GWT-010 | 构建 | `@mango/admin-shell` 构建 | 依赖已安装 | 执行 `pnpm.cmd -F @mango/admin-shell build` | 构建成功 |
| TC-GWT-011 | 样式治理 | admin 样式聚合检查 | 依赖已安装 | 执行 `pnpm.cmd admin:styles:check` 和 `pnpm.cmd admin:module-styles:check` | 样式聚合和模块样式治理检查通过 |
| TC-GWT-012 | 文档/契约 | 设计与台账契约检查 | 设计文档和台账存在 | 执行 delivery contract check | 检查通过，无缺失交付项 |

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 微前端子应用菜单跳转 | 当前只在单体前端开发服务验证 | 子应用菜单入口和宿主跳转适配仍需集成环境确认 | 后续在微前端模式补充专项 E2E | 否 |
| 外链菜单新窗口跳转 | 本地菜单数据未必稳定包含外链入口 | 外链 `pageType=EXTERNAL_LINK` 的新窗口行为未在本次自动化中断言 | 后续准备稳定外链菜单测试数据后补测 | 否 |
| 小组件权限过滤 | 第一版明确不做权限过滤 | 所有注册小组件进入组件库，数据权限交给接口 | 后续权限模型确认后再扩展 | 是 |
