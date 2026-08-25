# Mango Extension 使用说明

## 1. 概览

`mango-extension` 是 Mango 可选扩展模块的聚合入口。当前提供基于 Spring Boot 3.5.14 与 Spring AI 1.1.8 的 `mango-ai`，用于统一管理模型供应商、Prompt、Skill、工具和面向业务的 AI 服务，并通过一个对话式流式接口运行 `CHAT`、`EXTRACTION`、`CLASSIFICATION` 服务。

## 2. 功能清单

| 能力 | 说明 |
|---|---|
| 模型管理 | 管理供应商连接、多模型目录、文本/图片/向量/音频/视频等模态声明和默认能力路由 |
| 供应商接入 | 内置 DeepSeek、火山方舟、阿里云百炼、智谱 AI、硅基流动、Kimi、OpenAI 兼容协议和 Ollama 八类供应商 |
| AI 配置 | 管理 Prompt 模板、Skill 指令、MCP/HTTP 工具和 AI 服务定义 |
| 统一运行 | 所有服务通过标准 HTTP 受理，并由 Mango Realtime 投递模型增量 |
| 按轮设置 | 发送时冻结本轮模型和思考设置；生成期间的调整从下一轮生效 |
| 审计与会话 | 持久化调用审计、用量、会话、消息内容块和每条助手消息的实际模型事实 |

## 3. 接入方式

在需要 AI 能力的 Spring Boot 应用中引入 Starter：

```xml
<dependency>
    <groupId>io.mango.extension.ai</groupId>
    <artifactId>mango-ai-starter</artifactId>
    <version>${mango.version}</version>
</dependency>
```

Starter 自动装配 AI API、Spring AI 模型适配器、持久化服务、Controller、Flyway 迁移和 Mango 资源清单。前端管理页与统一运行台由 [`@mango/ai`](../../mango-ui/packages/ai/README.md) 提供，传输契约由 [`@mango/ai-api`](../../mango-ui/packages/ai-api/README.md) 提供。

## 4. 配置说明

供应商连接和模型目录通过“平台能力 → AI 管理 → 模型管理”维护，不在应用配置文件中保存供应商 API Key。API Key 使用 Mango Crypto 加密后入库，生产环境必须提供 SM4 密钥：

```yaml
mango:
  crypto:
    sm4:
      secret-key: ${MANGO_CRYPTO_SM4_SECRET_KEY}
      mode: CBC
      padding: PKCS5Padding
```

模型协议必须显式选择。DeepSeek 可使用 Spring AI DeepSeek 协议；其余内置供应商及 OpenAI 兼容端点使用已配置的 OpenAI Chat Completions 或 Responses 协议。运行时不会自动切换协议，也不会回退到旧 Provider。

## 5. API 与扩展

| 资源 | 主要接口 | 权限前缀 |
|---|---|---|
| 模型 | `/ai/models`、`/ai/models/providers`、`/ai/models/routes` | `ai:model:*` |
| Prompt | `/ai/prompts`、`/ai/prompts/publish` | `ai:prompt:*` |
| Skill | `/ai/skills` | `ai:skill:*` |
| 工具 | `/ai/tools` | `ai:tool:*` |
| AI 服务 | `/ai/services` | `ai:service:*` |
| 运行选项 | `GET /ai/services/options?serviceCode=<code>` | `ai:service:invoke` |
| 统一运行 | `POST /ai/services/chat?serviceCode=<code>` | `ai:service:invoke` |
| 停止生成 | `DELETE /ai/services/chat?requestId=<id>` | `ai:service:invoke` |
| 会话 | `/ai/services/conversations`、`/ai/services/conversation` | `ai:service:invoke` |

`POST /ai/services/chat` 返回标准 `R<AiServiceChatStartVO>`，模型增量通过 Mango Realtime 的 `ai.service.chat` 事件定向投递给当前租户和用户，事件 payload 包含 `thinking`、`message`、`done`、`error`。OpenAI Responses 适配器明确支持文本、图片和 PDF；TXT、CSV、JSON、XML、Markdown 文本文件由服务端读取为文本上下文。当前不声明 Responses 协议具备音频或视频理解能力，输入是否可用还必须同时满足模型目录模态与实际协议适配器能力。

当前运行链路不存在旧 `/invoke` 接口、协议自动 fallback 或会话级模型锁定。扩展新模型协议时应实现明确的 Spring AI `ChatModel` 适配边界，并在模型管理中声明协议和模态能力。

## 6. 数据与初始化

`mango-ai-core` 通过 Flyway `db/migration/ai/V1__...sql` 至 `V10__...sql` 创建并演进供应商连接、模型、能力路由、Prompt、Skill、工具、服务、调用审计、会话和消息表。Flyway 只负责结构演进，不写入新的供应商、模型或 Demo seed。

