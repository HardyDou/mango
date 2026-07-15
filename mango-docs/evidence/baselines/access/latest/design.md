# Mango Access 历史债务治理设计

## 1. 目标与批准基线

按已批准的 Payment 历史债务政策一次性治理 `mango-access`：先冻结可观察行为，补齐有价值的单元、集成和入口流程测试，再修复结构与安全债务，最后以同一测试基线和当前源码消费者验证逻辑、接口与特性。

用户已持续确认“推荐方案”“一步到位”“不要停止直到所有模块结束”，因此本模块采用推荐的完整边界治理方案，不重复等待形式确认。

## 2. 范围

- 直接模块：`mango-access-api`、`mango-access-core`、`mango-access-web-starter`、`mango-access-gateway-starter`。
- 必要消费者：System 的 `AccessContextValidator`、Auth Starter、Admin Starter、Gateway App。
- 产品边界：Servlet Filter、Spring Cloud Gateway GlobalFilter、token credential、API resource policy、permission snapshot、Mango 上下文与下游身份头。
- 不处理：Authorization 内部业务、登录签发逻辑、菜单/角色管理、前端页面和其它模块历史债务。
- 数据边界：Access 没有数据库、Flyway、初始化数据、演示数据或菜单资源，不执行数据库重建。

## 3. 治理前基线

1. 四子模块同 Reactor 构建通过，但 Access 自有测试只有 Web Starter 的 8 条；API、Core、Gateway 均为 0 条。
2. 现有测试使用 Spring mock request，只覆盖部分 Servlet 行为；没有真实 HTTP 入口、Gateway 路由或自动配置组合验证。
3. 正式历史预算记录 12 条结构债务：API 3、Core 6、Web Starter 1、Gateway Starter 2。
4. 外部请求携带的 `X-Mango-Tenant-Id`、`X-Mango-User-Id` 等身份头在 PUBLIC 或 token claim 缺失时可能进入下游上下文。
5. API resource 查询异常或失败响应被当作 unmatched LOGIN，原本需要 PERMISSION 的资源可能在依赖异常时降级为仅登录访问。
6. Gateway 没有 realtime probe ticket 入口兼容，且同步授权调用直接运行在 reactive filter 链上。

## 4. 兼容边界

- 保持 PUBLIC、LOGIN、PERMISSION、INTERNAL 的正常决策语义和既有 HTTP 状态。
- 保持 credential 优先级：Authorization Header、query `token`、Cookie `MANGO_TOKEN`。
- 保持外部 `/api` 前缀重试、IP 白名单、合法 access token、上下文校验和权限码匹配。
- 保持 realtime probe 由 realtime 自身验证已签发 ticket；Access 只允许该明确入口到达下游，不把任意 ticket 当作认证主体。
- API 既有 `AccessPrincipal`、`AccessResult`、`AccessContextValidationResult` 名称和 accessor/static factory 源码用法通过接口桥接保留；实现收敛为架构允许的 VO。
- 授权策略或权限提供方不可用属于已证明的安全缺陷，改为 503 fail-closed，不再降级放行或暴露未处理异常。

## 5. 方案

### 5.1 契约与核心决策

- 将三个 API record 收敛为同名只读接口，并由 `*VO` 不可变实现承载，保留调用方 accessor 与 factory 语义。
- 将承担纯决策职责的 `AccessService` 改为 `AccessEvaluator`，避免把跨模块 `R<T>` 适配伪装成业务 Service。
- 明确区分“资源未登记”和“资源策略服务失败”：前者保留 LOGIN 默认值，后者返回 unavailable。
- 权限快照为空、异常或不可读取时拒绝请求并返回 unavailable；正常的权限不足仍返回 403。

### 5.2 身份与上下文安全

- Servlet 入口在任何放行模式下都重建安全上下文，只保留 request/trace/client 元数据；tenant/user/member/realm/party/app 只能来自已验证 token。
- Gateway 在转发前先删除所有外部安全身份头，再按已验证 principal 写入；PUBLIC、auth-disabled 和 realtime probe 流也不得透传伪造身份。
- 401/403/503 JSON 使用统一安全编码，validator message 不得破坏 JSON 响应。

### 5.3 Web/Gateway 对齐与装配

- Web 与 Gateway 复用相同的 credential、realtime probe 与结果状态语义。
- Gateway 的同步授权决策切到 bounded elastic worker，避免阻塞 Netty event loop。
- Web/Gateway Starter 只依赖本域实现和必要 infra API/starter；Nacos 与 Authorization Remote 由 Gateway App 显式装配。
- 当前 System/Auth/Admin/Gateway App 消费者随契约同步并在同一 Maven reactor 编译，不读取本地旧 JAR。

## 6. 测试设计

| 用例 | 层级 | 稳定契约 |
|---|---|---|
| policy 未登记、失败响应、异常、PUBLIC/LOGIN/PERMISSION/INTERNAL | 单元 | 正常行为不变，失败 fail-closed |
| Bearer/query/Cookie、非法 token type、context validator、权限通配 | 单元 | credential 与权限边界 |
| IPv4/IPv6/CIDR、方法、路径、非法配置 | 单元 | 白名单边界 |
| Servlet 外部身份头清理、认证主体写入、401/403/503 JSON | 组件/集成 | 单体边界安全 |
| Gateway 外部身份头清理、上下文转发、probe、失败响应 | 组件/集成 | 微服务边界安全 |
| 随机端口 Servlet HTTP | 入口流程 | 真实 Filter、自动配置、HTTP 状态和下游上下文 |
| 随机端口 Gateway + 真实下游路由 | 入口流程 | 真实 reactive filter、route、身份头和 fail-closed |
| 当前源码消费者同 Reactor | 集成/静态 | Issue #522 防护与装配兼容 |

入口流程使用 JUnit 5 `flow`、`access` 标签，可按单用例、标签和完整 Access 套件执行。测试替身只替换 Access 之外的 token/resource/authorization 协作者，不替换 Access evaluator、Filter、Gateway、HTTP 或下游路由。

## 7. 风险与恢复

- 需求影响：L3。Access 是所有外部请求的身份、租户和权限边界，失败会造成越权或平台不可用。
- 方案风险：L3。修改 API/Core/Web/Gateway 及必要装配消费者，但无数据迁移，可按单 PR 回退。
- 最终风险：L3。
- 恢复：单 PR 回退；无数据库恢复动作。正常合法流以治理前 8 条基线、补充回归和真实入口流程共同证明。
