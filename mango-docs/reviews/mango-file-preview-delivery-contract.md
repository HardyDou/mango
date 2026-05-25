# mango-file 集成 file-preview 交付契约

## 1. 目标

让管理后台文件管理的预览按钮真实打开 `mango-file-preview` 预览页；默认按 `fileId` 预览，避免直接把受保护预览地址放入 iframe 导致 401；zip、txt、pptx 等文件统一走预览服务能力。

## 2. 范围

- `mango-file` 预览地址生成规则。
- `mango-file-preview` token 化预览入口与源文件读取。
- `@mango/file` 的文件管理预览面板。
- JDK 21、MySQL、内存 KV 模式下的服务启动和真实链路验证。

## 3. 不做什么

- 不在本轮引入 Redis 强依赖。
- 不绕过 `mango-file` 权限和租户校验读取源文件。
- 不用 `fileUrl` 作为默认预览方式，默认使用 `fileId`。
- 不在本机缺少 Office manager 的情况下声明 pptx 转换成功。

## 4. 设计输入

- 用户要求：`fileId/fileUrl` 二选一，默认 `fileId`。
- 用户要求：zip、txt、pptx 等所有文件都要支持预览入口。
- 用户要求：管理后台点击预览必须真实预览。
- 用户要求：服务之间只能依赖 API，禁止依赖其他服务 core。
- 设计文档：`mango-docs/designs/mango-file集成file-preview方案.md`。

## 5. 设计说明

### 5.1 影响模块

- `mango-platform/mango-file`
- `mango-platform/mango-file-preview`
- `mango-platform/mango-system`
- `mango-ui/packages/file`
- `mango-infra/mango-infra-kv`

### 5.2 接口变化

- `GET /file/files/preview?id=...` 仍返回预览元数据。
- `GET /file-preview/files/preview-link?fileId=...` 返回 `preview-entry` 短期入口。
- `GET /file-preview/files/preview-entry?token=...` 作为公开短期 iframe 入口。
- `GET /file-preview/sources/{token}` 供预览引擎读取源文件。

### 5.3 数据变化

- 不新增业务表。
- 复用 `file_settings.preview_provider_url`、`preview_external_extensions`。
- `mango-file-preview` 入口 token 和源 token 使用 `ITokenStore`，内存 KV 可用，Redis 非强制。
- 新增系统字典 `file_access_level`，用于文件管理查询区和列表访问级别展示。

### 5.4 菜单/页面/权限变化

- 文件管理列表的“预览”按钮继续使用 `file:files:query`。
- iframe 只使用 token 化 `preview-entry`。
- `preview-entry` 与 `sources` 仍是短期公开入口，权限由签发 token 的接口完成。

### 5.5 测试范围

- JDK 21 目标模块单测。
- 真实 MySQL 服务启动健康检查。
- zip/pptx 真实接口链路。
- 管理后台文件管理页真实点击预览。
- 文件管理查询区访问级别字典加载与筛选。
- 模块依赖边界自查。

## 6. 风险与限制

