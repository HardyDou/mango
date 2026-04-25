# Mango 后端架构边界收敛总文档

- 创建日期：2026-04-14
- 状态：进行中
- 类型：总目标文档

---

## 1. 文档定位

本文件不是 Sprint，不直接作为开发交付物验收。

本文件用于回答三个问题：

1. Mango 后端为了达成最终目标，当前还存在哪些架构缺陷
2. 后端重构的总目标是什么
3. 后续应拆成哪些可独立交付的子阶段 / Phase

---

## 2. 最终目标

Mango 后端的最终目标不是“目录看起来分层”，而是形成一套真正适合 AI Agent 持续开发的后端底座。

目标状态如下：

- 采用 **模块化单体优先、远程化兼容保留** 的架构
- 通过 `api/core/starter/starter-remote` 提供稳定模块边界
- `starter` 只负责装配，不承载长期业务语义
- `app` 负责部署装配与运行配置
- `common` 只保留稳定、低耦合、跨模块公共契约
- `infra` 只提供技术能力，不依赖平台业务模型
- `infra` 的客户端实时通信能力统一由 `mango-infra-realtime` 承担，旧 `mango-infra-sse` / `mango-infra-websocket` 不再作为独立上层依赖入口
- `platform` 模块按 bounded context 拆分，不再形成“平台杂项大模块”
- 文档、POM、目录、代码中的领域命名保持一致

---

## 3. 当前主要问题

### 3.1 公共层过重

`mango-common` 当前同时承载：

- 公共异常
- 统一返回模型
- 分页模型
- 校验注解
- JSON 工具
- Base64 工具
- TokenContext 兼容类
- 日志语义注解

风险：

- 成为全局耦合源
- 新增代码天然往 `common` 里堆
- AI Agent 无法稳定判断“什么该放 common、什么不该放”

### 3.2 基础设施层反向感知业务层

示例：

- `mango-infra-web` 依赖 `mango-authorization-api`

风险：

- infra 不再是通用底座
- 任何平台业务变动都可能反向污染基础设施层

### 3.3 平台重模块职责膨胀

重点模块：

- `mango-identity`
- `mango-authorization`
- `mango-security`
- `mango-system`
- `mango-auth`

风险：

- `mango-authorization` 曾混入身份事实，现已抽离到 `mango-identity`
- `mango-security` 仅作为安全聚合入口，不承载认证、身份、授权业务事实
- `mango-system` 演变为配置/租户/字典/日志/路由的杂项箱
- `mango-auth` 同时承担认证与安全治理

### 3.4 领域事实源不一致

表现：

- 历史 Sprint 文档中保留过 `mango-user` / `mango-permission` 旧叙述
- 当前有效模块以源码目录和聚合 POM 为准：身份事实归入 `mango-identity`，授权事实归入 `mango-authorization`
- 当前有效 README、索引和架构文档不得再把 `mango-user` / `mango-permission` 当作有效模块

风险：

- AI 生成代码时会错误引用模块
- Review 与实现基线不一致

---

## 4. 总体收敛原则

### 4.1 分层依赖

```text
app -> starter/adapter -> core -> api -> common-kernel
```

### 4.2 职责边界

- `common`: 公共内核与契约
- `infra`: 技术能力
- `platform`: 通用业务域
- `app`: 部署装配

### 4.3 当前重构顺序

原文档曾按 Sprint 09-12 规划为：

1. `mango-common`
2. `mango-infra-web` / `mango-infra-security`
3. `mango-infra-kv`
4. `mango-authorization`
5. `mango-system`
6. `mango-auth`
7. `mango-admin-app`

但实际执行中，阶段 3 没有继续沿用旧 Sprint 顺序，而是先完成了 `mango-infra-realtime`，将旧 `mango-infra-sse` 与 `mango-infra-websocket` 收敛为统一 server-client realtime 能力。该调整是合理的，因为 realtime 是 infra 层协议能力收口，且会影响 `mango-biz-notification`、`mango-admin-app` 和后续平台模块的实时投递接入方式。

因此，自 2026-04-19 起，本文档以 `2026-04-17-backend-module-by-module-refactor-plan.md` 的 Phase 主链路为准，重构顺序调整为：

