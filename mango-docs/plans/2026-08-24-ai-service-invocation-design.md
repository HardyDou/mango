# AI 服务统一会话与内容块设计

## 目标

所有当前可运行的 AI 服务使用同一个会话入口。调用方提交服务编码、会话模型、思考设置和类型化内容块；平台从数据库加载服务、已发布 Prompt、Skill、Schema 和模型配置，通过 Spring AI 执行，并把文本、结构化数据或媒体结果作为内容块返回。

## 范围

- 唯一运行接口为 `POST /ai/services/chat?serviceCode=<code>`，返回 `text/event-stream`。
- `CHAT`、`EXTRACTION`、`CLASSIFICATION` 均进入 `/ai/services/run?serviceCode=<code>` 对话工作台，不再按服务类型切换到表单或同步接口。
- 用户消息支持 `TEXT`、`IMAGE`、`AUDIO`、`VIDEO`、`FILE`；助手消息支持 `RICH_TEXT`、`STRUCTURED_DATA`、`IMAGE`、`AUDIO`、`VIDEO`、`FILE`。
- 文件只以 Mango `fileId` 和必要元数据持久化。请求时通过文件服务执行租户、用户和访问权限校验，不保存 URL、base64 或对象存储地址。
- 模型选项只返回当前 Spring AI/供应商适配器真实可处理的输入、输出模态。页面在上传前拦截不支持的格式，服务端再次 fail-closed 校验。
- 每条消息最多一段文本和六个附件；单文件不超过 20MB，单条消息附件总量不超过 40MB，发送给模型的当前多轮上下文附件总量不超过 80MB。
- 文本文件作为文本上下文，图片、音频、PDF 等按模型模态转换为 Spring AI `Media`。当前 Chat 适配器不声明视频理解能力，因此不能选择视频上传；内容块协议和页面仍可展示文件中心中的视频结果。
- 结构化服务在同一会话内继续执行输入与输出 JSON Schema 校验。纯附件输入在 Schema 声明标准 `text` 字段时映射为“请处理我上传的附件。”，附件内容仍作为文本或媒体发送给模型。
- Prompt 只允许使用已发布版本；Skill instructions 进入 system 指令；已配置未接入工具时明确拒绝。
- 会话、消息、调用审计和真实供应商 usage 按租户、用户、服务及 session 持久化。

## 内容块与展示边界

| 类型 | 输入 | 输出/历史展示 | 说明 |
|---|---|---|---|
| `TEXT` | 是 | 是 | 用户纯文本或文本文件提取后的模型上下文 |
| `RICH_TEXT` | 否 | 是 | 模型 Markdown；禁用原始 HTML，并拒绝危险链接 |
| `STRUCTURED_DATA` | 否 | 是 | 通过服务输出 Schema 校验的 JSON，可复制 |
| `IMAGE` | 是 | 是 | 必须由所选模型声明图片输入；展示从文件中心读取 |
| `AUDIO` | 是 | 是 | 当前输入只接受 MP3/WAV；展示从文件中心读取 |
| `VIDEO` | 条件支持 | 是 | 当前 Chat 适配器不声明视频输入；无真实 Provider 时上传前和服务端均拒绝 |
| `FILE` | 是 | 是 | 当前支持 PDF、TXT、Markdown、CSV、JSON、XML |

## 数据迁移

Flyway V9 将 `ai_chat_message.content` 一次性转换为 `content_parts_json`：用户历史为 `TEXT`，助手历史为 `RICH_TEXT`。迁移完成后删除旧 `content` 列；运行时代码只读取内容块，不保留旧列或字符串 fallback。

## 失败口径

- 服务、Prompt、Skill、模型、租户、用户、文件权限、MIME、大小、模态、输入 Schema 或输出 Schema 任一不满足时明确失败。
- 模型不支持的附件在上传前提示；直接构造请求仍由服务端拒绝。
- 模型调用失败不切换供应商、协议或旧入口。
- 媒体输出保存到文件中心失败时，本轮失败且不保存不完整会话。
- 富文本不执行模型输出中的 HTML、脚本或危险 URL。

## 验收

1. 三类服务均从服务列表进入同一个对话工作台，旧 `/ai/services/invoke` 返回 404。
2. 文本对话、结构化结果、会话刷新恢复和模型/思考锁定保持可用。
3. 页面只允许选择当前模型支持的附件；绕过页面提交不支持模态时服务端拒绝。
4. 图片、音频、视频和普通文件内容块能按文件权限预览或下载；业务数据中没有持久化访问 URL。
5. 文件权限、MIME、单文件大小、单条总量和多轮上下文预算均有失败测试。
6. 结构化服务仅上传文本文件时仍可形成标准 `text` 输入并通过输入、输出 Schema。
7. V9 执行后历史消息可读，旧 `content` 列和同步调用代码均不存在。