V10 将会话字段定义为最近一次成功回复的模型摘要，并在每条助手消息上保存本轮实际 `modelId`、`modelName`、`providerCode` 和 `thinkingEnabled`。用户消息不保存模型事实。附件业务数据只保存 Mango `fileId`、文件名、内容类型和大小，不持久化临时访问地址或文件二进制。

正式初始化资源位于 `mango-ai-starter/src/main/resources/META-INF/mango/resources/`：

- `ai-menu.json` 声明菜单和权限。
- `ai-provider-model-catalog.json` 为租户 `1` 首次创建 DeepSeek、火山方舟、阿里云百炼、智谱 AI、硅基流动、Kimi、OpenAI 兼容协议和 Ollama 八个供应商连接，并为每家创建一个代表模型。

这些连接不包含 API Key，连接和模型均默认停用。声明使用 `INIT_ONLY`，同租户同编码或模型已存在时保留管理员配置，不覆盖密钥、地址、启用状态、协议、模态或模型参数。火山方舟等按账号使用 endpoint ID 的供应商，需要管理员把代表模型名改为真实可调用值后再启用。

三个可运行示例位于 `META-INF/mango/demo/ai-demo-services.json`，分别为通用对话、合同五要素抽取和文本情感分类。Demo 只有在 `mango.resource.registry.demo-enabled=true` 时参与资源扫描；Prompt、Skill 和服务同样使用 `INIT_ONLY`，且服务不绑定固定模型，运行时由用户选择已配置且可用的模型。

## 7. 管理入口

默认入口位于“平台能力 → AI 管理”，资源包为 `platform_admin`：

| 菜单 | component key | 主要权限 |
|---|---|---|
| 模型管理 | `ai/models/index` | `ai:model:list` 及供应商、模型、路由维护权限 |
| 提示词配置 | `ai/prompts/index` | `ai:prompt:list` 及新增、编辑、删除、发布权限 |
| Skill 与工具 | `ai/skills/index` | `ai:skill:*`、`ai:tool:*` |
| AI 服务 | `ai/services/index` | `ai:service:list` 及维护、调用权限 |
| AI 服务运行台 | `ai/services/run/index` | `ai:service:invoke`，由前端隐藏路由注册 |

所有管理与运行数据按当前登录租户隔离；会话进一步按当前用户和服务隔离。后端 `@ApiAccess` 是权限边界，前端菜单可见性不能替代接口授权。

## 8. 快速开始

1. 引入 `mango-ai-starter`，配置 Mango 数据源、Flyway、文件能力和 SM4 密钥。
2. 执行 Bootstrap，使 Flyway V1 至 V10 和正式 Resource 声明同步完成；需要示例服务时显式启用 `mango.resource.registry.demo-enabled=true`。
3. 在“模型管理”为内置供应商连接填写自己的 API Key，核对基础地址和代表模型名，并启用至少一个 Chat 模型。
4. 发布 Prompt，按需维护 Skill/工具，再创建并启用 AI 服务。
5. 从“AI 服务 → 运行”进入统一工作台，在输入框旁选择下一条消息使用的模型和思考模式。
6. 发送后可继续调整选择器；当前回复仍使用发送时冻结的设置，新设置仅在下一次发送时生效。

## 9. 返回字段

`done` 事件返回 `sessionId`、`requestId`、本轮实际 `modelId/modelName/providerCode/thinkingEnabled` 和最终 `contentParts`。历史助手消息返回相同的本轮模型事实，页面据此展示，不使用输入器当前选择进行推测。

消息内容块支持文本、富文本、图片、视频、音频、普通文件和结构化数据的持久化展示。展示格式不等同于模型输入能力；输入仍按模型模态与协议适配器的交集校验。

## 10. 问题排查

- 模型不可用：检查供应商连接、API Key、模型启用状态、显式协议和能力路由；系统不会尝试旧协议或其它模型。
- 切换模型被阻止：待发送附件与目标模型不兼容；系统保留原模型和附件，需先移除附件或选择兼容模型。
- 生成期间修改未影响当前回复：这是按轮设置语义，修改将在下一轮发送时生效。
- 文件上传成功但模型拒绝：检查文件类型、大小、模型输入模态和协议真实能力；媒体展示能力不能证明模型可理解该媒体。
- API Key 无法解密：确认当前环境 `MANGO_CRYPTO_SM4_SECRET_KEY` 与写入密文时一致，不要清空或更换密钥后继续使用旧密文。
- 页面 404：核对 `ai-menu.json` 的 component key、前端包注册和 Hash 路由地址。

## 11. 相关文档

- [AI 管理与统一运行台](../../mango-ui/packages/ai/README.md)
- [AI TypeScript API](../../mango-ui/packages/ai-api/README.md)
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
- [AI 统一会话交付记录](../../mango-docs/plans/2026-08-24-ai-service-chat-delivery-record.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
