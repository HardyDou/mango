# Mango AI 管理与统一会话工作台验收基线

## 当前状态

- 管理入口：`平台能力 → AI 管理`，当前包含模型管理、提示词配置、Skill 与工具、AI 服务四个菜单；Skill 与工具在同一页面分为两个独立页签。
- 模型管理路由：`/ai/models`；内置 DeepSeek、火山方舟、阿里云百炼、智谱 AI、硅基流动、Kimi、OpenAI 兼容协议和 Ollama 八类供应商。
- 八类供应商连接及每家一个代表模型由 `META-INF/mango/resources/ai-provider-model-catalog.json` 以 `INIT_ONLY` 首次创建，全部不含 API Key 且默认停用；已存在租户配置不覆盖。通用对话、合同五要素抽取和文本情感分类示例位于 `META-INF/mango/demo/ai-demo-services.json`，仅在 Demo 开关开启时加载。
- 统一运行入口：`平台能力 → AI 管理 → AI 服务 → 运行`，路由 `/ai/services/run?serviceCode=<code>`。
- `CHAT`、`EXTRACTION`、`CLASSIFICATION` 共用唯一运行链：`POST /ai/services/chat?serviceCode=<code>` 返回标准受理响应，Mango Realtime `ai.service.chat` 投递增量，`DELETE /ai/services/chat?requestId=<id>` 停止生成；旧 `/ai/services/invoke`、AI 自有 SSE 和旧 Schema 表单运行器已删除，不存在 fallback。
- 用户输入采用内容块：文本及所选模型真实支持的图片、音频、视频和普通文件。可输入模态始终取“模型配置能力与适配器真实能力的交集”，不支持的模态在页面和服务端均拒绝。
- 消息输出与历史恢复采用内容块：文本、Markdown 安全富文本、结构化 JSON、图片、视频、音频和普通文件；媒体可展示不等于当前模型可理解该媒体。
- 会话输入器显式选择下一条消息的模型和思考配置；发送瞬间冻结本轮设置，生成期间仍可调整，调整从下一轮生效。每条助手消息显示并持久化本轮实际设置。
- `AiConversationWorkspace` 是 `@mango/ai` 唯一会话 UI 承载；组件只接收会话、消息和状态并派发语义事件，不依赖 Router、HTTP、权限或宿主 Store，页面只负责真实 API、Realtime、文件、模型和持久化编排。
- 流式交互发送后立即显示连接状态；网络增量和供应商完成事件中的最终正文统一进入帧级显示缓冲，每帧只增长 1-3 个字符，超长积压最多 4 个。生成阶段使用稳定文本节点，完成后再渲染 Markdown 或结构化内容；减少动态效果时直接显示网络增量。
- OpenAI 兼容模型显式选择 `CHAT_COMPLETIONS` 或 `RESPONSES` 协议；运行时不自动重试、切换协议或回退旧实现。
- 会话和消息持久化使用 `ai_chat_conversation`、`ai_chat_message`；消息只保存内容块和 Mango 文件 ID，不保存文件 URL 或 base64。
- AI 数据库迁移当前为 Flyway V10；V9 回填 `content_parts_json` 后删除旧 `content` 列，V10 将会话设置列直接改为最近成功设置，并为助手消息保存本轮实际模型和思考状态，运行时无双读兼容。

## 验收矩阵