```text
Phase -1 模块归属决策门
  -> Phase 0 事实源校准
  -> Phase 1 mango-common
  -> Phase 2 mango-infra-kv
  -> Phase 3 mango-infra-realtime
  -> Phase 4 mango-infra-security + mango-infra-web
  -> Phase 5 mango-gateway
  -> Phase 6 mango-authorization
  -> Phase 7 mango-system
  -> Phase 8 mango-auth
  -> Phase 9 mango-admin-app
  -> Phase 10 平台非主链路模块标准化（按需）
```

阶段排序原则同步调整为：

1. 先校准事实源，避免旧模块命名继续误导后续生成
2. 先收敛 `common`，稳定公共契约
3. 再收敛 infra 主链路：`kv -> realtime -> security/web -> gateway`
4. infra 稳定后再进入 platform 主链路：`rbac -> system -> auth`
5. 最后处理 `admin-app` 装配层
6. 非主链路平台模块进入按需标准化，不阻塞主链路

---

## 5. 子阶段路线图

### Phase -1：模块归属决策门

文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：已作为模块级计划前置决策写入

目标：

- 固定身份事实、`mango-common` 准入边界、infra 能力域、platform 合并策略等关键决策
- 防止后续 Phase 在基础归属问题上反复返工

### Phase 0：事实源校准

文档：`mango-docs/plans/2026-04-17-phase-0-fact-source-delivery-record.md`

状态：已完成

目标：

- 清理当前有效源码、POM、README、索引、架构文档中的旧模块事实源
- 明确历史 Sprint 文档允许保留旧名，但不得作为当前事实源

### Phase 1：`mango-common`

文档：

- `mango-docs/plans/2026-04-17-phase-1-common-class-ownership.md`
- `mango-docs/plans/2026-04-17-phase-1-common-delivery-record.md`

状态：已完成

目标：

- 将 `mango-common` 收敛为公共内核与公共契约
- 清理技术工具类、历史兼容类和领域色彩类型的错误归属
- 冻结 `common` 准入规则

### Phase 2：`mango-infra-kv`

文档：

- `mango-docs/plans/2026-04-17-phase-2-kv-configuration-rules.md`
- `mango-docs/plans/2026-04-17-phase-2-kv-delivery-record.md`

状态：已完成

目标：

- 收口 KV 配置命名、store 选择、use case bean 装配规则
- 明确 memory / redis / jdbc 实现选择口径
- 为后续 infra 模块提供稳定横切状态能力

### Phase 3：`mango-infra-realtime`

文档：

- `mango-docs/plans/2026-04-18-infra-realtime-design.md`
- `mango-docs/plans/2026-04-18-phase-3-messaging-delivery-record.md`

状态：已完成

目标：

- 新建 `mango-infra-realtime` 聚合模块，拆分为 `api`、`core`、`starter`、`starter-remote`
- 将旧 `mango-infra-sse` 与 `mango-infra-websocket` 合并为统一 server-client realtime 能力
- 提供 SSE、WebSocket、HTTP Polling、transport 协商、远程发布和 WebSocket 入站分发
- 将 `mango-biz-notification` 改为依赖 `mango-infra-realtime-api` 完成在线实时投递
- 将 `mango-admin-app` 的 WebSocket 运行时能力收敛到 `mango-infra-realtime-starter`

本阶段补充说明：

- 这是对旧路线中阶段 3 偏离原计划后的正式校准。
- `mango-infra-realtime` 应位于 `mango-infra-kv` 之后、`mango-infra-security/web` 之前。
- 原因是 realtime 当前仍有轻量 header/query 读取，后续需要在 Phase 4/5 通过统一请求上下文、安全上下文和 gateway 可信 header 规则做被动适配。
- `mango-infra-realtime` 不承担业务通知、消息中心、离线消息、已读未读、MQ/Event、Agent 聚合、短信、邮件等业务或事件总线职责。
- 业务通知域继续由 `mango-biz-notification` 承担；未来 MQ/Event 能力如需要，另行规划 `mango-infra-messaging`。

### Phase 4：`mango-infra-security` + `mango-infra-web`

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：已完成，待人工验收

目标：

- 去除 infra 对平台业务模型的反向依赖
- 冻结统一请求上下文 / 安全上下文契约
- 明确 userId、tenantId、认证状态、requestId、traceId、clientIp 的来源和读取方式
- 让 `mango-infra-realtime`、notification、audit、业务模块后续只通过 resolver/provider 被动消费统一上下文

### Phase 5：`mango-gateway`

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：待执行

目标：

