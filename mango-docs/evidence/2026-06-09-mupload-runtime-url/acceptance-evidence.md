# MUpload 运行时文件地址回显验收证据

## 1. 验收范围

- 页面：`/#/components/upload`、`/#/file/files`
- 接口：`/file/files`、`/file/files/preview`、`/file/local-objects/{bucket}/**`
- 权限：`admin` 登录，租户 `芒果集团`
- 数据：`mango_dev_ecb631`，LOCAL 默认存储，DIRECT 访问模式
- 部署形态：本地单体后端 + mango-admin 前端开发服务

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8267`
- 后端地址：`http://127.0.0.1:18577`
- 数据库或租户：`mango_dev_ecb631` / `tenantId=1`
- 测试账号：`admin`
- 浏览器：Chrome / Playwright chromium project

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| TASK-001 | `/#/components/upload` | MUpload 图片上传后使用运行时文件地址显示缩略图 | `mango-upload-image-*.png` | `POST /api/file/files` 的 `response.status()` 等于 200；返回 `mango-upload-image` 文件名和文件 ID；缩略图 `src` 匹配运行时 URL 正则 `/.+/`；预览弹窗 URL 不等于 `about:blank` | 上传列表出现 1 个文件，缩略图可见，预览按钮可打开 | `upload-components-file-api.spec.ts` 8 passed；截图脚本 console error 为 0 | `screenshots/components-upload-image-thumbnail.png` | PASS |
| TASK-002 | `/#/components/upload` | MUpload 混合、手动、批量、办公文档上传 | `mango-upload-component-*.txt`、`mango-upload-manual-*.txt`、`mango-upload-batch-*.txt`、`mango-upload-excel-*.xlsx` | 单文件走 `/file/files`；批量走 `/file/files/batch`；手动上传前成功项为 0，上传后成功项数量正确 | 文件名回显；手动上传按钮可用；非图片在图片模式被拦截 | `upload-components-file-api.spec.ts` 8 passed | Playwright 输出记录 | PASS |
| TASK-003 | `/#/file/files` | 文件中心 LOCAL 图片上传后预览显示 | `mango-file-e2e-*.png` | `directPreviewUrl` 有值；`directDownloadUrl` 有值；图片 `naturalWidth=12`、`naturalHeight=10` | 文件列表显示文件名；预览弹窗显示图片 | `file-management.spec.ts` 4 passed；截图脚本 console error 为 0 | `screenshots/file-center-local-image-preview.png` | PASS |
| TASK-004 | `/file/local-objects/{bucket}/**` | LOCAL 访问入口按 Java 层注册路径可访问 | `mango-file-e2e-check.png` | `directPreviewUrl` 为 `/file/local-objects` 入口；`directDownloadUrl` 带 `download=1`；下载响应 200 且 `Content-Disposition=attachment` | API-only 验证：通过响应头确认下载语义，页面下载交互由 `file-management.spec.ts` TASK-003 覆盖 | `curl -I` 返回 200，无 404 | `.mango/logs/backend.log`、命令输出 | PASS |
| TASK-005 | `/file/files`、`/file/files/preview` | 代理外部化 URL contract | `issue-99-upload-*`、`issue-99-batch-*` | 上传、详情、预览、批量返回 `https://files.example.com/api/...` 运行时 URL | API-only 验证：通过接口响应 contract 确认代理前缀；页面消费由 TASK-001 和 TASK-003 覆盖 | `file-upload-url-contract.spec.ts` 1 passed | Playwright 输出记录 | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 文件组件 | `/#/components/upload` | 混合附件、图片、手动、批量、办公文档上传 | 非图片格式拦截与服务端禁止扩展名错误码 | 缩略图可见，上传列表状态正确，无错误提示 | `screenshots/components-upload-image-thumbnail.png` | PASS |
| 文件中心 | `/#/file/files` | 目录、新建、上传、预览、下载、归档 | 单删、批量删除、PNG 图片预览 | 管理页表格、预览弹窗、按钮交互正常 | `screenshots/file-center-local-image-preview.png` | PASS |
| 后端文件存储 | `mango-file-core` / `mango-file-starter` | LOCAL 默认入口与代理前缀外部化 | LOCAL 下载 attachment 语义 | 不适用 | Maven 输出记录 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 真实 MinIO/S3 live 对象存储 | 本轮问题集中在 LOCAL 默认访问入口和 MUpload 回显；未启动真实对象存储容器 | 不能声明对象存储 live 上传下载在本轮重新验收通过；但 URL contract 保持覆盖对象存储不被 Java public base 改写的契约 | 后续对象存储变更时单独跑 MinIO live E2E | 无 |
| workflow/template 中 MUpload 业务页逐页操作 | `workflow-management.spec.ts` 之前存在流程定义创建 400，未作为本轮主线修复；模板页未新增专项走查 | 不能声明这两个业务页面本轮逐页验收通过；公共 MUpload 组件和文件中心主链路已验证 | 发现具体页面回显问题时登记独立 Issue | 无 |
