# Mango AI 模型管理优化交付记录

## 范围

本次将原单层模型配置替换为最终模型管理领域：供应商接入、供应商模型目录和按能力默认路由。页面入口为“平台能力 → AI 管理 → 模型管理”，采用 PigX 风格双栏工作区，但保留 Mango baseline 的 API 包、权限和真实数据边界。

供应商类型：DeepSeek、火山方舟、阿里云百炼、智谱 AI、硅基流动、Kimi、OpenAI 兼容协议、Ollama。模型用途与输入/输出模态分开：用途包括 Chat、Embedding、Rerank、图片生成、语音识别、语音合成和视频生成；模态包括文本、图片、音频、视频和向量。

## 架构决定

1. `ai_provider_connection` 保存租户接入信息和加密密钥；同一租户可维护同厂商多个接入实例。
2. `ai_model` 以 `provider_connection_id` 关联供应商，一个接入下可维护多个模型，能力和模态以 JSON 集合保存。
3. `ai_capability_route` 以租户和能力唯一，为 Chat 等能力指定默认模型；不再使用单一全局默认模型布尔值。
4. DeepSeek 与 OpenAI Chat 兼容供应商使用真实 Spring AI 适配器；Ollama 仅可配置，未实现适配器时不会返回可调用标识。
5. API Key 只允许写入命令出现，服务端使用 Mango Crypto/SM4 加密，VO 和页面不回显密文。

## 删除清单

旧 `AiModelConfig*` API、Command、Query、VO、Entity、Mapper、Service、Controller、单层 Flyway 表、旧页面、旧权限和旧前端 API 已删除；没有保留兼容路径、旧入口或运行时 fallback。

## 验证

- `mvn -pl mango-extension/mango-ai/mango-ai-starter -am -DskipTests=false test`：通过，AI Core 8 项、AI Starter 7 项及依赖模块通过。
- 前端需执行 `pnpm --filter @mango/ai-api test`、`pnpm --filter @mango/ai typecheck`、`pnpm --filter @mango/ai build` 及页面浏览器验收。
- 当前工作区未执行破坏性数据库重建；既有本地旧库需按用户确认后清理并使用 Fresh MySQL 重新运行 Flyway。

## 兼容性与发布

这是 AI 模块未发布阶段的破坏性领域替换，不提供旧 `/ai/model-configs` 兼容接口。Spring Boot 3.5.14 和 Spring AI 1.1.8 保持不变；Boot 4 升级另行评估。
