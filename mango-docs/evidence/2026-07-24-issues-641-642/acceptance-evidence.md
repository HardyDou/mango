# Issues #641、#642 Notice 合并交付验收证据

> 记录日期：2026-07-24。核心实现、数据库验证、自动化回归、slot 5 真实管理页面 Playwright E2E，以及受控外部 SMTP 带附件送达均通过。未执行生产数据库操作、发布、提交、推送或 PR。

## 1. 验收范围

- Issue #641：EMAIL 根据 `attachmentFileIds` 从 Mango File 读取真实文件，构造 `multipart/mixed`，限制数量、单文件/总大小、MIME 与读取时长；任一附件失败时不提交 SMTP。
- Issue #642：渠道账号稳定编码、Resource 非敏感声明、Secret 引用/人工补录、路由标签、引用保护，以及模板 `EXACT / TAG / AUTO` 三模式路由与可重试切换。
- 消费端：Notice API、Controller、Feign、数据库 V1/V2、管理前端类型/API/页面、模块 README。

## 2. 验收证据台账

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| ISSUE-641-EMAIL | TC-001 | `EmailNoticeChannelSender` | Mango File 真实附件读取与 SMTP MIME 发送 | 无附件、中文文件名、单双附件、伪造长度、超限、非法 MIME、超时、读取失败和 CR/LF header 输入 | MIME 内容与文件一致；限制在 SMTP 前生效；任一附件失败不提交 SMTP；安全摘要不含内容、URL 或 Secret | 后端发送器专项，不涉及管理页面；检查 MIME 层级、文件名编码与兼容路径 | 测试 SMTP 捕获消息；失败场景断言提交数为零，不访问外部网络 | `EmailNoticeChannelSenderTest` Surefire：12/12 | PASS |
| ISSUE-642-DATA | TC-002 | Notice Resource、Secret SPI、V1/V2 | 稳定身份、非敏感 Resource、Secret 分层与迁移 | fresh V1 临时库、旧 schema 临时库、`LEGACY_101/102`、旧精确/空模板、标签关系 | Resource 重放保留人工 Secret；明文 Secret 被拒；provider 缺失键准确；旧账号和路由模式无损回填 | 后端与数据库专项；管理字段只读和 Secret 不回显由组件测试另行覆盖 | 本地独立 MySQL 执行，未连接共享业务库；验证后删除临时库 | fresh 核心表 22；upgrade 回填 `LEGACY_101/102`、`EXACT/AUTO`，标签表 2 | PASS |
| ISSUE-642-ROUTE | TC-003 | Notice API、Core、Controller、Feign | 标签维护、引用影响和三模式候选发送 | tenant 1/2、不同渠道账号、启停/健康/priority/weight、TAG 无候选、可重试与不可重试错误 | TAG 不回退 AUTO；排序稳定；可重试达到账号上限后切换；不可重试不切身份；删除/停用/移除标签受 EXACT 与 TAG 引用保护 | 后端契约专项；接口参数、权限和 Feign 映射逐项纳入快照 | H2/MyBatis/Spring 测试上下文执行，不访问外部网络 | Notice Core 46/46、Starter 13/13、Starter Remote 3/3；Notice 各渠道合计 99 个测试通过 | PASS |
| ISSUE-642-UI | TC-004 | `@mango/notice` 渠道与业务配置页 | 来源、Secret 状态/补录、标签、引用影响和 `EXACT / TAG / AUTO` | API fixture、三模式表单、空模板、Resource 账号、TAG 候选数量 | 三模式互斥清理；EXACT/TAG 保存前校验；Secret 只写不回显；语义锚点和 Resource 只读字段存在 | Vitest 组件/逻辑测试覆盖表单状态；Vite 生产构建验证页面可打包，但不替代真实浏览器验收 | Vitest/Vite 本地执行；6 个测试文件 22/22，构建成功；未发起真实业务网络请求 | `pnpm -F @mango/notice... build` 与 `pnpm -F @mango/notice test` 输出摘要 | PASS |
| ISSUE-642-UI-E2E | TC-005 | slot 5 管理页面，backend `18005`、frontend `30005`、独立数据库 `mango_dev_mango_issues_641_642_005` | 真实浏览器下 Resource 只读、渠道/标签/Secret、引用保护、EXACT/TAG/AUTO 与故障切换 | tenant 1；运行时唯一 `E2E_*` 标签、渠道、业务配置、bizId；本地受控 SMTP 捕获服务 | UI 保存载荷不包含只读响应字段；Secret 写入后不回显；EXACT/TAG 精确命中；TAG 空候选不回退；AUTO 先失败后切换；目标 case MIME 各一次；附件失败时目标 case MIME 为零 | Chromium 真实点击、表单保存、版本发布、删除保护弹窗和状态回读；Element Plus 自定义控件按可见容器交互 | console/pageerror/失败 network 收集器为 0；真实后端/API/File/MySQL，不使用业务接口 Mock | `notice-routing-email-live.spec.ts`：1/1，46.7s；HTML report、成功截图与 trace 位于 `.runtime/issues-641-642/`，运行时认证数据不进入版本库 | PASS |
| ISSUE-641-LIVE-DELIVERY | TC-006 | Mango File → Notice → 126 SMTP → 外部 QQ 邮箱 | 真实外部 SMTP 带附件送达 | 授权测试发件箱、授权 QQ 收件箱、170 B 文本附件；账号和授权码不写入仓库/证据 | Notice 记录 `SUCCESS`；SMTP 返回接受；`providerMessageId` 非空；响应摘要含 1 个 `text/plain` 附件且状态 `SENT`；收件人确认收到邮件和附件 | 外部邮件客户端最终送达由用户在会话中人工确认 | TLS 1.3、服务端证书校验通过；授权码仅经运行进程 stdin 使用，临时渠道、业务配置和文件随后清理 | 发送时间 2026-07-24；收发地址在证据中脱敏，不记录 Secret、Token 或附件正文 | PASS |

