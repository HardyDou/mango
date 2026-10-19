# Issue #1055 信息采集管理 验收证据

## 1. 验收范围

- 页面：`/information/forms`（表单列表、详情、配置）、`/information/submissions`（动态列列表与详情）、`/#/information/hosted?formCode=customer-contact`（匿名托管表单）。
- 接口：`/information/forms/*`、`/information/submissions/*`、`/information/open/forms/detail`、`/information/open/forms/submit`。
- 权限：后台接口登录访问；公开查询与提交匿名访问；服务端提交校验当前表单专属 SK。
- 数据：`information_form`、`information_submission`、`information_submission_snapshot` 三张表与真实租户归属。
- 部署形态：本地单体后端 `mango-monolith-app` 与 `mango-admin` 前端。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30084`
- 后端地址：`http://127.0.0.1:18084`
- 数据库或租户：`mango_dev_mango_information_collection_084`，`tenantId=1` / `tenantCode=default`
- 测试账号：初始化管理员 `admin`（不记录密码与 Token）
- 浏览器：Chromium（Playwright 1.61.1，`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true`）

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-008 | TC-001 | `GET /authorization/menus/user`、`GET /information/forms/page` | 菜单与表单查询 | 管理员登录后的真实租户 | 信息采集管理下只有 `表单管理` 一个可见菜单；列表按原型返回表单名称/编码/备注/状态/提交量/创建日期/更新日期/创建人 | 侧边菜单与原型一致，页面级 `data-page` 存在 | 接口 HTTP 200，页面无 Vue 运行时错误 | `screenshots/01-form-list.png` | DONE |
| TASK-002 | TC-001 | `/#/information/forms/:id/detail`、`.../config` | 表单详情与表单设计 | 表单 `customer-contact` | 详情展示 3 列描述表、字段配置表和接入方式卡片；配置页直接复用平台表单设计器（`form-create-designer`，与工作流表单设计器同源），提供组件面板、可拖拽画布、字段选中与属性配置；保存和刷新后规则保持不变；重复编码返回 `105501` | 详情返回栏与操作按钮可点，设计器组件面板、画布和属性面板可见 | 接口 HTTP 200 + 业务错误码 | `screenshots/02-form-detail.png`、`screenshots/03-form-designer.png` | DONE |
| TASK-003 | TC-003 | `/#/information/submissions` | 独立菜单入口、筛选、动态提交列与处理抽屉 | 真实提交记录 | 提交记录是「信息采集管理」下的独立菜单，页面不依赖表单详情、无返回按钮；顶部筛选框为「表单 / 关键字 / 状态 / 提交时间范围」＋查询/重置，筛选与页码写入地址栏，刷新保持同一视图；记录以雪花主键 ID 展示（已移除独立提交编号）；固定列 + 当前表单动态列；整行可点开抽屉，抽屉支持快捷处理并提示处理结果 | 独立菜单可直达，表单筛选与时间范围生效，`?formId=` 深链定位表单，手机号脱敏为 `136****00` | 接口 HTTP 200 | `screenshots/05-submission-list.png`、`screenshots/06-submission-detail.png` | DONE |
| TASK-003 | TC-003 | `/#/information/submissions` 表单筛选 | 提交记录列由当前表单字段定义驱动 | `customer-contact`（姓名/手机号/需求描述）与 `e2e-dynamic-columns`（公司名称/企业规模/分割线）两份表单 | 切换表单后列集合随之变化：只出现本表单采集项，不出现其它表单字段；展示型/布局型规则不生成列；选择型控件展示选项标签（`大型`）而不是存储值（`l`） | 两个表单的列表列集合互不相同 | 列表接口 HTTP 200 | `screenshots/05-submission-list.png`、`screenshots/09-submission-dynamic-columns.png` | DONE |
| TASK-004 | TC-002 | `/#/information/hosted?formCode=e2e-controls` | 设计器全控件在托管页面原样渲染 | 含 input/number/select/radio/checkbox/switch/rate/slider/date/color/divider/alert/tableForm 的规则 | 评分渲染为 5 星、滑块为滑块、开关为开关、单选多选为选择组、日期与颜色为对应选择器、分割线与提示为展示组件、表格表单带「项目/数量」列；评分与滑块取值进入提交值 | 各控件可见可交互，提交后展示成功结果 | 公开接口匿名 HTTP 200 | `screenshots/10-hosted-all-controls.png` | DONE |
| TASK-004 | TC-002 | `/#/information/hosted?formCode=customer-contact` | 托管页面控件与设计器一致 | 设计器规则含 `rate`（评分）控件 | 托管页面用设计器同源的 form-create 渲染：评分渲染为 5 星评分组件而不是文本框；提交后评分数值进入提交值（`${...:4}`） | 星级可点选，提交后展示成功弹窗 | 公开接口匿名 HTTP 200 | `screenshots/07-hosted-form.png` | DONE |
| TASK-004 | TC-002 | 分享地址 `http://127.0.0.1:30084/#/information/hosted?formCode=customer-contact` | 分享网址免登录提交 | 姓名、手机号、需求描述 | 全新浏览器会话、无登录态直接打开分享地址即可渲染表单并提交；接入页复制的地址为绝对地址 | 未跳转登录页，必填校验生效，提交后弹出成功结果 | 公开接口匿名 HTTP 200 | `screenshots/07-hosted-form.png`、`screenshots/08-hosted-submit-success.png` | DONE |
| TASK-005 | TC-004 | `POST /information/open/forms/submit` | 服务端 SK 校验 | 错误 SK、正确 SK | 错误 SK 返回 `105507`；正确 SK 成功写入提交 | 接口层验收，无页面交互 | 公开接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-005 | TC-005 | `POST /information/open/forms/submit` | 必填、未知字段与幂等 | 缺 `phone`、未知字段 `hack`、重复幂等键 | 分别返回 `105505`、`105505`、同一记录ID | 接口层验收，无页面交互 | 公开接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-003 | TC-006 | `PUT /information/submissions/process` | 状态流转与已关闭限制 | COMPLETED → CLOSED → PROCESSING | 前两次成功，已关闭再处理返回 `105506` | 接口层验收，无页面交互 | 管理接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-009 | TC-007 | `GET /information/submissions/export` | 导出与默认脱敏 | 手机号字段 | CSV 仅含当前表单记录，手机号脱敏为 `138****00`；列表和详情同样脱敏 | 列表、抽屉和导出三处展示一致的脱敏结果 | 导出接口返回 HTTP 200，响应体为 CSV 文本 | `screenshots/05-submission-list.png`、`screenshots/06-submission-detail.png` | DONE |
| TASK-009 | TC-007 | `GET /information/submissions/export` | 复杂值导出 | 多选、子表单、表格表单值 | CSV 中复杂值序列化为可读 JSON，不出现 `[object Object]`；子表单/表格表单计为一列，内部模板不单独成列 | 接口层验收，无页面交互 | 导出接口返回 HTTP 200 | 后端集成测试记录 | DONE |
| TASK-006 | TC-008 | 数据库与提交详情 | 历史记录字段快照 | 提交后回读 `information_submission_snapshot` | 每条提交存在字段定义快照，且与提交值字段对齐 | 接口层验收，无页面交互 | 数据库会话直接读取快照列，无 HTTP 调用 | 本节数据库记录 | DONE |
| TASK-007 | TC-009 | `information_submission` 表 | 租户归属 | 匿名提交 | `tenant_id=1` 由服务端写入，请求体不接受租户覆盖 | 接口层验收，无页面交互 | 数据库会话直接读取快照列，无 HTTP 调用 | 本节数据库记录 | DONE |
| TASK-011 | TC-001 | 前端包与门禁 | 架构与样式边界 | `@mango/information` | `pnpm architecture:check` 0 错误且未新增例外 | 接口层验收，无页面交互 | 门禁脚本标准输出，无网络请求 | 本节门禁记录 | DONE |
| TASK-012 | TC-010 | `InformationServiceIntegrationTest` | 字段校验、快照、幂等、SK、状态、导出与租户隔离 | H2 MySQL 兼容内存库、两租户、两份表单 | 7 个用例覆盖必填/未知字段、幂等同号、SK 拒绝、已关闭拒绝、手机号脱敏、租户隔离 | 服务层集成测试，无页面交互 | 不发起 HTTP，直接断言持久化结果 | `mvn -pl mango-platform/mango-information/mango-information-core test` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-information` | 后台表单列表、详情、配置、接入与提交记录 | 五个原型页面按原型结构可用 | 动态列、脱敏和处理抽屉可用 | 菜单高亮与页面级 `data-page` 正常 | `screenshots/01`~`06` | DONE |
| `mango-information` | 匿名托管表单 | 匿名打开 | 匿名提交成功 | 独立页面无管理端外壳，含原型同款品牌行与提交结果弹窗 | `screenshots/07`、`screenshots/08` | DONE |
| `@mango/information` 包 | 构建与类型 | `pnpm --filter @mango/information build` | `vue-tsc --noEmit` | 产出 index/admin-pages 与页面 chunk | 本节构建记录 | DONE |
| `apps/mango-admin` | 生产构建 | `pnpm --filter mango-admin build` | 信息采集页面 chunk 产出 | 构建成功，无 chunk 解析失败 | 本节构建记录 | DONE |
| `mango-information` 后端 | 模块与聚合验证 | `mvn -pl mango-platform/mango-information/mango-information-starter -am -DskipTests verify` | `mvn -pl mango-admin-starter -am -DskipTests verify` | 两次均 BUILD SUCCESS | 本节构建记录 | DONE |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| `@mango/admin-extension` 使能修复 | source 模式缺少 `__MANGO_ADMIN_EXTENSION_VERSION__` 导致管理端整站空白，阻断本次浏览器验收 | 属于范围外的 FE1 包变更 | 在后续发布批次单独说明该修复 | 待确认 |
| 独立对外托管站点 | 托管表单当前注册在 `mango-admin` 宿主应用 | 访客入口依赖管理端部署形态 | 确认独立托管站点承载应用后迁移 | 待确认 |
| 文件上传 | BRD/SRS 定为 P1 | 首版不覆盖文件类采集项 | 按 P1 设计预留实现 | 待确认 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 业务开发者 | 表单定义、托管入口、服务端 SK 提交、动态提交列、处理状态与导出能力及验证边界 | `mango/mango-platform/mango-information/README.md`、`mango-ui/packages/information/README.md` | `mvn -pl mango-platform/mango-information/mango-information-starter -am verify`；`pnpm --filter @mango/information build/test` | 服务端接入必须持有当前表单专属 SK；浏览器与托管页面不得携带 SK；测试数据按租户隔离 | SK 失效或校验失败不形成记录；已关闭记录不可再处理；历史记录按快照读取 | DONE |
