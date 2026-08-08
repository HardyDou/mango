---
documentType: delivery-l4
deliveryLevel: L4
pageBudget: 5
---

# 租户第三方身份登录

## 业务与用户故事

企业客户需要使用自有身份平台登录，当前只能维护独立密码。公司需要降低大客户接入成本；租户管理员需要控制身份源和账号绑定；系统用户需要从企业入口直接进入正确租户。本次支持每租户一个 OIDC 身份源，不支持跨租户账号合并。

1. BR-001 租户可配置独立 OIDC 身份源，外部身份只能进入其绑定租户和已绑定本地账号。
2. US-001 -> BR-001：前置为租户已启用 OIDC 且用户已绑定；用户从租户登录入口跳转身份平台并完成认证；系统校验回调后建立该租户会话；签名、state、issuer、绑定或租户不匹配时拒绝且不创建会话。

## 系统满足方式

1. SR-001 -> US-001：认证服务按入口租户加载 OIDC 配置，生成一次性 state 并发起授权；回调校验 state、nonce、issuer、audience 和签名，再按 `tenant_id + issuer + subject` 查绑定并签发租户会话；任何校验失败均记脱敏审计。

## 名称术语与适用图

- 系统/模块/功能：Mango 管理平台 / 租户认证模块 / OIDC 登录；简称 OIDC 仅指协议。
- 业务术语：外部身份是 `issuer + subject` 的稳定组合；账号绑定是外部身份到单一租户本地用户的关系。
- 适用图：跨系统认证数据流。

```mermaid
flowchart LR
  U[系统用户] --> M[Mango 租户登录入口]
  M --> I[租户 OIDC 身份平台]
  I --> C[Mango OIDC 回调]
  C --> B[租户账号绑定]
  B --> S[租户会话]
```

## 参考规范与代码

- 规范：`OpenID Connect Core 1.0@errata-set-2`；采用：授权码、ID Token、nonce、issuer 和 audience 校验。
- 代码：`mango-security/.../TenantLoginService.java@c52de19`；采用：扩展租户会话签发和登录审计，不复用密码认证凭据。

## 技术设计

1. TD-001 -> SR-001：认证 core 新增租户 OIDC 端口和回调服务；starter 提供发现文档/JWK 客户端；配置密文存储并按租户隔离缓存；绑定查询以租户、issuer、subject 唯一；回调事务只在全部校验成功后创建会话。
- 条件设计：新增 `/auth/oidc/{tenantCode}/start` 与固定回调接口；state 存储 5 分钟且单次消费；JWK 按响应缓存并在未知 key 时刷新一次；旧密码登录保持可用，由租户管理员独立关闭。
- 变更字典：配置 `OIDC_ISSUER/CLIENT_ID/CLIENT_SECRET`；字段 `external_identity(tenant_id, issuer, subject, user_id)`；错误码 `OIDC_STATE_INVALID/OIDC_TOKEN_INVALID/OIDC_BINDING_MISSING`；权限 `tenant:auth:oidc:manage`。

## 实施与验证

1. TASK-001 -> TD-001：先完成配置、密文和绑定表 migration，再实现 core 校验端口、starter 客户端、API 与管理页面；完成标准是租户配置和身份绑定均由服务端校验租户归属。
2. VAL-001 -> SR-001：使用两个租户和独立测试 issuer 执行 API/集成/浏览器验证；断言合法用户进入正确租户，篡改 state、错误 audience、跨租户 subject 和重放回调均失败且无会话；证据保存脱敏请求和审计记录，失败则停止启用。
- 切换/回滚：默认关闭 OIDC；按租户启用。异常时关闭租户开关并撤销客户端密钥，密码入口继续可用；绑定数据保留供复盘。
- 剩余风险：外部身份平台不可用时 OIDC 用户无法登录，租户管理员可使用保留的本地应急账号。