| 项目 | 结果 | 断言 |
|---|---|---|
| 后端编译与测试 | PASS | AI Core 41/41、Starter 8/8，其中服务会话高风险套件 15/15；受影响模块 `verify` 的依赖、ArchUnit、PMD、Checkstyle 和 SpotBugs 均为 0 问题。覆盖内容块、文件权限与预算、模型模态交集、Responses 图片/PDF 映射、按轮模型选择、结构化输出、会话持久化、重复请求、所有权、跨实例取消、Realtime 终态投递失败、审计存储失败和旧接口 404。 |
| 前端测试与构建 | PASS | `@mango/ai-api` 7/7、`@mango/ai` 36/36、Common Realtime 315/315；Node 22.23.1 下完整 41-workspace production build 和全量 typecheck ratchet 通过。 |
| 供应商与模型目录 | PASS | 管理员从真实 API 加载八类供应商；页面只展示供应商名称、代码和启用状态，不回显密钥；模型能力、模态和协议来自数据库。证据 `screenshots/04-model-management-v4.png`。 |
| Resource 初始化 | PASS | 项目内 Mango CLI 创建并完成 Bootstrap generation 14；`FLYWAY_EXPAND`、`RESOURCE_REQUIRED`、`TENANT_PREREQUISITES`、`TENANT_RECONCILIATION`、`FLYWAY_CONTRACT`、`RESOURCE_FINALIZE` 均为 `SUCCEEDED`。五类 AI Resource Handler 将 8 个供应商、8 个代表模型、3 个 Prompt、2 个 Skill 和 3 个服务共 24 条 `INIT_ONLY` 声明物化为 `ACTIVE`；既有配置未被覆盖。受权限保护的 `/resource/handler-specs` 对当前 admin 返回 403，不作为本项通过依据。 |
| 唯一服务运行链 | PASS | CHAT、EXTRACTION、CLASSIFICATION 均从服务列表进入同一对话工作台并调用 `/ai/services/chat`；合同五要素识别直接在对话中选择模型、输入文本并得到 `STRUCTURED_DATA` JSON 卡片。 |
| 文本与文件输入 | PASS | 真实文本流式回复、刷新恢复通过；文本附件完成上传、文件 ID 持久化、消息内下载及模型读取，测试标识为 `AI-MULTIMODAL-20260825`。 |
| 图片与 PDF 理解 | PASS | Responses 适配器把 Spring AI 媒体明确映射为 `input_image`/`input_file`；`gpt-5.6-sol` 完成真实 174 KB PNG 与 21 KB PDF 上传、本地预览/文件卡片、模型理解、落库与刷新恢复；证据 `screenshots/14-ai-image-attachment-responses.png`、`screenshots/15-ai-pdf-attachment-responses.png`。 |
| 动态模态限制 | PASS | 当前 GPT 5.5 无视频输入适配器时，页面明确提示“当前模型不支持视频输入，请更换模型或文件”，视频未进入附件队列；服务端同样按能力交集 fail-closed。 |
| 多格式展示 | PASS | 组件与契约测试覆盖文本、Markdown 富文本、结构化 JSON、图片、视频、音频和普通文件内容块；刷新后仍按内容块恢复。真实浏览器已覆盖文本、富文本、JSON、图片和普通文件。 |
| 按轮模型与思考设置 | PASS | 使用 GPT 5.5 发送长回答，生成期间选择 DeepSeek V4 Flash 并关闭深度思考；当前回复仍标记 GPT 5.5 与深度思考，下一轮标记 DeepSeek 且关闭思考，刷新后不变。数据库逐条消息回读一致；证据 `screenshots/16-ai-turn-model-switch.png`。 |
| 安全与持久化 | PASS | 文件由 Mango 文件中心授权读取并仅持久化文件 ID；Markdown 禁止原始 HTML；前端 API 无明文密钥返回字段；租户和用户隔离保持有效。 |
| Realtime | PASS | 真实浏览器未出现 WebSocket/SSE 401；AI 工作台最终页面 Console 为 0 error。直接刷新动态路由时存在一条既有 Vue Router 开发警告，已与 Realtime 认证结果分开记录。 |

## 真实运行时证据