- 本机未安装 LibreOffice/OpenOffice，且服务以 `office.plugin.enabled=false` 启动；pptx 已验证不再 401/500，会返回明确的 Office 组件不可用提示页。
- 当前前端 dev 页面存在历史 CSP meta console 提示，不影响本次文件预览接口链路。
- Office 兜底页引用的 `file-preview/files/images/sorry.jpg` 在当前 dev 代理下返回 404，不影响接口状态和错误文案展示。

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 默认使用 fileId 预览，fileId/fileUrl 二选一 | 默认 provider 自动追加 fileId；无 fileId 时才可走 fileUrl | `FilePreviewUrlBuilder`、`FilePreviewUrlBuilderTest` | JDK 21 单测覆盖相对地址、绝对地址、占位符和无 fileId 场景 | DONE | `mango/mango-platform/mango-file/mango-file-core/src/test/java/io/mango/file/core/service/impl/FilePreviewUrlBuilderTest.java` |
| TASK-002 | 用户要求 | 管理后台预览必须真实打开预览服务 | 前端对默认 provider 先换取 preview-entry token，再挂 iframe | `FilePreviewPanel.vue` | Playwright 打开文件管理并点击 zip 预览，iframe src 为 `/api/file-preview/files/preview-entry?token=...` | DONE | `.playwright-cli/network-2026-05-24T15-36-59-592Z.log` |
| TASK-003 | 用户要求 | zip 不应下载，应能预览 | zip 走 mango-file-preview 压缩包预览页 | `FilePreviewPanel.vue`、`FilePreviewServiceImpl` | curl 和 Playwright 均验证 zip 预览为 HTML 页面，标题为压缩包预览 | DONE | `.playwright-cli/network-2026-05-24T15-36-59-592Z.log` |
| TASK-004 | 用户要求 | pptx 不应 401 | preview-link 签发短期入口 token，entry 恢复上下文后生成 engine preview；Office 组件不可用时返回明确兜底页 | `FilePreviewServiceImpl`、`FilePreviewController`、`OfficeFilePreviewImpl` | Playwright 验证 pptx 请求为 200，iframe 显示 `Office预览组件不可用`，不再 401/500 | DONE | `.playwright-cli/network-2026-05-25T01-11-40-195Z.log` |
| TASK-005 | 用户要求 | Redis 不应是强依赖，可用内存模式 | starter 缺省提供内存 `ITokenStore`，有正式 KV bean 时由正式实现接管 | `FilePreviewAutoConfiguration` | JDK 21 + MySQL + `mango.kv.store.type=memory` 启动服务健康检查 UP | DONE | `curl http://127.0.0.1:18081/actuator/health` |
| TASK-006 | 用户要求 | 服务之间只能依赖 API，禁止依赖其他服务 core | `mango-file-preview-core` 只依赖 `mango-file-api` 和 `mango-infra-kv-api` | POM 依赖 | `mvn ... test` 中 `mango:check dependency` 通过；手工查看 POM | DONE | `mango/mango-platform/mango-file-preview/mango-file-preview-core/pom.xml` |
| TASK-007 | 用户要求 | JDK 统一切换到 21 并验证 | 移除 KV 模块 JDK 17 override，启动服务进程使用 JDK 21 | `mango-infra-kv/pom.xml`、运行命令 | `ps` 显示后端进程为 openjdk@21；目标模块单测 BUILD SUCCESS | DONE | `mvn -pl mango-platform/mango-file-preview/mango-file-preview-core,mango-platform/mango-file-preview/mango-file-preview-starter,mango-platform/mango-file/mango-file-core -am test -DskipITs` |
| TASK-008 | 环境限制 | pptx 真实内容转换需要 Office 转换能力 | 本轮不伪造转换成功，缺少 Office manager 时返回明确兜底页 | `OfficeFilePreviewImpl` | Playwright 验证 pptx 不再 401/500；本机 `soffice/libreoffice` 不存在，不能验证真实 PPTX 转 PDF 渲染 | EXCEPTION | `.playwright-cli/network-2026-05-25T01-11-40-195Z.log` |
| TASK-009 | 用户反馈 | `preview-entry` 在 iframe 可用，新窗口打开同一 token 报令牌无效 | 保持后端一次性 token 安全语义；新窗口按钮重新调用 `preview-link` 签发 fresh token | `FilePreviewPanel.vue` | 前端构建；浏览器验证新窗口请求新的 `preview-entry?token=...`，不复用 iframe token | DONE | `mango-ui/packages/file/src/components/FilePreviewPanel.vue` |
| TASK-010 | 用户反馈 | 文件管理查询区域字典无法正常显示 | 新增 `file_access_level` 系统字典；文件管理查询区和列表展示统一使用 `DictSelect` / `DictTag` | `V4__file_dict.sql`、`files/index.vue` | 前端构建；浏览器验证 `/system/dict/data/options?typeCode=file_access_level` 返回选项并在查询区显示 | DONE | `mango/mango-platform/mango-system/mango-system-core/src/main/resources/db/migration/system/V4__file_dict.sql` |
| TASK-011 | 用户反馈 | 压缩包点击内部文件后不能预览，提示跨域 unsafe frame | 压缩包内部文件预览在前端代理场景下使用同源 `/api/onlinePreview`，并只放行合法的内部压缩包条目 URL | `compress.ftl`、`TrustHostFilter`、`FilePreviewFrameOptionsFilter`、`TrustHostFilterTests` | Playwright 点击 zip 内 `mango-preview-zip-entry.txt`，iframe URL 为 `127.0.0.1:5173/api/onlinePreview`，正文包含 `zip preview payload` | DONE | `.playwright-cli/network-2026-05-25T01-11-40-195Z.log` |
| TASK-012 | 用户要求 | 使用 JDK 21、真实 MySQL、内存 KV 进行 E2E 验证 | 后端以 openjdk@21、MySQL `mango_e2e_file_preview`、`mango.kv.store.type=memory` 启动；Redis 不作为强依赖 | 运行配置与服务进程 | `curl /actuator/health` 返回 MySQL `UP`；Playwright 完成文件管理字典、zip、pptx 真实链路验证 | DONE | `.playwright-cli/network-2026-05-25T01-11-40-195Z.log` |
