# Mango Admin 全模块全流程回归基线

## 1. 验收范围

- 页面：`mango-admin` 的 58 个 Chromium E2E 规格文件，覆盖登录、菜单、系统管理、平台能力、文件、通知、任务、支付、模板、工作流等模块。
- 接口：浏览器页面实际调用本工作区单体后端；用例同时校验关键请求、响应字段、状态流与权限边界。
- 权限：平台管理员、A 公司机构用户、匿名入口及公开支付入口的可见性和操作边界。
- 数据：删除旧库后创建独立空库，由 Flyway、必要资源和显式 Demo 资源重新形成测试数据。
- 部署形态：本地单体后端加 Vite 管理端；单 Worker 顺序执行，重试次数为 0。

## 2. 执行环境

- 执行日期：2026-07-18（Asia/Shanghai）
- 前端地址：`http://127.0.0.1:30003`
- 后端地址：`http://127.0.0.1:18003`
- 数据库或租户：`mango_dev_mango_full_e2e_baseline_003`；执行前已删除并重建；必要 Demo 资源显式开启。
- 测试账号：仓库 E2E 固定测试账号；覆盖平台机构、A 公司机构和匿名访问，不在证据中记录口令或令牌。
- 浏览器：Playwright 1.59.1 Chromium。
- 源码：完整 189 项执行时的基础提交为 `1e078cb1fab07d1194e4102c0725e8ee1c1945c7`；提交前已同步到 `origin/main@6b222187a59caa1626329befe7929f94c5fd1e6b`。两者之间只包含 PMO/Workflow Skill 文档和 Checkstyle 默认格式规则放宽，不涉及本基线运行时代码。
- 命令：`PAYMENT_E2E_ALLOW_SHARED_DB_MUTATION=true PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm -F mango-admin exec playwright test --project=chromium --workers=1 --retries=0 --reporter=list,json,html`
- 总结果：189/189 PASS，failed 0，skipped 0，unexpected 0，flaky 0，retry 0，耗时 1,400,003.694 ms（23.3 分钟）。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| BASELINE-ADMIN-001 | TC-001 | 登录、应用、行政区划、验证码、组件页面 | 会话、应用维护、区划权限、组件入口 | 全新库平台机构与 A 公司固定账号 | 真实登录、刷新、退出撤销；应用增改删；机构隔离与 403 边界均符合契约 | 登录表单、区划选择器、上传与图表组件均完成可见和交互断言 | Playwright 结果为 expected；错误数组为空；关键授权和保存响应由用例直接断言 | `mango-ui/apps/mango-admin/playwright-report/index.html`、`mango-docs/evidence/baselines/captcha/latest/captcha-ui-success.png` | PASS |
| BASELINE-ADMIN-002 | TC-002 | 文件管理、`/file/files/**`、文件处理 | 存储配置、上传、详情、下载、归档、删除、预览、PDF 合并 | 运行时上传 PNG、TXT、PDF、ZIP、XLSX、DOCX 等文件 | 预览 URL 使用当前代理或存储地址；复杂格式进入预览服务；下载端点不会被误作预览 | 列表、预览入口、批量操作、全格式预览和下载均由浏览器操作完成 | 文件请求状态和响应字段被逐项断言；Playwright unexpected 为 0 | `mango-ui/apps/mango-admin/playwright-report/index.html`、`mango-docs/evidence/baselines/file-preview/latest/browser-results.json` | PASS |
| BASELINE-ADMIN-003 | TC-003 | Grid、首页、快捷入口、网址导航 | 首页模板、授权优先级、布局保存、导航数据 | 全新库必要首页资源与运行时创建的模板、页面、链接 | CRUD、租户隔离、默认首页解析、个人/部门/角色授权优先级与导航接口一致 | 首页组件、模板管理、快捷入口搜索保存及网址导航交互均有 DOM 断言 | API 主键、排序、授权来源及保存响应被用例校验；错误数组为空 | `mango-docs/evidence/baselines/grid-layout/latest/grid-layout-ui-success.png`、`mango-docs/evidence/baselines/home/latest/e2e/05-home-layout-editing.png` | PASS |
| BASELINE-ADMIN-004 | TC-004 | KV 资源同步、Job 管理与调度 | 资源注册、任务 CRUD、Worker 治理、Cron 稳定性 | 新建任务、手动 Worker、每分钟 Cron 任务 | 任务触发、实例与日志闭环成立；连续 6.1 分钟窗口无重复调度且日志可读 | 任务列表、详情、Worker 启停、告警规则和执行日志均实际操作 | 真实 Job API 与调度日志由用例轮询核验；没有重试或提前结束 | `历史验收文件已清理（可从 Git 历史恢复）`、`历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| BASELINE-ADMIN-005 | TC-005 | 布局、主题、菜单、套餐、登录与审计日志 | 多布局、侧栏折叠、响应式、菜单树、机构菜单隔离、日志 | 平台机构、A 公司和运行时菜单/套餐数据 | 四类布局、侧栏宽度、主题持久化、平台完整导航与 A 公司授权导航均符合既有逻辑 | TagsView、面包屑、右键菜单、主题面板和 1000px 断点均完成 DOM/CSS 断言 | 菜单与日志分页使用真实后端；隔离查询和 403 响应被直接校验 | `mango-ui/apps/mango-admin/test-results/specs-layout-shell-tags-st-a374e--defaults-布局固定顶部和底部，仅中间内容滚动-chromium/defaults-fixed-shell.png`、`mango-ui/apps/mango-admin/playwright-report/index.html` | PASS |
| BASELINE-ADMIN-006 | TC-006 | 通知、消息中心、编号、组织、岗位、成员 | 公告发布确认、系统消息、编号版本、组织成员维护 | 公告混合目标、消息记录、编号版本、A 公司组织成员 | 公告对象、用户确认、消息跳转、编号生效版本、根组织保护及成员角色分配均符合契约 | 公告管理、消息入口、组织树、岗位和成员页面均完成真实交互 | 通知投递、组织选择与权限响应被用例断言；unexpected 为 0 | `历史验收图片已清理（可从 Git 历史恢复）`、`mango-ui/apps/mango-admin/test-results/notice-message-actions.png` | PASS |
| BASELINE-ADMIN-007 | TC-007 | 支付中心、开放接口、收银台、对账与结算 | 36 项支付全流程，包括退款、通知、异常、线下支付、对账、审计和结算 | 运行时创建应用、通道、业务订单、支付订单、退款、账单及差异数据 | 公开支付入口在无登录租户上下文下按订单归属租户处理；预览不选择历史订单；36/36 全部通过 | 支付菜单、表单、列表、详情、审批、重推、对账和结算操作均由页面执行 | 签名、防重放、状态流、通知重试和审计响应被直接断言；failed/skipped/flaky 均为 0 | `mango-ui/apps/mango-admin/playwright-report/index.html`、`mango-docs/evidence/baselines/admin-regression/latest/report.json` | PASS |
| BASELINE-ADMIN-008 | TC-008 | 平台元数据、Realtime、角色和数据权限、系统事件 | 元数据租户边界、Polling 消息、角色授权、Outbox 查询 | 平台机构与 A 公司、运行时角色和数据权限配置 | 普通机构不可维护平台元数据；Realtime 完成协商与消息交互；角色不能越权授权平台菜单 | 移动端 Realtime 无水平溢出；角色资源树和系统事件详情完成交互断言 | Polling、权限保存回显、Outbox 分页响应均由真实请求验证 | `mango-docs/evidence/baselines/infra-event/latest/system-event-detail.png`、`历史验收图片已清理（可从 Git 历史恢复）` | PASS |
| BASELINE-ADMIN-009 | TC-009 | 模板、机构、Upload 与多格式输出 | TEXT/Word/Excel/PDF、机构生命周期、统一上传组件 | 运行时模板版本、新机构、混合附件与 Office 文档 | 模板发布和生效版本、机构禁用后的旧令牌失效、上传文件标识和批量接口均符合契约 | 模板管理、机构启停、主题切换、手动与批量上传均完成浏览器交互 | 多格式输出、机构依赖阻断和上传校验业务码被用例直接断言 | `mango-ui/apps/mango-admin/playwright-report/index.html`、`mango-docs/evidence/baselines/admin-regression/latest/report.json` | PASS |
| BASELINE-ADMIN-010 | TC-010 | 工作流设计、发布、发起、待办与业务审批 | 分类、业务域、模板、动态表单、自选审批、会签、费用报销 | 运行时流程定义、模板、审批人、费用报销业务数据 | Flowable 发布、认领/释放/暂存/转办/通过/驳回/抄送、加签和再申请状态流全部成立 | 三步设计台、动态表单、成员选择器、待办详情和自定义审批页均完成操作 | 流程实例、任务归属、审批动作和历史响应由真实接口断言；最后 14 项全部 expected | `mango-ui/apps/mango-admin/playwright-report/index.html`、`mango-docs/evidence/baselines/admin-regression/latest/report.json` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 文件中心 | 文件管理 | 存储配置 | 全格式预览和下载 | 当前前端代理地址与复杂文件预览服务路由正确 | Playwright HTML report、文件用例产物 | PASS |
| 菜单与权限 | 后端菜单导航 | 平台完整菜单 | A 公司授权菜单 | 业务域侧栏、系统管理、审批中心、平台能力层级由真实菜单树渲染 | Playwright HTML report | PASS |
| 支付中心 | 支付全域 | 公开入口租户解析 | 退款、对账、结算闭环 | 列表、详情、表单、审批和审计布局均有断言 | Playwright HTML report、`report.json` | PASS |
| 工作流 | 流程设计与审批 | 发布、发起、驳回、再申请 | 会签加签和自定义审批页 | 三步设计、动态表单与成员选择均由浏览器完成 | Playwright HTML report | PASS |

## 5. 未验证项和风险

- 本次 189 项 Chromium 功能回归没有未验证项、跳过项、失败项或重试项。
- Firefox 与 WebKit 是独立浏览器兼容项目，不属于本次 Chromium 功能基线；不能用本基线替代跨浏览器兼容结论。
- Node 运行时报告 `module.register()` 弃用提示；不影响本次产品断言，后续升级 Playwright/Node 时应消除。
- Java 聚合质量门禁为 PASS，工具失败 0；仓库已登记的历史静态基线仍保留，本次未以增加 suppression 或跳过检查处理新代码。
- 子模块独立执行旧版 PMD 6.42 时对当前 Java 源产生 216 个解析错误，随后独立 `mango:check` 又把模块外 717 条存量误判为新增；这是质量工具直接调用口径的已知债务，不能将该次命令记为通过。受控 CLI 聚合门禁、支付模块 `verify`、Checkstyle、SpotBugs 和测试质量检查均已分别完成。

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者与 QA | 以本文件和 `report.json` 作为 2026-07-18 管理端全模块 Chromium 基线；后续行为变化需说明与此基线的差异 | `mango-docs/evidence/baselines/admin-regression/latest` | 本文件第 2 节的 Playwright 命令 | 每次使用独立空库；Demo 开关显式开启；不得提交口令或令牌 | 任一 unexpected、skip、flaky 或 did-not-run 均不得标记基线通过；PMD 直接调用需先使用兼容版本和受控增量口径 | DONE |
