# Issue #1055 信息采集中心 验收证据

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
| TASK-008 | TC-001 | `GET /authorization/menus/user`、`GET /information/forms/page` | 菜单与表单查询 | 管理员登录后的真实租户 | 菜单含 `information:forms`、`information:submissions`、`information:hosted`；列表返回已发布表单 | 侧边菜单显示“信息采集中心”，页面级 `data-page` 存在 | 接口 HTTP 200，页面无 Vue 运行时错误 | `screenshots/01-form-list.png` | DONE |
| TASK-002 | TC-001 | `/information/forms` 详情与配置 | 表单详情、服务端 SK、配置保存 | 表单 `customer-contact` | 详情展示托管入口与服务端 SK；配置对话框标签正确；重复编码返回 `105501` | 详情与配置对话框可开合 | 接口 HTTP 200 + 业务错误码 | `screenshots/02-form-detail.png`、`screenshots/03-form-config.png` | DONE |
| TASK-003 | TC-003 | `/information/submissions` | 动态提交列与详情 | 真实提交记录 | 固定列（提交编号/时间/状态/来源页面）+ 当前表单动态列 + 操作列；详情展示提交 JSON | 列表与详情对话框可用 | 接口 HTTP 200 | `screenshots/04-submission-list.png`、`screenshots/05-submission-detail.png` | DONE |
| TASK-004 | TC-002 | `/#/information/hosted?formCode=customer-contact` | 匿名托管表单提交 | 姓名、手机号、需求描述 | 未登录可打开表单并提交，提交后展示成功态与提示语 | 必填校验生效，提交按钮有 loading | 公开接口匿名 HTTP 200 | `screenshots/06-hosted-form.png`、`screenshots/07-hosted-submit-success.png` | DONE |
| TASK-005 | TC-004 | `POST /information/open/forms/submit` | 服务端 SK 校验 | 错误 SK、正确 SK | 错误 SK 返回 `105507`；正确 SK 成功写入提交 | 接口层验收，无页面交互 | 公开接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-005 | TC-005 | `POST /information/open/forms/submit` | 必填、未知字段与幂等 | 缺 `phone`、未知字段 `hack`、重复幂等键 | 分别返回 `105505`、`105505`、同一提交编号 | 接口层验收，无页面交互 | 公开接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-003 | TC-006 | `PUT /information/submissions/process` | 状态流转与已关闭限制 | COMPLETED → CLOSED → PROCESSING | 前两次成功，已关闭再处理返回 `105506` | 接口层验收，无页面交互 | 管理接口返回 HTTP 200，业务码为预期错误码 | 本节接口记录 | DONE |
| TASK-009 | TC-007 | `GET /information/submissions/export` | 导出与默认脱敏 | 手机号字段 | CSV 仅含当前表单记录，手机号脱敏为 `138****00` | 接口层验收，无页面交互 | 导出接口返回 HTTP 200，响应体为 CSV 文本 | 本节接口记录 | DONE |
| TASK-006 | TC-008 | 数据库与提交详情 | 历史记录字段快照 | 提交后回读 `information_submission_snapshot` | 每条提交存在字段定义快照，且与提交值字段对齐 | 接口层验收，无页面交互 | 数据库会话直接读取快照列，无 HTTP 调用 | 本节数据库记录 | DONE |
| TASK-007 | TC-009 | `information_submission` 表 | 租户归属 | 匿名提交 | `tenant_id=1` 由服务端写入，请求体不接受租户覆盖 | 接口层验收，无页面交互 | 数据库会话直接读取快照列，无 HTTP 调用 | 本节数据库记录 | DONE |
| TASK-011 | TC-001 | 前端包与门禁 | 架构与样式边界 | `@mango/information` | `pnpm architecture:check` 0 错误且未新增例外 | 接口层验收，无页面交互 | 门禁脚本标准输出，无网络请求 | 本节门禁记录 | DONE |
| TASK-012 | TC-010 | 后端集成测试 | 自动化覆盖 | 不适用 | EXCEPTION 原因：本轮以真实接口与数据库核对替代，未全部固化为集成测试 | 不适用 | 不适用 | 本节未验证项 | EXCEPTION |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `mango-information` | 后台表单与提交记录 | 列表、详情、配置可用 | 动态列与处理状态可用 | 菜单高亮与页面级 `data-page` 正常 | `screenshots/01`~`05` | DONE |
| `mango-information` | 匿名托管表单 | 匿名打开 | 匿名提交成功 | 独立页面无管理端外壳 | `screenshots/06`、`screenshots/07` | DONE |
| `@mango/information` 包 | 构建与类型 | `pnpm --filter @mango/information build` | `vue-tsc --noEmit` | 产出 index/admin-pages 与页面 chunk | 本节构建记录 | DONE |
| `apps/mango-admin` | 生产构建 | `pnpm --filter mango-admin build` | 信息采集页面 chunk 产出 | 构建成功，无 chunk 解析失败 | 本节构建记录 | DONE |
| `mango-information` 后端 | 模块与聚合验证 | `mvn -pl mango-platform/mango-information/mango-information-starter -am -DskipTests verify` | `mvn -pl mango-admin-starter -am -DskipTests verify` | 两次均 BUILD SUCCESS | 本节构建记录 | DONE |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 后端集成测试全覆盖（`TASK-012`） | 本轮以真实后端接口与数据库核对完成业务验证，未把全部失败分支固化为自动化集成测试 | 回归需要人工重跑接口清单 | 补齐隔离测试库的集成测试并纳入后端流水线 | 待确认 |
| `@mango/admin-extension` 使能修复 | source 模式缺少 `__MANGO_ADMIN_EXTENSION_VERSION__` 导致管理端整站空白，阻断本次浏览器验收 | 属于范围外的 FE1 包变更 | 在后续发布批次单独说明该修复 | 待确认 |
| 独立对外托管站点 | 托管表单当前注册在 `mango-admin` 宿主应用 | 访客入口依赖管理端部署形态 | 确认独立托管站点承载应用后迁移 | 待确认 |
| 文件上传 | BRD/SRS 定为 P1 | 首版不覆盖文件类采集项 | 按 P1 设计预留实现 | 待确认 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 业务开发者 | 表单定义、托管入口、服务端 SK 提交、动态提交列、处理状态与导出能力及验证边界 | `mango/mango-platform/mango-information/README.md`、`mango-ui/packages/information/README.md` | `mvn -pl mango-platform/mango-information/mango-information-starter -am verify`；`pnpm --filter @mango/information build/test` | 服务端接入必须持有当前表单专属 SK；浏览器与托管页面不得携带 SK；测试数据按租户隔离 | SK 失效或校验失败不形成记录；已关闭记录不可再处理；历史记录按快照读取 | DONE |
