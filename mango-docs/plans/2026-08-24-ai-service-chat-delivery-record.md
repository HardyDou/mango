# 标准交付记录

任务：AI 服务统一多模态会话工作台

## 1. 元数据

- 任务 ID：AI-SERVICE-CHAT
- 交付模式：STANDARD
- 需求影响：L2 - 三类 AI 服务统一为会话交互，并改变公共 API、消息持久化与用户文件交互
- 方案风险：L2 - 涉及前后端内容块契约、文件权限、模型模态、Flyway 数据迁移和浏览器运行链路
- 最终风险：L2
- 工作区决策：REUSE

## 2. 目标与范围

- 目标：`CHAT`、`EXTRACTION`、`CLASSIFICATION` 全部从 AI 服务列表进入同一个 ChatGPT 式对话工作台；输入支持文本和受模型能力约束的文件，输出统一用内容块展示。
- 成功条件：文本、结构化 JSON、图片、视频、音频和文件可按各自格式展示；图片、音频、视频、PDF/文本文件在上传前按所选模型真实模态拦截，服务端再次校验；附件支持选择、拖拽、粘贴、进度、取消、失败重试和本地预览；同一会话可按轮选择模型和思考设置，生成期间调整只影响下一轮，每条助手消息可回读本轮实际设置；旧同步表单、会话绑定校验和 `/ai/services/invoke` 不可进入。
- 处理范围：`@mango/ai`、`@mango/ai-api`、AI API/Core/Starter、会话表 Flyway V9/V10、Mango 文件能力接入、使用说明、能力说明和验收基线。
- 不处理范围：新增视频理解 Provider、图片/音频/视频生成业务服务、Embedding、Rerank、MCP/HTTP 工具执行和具体“五要素识别”业务定义。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | 管理员从 `AI 管理 → AI 服务 → 运行` 进入 | 已登录并具有 AI 服务查看和调用权限 | 三类服务进入同一路由和会话外壳 | 服务、Prompt、Skill 或模型不可用时明确错误 | 不出现旧表单运行器或跨服务残留状态 |
| REQ-002 | 用户使用会话输入器 | 服务和模型已启用，附件来自本地合法文件 | 直接输入文本，或通过选择、拖拽、粘贴添加所选模型支持的图片、音频、视频、PDF/文本文件；显示本地预览和真实上传进度 | 不支持格式、模态或大小在上传前拒绝；上传失败可原位重试，移除或切换会话时取消在途请求 | 不产生明知不可调用的上传请求，不把取消显示为失败 |
| REQ-003 | 前端调用 `POST /ai/services/chat` | 请求带真实登录态、租户、服务编码和内容块 | 服务端重新校验文件权限、MIME、大小、模型模态和多轮附件预算 | 任一边界不满足时 fail-closed | 绕过页面构造请求仍不能进入模型 |
| REQ-004 | 用户运行结构化 AI 服务 | 服务绑定已发布 Prompt、Schema 和可用模型 | 仍执行输入/输出 Schema；纯附件输入可映射标准 `text` 字段 | Schema 不匹配或模型输出非法 JSON 时失败 | 结果以 `STRUCTURED_DATA` 卡片返回 |
| REQ-005 | 用户查看消息区与历史会话 | 会话已创建并产生一种或多种内容块 | 展示文本、安全富文本、结构化 JSON、图片、视频、音频和普通文件 | 预览失败保留下载入口；危险 HTML 不执行 | 刷新后按内容块恢复，不保存 URL/base64 |
| REQ-006 | 用户选择模型和思考配置 | 模型目录、供应商连接和适配器能力可读取 | 输入/输出模态取模型配置与实际适配器能力交集 | 当前无视频适配器时不得声明视频理解 | 页面和服务端均拒绝不真实模态 |
| REQ-007 | 登录用户访问已删除入口 | 使用旧 URL、旧 Command 或旧页面状态 | `/ai/services/invoke`、旧 Schema 表单和旧字符串消息路径不可用 | 不允许 fallback 或兼容调用 | 旧接口返回 404，生产源码精确扫描为空 |
| REQ-008 | 用户发送消息并等待模型回复 | 模型可能连续返回 delta，也可能在完成事件一次返回最终正文 | 发送后立即显示连接反馈；正文按帧以 1-3 个字符连续增长，超长积压最多 4 个；完成后再渲染最终 Markdown/结构化内容 | 停止或失败时取消显示缓冲、回滚未完成轮次并恢复输入 | 不出现长时间无反馈或一批一批跳字；减少动态效果偏好下直接显示网络增量 |
| REQ-009 | 用户在同一会话切换模型或深度思考 | 当前轮已经发送或正在生成，输入器可能存在待发送附件 | 发送瞬间冻结本轮设置；生成期间设置保持可调且仅影响下一轮；助手消息显示并持久化实际模型和思考状态 | 待发送附件与目标模型不兼容时明确阻止切换，不删除附件、不切换模型、不 fallback | 模型 A 生成期间可选择模型 B；当前回复仍标记 A，下一轮使用 B；刷新后两轮事实不变 |
| REQ-010 | 管理员首次启用 AI 模块或 Demo | Bootstrap 扫描正式资源；Demo 开关按环境显式设置 | 首次创建八个供应商及代表模型，全部空密钥且停用；Demo 开启时补齐 CHAT、EXTRACTION、CLASSIFICATION 三个示例 | 不覆盖同租户已有密钥、地址、状态、模型、Prompt、Skill 或服务配置 | 正式资源与 Demo 分目录；全部为 `INIT_ONLY`；无 Flyway seed、运行时硬编码写库或 fallback |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001、REQ-007 | `IAiServiceChatService` 和 `/ai/services/chat` 为三类服务唯一执行链，删除同步 Command/VO/Service/Controller/前端表单状态 | AI API/Core/Starter、`@mango/ai*` | 回退任务提交，不恢复旧入口 |
| DEC-002 | REQ-002、REQ-003 | 用户消息只提交 `TEXT` 或 Mango `fileId` 内容块；选择、拖拽和粘贴共用唯一附件入队/校验/上传链；页面前置校验，服务端通过 `IFileContentProvider.downloadForService` 重新授权并读取 | 附件接收/上传组件、`attachmentSupport.ts`、`AiMessageContentResolver` | 回退内容块能力，不写 URL/base64 |
| DEC-003 | REQ-003、REQ-006 | 模型模态取配置与适配器交集；Responses 显式映射 `input_text`、`input_image`、`input_file`；单文件 20MB、单条 40MB、多轮上下文 80MB | 模型管理、Responses 适配器、内容解析器 | 调整显式预算或适配器能力，不增加协议或媒体 fallback |
| DEC-004 | REQ-004 | 对话文本映射 `text`；纯附件且 Schema 声明字符串 `text` 时生成标准附件指令，实际附件仍单独进入模型消息 | `AiServiceChatService` | 调整明确映射合同，不恢复 Schema 表单 |
| DEC-005 | REQ-005 | 助手 Markdown 禁止原始 HTML；媒体先保存文件中心，再以文件 ID 内容块完成会话 | `ChatMessageContent`、`ChatFilePart`、内容解析器 | 回退媒体内容块，不保存访问地址 |
| DEC-006 | REQ-005、REQ-007 | Flyway V9 一次性回填 `content_parts_json` 并删除 `content`；运行时只读新列 | AI migration、会话存储 | 回滚前必须停服并使用数据库备份，不保留双读 |
| DEC-007 | REQ-001、REQ-005 | `AiConversationWorkspace` 作为 `@mango/ai` 的受控独立组件，只通过 props、slots 和语义事件与页面协作；真实 API、SSE、文件和持久化留在运行页 | `@mango/ai` 组件、导出、运行页和样式 | 回退组件化改动，不恢复页面级重复会话外壳 |
| DEC-008 | REQ-008 | 新建消息本身使用 Vue 响应式对象；网络 delta 与完成事件最终正文进入同一 `requestAnimationFrame` 调度器；生成阶段只更新稳定文本节点，结束后以服务端内容块校准并渲染最终格式 | `smoothStream.ts`、运行页、消息内容块、会话组件和样式 | 回退流式呈现调度，不恢复修改原始非响应式对象或整块追加路径 |
| DEC-009 | REQ-006、REQ-009 | 会话只保存最近一次成功使用的设置以恢复下一轮默认值；每条助手消息保存本轮实际模型和思考状态；发送命令创建不可变轮次快照，`done` 事件回传服务端事实。Flyway V10 直接把旧会话列重命名为 `last_*`，不双读旧列 | AI API/Core、V10、`@mango/ai-api`、输入器和会话组件 | 回退任务提交与 V10 前数据库备份，不恢复会话级锁定或兼容字段 |
| DEC-010 | REQ-010 | AI 初始化统一使用 Resource Registry：正式供应商/模型放 `META-INF/mango/resources/`，示例放 `META-INF/mango/demo/`；五类 Handler 以租户和业务编码幂等创建，已存在时整条保留；供应商和模型新建时强制空密钥、停用 | Resource Support、AI Core/Starter、资源声明和能力说明 | 删除本次资源声明与 Handler；不通过 migration 清理或覆盖租户数据 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | DEC-001 | 1 | AI API、Core、Starter 和运行页 | 三类服务统一会话 API 和页面；唯一执行链测试通过，旧接口 404 |
| TASK-002 | DEC-002、DEC-003 | 2 | 附件接收/上传组件、文件解析器、Responses 映射、模型解析和会话输入器 | 动态附件选择/拖拽/粘贴、进度/取消/重试、本地预览、文件权限/类型/大小/模态校验通过边界测试和真实文件调用 |
| TASK-003 | DEC-004 | 3 | `AiServiceChatService` 和结构化内容块 | 结构化结果和纯附件映射通过输入、输出 Schema 测试 |
| TASK-004 | DEC-005 | 4 | 内容块组件、媒体预览/下载和 Markdown 渲染 | 组件测试与浏览器走查通过 |
| TASK-005 | DEC-006 | 5 | AI Flyway V9、会话实体和存储 | 真实数据库迁移与列回读通过 |
| TASK-006 | 全部 | 6 | AI 前后端生产源码、README、设计、能力说明和验收基线 | 精确扫描无废弃生产引用，说明和证据同步 |
| TASK-007 | DEC-007 | 7 | `AiConversationWorkspace`、会话列表组件、运行页和包样式 | 独立组件为唯一会话 UI 承载，桌面可折叠会话栏、移动端抽屉和输入器体验通过浏览器验证 |
| TASK-008 | DEC-008 | 8 | 流式呈现调度器、响应式消息、生成状态和内容块渲染 | 调度器与组件测试通过；真实模型首个反馈、逐帧增长、完成校准、停止回滚和移动端体验通过浏览器验证 |
| TASK-009 | DEC-009 | 9 | Flyway V10、会话/消息实体与 VO、SSE 契约、运行页和输入器 | 同会话跨模型历史、生成中切换、下一轮生效、消息级事实回读和附件不兼容阻断通过自动化与真实浏览器验证；旧锁定标识扫描为空 |
| TASK-010 | DEC-010 | 10 | ResourceTypes、AI Resource Handler、正式供应商/模型资源、Demo 资源和当前文档 | 八个供应商、八个模型和三个示例声明可加载；Handler 编译通过；当前开发库密钥清空并回读为零长度 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001、REQ-007 | AI API/UI/Core/Starter 测试、旧标识扫描和旧接口合同验证 | 运行 AI Maven/Vitest 定向套件；登录态调用旧 `/invoke`；扫描旧类、端点和重复页面外壳 | PASS | 唯一 API 为 `POST /ai/services/chat?serviceCode=<code>`；AI API 7/7、AI UI 32/32、Core 36/36、Starter 8/8；旧 `/invoke` 仅保留在 404 合同测试和“已删除”说明中 |
| REQ-002、REQ-003、REQ-006 | 附件接收/上传、文件解析、模型模态、Responses 请求和真实浏览器调用 | 组件测试选择/拖拽/粘贴、进度/取消/失败重试；上传文本文件、174 KB PNG 和 21 KB PDF 并发送；尝试不支持的视频；刷新并下载历史附件 | PASS | 文件 ID 持久化、下载和模型读取通过；Responses 请求包含 `input_image`/`input_file`；`gpt-5.6-sol` 真实识别图片模型状态与 PDF 加密错误标题；无视频适配器的模型在上传前明确拒绝；截图 `screenshots/14-ai-image-attachment-responses.png`、`screenshots/15-ai-pdf-attachment-responses.png` |
| REQ-004 | Schema 单测和结构化服务真实运行 | 在统一入口输入合同文本，检查结构化内容块固定字段 | PASS | 同一对话入口返回 `STRUCTURED_DATA`，包含双方、标的、金额、签订日期和履约条件；截图 `screenshots/10-ai-structured-service-run.png` |
| REQ-005、REQ-008 | 组件测试和桌面/移动端真实模型浏览器验证 | 运行 `@mango/ai` 测试与构建；使用 DeepSeek V4 发送约 180 字请求并采样每次 DOM 文本增长；验证刷新、停止生成和 390×844 布局 | PASS | AI UI 32/32；首个反馈 27ms，271 字回答产生 231 次增长，每次增加 1-2 字；完成后切换 Markdown 并成功落库；停止后未完成消息删除且输入恢复；主要触控目标至少 44×44px |
| REQ-005、REQ-006 | 数据库回读、会话刷新恢复和 Realtime 检查 | 回读 Flyway/列结构；刷新持久会话；检查 AI 页面 Console、Network 和 Realtime 连接 | PASS | V10 已执行，旧 `content` 列和会话绑定字段删除；刷新恢复通过；无 WebSocket/SSE 401 |
| REQ-009 | 真实跨模型浏览器验收与数据库逐消息回读 | GPT 5.5 发送长回答；生成期间切换 DeepSeek V4 Flash 并关闭思考；下一轮发送固定短语；刷新、数据库回读并删除测试会话 | PASS | 生成期间模型和思考控件可用；第一轮保存 `gpt-5.5/openai-compatible/thinking=1`，第二轮保存 `deepseek-v4-flash/deepseek/thinking=0`；刷新不变；测试会话与消息删除无残留；截图 `screenshots/16-ai-turn-model-switch.png` |
| REQ-010 | JSON 静态解析、定向 Reactor 编译、官方 Bootstrap 与数据库只读回读 | 解析正式和 Demo JSON；`mvn -pl mango-extension/mango-ai/mango-ai-starter -am -DskipTests compile`；使用 `mango dev start backend` 创建 generation 13；回读 lifecycle、资源注册和目标表 | PASS | 8 个供应商、8 个代表模型、3 个 Prompt、2 个 Skill、3 个服务声明语法有效，44 个相关模块编译成功；generation 13 为 `FINALIZED`，24 条 AI 资源均为 `ACTIVE/INIT_ONLY`，目标表物化成功；供应商密钥字段为空，8 个供应商和 21 个模型均停用 |

