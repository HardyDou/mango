# @mango/ai 使用说明

## 1. 概览

`@mango/ai` 是 Mango 管理端 AI 页面与统一会话工作台。它提供模型、Prompt、Skill/工具、AI 服务管理，以及 `CHAT`、`EXTRACTION`、`CLASSIFICATION` 共用的对话式运行入口。

## 2. 功能清单

| 能力 | 页面或组件 | 说明 |
|---|---|---|
| 模型管理 | `ai/models/index` | 供应商接入、多模型目录、模态与默认能力路由 |
| 提示词配置 | `ai/prompts/index` | 模板、变量、版本和发布状态 |
| Skill 与工具 | `ai/skills/index` | Skill 指令与 MCP/HTTP 工具定义 |
| AI 服务 | `ai/services/index` | 服务、Schema、Prompt/Skill 绑定和运行入口 |
| 统一运行台 | `ai/services/run/index` | 多会话、按轮模型/思考设置、附件、流式输出和历史恢复 |
| 独立会话组件 | `AiConversationWorkspace` | 会话栏、消息区、输入器插槽和语义事件 |

## 3. 接入方式

```bash
pnpm add @mango/ai @mango/ai-api @mango/admin-extension element-plus vue vue-router
```

```ts
import { registerMangoAiAdminPages } from '@mango/ai/admin-pages';
import '@mango/ai/style.css';

registerMangoAiAdminPages();
```

| Mango 能力 | 本包使用位置 | 文档入口 |
|---|---|---|
| AI API | 页面数据、SSE、文件和会话 | [AI API README](../ai-api/README.md) |
| Admin Extension | 页面与隐藏路由注册 | [Admin Extension README](../admin-extension/README.md) |
| Common | HTTP、权限指令、文件预览 | [Common README](../common/README.md) |

## 4. 配置说明

| 配置入口 | 字段 | 默认值 | 含义 | 影响行为 | 源码入口 |
|---|---|---|---|---|---|
| `mangoAdmin` | `registrars` | `registerMangoAiAdminPages` | Admin 页面注册器 | 页面 key 与隐藏运行路由 | `package.json` |
| `mangoAdmin` | `style` | `@mango/ai/style.css` | 包样式入口 | 管理页与会话组件样式 | `package.json` |
| 后端运行选项 | `models` | 后端返回 | 当前服务可调用模型 | 模型选择、思考开关和附件格式 | `views/service-run/index.vue` |

API Key 只在供应商保存时提交；编辑留空保留原密钥，页面不回显密文。

## 5. API 与扩展

包入口导出管理页面、`AiConversationWorkspace`、`AiConversationSessionList` 及对应类型。独立会话组件只通过 props、slots 和 `create/select/delete/suggestion` 等语义事件与宿主协作，不直接依赖 Router、HTTP、权限或宿主 Store。

统一运行台发送时复制本轮模型与思考设置。生成期间选择器仍可使用，调整从下一轮生效；每条助手回复显示后端 `done` 事件确认的实际模型、供应商和思考状态。

## 6. 数据与初始化

页面本身不写初始化数据。后端资源清单提供菜单与权限，AI Flyway 提供供应商、模型、Prompt、Skill、工具、服务、审计和会话表。附件与历史消息只保存 Mango 文件 ID 和必要元数据，展示时重新获取受权预览或下载内容。

## 7. 管理入口

| 菜单 / 页面 | component key | 主要权限 | 入库来源 | 默认套餐 | 后端校验入口 |
|---|---|---|---|---|---|
| 模型管理 | `ai/models/index` | `ai:model:list` | `ai-menu.json` | `platform_admin` | `AiModelManagementController` |
| 提示词配置 | `ai/prompts/index` | `ai:prompt:list` | `ai-menu.json` | `platform_admin` | `AiConfigurationController` |
| Skill 与工具 | `ai/skills/index` | `ai:skill:list`、`ai:tool:list` | `ai-menu.json` | `platform_admin` | `AiConfigurationController` |
| AI 服务 | `ai/services/index` | `ai:service:list`、`ai:service:invoke` | `ai-menu.json` | `platform_admin` | `AiConfigurationController`、`AiServiceChatController` |
| AI 服务运行台 | `ai/services/run/index` | `ai:service:invoke` | 前端隐藏路由 | 随 AI 服务权限 | `AiServiceChatController` |

菜单路径为“平台能力 → AI 管理”。数据按当前登录租户和用户隔离，客户端页面不提交可覆盖租户边界的字段。

## 8. 快速开始

1. 后端引入并启用 `mango-ai-starter`，完成资源同步和 Flyway。
2. 前端注册 `@mango/ai/admin-pages` 并引入 `@mango/ai/style.css`。
3. 在模型管理中保存供应商连接和至少一个可调用 Chat 模型。
4. 发布 Prompt，创建并启用 AI 服务。
5. 从“AI 服务 → 运行”进入统一工作台，在输入器选择下一条消息的模型与思考模式。

输入支持文本及所选模型真实支持的图片、音频、视频、PDF 和文本文件。切换模型会重新校验待发送附件；不兼容时保留附件和当前选择，并显示原因。

## 9. 返回字段

助手消息可展示文本、安全 Markdown、结构化 JSON、图片、视频、音频和普通文件。媒体展示能力与模型输入/生成能力分别判断。助手消息下方的模型标签来自本轮实际执行结果，不从当前输入器选择推测。

## 10. 问题排查

- 页面 404：确认 URL 使用 Hash 路由，并核对菜单 `component` 与页面 key、包注册器和样式依赖。
- “当前模型不可用”：检查供应商连接、模型启用状态、显式 API 协议和模型适配器，不会自动切协议或回退旧实现。
- PDF、图片或视频不可选：查看输入器“当前模型输入”提示，并核对模型模态与适配器能力。
- 切换模型被阻止：待发送附件与目标模型不兼容；移除附件或选择支持对应格式的模型。
- 生成期间调整未改变当前回复：这是按轮设置语义，调整从下一条消息生效。

## 11. 相关文档

- [AI API 包](../ai-api/README.md)
- [Mango Extension](../../../mango/mango-extension/README.md)
- [能力地图](../../../mango-docs/capabilities/README.md)
- [AI 统一会话交付记录](../../../mango-docs/plans/2026-08-24-ai-service-chat-delivery-record.md)
- [前端组件规范](../../../mango-pmo/rules/frontend/03-component-development.md)