- 2026-08-25 后端 `http://127.0.0.1:18077` health 为 `UP`；前端 `http://127.0.0.1:30077`；数据库为 `mango_dev_mango_ai_spring_ai_foundation_077`。
- AI Flyway V10 已在现有任务数据库执行成功；`ai_chat_message.content_parts_json` 为唯一消息内容列，旧 `content` 列不存在；历史助手消息完成本轮模型信息回填，用户消息的模型字段为空。
- `POST /api/ai/services/chat?serviceCode=assistant.general` 完成文本、文本文件和图片真实模型调用；Realtime `done.contentParts` 中可空字段按真实后端 `null` 合同解析，不再误报未知事件。
- DeepSeek 接入密钥在当前工作区使用现行 SM4 密钥重新保存后，DeepSeek V4 完成真实流式回复和会话落库；验收材料未记录密钥正文。
- OpenAI 兼容网关的模型目录包含 `gpt-5.6-sol`，该模型明确拒绝 Chat Completions 并要求流式 Responses；模型配置改为 `RESPONSES` 后完成真实回复和会话落库，运行时未增加协议 fallback。
- 附件上传状态使用 Vue 响应式对象维护；真实上传后从“上传中”进入可发送状态，发送、持久化、刷新恢复和下载均通过。
- 统一输入器的文件选择、拖拽和粘贴进入同一校验/上传链；上传显示真实进度，失败附件原位重试，移除或清空附件时取消在途请求且不误报上传失败；图片和视频在发送前使用本地对象地址预览并在组件卸载时释放。
- Responses 请求单测确认图片使用 `input_image`、PDF 使用 `input_file`；真实 `gpt-5.6-sol` 图片会话识别出截图中的模型名称与生成状态，PDF 会话识别出文档的加密错误标题；刷新后通过文件中心重新加载附件，最终观察窗口 Console 0 error、0 warning、失败响应 0。
- 合同五要素识别通过同一会话入口返回结构化结果，实际包含双方、标的、金额、签订日期和履约条件；证据 `screenshots/10-ai-structured-service-run.png`。
- 桌面统一工作台证据为 `screenshots/09-ai-chat-unified-workbench.png`；390×844 移动端无横向溢出，会话抽屉选择后自动关闭，主要触控目标不小于 44×44px，证据为 `screenshots/11-ai-chat-mobile.png`。
- 最终 Realtime 流式回归使用本机确定性 Responses SSE 服务且不使用供应商密钥：点击发送后 24ms 出现可见反馈，正文产生 110 次正向可见增长，每次增加 1-4 个字符，完成后切换为最终 Markdown 并刷新恢复。停止生成会执行 `DELETE /ai/services/chat`、删除未完成轮次并恢复原输入；此前 DeepSeek V4 真实生成中证据仍为 `screenshots/13-ai-chat-smooth-streaming.png`。
- 按轮模型真实验收：GPT 5.5 长回答生成期间，模型选择器和思考开关均保持可操作；切换到 DeepSeek V4 Flash 并关闭思考后，当前助手消息仍保存 `gpt-5.5/openai-compatible/thinking=1`，下一轮保存 `deepseek-v4-flash/deepseek/thinking=0`，刷新后显示不变。测试会话随后通过页面删除，并确认会话与消息无残留；证据为 `screenshots/16-ai-turn-model-switch.png`。
- 登录态访问旧 `POST /api/ai/services/invoke` 返回 HTTP 404；生产代码中不存在旧调用链、旧 Schema 表单状态或协议 fallback。
- 供应商模型同步接口已删除；模型由管理页面显式维护，旧同步接口返回 HTTP 404。
- Realtime 自动协商、WebSocket 和 SSE 使用短期票据恢复可信身份；真实浏览器未出现 Realtime 401，Console 为 0 error。直接刷新动态路由时仅出现一条既有 Vue Router 开发警告。
- 本次证据未记录密码、访问令牌或 API Key。
- 正式资源声明不包含 `apiKey`、密文或 hint；当前开发库清理后所有租户 `1` 供应商的 `api_key_ciphertext` 和 `api_key_hint` 均应为空，供应商和模型均为停用状态。
- 2026-08-25 使用项目内 Mango CLI 源码入口完成 generation `14`；`mango_bootstrap_control` 为 `FINALIZED`，stable/authoritative generation 均为 `14`，Expand/Finalize 六个步骤全部 `SUCCEEDED`。资源注册中心回读为 8 个供应商、8 个代表模型、3 个 Prompt、2 个 Skill、3 个服务，均为 `ACTIVE/INIT_ONLY`。
- generation `14` 物化后，当前开发库仍有 8 个供应商，密文、hint 和启用数量均为 `0`；模型总数为 21，启用数量为 `0`；3 个 Prompt 均已发布，2 个 Skill 和 3 个服务均已启用。该回读只作用于 `mango_dev_mango_ai_spring_ai_foundation_077` 的租户 `1`，没有直接 SQL seed，也未修改 Flyway。
- 最终浏览器回归创建的本机 Provider、两个模型和指定测试会话已通过正式管理 API 删除；CHAT 路由恢复到内置 `gpt-5.5`。API 与数据库回读确认临时 Provider、模型、会话和密钥均为零残留，内置 OpenAI 兼容 Provider 与 `gpt-5.5` 仍为禁用、不可调用且无密钥。
- generation `14` 首次真实启动暴露 File Preview 对 `SecurityProperties` 不可变许可路径快照直接执行 `add` 的兼容缺陷；修复改为复制、合并并通过 setter 回写，新增不可变快照回归测试。修复后同一项目内 CLI 启动成功，后端 health 为 `UP`。

## 现场证据要求

本轮未重建或重置数据库，也未验证当前缺少适配器的真实音频/视频理解或媒体生成。音频、视频输入只在具体模型配置与适配器同时支持时开放；图片、视频、音频和文件输出展示能力不得被表述为模型生成能力。

