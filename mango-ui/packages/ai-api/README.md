# @mango/ai-api 使用说明

## 1. 概览

`@mango/ai-api` 是 AI 管理与服务运行的传输无关 TypeScript 契约包。它只依赖 `@mango/api-schema`，由宿主注入实例级 `HttpClient`，不读取 Vue、Router、Store 或运行时环境变量。

## 2. 功能清单

| 能力                      | 用途                                               | 入口                                                   |
| ------------------------- | -------------------------------------------------- | ------------------------------------------------------ |
| 模型管理                  | 维护供应商接入、多模型目录和能力默认路由           | `createAiModelManagementApi()`                         |
| Prompt、Skill、工具、服务 | 租户级配置、发布和关联                             | `createAiConfigurationApi()`                           |
| 统一会话                  | 读取运行选项、管理会话、受理和取消三类 AI 服务调用 | `startServiceChat()`、`cancelServiceChat()` 及会话方法 |
| 文件输入                  | 上传模型输入文件并返回 Mango 文件记录              | `uploadChatFile()`                                     |

## 3. 接入方式

```bash
pnpm add @mango/ai-api @mango/api-schema
```

宿主创建带登录态、租户和服务根地址的 `HttpClient`，再传给 API factory。管理页面和运行工作台由 [`@mango/ai`](../ai/README.md) 提供。

| Mango 能力 | 本包使用位置                    | 文档入口                                                     |
| ---------- | ------------------------------- | ------------------------------------------------------------ |
| API Schema | `HttpClient`、`ApiId`、错误契约 | [API Schema README](../api-schema/README.md)                 |
| AI 后端    | AI 管理与服务接口               | [Extension README](../../../mango/mango-extension/README.md) |

## 4. 配置说明

本包没有全局配置和环境变量。实例行为全部来自传入的 `HttpClient`：

| 配置入口                     | 字段          | 默认值 | 含义                 | 影响行为                                     | 源码入口       |
| ---------------------------- | ------------- | ------ | -------------------- | -------------------------------------------- | -------------- |
| `createAiModelManagementApi` | `httpClient`  | 无     | 实例级请求客户端     | 模型管理请求的服务地址、登录态与租户         | `src/index.ts` |
| `createAiConfigurationApi`   | `httpClient`  | 无     | 实例级请求客户端     | 配置、文件、会话与运行请求                   | `src/index.ts` |
| `startServiceChat`           | `AbortSignal` | 可选   | 当前受理请求取消信号 | 页面离开或请求过期时中止尚未完成的 HTTP 受理 | `src/index.ts` |

## 5. API 与扩展

`createAiModelManagementApi(httpClient)` 提供供应商、模型和能力路由的查询与维护。读取方法接受 `AbortSignal`；API Key 只存在于写入 Command，不进入返回类型。

`createAiConfigurationApi(httpClient)` 提供 Prompt、Skill、MCP/HTTP 工具、AI 服务、文件上传、会话列表/详情/删除，以及 `POST /ai/services/chat?serviceCode=<code>` 标准受理和 `DELETE /ai/services/chat?requestId=<id>` 取消调用。

模型增量由 Mango Realtime 的 `ai.service.chat` 事件投递，类型为 `thinking`、`message`、`done`、`error`。本包导出 `parseAiServiceChatEvent()` 解析业务 payload；Realtime 连接和订阅由调用方通过 `@mango/common` 管理。`done` 返回 `sessionId`、`requestId`、本轮实际 `modelId/modelName/providerCode/thinkingEnabled` 和最终 `contentParts`。

## 6. 数据与初始化

本包不直接初始化数据库或菜单。数据由 AI 后端 Flyway 和资源清单创建；前端只消费 API 返回的租户数据。文件类型字段保存 Mango `fileId`，不把临时访问地址作为业务数据。

## 7. 管理入口

本包不注册页面或菜单。权限由后端 Controller 校验，宿主通过 `@mango/ai` 注册页面后使用相同的登录态和租户上下文。

## 8. 快速开始

```ts
import { createAiConfigurationApi, createAiModelManagementApi } from '@mango/ai-api';

const modelApi = createAiModelManagementApi(httpClient);
const configurationApi = createAiConfigurationApi(httpClient);

const models = await modelApi.listModels({ providerConnectionId: '1001' });
const services = await configurationApi.listServices();
```

每次发送使用独立 UUID `requestId`，先订阅 `ai.service.chat`，再调用 `startServiceChat()`。模型与思考设置放在本轮 Command 中；后续修改不会改变已经发出的请求。停止生成时中止本地等待并调用 `cancelServiceChat(requestId)`。

## 9. 返回字段

标识字段使用 `ApiId` 字符串语义。模型运行选项包含供应商、协议、输入/输出模态和思考可配置状态；历史助手消息包含本轮实际模型事实，用户消息对应字段为空。完整类型以包入口导出为准。

## 10. 问题排查

- Realtime 提示未知事件：核对 `ai.service.chat` payload、后端 `done` 事件和当前包版本是否一致，并检查可空内容块字段。
- 页面能展示媒体但不能上传：核对所选模型配置与实际适配器能力的交集；展示格式不代表模型输入能力。
- 请求 401/403：核对宿主注入的登录态、租户头和当前角色的 AI 权限，不在 API 包内另建客户端。
- 停止后仍写入界面：确认调用方按 `requestId` 取消订阅、调用 `cancelServiceChat()`，并丢弃迟到或重复分片。

## 11. 相关文档

- [AI 页面包](../ai/README.md)
- [Mango Extension](../../../mango/mango-extension/README.md)
- [能力地图](../../../mango-docs/capabilities/README.md)
- [前端业务 API 规范](../../../mango-pmo/rules/frontend/12-business-api.md)
