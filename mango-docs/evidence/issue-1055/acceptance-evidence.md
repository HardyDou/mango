# Issue #1055 信息采集管理 验收证据

## 1. 验收范围

- 页面：`/information/forms`（表单列表、详情与接入方式、表单设计）、`/information/submissions`（动态列列表与处理抽屉）、`/#/information/hosted?formCode=customer-contact`（匿名托管表单）。
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
| TASK-002 | TC-001 | `/#/information/forms/:id/detail`、`.../config` | 表单详情与表单设计 | 表单 `customer-contact` | 详情展示 3 列描述表、字段配置表和接入方式（预览 + 页面/接口）；配置页直接复用平台表单设计器（`form-create-designer`，与工作流表单设计器同源），提供组件面板、可拖拽画布、字段选中与属性配置；保存和刷新后规则保持不变；重复编码返回 `105501` | 详情返回栏与操作按钮可点，设计器组件面板、画布和属性面板可见 | 接口 HTTP 200 + 业务错误码 | `screenshots/02-form-detail.png`、`screenshots/03-form-designer.png` | DONE |
| TASK-003 | TC-003 | `/#/information/submissions` | 独立菜单入口、筛选、动态提交列与处理抽屉 | 真实提交记录 | 提交记录是「信息采集管理」下的独立菜单，页面不依赖表单详情、无返回按钮；顶部筛选框为「表单 / 关键字 / 状态 / 提交时间范围」＋查询/重置，筛选与页码写入地址栏，刷新保持同一视图；记录以雪花主键 ID 展示（已移除独立提交编号）；固定列 + 当前表单动态列；整行可点开抽屉，抽屉支持快捷处理并提示处理结果 | 独立菜单可直达，表单筛选与时间范围生效，`?formId=` 深链定位表单，行操作为「处理」，手机号等字段按原值展示 | 接口 HTTP 200 | `screenshots/05-submission-list.png`、`screenshots/06-submission-detail.png` | DONE |
| TASK-003 | TC-003 | `/#/information/submissions` 表单筛选 | 提交记录列由当前表单字段定义驱动 | `customer-contact`（姓名/手机号/需求描述）与 `e2e-dynamic-columns`（公司名称/企业规模/分割线）两份表单 | 切换表单后列集合随之变化：只出现本表单采集项，不出现其它表单字段；展示型/布局型规则不生成列；选择型控件展示选项标签（`大型`）而不是存储值（`l`） | 两个表单的列表列集合互不相同 | 列表接口 HTTP 200 | `screenshots/05-submission-list.png`、`screenshots/09-submission-dynamic-columns.png` | DONE |
| TASK-004 | TC-002 | `/#/information/hosted?formCode=e2e-controls` | 设计器全控件在托管页面原样渲染 | 含 input/number/select/radio/checkbox/switch/rate/slider/date/color/divider/alert/tableForm 的规则 | 评分渲染为 5 星、滑块为滑块、开关为开关、单选多选为选择组、日期与颜色为对应选择器、分割线与提示为展示组件、表格表单带「项目/数量」列；评分与滑块取值进入提交值 | 各控件可见可交互，提交后展示成功结果 | 公开接口匿名 HTTP 200 | `screenshots/10-hosted-all-controls.png` | DONE |
| TASK-004 | TC-002 | `/#/information/hosted?formCode=customer-contact` | 托管页面控件与设计器一致 | 设计器规则含 `rate`（评分）控件 | 托管页面用设计器同源的 form-create 渲染：评分渲染为 5 星评分组件而不是文本框；提交后评分数值进入提交值（`${...:4}`） | 星级可点选，提交后展示成功弹窗 | 公开接口匿名 HTTP 200 | `screenshots/07-hosted-form.png` | DONE |
| TASK-004 | TC-002 | 分享地址 `http://127.0.0.1:30084/#/information/hosted?formCode=customer-contact` | 分享网址免登录提交 | 姓名、手机号、需求描述 | 全新浏览器会话、无登录态直接打开分享地址即可渲染表单并提交；接入页复制的地址为绝对地址 | 未跳转登录页，必填校验生效，提交后弹出成功结果 | 公开接口匿名 HTTP 200 | `screenshots/07-hosted-form.png`、`screenshots/08-hosted-submit-success.png` | DONE |
| TASK-005 | TC-004 | `POST /information/open/forms/submit` | 服务端 SK 校验 | 错误 SK、正确 SK | 错误 SK 返回 `105507`；正确 SK 成功写入提交 | 接口层验收，无页面交互 | 公开接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-005 | TC-005 | `POST /information/open/forms/submit` | 必填、未知字段与幂等 | 缺 `phone`、未知字段 `hack`、重复幂等键 | 分别返回 `105505`、`105505`、同一记录ID | 接口层验收，无页面交互 | 公开接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-003 | TC-006 | `PUT /information/submissions/process` | 状态流转与已关闭限制 | COMPLETED → CLOSED → PROCESSING | 前两次成功，已关闭再处理返回 `105506` | 接口层验收，无页面交互 | 管理接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-009 | TC-007 | `GET /information/submissions/export` | 导出按原值输出 | 手机号字段 | CSV 仅含当前表单记录，手机号按原值输出 `13800138000`；列表和抽屉同样不脱敏 | 列表、抽屉和导出三处展示一致 | 导出接口返回 HTTP 200，响应体为 CSV 文本 | `screenshots/05-submission-list.png`、`screenshots/06-submission-detail.png` | DONE |
| TASK-004 | TC-001 | Resource Registry 内置表单 | 通过资源声明发布内置表单 | `META-INF/mango/demo/information-demo-form.yml` 声明 `INFORMATION_FORM`（formCode=demo-feedback） | 启动同步日志 `Information form synced: demo-feedback`；`information_form` 生成记录且状态 PUBLISHED、SK 与摘要均写入；匿名查表单 200、匿名提交成功并返回记录 ID | 后台表单管理可直接看到该内置表单 | 资源同步记录 CREATE/APPLIED | `mango/mango-platform/mango-information/README.md` 第 6 节 | DONE |
| TASK-004 | TC-002 | 票据 + `POST /information/open/forms/{formCode}/files` | 匿名上传落 mango-file | 托管表单页面匿名上传 `probe-upload.txt` | 上传前先领票据（600 秒）；无票据、错误票据、跨表单复用票据一律拒绝；有效票据上传成功，文件保存到 `file_record`（`biz_type=information-form`） | 上传控件显示文件名与预览入口 | 公开入口匿名 HTTP 200 + 业务错误码 | `screenshots/11-hosted-upload.png` | DONE |
| TASK-002 | TC-001 | 配置页返回 | 返回路径直接回列表 | 从列表进入表单设计后点击返回 | 直接回到表单列表，不再绕经表单详情 | 列表页正常显示，`data-page=information.forms` | 端到端用例断言返回后列表页可见，无额外网络请求 | `screenshots/03-form-designer.png` | DONE |
| TASK-004 | TC-004 | `GET /information/open/forms/{formCode}/files/preview` | 公开预览签名与越权防护 | 同一文件换表单编码、缺失签名、伪造签名 | 正确签名返回文件内容；缺失/伪造签名、以及换用其它表单编码复用签名均被拒绝 | 接口层验收，无页面交互 | 公开接口匿名 HTTP 200 + 业务错误码 | "mango-docs/evidence/issue-1055/acceptance-evidence.md" | DONE |
| TASK-003 | TC-003 | 提交记录列表与抽屉 | 子表单/表格表单复杂值展示 | `tableForm` 值 `[{项目,数量},...]` | 列表与抽屉展示为「项目: 服务费，数量: 2」而不是 `[object Object]`，字段编码映射为列标题 | 列表与抽屉均可读 | 列表接口 HTTP 200 | "mango-docs/evidence/issue-1055/screenshots/05-submission-list.png" | DONE |
| TASK-004 | TC-002 | `/#/information/forms/{id}/detail` | 模拟浏览器预览 | 含 16 个控件的长表单 | 详情页接入方式区先展示模拟浏览器窗口（12px 圆角、投影、白色地址栏），预览区固定为浏览器视口高度并内部滚动，不再撑破窗口或拉长整页 | 预览可独立滚动，页面在其内结束 | 接口 HTTP 200 | `mango-docs/evidence/issue-1055/screenshots/04-form-detail-access.png` | DONE |
| TASK-009 | TC-007 | `GET /information/submissions/export` | 复杂值导出 | 多选、子表单、表格表单值 | CSV 中复杂值序列化为可读 JSON，不出现 `[object Object]`；子表单/表格表单计为一列，内部模板不单独成列 | 接口层验收，无页面交互 | 导出接口返回 HTTP 200 | 后端集成测试记录 | DONE |
| TASK-006 | TC-008 | 数据库与提交详情 | 历史记录字段快照 | 提交后回读 `information_submission_snapshot` | 每条提交存在字段定义快照，且与提交值字段对齐 | 接口层验收，无页面交互 | 数据库会话直接读取快照列，无 HTTP 调用 | 本节数据库记录 | DONE |
| TASK-007 | TC-009 | `information_submission` 表 | 租户归属 | 匿名提交 | `tenant_id=1` 由服务端写入，请求体不接受租户覆盖 | 接口层验收，无页面交互 | 数据库会话直接读取快照列，无 HTTP 调用 | 本节数据库记录 | DONE |
| TASK-011 | TC-001 | 前端包与门禁 | 架构与样式边界 | `@mango/information` | `pnpm architecture:check` 0 错误且未新增例外 | 接口层验收，无页面交互 | 门禁脚本标准输出，无网络请求 | 本节门禁记录 | DONE |
| TASK-012 | TC-010 | `InformationServiceIntegrationTest` | 字段校验、快照、幂等、SK、状态、导出与租户隔离 | H2 MySQL 兼容内存库、两租户、两份表单 | 7 个用例覆盖必填/未知字段、幂等同号、SK 拒绝、已关闭拒绝、导出原值、租户隔离 | 服务层集成测试，无页面交互 | 不发起 HTTP，直接断言持久化结果 | `mvn -pl mango-platform/mango-information/mango-information-core test` | DONE |
| TASK-013 | TC-011 | `informationDesignerRules.spec.ts` | 子表单/表格表单的规则可以安全载入设计器 | `group` 定义缺少 `props`、表格表单列缺少 `style` | 4 个用例：补齐 `props.rule`、补齐列 `style/rule`、保留既有结构、转换后仍被采集项解析识别 | 前端单测，无页面交互 | `toDesignerRules` 是纯函数，直接断言输出 | `pnpm --filter @mango/information test` | DONE |
| TASK-014 | TC-012 | 信息采集管理员 | 重新打开含子表单/表格表单的表单设计器不报错 | 以缺少 `props` 的历史定义创建 `e2e-subform` 表单 | 画布不出现空态提示、`成员明细` 与 `费用明细` 均渲染、控制台无 `Cannot read properties` 报错 | 浏览器交互，需 admin/admin123 | 定义通过后台接口写入，模拟历史数据 | `pnpm --filter mango-admin exec playwright test e2e/specs/information-collection.spec.ts` | DONE |
| TASK-015 | TC-013 | 信息采集管理员 | 托管表单与设计器预览的 label 不再悬空 | 表单含多选分组控件 | 页面内 `label[for]` 全部有对应元素（修复前 2 处悬空） | 浏览器交互，匿名可访问 | 修复在运行时渲染后校正，分组控件仍由 `aria-labelledby` 关联 | Playwright 探针 + 托管表单 E2E | DONE |
| TASK-016 | TC-014 | 信息采集管理员 | 详情与接入合并为一个页面 | 从表单列表进入详情 | 详情页在字段配置后直接展示接入方式：上方模拟浏览器预览，下方左「方式 ① · 页面接入」、右「方式 ② · 接口接入」（含 SK 显示/复制/重新生成）；列表行操作只保留 详情、配置、提交记录、停用、删除 | 浏览器交互，需 admin/admin123 | 独立接入页已删除，`/#/information/forms/{id}/hosted` 不再存在 | `pnpm --filter mango-admin exec playwright test e2e/specs/information-collection.spec.ts` | DONE |
| TASK-017 | TC-015 | 信息采集管理员 | 提交记录行操作为「处理」 | 打开某表单的提交记录列表 | 行操作显示「处理」且不再出现「详情」，点击后打开提交详情抽屉，可填写处理备注 | 浏览器交互，需 admin/admin123 | 抽屉标题保持「提交详情」，仅重命名行操作 | `pnpm --filter mango-admin exec playwright test e2e/specs/information-collection.spec.ts` | DONE |
| TASK-018 | TC-016 | `/#/information/forms/:id/detail` | 详情页按「上基础信息 / 下左页面・右接口」布局 | 表单 `customer-contact` | 顶部为表单摘要卡片（名称、编码、状态、6 项描述表）；下方左右两栏，左栏「方式 ① · 页面接入」含用法说明、免登录地址、复制按钮与模拟浏览器预览，右栏「方式 ② · 接口接入」含用法说明、`POST` 路径、`X-Mango-SK` 与字段配置 | 两栏并排且左页面右接口，预览在左栏内，字段配置只在右栏出现 | 接口 HTTP 200 | `screenshots/02-form-detail.png` | DONE |
| TASK-019 | TC-017 | `/#/information/forms/:id/config` | 输入框「类型」覆盖采集常用类型 | 平台设计器默认只提供 6 个原生类型 | 在平台默认（文字/数字/时间/日期/月份/日期时间）之外补齐「手机号、邮箱、网址、密码、搜索」，共 11 项 | 属性面板「类型」下拉可看到并按业务选择 | 平台设计器实例内包装，不改动其它模块 | `pnpm --filter mango-admin exec playwright test e2e/specs/information-collection.spec.ts` | DONE |
| TASK-020 | TC-018 | 提交记录列表、处理抽屉与导出 | 字段一律不脱敏 | 手机号 `13600136000`、`13800138000` | 列表、抽屉、导出三处都按原值展示，后端导出与前端展示均去掉脱敏分支 | 列表可直接看到完整手机号 | 接口 HTTP 200 + CSV 文本断言 | `screenshots/05-submission-list.png` | DONE |
| TASK-021 | TC-019 | `/#/information/forms/:id/detail` | 页面接入给地址/效果/嵌入代码，接口接入给多语言示例 | 表单 `customer-contact` | 左栏含「页面地址」「页面效果（模拟浏览器）」「页面嵌入」三段，嵌入代码为带域名与 `formCode` 的 `<iframe>`；右栏含「接口地址」「表单专属 SK」「接入代码示例」「字段配置」，示例覆盖 cURL、Postman、Java、JavaScript、Python 五种并带复制按钮 | 两个卡片头部各有底色区分页面与接口，代码可一键复制 | 接口 HTTP 200 | `screenshots/02-form-detail.png` | DONE |
| TASK-022 | TC-020 | 表单列表与提交记录 | 列表操作列完整可见、空态文案准确 | 1600×1000 视口、选中有 0 条记录的表单 | 操作列固定右侧且五个按钮完整可见；提交记录在已选表单无数据时提示「该表单暂无提交记录」，未选表单时提示「请选择表单后查看提交记录」，无数据时导出按钮禁用 | 操作列不被裁切，空态与导出状态随筛选变化 | 接口 HTTP 200 | `screenshots/01-form-list.png`、`screenshots/05-submission-list.png` | DONE |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-information` | 后台表单列表、详情（含接入方式）、配置与提交记录 | 四个页面按原型结构可用，接入方式并入详情页 | 动态列与处理抽屉可用 | 菜单高亮与页面级 `data-page` 正常 | `screenshots/01`~`06` | DONE |
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