## 3. 自动化回归摘要

| 范围 | 命令或验证 | 结果 |
|---|---|---|
| Notice 后端完整 Reactor | 选择 Notice 11 个子模块并使用 `-am test` 覆盖 60 个 Reactor 项 | PASS；Notice 自身 99 个测试通过。最终回归先发现旧 Resource fixture 缺路由列和 API/Feign 快照过期，修复后全绿。 |
| Notice 前端依赖链 | `pnpm -F @mango/notice... build` | PASS；最终复跑环境 Node 26.5.0，仓库声明 Node 22.23.x，存在 engine warning。 |
| Notice 前端测试 | `pnpm -F @mango/notice test` | PASS；6 个文件、22 个测试。 |
| Notice 真实环境 E2E | `PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true ... playwright test e2e/specs/notice-routing-email-live.spec.ts --project=chromium --workers=1 --trace=on` | PASS；1/1，46.7s；真实 backend/frontend/MySQL/Mango File，受控 SMTP 捕获；成功后 `E2E_*` 渠道、业务和标签残留均清理为 0。 |
| 外部带附件送达 | slot 5 临时 126 SMTP 渠道，Mango File 上传后 `/notice/send` | PASS；Notice/SMTP 均成功，用户确认 QQ 收件箱收到邮件及附件；临时 Secret 和测试数据已清理。 |
| 前端静态检查 | 修改文件定向 ESLint、Stylelint 仓库 ratchet | PASS；Stylelint 0 error/0 warning；ESLint 保留两个 `index.vue` 的既有 `vue/multi-word-component-names` 基线诊断，没有新增 diagnostic identity。 |
| 包级类型检查 | notice `vue-tsc` | 基线未通过；16 个错误均位于未改动的 realtime/client/retry 文件，本次修改文件没有类型诊断。 |
| 数据库迁移 | 独立临时 MySQL fresh/upgrade 双路径 | PASS；临时数据库已删除。 |

## 4. 剩余风险与边界

- 本次真实浏览器验收在 Chromium 桌面项目执行；Firefox、WebKit 与移动端响应式矩阵未纳入 ISSUE #641/#642 的本轮范围。
- Node 引擎与仓库声明不一致；当前构建/测试通过不能替代在 Node 22.23.x 的正式门禁复跑。
- notice 包仍有 16 个未改动文件的既有类型错误；本次不扩大范围修复历史 realtime/client/retry 债务。
- Playwright trace 含短时本地测试会话认证上下文，仅保存在忽略的 `.runtime` 目录，不进入版本库；本记录不包含真实 Secret、附件内容、完整外部账号、token 或生产环境数据。