### 6.1 验收缺陷与修复

| ID | 现场缺陷 | 根因 | 修复 | 回归证据 |
|---|---|---|---|---|
| DEF-001 | SSE 完成后页面提示“AI 服务返回了无法识别的事件” | 后端 `done.contentParts` 的未使用字段返回 `null`，前端事件校验只接受字段缺失 | 内容块事件校验显式接受可空字符串、文件 ID 和数值，并完整校验事件字段 | `@mango/ai-api` 新增真实 SSE 空值合同用例，7/7 通过 |
| DEF-002 | 附件上传接口成功后页面仍显示“上传中 0%” | 原始对象放入 Vue 响应式数组后继续修改原始引用，状态变化未被追踪 | 入队前创建 `reactive<PendingAttachment>()`，上传进度和完成态只更新响应式对象 | 真实文件上传、发送、模型读取、持久化和下载通过；最终 `@mango/ai` 15/15 |
| DEF-003 | 桌面会话栏收起和恢复时出现 Element Plus `ElOnlyChild` 警告 | 展开按钮的条件渲染放在 `el-tooltip` 子节点，未展开时 Tooltip 没有有效子节点 | 条件渲染上移到 Tooltip，使组件树不存在空触发器 | 桌面收起/恢复后 Console 0 error、0 warning；组件测试 15/15 |
| DEF-004 | 当前工作区 DeepSeek 运行时报 SM4 解密失败 | 工作区 SM4 密钥已变化，数据库中的旧密文不再属于当前密钥 | 使用当前工作区密钥通过供应商管理页重新加密保存，未绕过加密服务 | DeepSeek V4 真实流式回复、2 条消息落库和刷新恢复通过 |
| DEF-005 | `gpt-5.6-sol` 在工作台提示模型不可用 | 模型被配置为 Chat Completions，而上游明确只接受流式 Responses | 将该模型显式改为 `RESPONSES`；未增加自动重试或协议 fallback | 上游 Responses 200；工作台真实回复、2 条消息落库；Console 0 error、0 warning |
| DEF-006 | 输入内容后长时间没有可见响应，模型正文随后一批一批跳出 | 新消息以普通对象入队后，流式回调继续修改原始非响应式引用，逐次变化无法主动触发 Vue 渲染；上游 burst 又被直接整块追加 | 消息创建即使用响应式对象；删除整块追加路径，统一使用每帧 1-3 字的小粒度缓冲，最终正文也进入缓冲；生成期用纯文本节点，完成后再解析 Markdown | DeepSeek V4 首个反馈 27ms；231 次可见增长、每次 1-2 字；最终内容落库并刷新恢复；停止回滚和 390×844 验证通过 |
| DEF-007 | `gpt-5.6-sol` 配置了图片能力但运行选项只显示文本 | 自定义 Responses 适配器只序列化 `message.getText()`，因此适配器能力必须把媒体全部过滤 | Responses 唯一请求映射增加 `input_image` 和 `input_file`；运行模态只开放已实现的文本、图片和 PDF/文本文件，不开放音频、视频 | 请求 JSON 单测覆盖文本、图片和 PDF；真实 174 KB PNG、21 KB PDF 上传、模型识别、落库和刷新恢复通过；最终 Console 0 error、0 warning |

## 7. 例外与剩余风险

- 当前 Spring AI Chat 适配器没有可验证的视频理解实现，因此模型输入选项不暴露 `VIDEO`；协议和页面仍可正确持久化、预览与下载视频结果。新增真实视频 Provider 时必须独立交付适配器和真实调用验收。
- 当前未使用真实供应商验收音频理解、视频理解或图片/音频/视频生成；当前 Responses 适配器明确只开放文本、图片和 PDF/文本文件，输出渲染能力只证明前端可以安全展示对应内容块，不冒充模型能力。
- 用户已授权本任务 Commit、Push、创建 PR，并在保护检查通过后合并；未授权发布、部署或数据库重建。
