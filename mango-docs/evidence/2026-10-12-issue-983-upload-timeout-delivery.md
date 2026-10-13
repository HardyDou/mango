# 标准交付记录

> Issue #983 文件上传超时长期修复

## 1. 元数据

- 任务 ID：Issue #983
- 交付模式：STANDARD
- 需求影响：L2：影响普通上传、批量上传、服务端分片、S3 multipart 及富文本图片上传。
- 方案风险：L2：切换公共文件 API 的传输抽象并新增兼容入口。
- 最终风险：L2
- 工作区决策：REUSE：`/Users/hardy/Work/mango-issue-983`，分支 `fix/issue-983-upload-timeout`
- 决策复核：`AGENT_ONLY`；未指定外部同行评审。

## 2. 目标与范围

- 目标：消除文件上传沿用普通请求 30 秒超时导致的慢速公网失败，并统一上传错误语义。
- 成功条件：普通请求仍使用 30 秒默认值；上传请求具备独立总超时和无进度超时；上传、批量、分片、multipart、富文本和头像上传均经注入式 HttpClient。
- 处理范围：`@mango/file` 文件、目录、设置、存储 API；`@mango/common` 旧上传调用方；HTTP 客户端超时和进度契约；宿主注入和文档。
- 不处理范围：断点续传协议、已完成分片查询、跨设备上传恢复、后端上传会话模型改造。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| U983-01 | `MUpload` 普通/批量上传 | 已注入 HttpClient，网络持续慢但有进度 | 上传不被 30 秒普通请求边界中断 | 超时为结构化 `HttpError.kind=timeout` | 上传请求携带独立 timeout 配置，普通请求默认不变 |
| U983-02 | `SERVER_CHUNK` | 文件达到分片阈值 | 初始化、签名、分片、完成请求使用上传超时 | 任一步失败仍执行既有会话清理 | 单元/组件测试通过；请求入口均走 HttpClient |
| U983-03 | `S3_MULTIPART` | 对象存储返回签名 URL | 签名、对象存储 PUT、完成请求使用上传超时 | 网络、HTTP、超时错误可区分 | 代码路径无固定 30 秒 Axios 上传调用 |
| U983-04 | 富文本、头像、通知附件 | 应用提供 HttpClient | 上传/下载使用实例级客户端 | 不依赖全局请求实例 | common/auth/notice 定向测试或构建通过 |
| U983-05 | 上传配置 | 可选覆盖 timeout/inactivityTimeout | 默认总超时 10 分钟、无进度超时 60 秒，可单次覆盖；旧入口无需改代码 | 不改变普通 JSON API 30 秒默认值 | HTTP 客户端超时测试通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D983-01 | U983-01/U983-05 | `HttpRequest` 增加 `inactivityTimeoutMs`；进度事件重置无进度计时器；总超时和无进度超时分别配置 | `packages/api-schema`、`packages/http-client` | 回退客户端契约和实现 |
| D983-02 | U983-01/U983-03 | 文件 API 使用可选 `createFileApi(httpClient?)`；无参数保留原有默认请求路径，传入客户端才启用新链路；上传默认 10 分钟总超时/60 秒无进度超时；普通请求不改全局 30 秒 | `packages/file/src/api/*` | 回退文件 API 工厂及组件调用 |
| D983-03 | U983-04 | Vue 组件存在注入客户端时使用实例级客户端；无注入时返回原有静态 API；`fileApi`、`fileSettingsApi`、`fileStorageApi` 及 common 旧函数保留兼容 | `packages/file`、`packages/common`、`packages/auth`、`packages/notice`、`packages/admin-shell` | 回退消费者迁移，保留旧入口 |
| D983-04 | U983-02 | 不新增断点续传协议；超时后的会话清理保持既有行为，恢复上传另行立项 | 前端上传会话代码 | 回退上传会话调用变化 |
| D983-05 | U983-01/U983-04 | 保留旧 `fileApi`、`fileSettingsApi`、`fileStorageApi` 和 common 上传函数；未传入 `HttpClient` 时继续走原有默认请求逻辑，传入后才使用新 HttpClient；配置函数返回实例，不修改全局 singleton | `packages/file/src/api/*`、兼容 facade | 不要求业务代码改造；回退新增 HttpClient 分支即可 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I983-01 | D983-01 | 1 | `mango-ui/packages/api-schema/src/index.ts`、`packages/http-client/src/index.ts` | 契约、超时归一化和进度重置完成 |
| I983-02 | D983-02 | 2 | `mango-ui/packages/file/src/api/*`、`packages/file/src/composables/*`、上传组件与页面 | 文件域 API 无旧 Axios API 依赖，上传入口支持超时覆盖 |
| I983-03 | D983-03 | 3 | `mango-ui/packages/common/*`、`packages/auth`、`packages/notice`、`packages/detail`、`packages/cms`、`packages/workflow`、`packages/admin-shell`、应用宿主 | 相关消费者完成注入迁移 |
| I983-04 | D983-04 | 4 | `mango-ui/packages/file/README.md`、本记录 | 范围、默认值、非范围和回滚方式可追溯 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| U983-01/U983-02/U983-03 | API/组件测试 | `pnpm --filter @mango/file test`；API 子集 4 个文件/12 个测试；组件子集 4 个文件/31 个测试 | PASS：10 个测试文件，52 个测试通过 | Vitest 输出（本次会话） |
| U983-04 | 单元测试 | `pnpm --filter @mango/common test` | PASS：36 个测试文件，367 个测试通过 | Vitest 输出（本次会话） |
| U983-05 | HTTP 接口集成测试 | `pnpm --filter @mango/http-client test` | PASS：2 个测试文件，16 个测试通过，含真实本地 HTTP 服务、请求头、查询参数、错误响应和取消请求验证 | Vitest 输出（本次会话） |
| 构建/类型边界 | 构建 | `pnpm --filter @mango/api-schema build`；`@mango/http-client build`；`@mango/file build`；`@mango/common build`；`@mango/auth build`；`@mango/detail build`；`@mango/cms build`；`@mango/workflow build`；`@mango/notice build`；`@mango/admin-shell build` | PASS | 各包 Vite build 输出（本次会话） |
| 真实浏览器慢速上传 | Chrome + 本地真实后端，CDP 上行限速 24 KiB/s | PASS：1 MiB 文件实际上传 43.2 秒（超过原 30 秒），`POST /api/file/files` 返回 200，文件记录成功，无前端错误提示；环境为 `127.0.0.1:30024` / `127.0.0.1:18024` | PASS（本地限速验收）；公网环境、`SERVER_CHUNK`、`S3_MULTIPART` 和真实断网仍待 QA | 浏览器脚本与 Playwright 输出（本次会话） |
| 前端变更静态门禁 | 受影响文件质量检查 | `pnpm check:affected -- --base=origin/main --head=HEAD` | 受影响文件 ESLint、Prettier、Stylelint 和边界检查通过；聚合 `catalog --check` 仍报告基线投影不一致，未由本任务修改 | `.runtime/frontend-quality/issue983-check-affected-final.log` |
| U983-04 | detail 组件测试（PR #1026 验收返工） | `pnpm --filter @mango/detail test` | PASS：3 个测试文件，8 个测试通过。返工前 2 个 `MangoRichTextPreview` 用例失败，原因为 `@mango/detail` 的 `@mango/file` mock 未导出组件新使用的 `useFileApi`；已将 mock 改为 `useFileApi: () => ({ preview, previewContent })` | Vitest 输出（本次会话） |
| 前端 PR 门禁 | CI 同源本地入口 | `pnpm check:pr -- --base=9c45049f36198858b01fe3bf17125236a3c5c702 --head=bdd6d01610cc9f0efa50c343dee1bf214c5a2a21` | PASS：14 个直接变更 workspace 加传递消费者检查通过，advisories=3 | 命令输出（本次会话） |
| 测试质量门禁 | 测试改动检查 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | PASS：4 个文件 | 命令输出（本次会话） |

## 7. 例外与剩余风险

- 当前未实现上传会话恢复和已完成分片查询；本次仅解决固定 30 秒失败，不宣称断点续传。
- S3 预签名 PUT 在浏览器使用 XHR，支持进度事件、总超时和无进度超时；非浏览器环境保留 `fetch` 总超时路径。
- 未执行真实浏览器限速公网验收，不能据此关闭现场网络回归风险。
- 全量仓库 typecheck 当前仍有基线中已有的跨包构建顺序/测试类型诊断；受影响源文件无新增诊断，受影响包的构建和定向测试已通过。
