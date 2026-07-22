# Mango 富文本托管资源验收证据

## 执行环境

- Worktree：`D:\Project\mango-rich-text-managed-assets-design`
- 前端：Node/pnpm workspace，Vitest happy-dom；后端：JDK/Maven workspace。
- 数据与权限：自动化用例使用当前租户 mock；未在真实登录浏览器中保存账号、token 或外部 URL。
- 约束：证据不保存图片 Base64、Blob URL、下载 URL 或第三方图片 URL。

## 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| RICH-TEXT-001 | TC-001 | `@mango/common` Editor | 工具栏 PNG 上传、编辑态预览与 token 出站 | mock PNG；文件 ID `1935600000000000001`；previewUrl 仅用于编辑态 | Vitest 断言 `imageValueType=token` 插入预览，序列化只保留 `mango-file:1935600000000000001` 和 `data-file-id` | 组件测试检查 Toolbar 与内容节点存在 | 单元测试不发网络请求；断言对外 HTML 不含 previewUrl/Base64/Blob | `pnpm --dir mango-ui --filter @mango/common test -- --run`，272 tests passed | PASS |
| RICH-TEXT-002 | TC-002 | Editor 粘贴处理与 `/file/files/import-image` | File、Data URI、远程 HTML 图片、既有 token 分类 | 前端 happy-dom mock；远程导入调用受控 API | 自动化覆盖粘贴任务分流、token 不重复上传、失败局部移除；`FileImportControllerTest` 与 `RemoteFileImportServiceTest` 验证接口和复用保存链路 | 组件测试检查非图片 HTML 保留 | 自动化 mock 无外网；不会把来源 URL 写入 v-model | common 272 tests；后端定向测试 10/10 通过 | PASS |
| RICH-TEXT-003 | TC-003 | `mango-file` 远程导入安全链路 | SSRF 地址、混合 DNS、逐跳校验、请求头、超限与伪图片 | localhost、IP literal、混合公网/loopback DNS；本地 HTTP PNG/redirect/超限/SVG fixture | 地址策略 3/3、HTTP fetcher 3/3 通过；每跳重新校验，不转发 Authorization/Cookie/Referer，限制字节并校验 MIME/magic；原因：HTTPS 与超时 fixture 尚未执行 | 未执行真实浏览器交互 | 本地 HTTP fixture 无外网；完整模块 verify 被数据库占位账号阻断 | 后端相关定向测试 10/10 通过；完整 verify 48 passed、1 environment error | EXCEPTION |
| RICH-TEXT-004 | TC-004 | Editor token 回显与异步状态 | 可见 token、无 previewUrl、过期响应 | happy-dom mock file detail | managedImages 序列化/回显测试通过，失败图片不输出第三方 URL | 组件测试覆盖失败占位 DOM | 单元测试不产生网络请求；未保存失败签名 URL | common Vitest 通过，包含 managedImages 3 条断言 | PASS |
| RICH-TEXT-005 | TC-005 | Mango Editor demo | `toolbar-actions` 与格式工具同流；MUpload 附件语义 | `purpose=attachment`、`PRIVATE`、value-type=id、3 个文件上限 | 源码已接入 slot 和稳定 `data-testid`；原因：浏览器登录页获取登录机构返回服务器内部错误，且 workspace 构建被依赖解析阻断 | 浏览器页面、附件列表、禁用态和 responsive 尚未执行 | admin-shell build 无法解析 `@mango/job/style.css`；浏览器未进入 demo | 失败日志见 `tc-005.md` | EXCEPTION |
| RICH-TEXT-006 | TC-006 | common Editor 兼容模式 | 默认、url、id、token 四组行为和包构建 | 既有 HTML；mock 上传返回 ID/URL | common 测试 272 条通过；common build 成功 | 未做真实页面回归 | 构建未发网络请求 | `pnpm --dir mango-ui --filter @mango/common build` 成功 | PASS |
| RICH-TEXT-007 | TC-007 | mixed HTML 序列化 | 成功图片、失败图片、中文、链接、列表混合内容 | managedImages fixture | 成功图片变 token，失败图片移除，非图片节点保留 | happy-dom DOM 断言通过 | 失败来源 URL 不出站；序列化结果无 Base64/Blob/第三方 URL | `managedImages.spec.ts` 3 条断言通过 | PASS |

## 未验证项与处理

- TC-003 已执行本地可控 HTTP、重定向、超限、MIME/magic 不匹配和无凭据转发；HTTPS 与超时 fixture 尚未执行。完整 Maven verify 还被 `${MANGO_DB_USERNAME}` 占位账号导致的 MySQL Access denied 阻断。
- TC-005 需先补齐 workspace 的 `@mango/job/style.css` 构建产物或正确的依赖构建顺序，再进行 Mango Editor demo 浏览器验收。