## 本阶段验收台账

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| AI-CFG-001 | `/ai/prompts`、`/ai/skills`、`/ai/tools`、`/ai/services` | 配置 API 与页面真实交互 | 租户 1 管理员、唯一 `qa_ai_<timestamp>` 数据 | Prompt、MCP 工具、Skill 和 AI 服务定义完成新增、编辑、删除；测试数据已清理 | 覆盖空态、必填校验、JSON 校验、绑定选择、删除确认和成功提示 | AI 配置请求成功，无 AI 模块错误 | `screenshots/05-ai-service-clean-state.png` | PASS |
| AI-CFG-002 | `flyway_schema_history_ai` | 内容块、服务、会话和模型协议持久化 | `mango_dev_mango_ai_spring_ai_foundation_077` | Flyway V1-V10 成功；`content_parts_json` 和助手消息本轮设置存在，旧 `content`、会话绑定字段和模型同步遗留字段不存在 | 数据库只读回读 | 启动日志和 schema 回读一致 | 数据库只读回读记录 | PASS |
| AI-CFG-003 | `http://127.0.0.1:18077/actuator/health` | 真实服务启动 | 当前任务 workspace、Bootstrap generation 14 | File Preview 许可路径兼容不可变 `SecurityProperties` 快照；health HTTP 200，状态 `UP` | 前端 HTTP 200 | Mango CLI runtime receipt 有效 | `.mango/run/logs/mango-backend.log`、健康响应、`FilePreviewEngineResourceRegistrarTest` | PASS |
| AI-CFG-004 | `@mango/ai-api`、`@mango/ai`、Common Realtime | 前端内容块、会话与实时增量质量 | Vitest、Vite build、全量 typecheck | AI API 7/7、AI UI 36/36、Common Realtime 315/315；Node 22.23.1 下完整 41-workspace production build 和 typecheck ratchet 成功 | 覆盖请求/服务隔离、重复与迟到分片、断线、超时、取消、独立组件会话事件、生成阶段状态、按轮模型控件、帧级小粒度输出、Markdown、JSON、媒体、上传和停止回滚 | Realtime 业务错误不再被系统消息分支吞掉；本任务无新增类型诊断 | 检查命令输出 | PASS |
| AI-CFG-005 | 旧 `/ai/sse`、`/ai/services/invoke`、模型同步接口 | 废弃入口清理 | MockMvc 404 合同测试、生产源码扫描 | 旧接口均不可访问；旧 Service/Command/VO、表单运行器和 fallback 不存在 | 页面无旧入口或同步按钮 | 预期 404，正式入口正常 | Starter 合同测试报告、源码扫描 | PASS |
| AI-CHAT-001 | `/ai/services/run?serviceCode=assistant.general`、`GET /ai/services/options`、`POST /ai/services/chat` | 独立会话组件、平滑文本对话、持久会话与按轮模型配置 | 租户 1 管理员、真实供应商回归与本机确定性 Responses SSE | 页面可新建、加载、删除会话；最终回归发送后 24ms 有可见反馈，产生 110 次正向增长且每次仅 1-4 字；生成期间设置可调且下一轮生效；刷新保持 4 条消息及两轮消息级模型事实 | 生成状态分阶段可见；桌面侧栏可收起恢复；移动端 390px 无横向溢出；停止生成恢复输入；主要触控目标至少 44×44px | chat 与历史接口成功；无 AI 或 Realtime 401；直接刷新动态路由会在路由安装前产生一条既有 Vue Router 开发警告，不影响运行结果 | `screenshots/09-ai-chat-unified-workbench.png`、`screenshots/11-ai-chat-mobile.png`、`screenshots/13-ai-chat-smooth-streaming.png`、`screenshots/16-ai-turn-model-switch.png`、浏览器帧采样和数据库回读记录 | PASS |
| AI-MEDIA-001 | 通用对话助手统一输入器 | 文件与图片真实输入、视频能力拒绝 | `AI-MULTIMODAL-20260825`、真实文本文件、174 KB PNG、21 KB PDF | 文件 ID 持久化且模型读取成功；Responses 映射 `input_image`/`input_file`；`gpt-5.6-sol` 图片和 PDF 理解成功；不支持的视频在上传前拒绝 | 选择、拖拽、粘贴、真实进度、取消、失败重试、本地缩略图、文件卡片、发送、下载和错误提示正常 | 上传、chat、刷新后的文件读取请求均成功；最终 Console 0 error、0 warning、失败响应 0；拒绝场景未发上传请求 | `screenshots/14-ai-image-attachment-responses.png`、`screenshots/15-ai-pdf-attachment-responses.png`、组件/协议测试、浏览器现场记录 | PASS |
| AI-RUN-001 | `/ai/services/run?serviceCode=contract.five-elements`、`POST /ai/services/chat`、Realtime `ai.service.chat` | 结构化服务统一会话运行 | 租户 1 管理员、真实合同文本 | 可选择模型和思考设置；返回双方、标的、金额、签订日期、履约条件固定字段 | 以对话消息和 `STRUCTURED_DATA` JSON 卡片展示，无旧表单运行器 | 标准受理与 Realtime 完成事件成功；Console 0 error；无 Realtime 401 | `screenshots/10-ai-structured-service-run.png` | PASS |
