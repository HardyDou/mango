# Issue #337 文件预览下载地址分离 E2E 验收证据

## 1. 验收范围

- 页面：后台管理系统文件管理页 `/#/file/files`
- 接口：登录、文件上传、文件预览元数据、文件下载、文件删除清理
- 权限：使用管理员账号 `admin` 验证文件管理入口和下载按钮能力；不记录密码、token 或密钥
- 数据：E2E 上传临时 PNG 文件，文件名前缀 `mango-file-preview-download-split-`，用例结束后按文件 ID 清理
- 部署形态：本地真实后端、真实前端、真实 MySQL，Playwright Chromium 执行

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8740`
- 后端地址：`http://127.0.0.1:18007`
- 数据库或租户：MySQL 开发库 `mango_dev_007`，租户 `default`
- 测试账号：`admin`
- 浏览器：Playwright Chromium

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-337-001 | TC-001 | `/#/file/files`、`/api/file/files/preview`、`/api/file/files/download` | 预览地址与下载地址分离：预览接口异常返回下载端点时，预览动作不得触发浏览器下载；下载按钮仍可下载 | 真实上传 `mango-file-preview-download-split-*.png`，上传接口返回真实文件 ID；仅对该文件的预览接口定向注入 `previewUrl/downloadUrl/directDownloadUrl=/api/file/files/download?id=...` | 弹框可见；预览区域没有任何 `img/iframe/video/audio` 使用 `/file/files/download`；点击预览未触发 `download` 事件；新窗口预览按钮置灰；点击下载按钮触发真实下载且文件名包含上传文件名 | 文件管理页列表能看到上传文件；预览弹框展示下载查看提示；下载按钮可操作；用例 finally 按文件 ID 调用删除接口清理测试文件 | 登录、上传、预览、下载、删除接口均走真实后端；E2E 只定向改写预览接口返回体以复现历史异常数据；无失败接口导致用例中断 | `pnpm.cmd -F mango-admin exec playwright test --config playwright.config.ts e2e/specs/file-preview-download-split.spec.ts --project=chromium --reporter=list`；结果 `1 passed (4.7s)` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `@mango/file` | 文件管理页 | 真实上传 PNG 后列表回显 | 预览弹框、下载按钮、文件清理链路 | 预览弹框可见，错误下载地址不会进入内联媒体元素 | Playwright list 输出记录在本文件第 3 节 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| Firefox/WebKit 多浏览器回归 | 当前按 PMO 日常 E2E 策略执行 Chromium；多浏览器放到夜间或发布前矩阵 | 不影响本次 Chromium 主链路验收；跨浏览器下载事件差异仍需发布前关注 | 发布前可追加 `--project=firefox`、`--project=webkit` | 用户要求本次先做真实 E2E |
| 文档预览服务转换链路 | 本次 Issue 聚焦前端预览地址与下载地址分离，不扩展 Word/PDF 转换能力 | 不影响图片预览和下载地址隔离判断 | 文件预览服务转换能力按独立 Issue 或发布前专项验证 | 用户已确认 Issue #337 范围 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | 业务侧只应持久化文件 ID；`previewUrl/downloadUrl/directPreviewUrl/directDownloadUrl` 只用于即时展示或下载，不应写入业务表单；`FilePreviewPanel` 会拒绝把下载接口当作预览源 | `mango-ui/packages/file/README.md`、`mango-ui/packages/file/src/components/README.md`、`mango-docs/guides/business-integration/file-upload-form.md`、`mango-ui/apps/mango-admin/e2e/specs/file-preview-download-split.spec.ts` | `pnpm.cmd -F mango-admin exec playwright test --config playwright.config.ts e2e/specs/file-preview-download-split.spec.ts --project=chromium --reporter=list` | 使用管理员账号 `admin`；E2E 数据前缀 `mango-file-preview-download-split-`；不记录密码或 token | 若预览点击触发下载、媒体元素 src 包含 `/file/files/download`、下载按钮无法下载或测试文件无法清理，则视为失败并查看 Playwright 输出 | DONE |