- 清理 gateway 默认配置中的旧 `user/permission` 命名
- 冻结匿名路径、登录必需路径、内部路径的来源接口
- 明确 gateway 对认证结果治理、可信 header 注入、外部伪造 header 清洗的责任边界
- 明确 WebSocket/SSE/Polling 等长连接入口的路由、超时、限流、CORS/Origin 接入约定

### Phase 6：`mango-authorization`

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：待执行

目标：

- 将 `mango-authorization` 收敛为授权与访问控制中心
- 冻结 `authorization -> auth` 的认证用户 / 权限桥接契约
- 收口用户事实、角色、菜单、按钮权限、公共路径的边界

### Phase 7：`mango-system`

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：待执行

目标：

- 将 `mango-system` 从系统杂项箱收敛为可维护的系统域集合
- 按 `dict/config/tenant/audit/route` 明确子域边界
- 输出未来物理拆分触发条件

### Phase 8：`mango-auth`

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：待执行

目标：

- 将 `mango-auth` 收敛为认证域
- 保留登录、登出、refresh token、token validate 等认证职责
- 迁出或隔离防重放、幂等、验证码拦截、Web MVC 拦截治理等非认证职责

### Phase 9：`mango-admin-app`

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：待执行

目标：

- 将 `mango-admin-app` 收敛为纯部署装配层
- 明确 local / remote starter 选择方式
- 输出 remote starter 覆盖清单与缺口清单

### Phase 10：平台非主链路模块标准化

主文档：`mango-docs/plans/2026-04-17-backend-module-by-module-refactor-plan.md`

状态：按需触发，不作为主链路阻断项

候选模块：

- `mango-org`
- `mango-captcha`
- `mango-area`
- `mango-i18n`
- `mango-biz-notification`
- `mango-ai`

目标：

- 只在业务需求、依赖风险、API/core/starter 边界不一致等条件触发时执行
- 不因代码少、目录多、命名不对称而合并模块
- `mango-biz-notification` 只作为业务通知域标准化，不与 `mango-infra-realtime` 合并

### Backlog 候选：持久化抽象层收敛

状态：待规划，当前不进入执行

目标：

- 为 `core` 层提供稳定的持久化抽象端口
- 让业务模块默认依赖 Mango 自己的 CRUD / Page 能力，而不是直接绑定 `mybatis-plus`
- 保留 `mybatis-plus`、`jpa` 等不同持久化实现的适配空间

当前结论：

- 这件事值得做
- 但现在只适合进入 Backlog，不适合立即做全仓重构
- 后续如启动，建议先做设计和单模块 POC，再决定是否推广到生成器和全仓模块

建议切入范围：

1. 定义持久化抽象端口（Repository / Page / CRUD）
2. 先仅提供 `mybatis-plus` 适配实现
3. 选择简单模块做 POC（如 `Post`）
4. 验证通过后再决定是否落入正式 Phase / Sprint

---

## 6. 各阶段之间的依赖关系

```text
Phase -1 -> Phase 0 -> Phase 1 -> Phase 2 -> Phase 3 -> Phase 4 -> Phase 5 -> Phase 6 -> Phase 7 -> Phase 8 -> Phase 9
```

说明：

- Phase -1 / Phase 0 完成后，后续阶段有稳定模块事实源
- Phase 1 完成后，后续模块有稳定公共依赖基础
- Phase 2 完成后，KV 横切状态能力的配置和装配规则稳定
- Phase 3 完成后，旧 SSE/WebSocket 已收敛为 `mango-infra-realtime`，平台模块统一通过 realtime API 做在线投递
- Phase 4 完成后，Web/Security/Context 的统一上下文契约稳定，realtime 可从轻量 header/query 读取过渡到被动消费 resolver/provider
- Phase 5 完成后，gateway 的认证结果治理、可信 header 注入、外部伪造 header 清洗和长连接入口接入规则稳定
- Phase 6 完成后，RBAC 的授权、用户事实、公共路径和认证桥接契约稳定
- Phase 7 完成后，system 子域边界稳定
- Phase 8 完成后，auth 回归认证域
- Phase 9 完成后，admin-app 完成装配层收口
- Phase 10 按需触发，不阻塞主链路

---

## 7. 总体验收口径

总文档本身不验收代码，验收口径体现在各子阶段 / Phase 中。

总文档完成的标志是：

- 总目标明确
- 重构顺序明确
- 子阶段边界明确
- 每个 Phase 都能独立交付
