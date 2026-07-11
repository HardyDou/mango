# 文件访问基线权限矩阵验收证据

## 1. 验收范围

- 页面：无新增页面；本次以 API E2E 验证文件访问基线真实权限链路。
- 接口：`/auth/login`、`/auth/password/change-required`、`/identity/users`、`/file/settings`、`/file/files`、`/file/files/detail`、`/file/files/preview`、`/file/files/download`、`/file/files/package`、`/file/files/page`、`/file/files/delete`。
- 权限：匿名、普通登录用户、管理员。
- 数据：临时用户 `E2E_FILE_BASE_*`，临时文件 `mango-file-access-baseline-*`，执行后清理。
- 部署形态：本地 worktree 单体后端与 mango-admin 前端。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30136`
- 后端地址：`http://127.0.0.1:18136`
- 数据库或租户：`mango_dev_file_access_baseline_136`，租户 `default / 1 / 芒果集团`
- 测试账号：管理员 `admin`；自动创建普通用户 `E2E_FILE_BASE_*`
- 浏览器：Chromium
- 执行命令：`PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm exec playwright test e2e/specs/file-access-baseline-permissions.spec.ts --project=chromium --workers=1`

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-file-access-baseline | TC-001 | `/file/files` | 匿名用户上传受限 | 独立无 Cookie APIRequestContext | 匿名上传返回 401/403 或业务拒绝 | 不适用，API E2E | Playwright 请求断言通过 | `mango-ui/apps/mango-admin/playwright-report` | PASS |
| TASK-file-access-baseline | TC-002 | `/file/settings` | 普通登录用户读取文件设置 | `E2E_FILE_BASE_*` 首次改密后正式登录态 | `accessMode=DIRECT`、启用 token、公开读签名、访问和预览 TTL 均为 `86400` | 不适用，API E2E | Playwright 请求断言通过 | `mango-ui/apps/mango-admin/playwright-report` | PASS |
| TASK-file-access-baseline | TC-003 | `/file/files`、`/detail`、`/preview`、`/download` | 普通登录用户基础上传、详情、预览、下载 | `mango-file-access-baseline-*.txt` | 上传成功；详情和预览返回运行时 `previewUrl/downloadUrl`；DIRECT 预览/下载有效期 `86400`；下载内容非空 | 不适用，API E2E | Playwright 请求断言通过 | `mango-ui/apps/mango-admin/playwright-report` | PASS |
| TASK-file-access-baseline | TC-004 | `/file/files/package` | 普通登录用户打包派生文件 | 上传文件 ID 字符串，避免雪花 ID 精度丢失 | 打包成功并返回 ZIP 文件记录和运行时访问 URL | 不适用，API E2E | Playwright 请求断言通过 | `mango-ui/apps/mango-admin/playwright-report` | PASS |
| TASK-file-access-baseline | TC-005 | `/file/files/page`、`/file/files`、`/file/files/delete`、`/file/settings` | 普通登录用户管理和危险操作受限 | 同一普通用户和同一临时文件 | 文件列表、归档、删除、保存设置均被 401/403 或业务拒绝 | 不适用，API E2E | Playwright 请求断言通过 | `mango-ui/apps/mango-admin/playwright-report` | PASS |
| TASK-file-access-baseline | TC-006 | `/identity/users`、`/file/files/delete` | 管理员清理测试数据 | `admin` | 管理员可删除测试文件和临时用户 | 不适用，API E2E | Playwright 请求断言通过 | `mango-ui/apps/mango-admin/playwright-report` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 文件中心 | 无页面打开 | 权限矩阵 API E2E | 运行时签名 URL 合同 | 本次未做页面截图；UI 按既有 `file-management.spec.ts` 覆盖 | `mango-ui/apps/mango-admin/playwright-report` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 文件管理页面截图走查 | 本次目标聚焦各种用户和功能权限，新增用例为 API E2E 权限矩阵；未重跑 `file-management.spec.ts`，避免其修改文件设置 TTL 影响本基线断言 | 页面按钮展示仍依赖既有文件页面 E2E 覆盖，本次只新增后端真实鉴权证据 | 如需提交前完整页面验收，可另跑文件管理页面专项并先隔离设置写入 | 未确认 |
| `mango-ui/pnpm-lock.yaml` 既有未提交改动 | 本次验收前已存在，非本次 E2E 新增内容 | 需要单独判断是否为依赖有效变更或安装副产物 | 提交前决定保留、提交或清理 | 未确认 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 文件基础能力权限矩阵长期回归：匿名拒绝，普通登录用户可上传/详情/预览/下载/打包/读取设置，危险操作仍受权限控制 | `mango-ui/apps/mango-admin/e2e/specs/file-access-baseline-permissions.spec.ts` | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true pnpm exec playwright test e2e/specs/file-access-baseline-permissions.spec.ts --project=chromium --workers=1` | 需要 worktree 后端、前端、数据库可用；自动创建 `E2E_FILE_BASE_*` 用户和 `mango-file-access-baseline-*` 文件 | 失败先看 Playwright 报告和接口响应消息；若失败来自非本任务历史问题，按 Mango Issue 流程登记 | DONE |
