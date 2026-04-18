# 2026-04-17 后端按模块重构计划

- 创建日期：2026-04-17
- 状态：建议执行
- 类型：模块级重构路线图
- 关联文档：
  - `../mango-backend-architecture-boundary-refactor-master-plan.md`
  - `./2026-04-14-sprint-11-platform-rbac-system-boundary-phase1.md`
  - `./2026-04-14-sprint-12-auth-admin-app-boundary-assembly-cleanup.md`
  - `./2026-04-17-sprint-15-capability-registry-remote-adapter.md`

---

## 1. 结论先行

结合当前项目目标不变、且希望按依赖层级稳定收敛的前提：

**后端重构应采用自底向上的顺序：`common -> infra -> platform -> app`。**

因此：

1. 先做 `Phase -1`：模块归属决策门
2. 再做 `Phase 0`：事实源校准
3. 第一个正式重构模块应是 `mango-common`
4. 然后进入 `mango-infra`
5. 底层稳定后，再进入 `mango-platform`
6. 最后处理 `mango-app`

在这个顺序里，`mango-rbac` 仍然是平台层最先要动的重模块，但它不应越过 `common` / `infra` 提前开刀。

---

## 2. 当前项目理解

### 2.1 当前后端分层

当前后端仍然是清晰的四层结构：

```text
app -> starter/starter-remote -> core -> api -> common/infra
```

对应目录：

- `mango-common`：公共内核与通用契约
- `mango-infra`：Web、安全、KV、日志、上下文、网关等技术基础设施
- `mango-platform`：认证、权限、系统配置、组织、国际化、验证码、消息、AI 等平台能力
- `mango-app`：部署装配层，目前以 `mango-admin-app` 为主

### 2.2 当前后端模块清单

核对时间：2026-04-17。

本清单以源码目录下存在 `pom.xml` 且被聚合 POM 纳入构建为准，不以残留 `target/` 产物为准。

#### 公共层

- `mango-common`

#### 基础设施层

- `mango-gateway`
- `mango-infra-context`
- `mango-infra-crypto`
- `mango-infra-db`
- `mango-infra-doc`
- `mango-infra-feign`
- `mango-infra-kv`
- `mango-infra-log`
- `mango-infra-module`
- `mango-infra-orm`
- `mango-infra-redis`
- `mango-infra-security`
- `mango-infra-sse`
- `mango-infra-test`
- `mango-infra-web`
- `mango-infra-websocket`

说明：

- 当前有效 infra 源码模块共 16 个。
- `mango-infra-observability` 已不在 `mango/mango-infra/pom.xml` 聚合模块中，也不应作为有效源码模块处理。
- 如果本地目录中仍看到 `mango-infra-observability/target`，只代表历史构建产物残留，不代表模块仍有效。

#### 平台能力层

- `mango-ai`
- `mango-area`
- `mango-auth`
- `mango-captcha`
- `mango-i18n`
- `mango-biz-notification`
- `mango-org`
- `mango-rbac`
- `mango-system`

说明：

- 当前有效 platform 模块共 9 个。

#### 应用层

- `mango-admin-app`

说明：

- 当前有效 app 模块共 1 个。

### 2.3 当前结构特征

从源码体量、依赖关系和职责分布看，当前真正的承重模块是：

1. `mango-common`
2. `mango-infra-kv`
3. `mango-rbac`
4. `mango-system`
5. `mango-auth`
6. `mango-admin-app`

其中：

- `mango-common` 决定底层公共契约是否稳定
- `mango-infra-kv` 决定大量横切能力的注入与默认行为
- `mango-rbac` / `mango-system` / `mango-auth` 构成平台主链路
- `mango-admin-app` 是最终装配出口

其余模块大多还处于薄能力模块状态，适合在主链路收敛后再逐个标准化。

### 2.4 各层核心职责

#### `mango-common`

- 统一结果模型
- 异常与错误码
- 分页与基础 DTO
- 最小公共注解与契约

#### `mango-infra`

- Web 请求、上下文、日志、安全、KV、网关、序列化、RPC、客户端消息通信
- 对平台层只提供技术能力，不持有业务事实

#### `mango-platform`

- 平台通用业务域
- 认证、授权、系统配置、组织、国际化、验证码、消息、区域、AI

#### `mango-app`

- 部署装配
- profile 与运行时配置
- local / remote 能力选择
- 不承载长期领域逻辑

---

## 3. 当前主要问题

### 3.1 领域事实源漂移

当前代码存在“目录结构、POM、文档、装配代码不是同一套事实”的问题：

- 根 `pom.xml` 曾残留 `mango-user-*`
- `mango-admin-app` 曾直接依赖 `mango-user-starter`
- `mango-admin-app` 启动类曾导入 `io.mango.user.starter.UserAutoConfiguration`
- 多份 README 和历史计划文档仍使用 `mango-user` / `mango-permission` 旧叙述

当前源码与 POM 层的 `mango-user-*` 直接引用已清理；后续 Phase 0 只继续收口当前有效 README、索引文档和架构文档中的命名漂移。历史 Sprint 文档允许保留旧名，但必须明确其历史背景，不作为当前事实源。

#### 幽灵模块清单

| 幽灵模块/旧命名 | 当前状态 | Phase 0 处理要求 |
|-----------------|----------|------------------|
| `mango-user-api` | 已从根 POM dependencyManagement 与 `mango-rbac-api` 依赖中清理 | 搜索确认源码/POM 不再引用 |
| `mango-user-core` | 已从根 POM dependencyManagement 清理 | 搜索确认源码/POM 不再引用 |
| `mango-user-starter` | 已从根 POM dependencyManagement 与 `mango-admin-app` 依赖中清理 | 搜索确认 app 不再装配 |
| `mango-user-starter-remote` | 已从根 POM dependencyManagement 清理 | 搜索确认无远程 starter 残留 |
| `io.mango.user.starter.UserAutoConfiguration` | 已从 `mango-admin-app` 启动类清理 | 搜索确认无 import |
| `io.mango.user.core.mapper` | 已从 `mango-admin-app` mapper scan 清理 | 搜索确认无 mapper scan |
| `mango-permission` | 属于旧叙述命名，不是当前有效模块 | 当前有效文档必须改为 `mango-rbac` 或明确历史背景 |

#### `mango-admin-app` 当前依赖清单

Phase 0 开始前必须以此表为交接基线。

| 类型 | artifactId | 判断 |
|------|------------|------|
| Mango | `mango-common` | 合法，公共契约 |
| Mango | `mango-infra-module-starter` | 合法，模块元数据与部署映射 |
| Mango | `mango-org-starter` | 合法，平台组织能力装配 |
| Mango | `mango-area-starter` | 合法，平台区域能力装配 |
| Mango | `mango-ai-starter` | 合法，平台 AI 能力装配 |
| Mango | `mango-i18n-starter` | 合法，平台国际化能力装配 |
| Mango | `mango-rbac-starter` | 合法，当前 RBAC 本地装配 |
| Mango | `mango-captcha-starter` | 合法，验证码能力装配 |
| Mango | `mango-gateway-starter` | 合法，网关/认证过滤装配 |
| Mango | `mango-infra-kv-starter` | 合法，KV 能力装配 |
| Mango | `mango-auth-starter` | 合法，认证能力装配 |
| Spring/第三方 | `spring-boot-starter-web`、`spring-boot-starter-jdbc`、`spring-boot-starter-actuator`、`spring-boot-starter-websocket`、`h2`、`mybatis-spring`、`springdoc-openapi-starter-webmvc-ui`、`lombok` | 合法；`spring-boot-starter-websocket` 在 messaging 完成后复核是否应由 infra messaging 间接提供 |

当前已知错误：

- `mango-admin-app` 描述仍包含 `user, permission` 旧叙述，应在 Phase 0 修正为 `rbac/auth/i18n` 等当前事实。
- 当前 POM 中未发现仍直接依赖 `mango-user-*` 的 artifact。

### 3.2 `mango-common` 仍是全局耦合源

如果 `common` 不先收敛：

- 后续每个模块都会继续把临时契约堆进去
- 平台层重构会不断被公共模型污染
- 开发者无法判断“什么该放 common”

### 3.3 `mango-infra` 的粒度与边界不稳定

当前 `infra` 既有真正需要独立的横切底座，也有一些体量很小、长期独立价值不高的模块。

问题不只是“模块多”，而是：

- 有些模块值得保留独立
- 有些模块过细，增加了维护和装配复杂度
- 有些自动配置边界仍偏宽

### 3.4 `mango-rbac` 实际上承担了“用户 + 权限 + 菜单 + 公共路径”

`mango-rbac` 当前不只是 RBAC：

- 用户实体与用户角色关系在这里
- 角色、菜单、按钮权限在这里
- 匿名路径 / 登录必需路径也在这里
- `AuthServiceImpl` 通过它获取认证用户事实

这使得 `mango-rbac` 成为身份域和授权域的混合体。

跨 Phase 风险：

- `mango-auth-core` 当前依赖 `mango-rbac-api`。
- `AuthServiceImpl` 已通过 `IAuthUserProvider` 做了一层认证用户 provider 抽象，但源码仍存在 `io.mango.rbac.api.*` import 与 POM 依赖。
- Phase 6 重构 `mango-rbac` 时，如果改变认证用户接口，会影响 Phase 8 才正式收口的 `mango-auth`。

处理规则：

- Phase 6 允许对 `auth` 做被动适配，但只允许适配认证用户 provider、权限 checker、POM 依赖，不允许顺手重构登录流程。
- Phase 6 必须冻结 `rbac -> auth` 的桥接契约：`IAuthUserProvider`、`IPermissionChecker` 或等价接口。
- Phase 8 再处理 auth 内部职责收敛，例如防重放、幂等、验证码拦截迁出。
- 如果 Phase 6 发现必须大改 auth 登录语义，必须停止并拆出 ADR，不允许把 Phase 8 的工作提前混入 Phase 6。

### 3.5 `mango-system` 是典型杂项箱

`mango-system` 当前同时包含：

- 字典
- 配置
- 登录日志
- 操作日志
- 租户
- 路由

这些能力耦合度并不天然一致，后续要么拆域，要么至少先收敛边界。

处理规则：

- Phase 7 可以重新定义 `dict/config/tenant/audit-log/route` 的 API 边界，但必须在 Phase 7 内完成决策。
- Phase 7 验收后，system 子域 API 视为冻结；后续如需再次调整，必须走 ADR 或新增 Phase，不允许在后续模块中顺手改。
- Phase 7 必须输出“子域物理拆分触发条件清单”，避免只做包名整理但无法后续拆分。

### 3.6 `mango-auth` 混入了认证之外的治理逻辑

`mango-auth` 当前除了登录、刷新 token，还承担：

- 防重放
- 幂等
- 验证码拦截
- Web MVC 拦截器

这会让 auth 从“认证域”膨胀成“安全杂项域”。

---

## 4. 为什么要先打底层

### 4.1 先收敛 `common`

`common` 是全局依赖源。它不稳定，后面所有模块都会跟着不稳定。

### 4.2 再收敛 `infra`

`infra` 决定：

- 注入边界是否清晰
- 自动配置是否可预测
- 上层是否会绕过底层直接耦合实现

### 4.3 最后再动平台主链路

只有在 `common` 和 `infra` 稳定之后，`rbac` / `system` / `auth` 的职责收敛才不会反复返工。

---

## 5. 重构总原则

### 5.1 一次只动一个主模块

单次交付只允许一个主模块承担结构变更，其它模块只做被动适配。

### 5.2 先边界，后优化

先解决：

- 职责归属
- 依赖方向
- API 暴露面
- starter 装配边界

再解决：

- SQL 效率
- 缓存
- 代码重复
- 性能与观测性

### 5.3 以装配层最薄为目标

`starter` 和 `app` 必须越来越薄，`core` 才能成为长期稳定的行为中心。

### 5.4 文档、POM、代码同时收口

任何一次模块重构，都必须同步收口：

- 包名与 artifactId
- README 与架构文档
- 自动配置和装配方式

### 5.5 以模块职责为边界，而不是以历史目录为边界

是否保留、拆分、合并某个模块，不看历史名字，而看三件事：

1. 它是否承载单一职责
2. 它是否拥有稳定对外 API
3. 它是否值得单独部署或单独演进

### 5.6 合并不是目标，价值成立才合并

本轮重构不以合并或拆分为交付目标。

只有当合并后能明确服务 Mango 的项目特性，且至少带来以下两项收益时，才允许合并：

- 上层依赖更容易选择
- 自动装配更简单
- 能力边界更符合行业惯例
- 后续扩展更自然
- 共同变化原因更明确

禁止用以下理由推动合并：

- 模块代码少
- 目录太多
- 名字不对称
- 看起来都属于某个很泛的概念

#### 合并判断机制

合并/拆分不能由执行 agent 单方面决定。

职责划分：

- 执行 agent：只能提交合并/拆分提案，列证据、收益、风险、迁移成本。
- 项目负责人：决定是否采纳提案。
- 计划文档：记录最终结论，后续 agent 必须按结论执行。

合并触发条件：

| 条件 | 含义 |
|------|------|
| 共同上游依赖 | 两个模块长期被 3 个及以上相同上游模块同时依赖 |
| 共同变化原因 | 最近连续多次改动总是需要同时修改两个模块 |
| 共同装配入口 | starter 自动配置高度重复，用户无法判断应该引入哪个 starter |
| 共同抽象模型 | 两个模块使用同一组核心抽象，例如 session/subscription/dispatcher |
| 行业惯例一致 | 主流框架或系统通常把它们归入同一能力域 |

合并否决条件：

| 条件 | 含义 |
|------|------|
| 变化原因不同 | 一个关注数据访问，一个关注缓存，或一个关注 RPC，一个关注客户端连接 |
| 生命周期不同 | 一个适合独立部署或独立演进，另一个只是本地库能力 |
| 语义层级不同 | 一个是基础设施机制，另一个是业务域模型 |
| 只是目录多 | 没有真实依赖、装配、认知收益 |

当前已确认结论：

- `sse + websocket -> mango-infra-realtime`：通过，原因是同属 server-client messaging，存在共同连接/会话/订阅/投递抽象。
- `feign + sse/websocket`：否决，原因是 Feign 是 service-to-service RPC，SSE/WebSocket 是 server-client messaging。
- `db + redis`：否决，原因是 DB 是持久化数据访问，Redis 是缓存/分布式数据结构/临时状态能力，变化原因和使用模型不同。
- `log + observability`：否决，原因是 observability 当前缺少实际能力，已删除；log 有实际内容，先独立保留。

### 5.7 术语表

| 术语 | 定义 | 验收口径 |
|------|------|----------|
| 事实源 | 当前源码、POM、README、架构文档对模块命名和职责的一致描述 | 同一模块不能同时出现新旧命名，历史文档必须标注历史语境 |
| 技术契约 | 只表达技术机制的接口、DTO、配置或 provider，不包含平台业务事实 | 不依赖 `mango-platform`，类名/字段不表达用户、角色、菜单、组织、业务消息等领域语义 |
| 业务模型 | 表达业务事实、业务流程或业务规则的 entity/DTO/VO/PO | 不允许进入 `common` 或 infra 技术实现层 |
| 直接感知内部模型 | 上游模块依赖下游 core/entity/mapper/service 实现，或 import 下游业务实体 | 只允许依赖 api/provider/checker 等稳定契约 |
| 边界收敛 | 模块职责、对外 API、包结构、POM 依赖、starter 装配与文档一致 | 不只是移动类；必须减少非法依赖或明确稳定接口 |
| store | KV 的实际存储实现，例如 memory、redis、jdbc | 只回答“底层用哪个存储实现” |
| use case bean | 基于 KV store 组装的具体使用场景 Bean，例如 cache、lock、rate-limit、idempotent | 只回答“哪些能力默认启用、何时启用、如何覆盖” |
| 默认 service name | gateway remote/local 路由配置中的默认服务名，例如 `authService`、`userService` | 不能再出现旧 `user/permission` 命名 |
| 小修 | 不改变模块边界、不新增跨层依赖、不修改公共 API 的 bugfix 或文档修正 | 长尾模块只允许小修，不允许绕过主链路做结构重构 |
| 装配标准 | starter、starter-remote、app 对 Bean、AutoConfiguration、profile、local/remote 选择的统一规则 | POM、自动配置、README 必须同步 |

### 5.8 通用准入与验收标准

#### `mango-common` 准入规则

允许进入 `common`：

- 通用返回结构、错误码接口、异常基类
- 分页查询和分页结果这类跨模块基础契约
- 不依赖 Spring Web、DB、Redis、Security、platform 的最小注解或校验契约
- Java 级别的轻量公共类型，且被 2 个及以上业务/基础设施模块稳定使用

禁止进入 `common`：

- `SysUser`、角色、权限、菜单、组织、消息、租户等业务模型
- mapper、repository、service、controller、starter 自动配置
- Web、DB、Redis、Security、Feign、SSE、WebSocket 等技术实现
- 为单个模块临时复用而上移的 DTO/VO/PO

#### “真正重构”判定

一个 Phase 不能只移动类。至少满足以下两项，才算有效重构：

- 删除或阻断一类非法依赖
- 收敛一个稳定 API/provider/checker
- POM 依赖方向更清晰
- starter 装配规则更可预测
- README/计划文档与源码事实一致
- 后续模块接入方式变简单

最终判定：

- 执行 agent 在交付记录中自评。
- review agent 按上述标准复核。
- 分歧由项目负责人决定，并写入交付记录。

---

## 6. 按模块执行顺序

### 执行总规则

本计划必须按 Phase 顺序执行。除非用户明确调整顺序，否则任何 agent 不得跳过前置 Phase。

### Phase、Task、Module 的关系

- `Phase` 是一个可验收的交付阶段，不一定等于一个 Maven 模块。
- 一个 `Phase` 通常有一个主模块，也可能包含一组强相关模块。
- 一个 `Task` 是 Phase 内的具体工作项，Task 才是实际拆给 agent 的执行单位。
- 一个 `Module` 是 Maven 模块或业务能力模块。

示例：

- `Phase 1: mango-common` 是单主模块 Phase。
- `Phase 3: mango-infra-realtime` 是新能力域 Phase，涉及 `mango-infra-sse`、`mango-infra-websocket` 和新模块 `mango-infra-realtime`。
- `Phase 4: mango-infra-security + mango-infra-web` 是双主模块 Phase，因为二者共同处理 Web/Security 横切边界。
- `Phase 10` 是模块组标准化 Phase，内部必须拆成多个独立 Task 执行。

### 串行与并行规则

默认按 Phase 串行推进。只有本节明确允许的 Task 才能并行。

#### 必须串行的阶段

```text
Phase -1 -> Phase 0 -> Phase 1 -> Phase 2 -> Phase 3 -> Phase 4 -> Phase 5 -> Phase 6 -> Phase 7 -> Phase 8 -> Phase 9 -> Phase 10
```

原因：

- `common` 是所有层的公共契约，必须先稳定
- `infra` 是 platform 的技术底座，必须先稳定
- `rbac/system/auth` 存在真实调用关系，必须按主链路收敛
- `admin-app` 是最终装配层，必须最后做

#### 可以并行的工作

| 位置 | 可并行 Task | 写入边界 | 合并要求 |
|------|-------------|----------|----------|
| Phase 0 | 文档搜索盘点、POM/源码残留搜索 | 只读或只改文档 | 汇总后统一提交 |
| Phase 3 | `sse` 盘点、`websocket` 盘点、下游依赖搜索 | 只读或各自旧模块 | 新 `messaging-api` 设计必须先串行确认 |
| Phase 4 | `infra-web` 盘点、`infra-security` 盘点 | 各自模块目录 | 适配接口必须统一后再改代码 |
| Phase 10 | `org`、`captcha`、`area`、`i18n`、`message`、`ai` 标准化盘点 | 各自平台模块目录 | 不允许跨模块互改；Phase 10 按需触发，不强制固定顺序 |

#### 禁止并行的工作

- 禁止同时修改同一个 POM 聚合文件
- 禁止多个 agent 同时修改同一模块的 `api`
- 禁止在 API 未确认前并行迁移实现
- 禁止在 Phase 3 中一边设计 `messaging-api`，一边迁移 SSE/WebSocket 实现
- 禁止跨 Phase 并行，例如 Phase 2 未完成时启动 Phase 6

### 每个 Phase 的固定步骤

每个 Phase 都按以下顺序执行：

1. **盘点**：列出文件、类、POM 依赖、配置项、下游引用。
2. **判定**：明确保留、迁出、删除、短期兼容四类对象。
3. **设计**：写清目标包结构、依赖方向、自动配置策略、对外 API。
4. **改动**：按先 API/POM、再 core、再 starter、再下游适配的顺序改。
5. **README 完善**：每处理一个主模块，必须同步完善该模块 README，写清职责边界、对外接口/API、POM 依赖、配置项、自动配置/装配规则、使用示例、禁止事项、验证命令和本 Phase 结论。
6. **文档**：同步计划文档、索引文档、交付记录；README 必须在交付记录中作为完成项登记。
7. **验证**：执行本 Phase 指定命令。
8. **收口**：记录完成项、不做项、遗留项、下一 Phase 前置条件。

如果任何一步无法完成，必须停止并记录阻塞原因，不允许跳到下一 Phase。

每个 Phase 的固定交付方式：

1. 先只处理该 Phase 明确列出的模块和文件范围
2. 不主动重构其它模块，只做编译所需的被动适配
3. 每个 Phase 结束必须执行验证命令
4. 每个主模块处理后必须完善对应模块 README；没有 README 的模块必须新增，除非该 Phase 明确说明不产出模块文档
5. 每个 Phase 结束必须在交付记录中写清楚完成项、不做项、遗留项

通用验证命令：

```bash
cd mango
mvn -q -DskipTests compile
```

如果该 Phase 涉及测试或行为改动，再追加：

```bash
cd mango
mvn test
```

### 验证体系

每个 Phase 的“验证通过”不能只理解为“能编译”。必须同时满足自动验证和人工验收。

#### 自动验证

| 检查项 | 何时执行 | 是否阻断 |
|--------|----------|----------|
| `mvn -q -DskipTests compile` | 每个 Phase 必跑 | 阻断 |
| `mvn test` | 修改行为、公共 API、starter 装配、协议适配时必跑 | 阻断 |
| `mvn verify` | 有集成测试、插件校验或发布前检查时执行 | 阻断 |
| `mango:check` | 规则稳定且当前仓库可运行时执行 | 稳定规则阻断，试验规则只记录 warning |
| `rg` 依赖/旧命名搜索 | 每个 Phase 按本节命令执行 | 命中禁止项则阻断 |

`mango:check` 当前不能作为唯一验收依据。若规则还在建设中，交付记录必须写清：

- 已运行哪些规则
- 哪些规则是 blocking
- 哪些规则只是 warning
- 未运行原因

warning 处理策略：

- blocking 规则失败：Phase 不通过。
- warning 规则命中：Phase 可以继续，但交付记录必须逐条登记。
- 每条 warning 必须给出处理方式：立即修复 / 标记已知 / 转成 backlog / 升级为 blocking。
- 同一条 warning 连续 2 个 Phase 未处理，必须在下一次计划评审中决定关闭或升级，禁止无限堆积。
- 不允许用“只是 warning”作为忽略依据。

#### 人工验收

每个 Phase 完成前必须人工确认：

- 依赖方向没有倒挂：`app -> platform -> infra/common`
- `infra` 没有依赖 `platform`
- `common` 没有业务实体、业务枚举、技术实现
- POM、README、计划文档、源码事实一致
- 被删除或合并的模块没有残留依赖
- 下游被动适配范围已记录

人工验收负责人：

- 默认由发起本轮 Phase 的项目负责人验收。
- 如果用户把某个 Phase 分配给其它 agent，该 agent 只能提交交付记录，不能自行宣布最终通过。
- reviewer 发现“只有类移动、边界未收敛”时，Phase 不通过，必须回到当前 Phase 修正，不挂到后续 Phase。

#### 跨 Phase 回归点

以下节点必须做更强回归：

| 节点 | 原因 | 要求 |
|------|------|------|
| Phase 1 完成后 | `common` 是全局公共契约 | 全量 compile + 旧命名/业务模型搜索 |
| Phase 3 完成后 | messaging 改动协议适配和下游依赖 | 全量 compile + 相关测试 + SSE/WebSocket 旧依赖搜索 |
| Phase 5 完成后 | infra 主链路基本稳定 | 全量 compile + 网关/安全/web 依赖方向搜索 |
| Phase 9 完成后 | app 装配收口完成 | 全量 compile/test，必要时启动 admin-app 验证 |

### 跨 Phase 接口冻结规则

为避免前置 Phase 的结论被后续 Phase 推翻，所有跨 Phase 接口必须按以下规则处理。

| 接口类型 | 冻结时间点 | 允许修改方式 |
|----------|------------|--------------|
| `common` 公共契约 | Phase 1 验收后 | 只能通过新增兼容接口或 ADR 修改 |
| infra provider/interface | 对应 infra Phase 验收后 | 下游只能被动适配，不允许直接绕过 |
| gateway 路径策略接口 | Phase 5 验收后 | Phase 6/8 可补 adapter，不可改变语义 |
| `rbac -> auth` 认证用户/权限桥接接口 | Phase 6 验收后 | Phase 8 只能清理内部实现，不改变接口语义 |
| `system` 子域 API | Phase 7 验收后 | 后续重定义必须走 ADR |

执行规则：

- 前置 Phase 可以定义“临时接口”，但必须标记为 provisional。
- provisional 接口最迟必须在它服务的主模块 Phase 内冻结或废弃。
- 后续 Phase 发现前置接口不合理时，不能直接改，必须先补充 ADR 或修改本计划。

### 退出条件

本轮重构不要求为了“做完 12 个 Phase”而继续投入。达到以下条件即可认为主线交付完成：

- Phase -1 到 Phase 9 全部完成
- `mango-common`、核心 infra、`rbac/system/auth/admin-app` 的边界已经收口
- `mango-infra-realtime` 已替代旧 `sse/websocket` 直接依赖
- 所有 blocking 验证通过
- POM、README、计划文档、源码事实一致
- 没有新增循环依赖或跨层倒挂
- Phase 10 未触发模块已记录为“按需标准化 backlog”

如果某个 Phase 卡住：

- 停止进入下一 Phase
- 在交付记录写清 blocker、影响模块、可选方案
- 只允许做不改变架构决策的 bugfix
- 需要变更 Phase 顺序时，先修改本计划再执行

如果业务需求恢复但只完成了部分 Phase：

- 已完成 Phase 作为新的稳定基线
- 未完成 Phase 转为 backlog
- 新业务不得假设未完成 Phase 已经落地
- 新业务改到相关模块时，优先补齐对应 Phase 的标准化任务

遗留问题归属：

- 当前 Phase 引入或发现、且属于当前 Phase 范围的问题，必须在当前 Phase 修复或记录 blocker。
- 当前 Phase 发现前置 Phase 遗漏，先判断是否阻塞当前 Phase；阻塞则回补前置 Phase，非阻塞则登记为前置 Phase follow-up。
- 当前 Phase 发现后续 Phase 问题，只能登记为后续 Phase 前置条件，不允许提前重构。

禁止事项：

- 禁止为了“顺手”修改后续 Phase 的模块
- 禁止把业务模型放进 `mango-common`
- 禁止让 `infra` 依赖 `platform`
- 禁止在 `app` 层补业务逻辑
- 禁止只改源码不改 POM / README / 文档
- 禁止用“模块太小”作为合并理由

## Phase -1：模块归属决策门

### 目标

在任何代码重构前，先固定会影响全局边界的关键决策，避免后续 Phase 因理解不一致返工。

### 必须决策

| 决策项 | 默认结论 | 影响范围 |
|--------|----------|----------|
| `SysUser` / 用户实体归属 | 本轮暂留 `mango-rbac`，定义为“授权侧用户事实”；不进入 `mango-common`；是否独立 `mango-identity` 进入后续 ADR | `common`、`rbac`、`auth`、`admin-app` |
| `mango-common` 准入边界 | 只放稳定跨模块公共契约，不放业务实体、不放技术实现 | 全仓 |
| `infra` 能力域边界 | 保留 web/security/kv/context/gateway/log/doc/feign；合并 sse+websocket 为 messaging；删除空壳 observability | infra |
| `platform` 合并策略 | 本轮不以合并/拆分为目标，只做领域边界收敛 | platform |

### 必须做

- 在本计划中记录上述默认结论
- 如果用户不同意默认结论，先修改本计划，再进入 Phase 0
- 任何 agent 执行后续 Phase 时，不得擅自改变这些决策

### 禁止做

- 禁止在 Phase 1 中把 `SysUser` 移入 `mango-common`
- 禁止未通过 ADR 直接新增 `mango-identity`
- 禁止以“以后可能复用”为理由扩大 `common`

### 验收

- 本节决策表已存在
- 后续 Phase 的任务描述不再与决策表冲突
- 若有不同意见，必须先形成 ADR 或修改本计划

### 产出

- 模块归属决策记录
- 如需偏离默认结论，输出 ADR

---

## Phase 0：事实源校准（前置，不算正式模块）

### 目标

消除当前仓库中“幽灵模块”与旧命名引用，建立唯一事实源。

Phase 0 是 Phase 1 的前置准备，不是独立目标。它的完成标准必须包含“为 Phase 1 扫清的障碍清单”。

### 范围

- 已完成：根 `pom.xml` 中 `mango-user-*` 依赖管理清理
- 已完成：`mango-admin-app/pom.xml` 中 `mango-user-starter` 依赖清理
- 已完成：`mango-admin-app` 启动装配代码中的 `UserAutoConfiguration` 和 `io.mango.user.core.mapper` 清理
- 已完成：`mango-rbac-api` 中无效 `mango-user-api` 依赖清理
- 待完成：当前有效 README 与架构文档中残留的 `mango-user` / `mango-permission` 叙述清理
- 不要求清理：历史 Sprint 计划中的旧命名，但必须按历史文档处理，不作为当前模块事实源

### 验收

- 当前源码与 POM 不再存在错误模块 artifactId 的直接依赖
- 当前有效 README、索引文档、架构文档与目录/POM 命名一致
- `admin-app` 装配叙事与实际实现一致
- 历史 Sprint 文档中的旧命名不参与当前事实源验收

### Phase 0 到 Phase 1 交接物

Phase 0 完成后，必须留下以下证据，才能进入 Phase 1：

| 交接物 | 要求 |
|--------|------|
| 有效模块清单 | infra/platform/app 模块数与聚合 POM 一致 |
| 幽灵模块搜索结果 | `mango-user-*`、`mango-permission`、`io.mango.user` 搜索结果已分类 |
| `mango-admin-app` 依赖清单 | 当前依赖全部标记为合法/错误/待迁移 |
| 当前事实源文档清单 | 哪些 README/架构文档已更新，哪些历史文档保留旧名 |
| Phase 1 障碍清单 | 明确不存在会影响 `mango-common` 收敛的旧命名/POM/装配问题 |
| 验证结果 | compile 结果、搜索命令、人工确认人或执行 agent |

交接判断：

- 自动证据：搜索命令和 compile 结果。
- 人工证据：执行 agent 在交付记录中逐项勾选上述交接物。
- 任一交接物缺失，Phase 1 不得开始。

### 执行清单

按以下顺序执行：

1. 从 POM 开始：检查根 `mango/pom.xml`、`mango/mango-infra/pom.xml`、`mango/mango-platform/pom.xml`、`mango/mango-app/pom.xml`。
2. 检查 `mango-admin-app/pom.xml`，更新 description 和依赖说明。
3. 检查 `mango-admin-app` 启动类与 mapper scan，确认无 `io.mango.user` 残留。
4. 搜索当前有效文档中的 `mango-user` / `mango-permission`。
5. 只更新当前事实源文档：根 README、模块 README、`mango-docs/index.md`、顶层架构文档。
6. 不批量改历史 Sprint 文档。
7. 记录 gateway 错误命名清单，作为 Phase 5 输入。
8. 更新后执行 `mvn -q -DskipTests compile`。

`admin-app` 装配叙事验证方式：

- POM 依赖清单与 README/description 中的模块名称一致。
- 启动类 import、`@Import`、`@MapperScan` 不包含旧 `user/permission` 命名。
- local starter 列表与当前 POM 依赖一致。
- Phase 0 不要求启动应用；如运行时配置被修改，才追加启动验证。

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P0-T1 | 搜索源码与 POM 中的 `mango-user` / `mango-permission` | 可并行 | 只读 |
| P0-T2 | 搜索当前有效文档中的旧命名 | 可并行 | 只读 |
| P0-T3 | 更新当前有效 README / 架构文档 | 不并行 | 文档 |
| P0-T4 | 更新 `mango-admin-app` 当前描述与依赖说明 | 不并行 | `mango-admin-app/pom.xml`、README |
| P0-T5 | 输出 gateway 错误命名清单 | 不并行 | 交付记录 |
| P0-T6 | 编译验证与搜索结果记录 | 不并行 | 交付记录 |

### 产出

- 一份事实源清理记录
- 搜索结果说明：哪些旧名已经清理，哪些属于历史文档保留
- Phase 1 障碍清单：哪些旧命名、POM、README、架构叙述已经不会阻碍 `mango-common` 收敛

---

## Phase 1：`mango-common`

### 目标

把 `mango-common` 收敛成真正的公共内核。

### 本阶段重点

- 只保留稳定、低耦合、跨模块公共契约
- 迁出技术型工具、历史兼容类、领域色彩明显的模型
- 建立 `common` 的准入规则

### 完成标志

- `common` 内没有明显业务语义
- 新模块可以稳定判断是否应依赖 `common`

### 必须做

- 盘点 `mango-common` 当前类清单
- 给每个类标记归属：保留 / 迁出 / 删除 / 待定
- 保留结果模型、异常、错误码、分页、基础注解等稳定契约
- 将技术实现类、业务语义类、历史兼容类迁出或删除
- 在 `mango-common/README.md` 写入准入规则
- 明确 `SysUser`、权限模型、组织模型、业务消息模型禁止进入 `common`

### 当前 `common` 现状盘点基线

Phase 1 开始前先以当前类清单为基线，逐个补充“被哪些模块依赖、是否保留、是否迁出”。

快速盘点命令：

```bash
cd mango
find mango-common/src/main/java -type f | sort
rg -n "io\\.mango\\.common" mango --glob '*.java' --glob '!mango-common/**'
```

当前 `common` 共有 11 个 Java 类。

| 当前类 | 当前依赖概况 | 初始判断 | Phase 1 必须补充 |
|--------|--------------|----------|------------------|
| `exception/BizException` | 约 3 个外部文件命中，涉及 infra web/security | 倾向保留 | 是否需要统一错误码 |
| `po/PageQuery` | 约 2 个外部文件命中，涉及 org/tools | 倾向保留 | 是否只承载分页契约 |
| `result/BizCode` | 约 3 个外部文件命中，涉及 org/tools | 倾向保留 | 和 `CommonCode` 的边界 |
| `result/CommonCode` | 当前未发现外部直接使用 | 待定 | 未使用则删除或保留为默认错误码基线 |
| `result/R` | 约 51 个外部文件命中，涉及 app/platform/infra/tools | 倾向保留 | 返回结构是否被所有 API 稳定使用 |
| `result/Require` | 约 2 个外部文件命中，涉及 org/tools | 待定 | 是否只是断言工具；若是技术实现，评估迁出 |
| `valid/IdCard` | 当前未发现外部直接使用 | 待定 | 身份证校验是否属于通用契约，还是业务/地区能力 |
| `valid/IdCardValidator` | 当前未发现外部直接使用 | 待定 | validator 实现是否应留在 common |
| `valid/Phone` | 约 3 个外部文件命中，涉及 rbac | 待定 | 手机号校验是否属于通用契约，还是业务/地区能力 |
| `valid/PhoneValidator` | 当前未发现外部直接使用 | 待定 | validator 实现是否应留在 common |
| `vo/PageResult` | 约 5 个外部文件命中，涉及 org/tools | 倾向保留 | 是否只承载分页结果契约 |

Phase 1 的第一份交付物必须是 `common` 类归属表，不允许直接开始移动代码。

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P1-T1 | 盘点 `mango-common` 类清单和被引用情况，输出每个类被多少模块依赖 | 可并行 | 只读 |
| P1-T2 | 形成类归属表：保留 / 迁出 / 删除 / 待定 | 不并行 | 文档 |
| P1-T3 | 执行 `common` 内部迁出/删除/保留调整 | 不并行 | `mango-common` 和必要下游适配 |
| P1-T4 | 更新 `mango-common/README.md` 准入规则 | 可与 P1-T3 并行，但需最终校对 | 文档 |
| P1-T5 | 编译验证与交付记录 | 不并行 | 交付记录 |

### 禁止做

- 禁止新增业务 DTO / VO 到 `common`
- 禁止把 Web、DB、Redis、Security 等技术实现放入 `common`
- 禁止为了减少依赖把 platform 模型上移到 `common`

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
```

### 产出

- `mango-common` 类归属表
- `mango-common` 准入规则
- 编译结果

---

## Phase 2：`mango-infra-kv`

### 目标

把 `mango-infra-kv` 从“功能很多的能力包”收敛成“装配规则稳定的底座”。

### 本阶段重点

- 收口配置命名与注释术语
- 拆分 `store` 选择与 `use case bean` 默认装配职责
- 统一 memory / redis / jdbc 三种实现的选择口径

术语定义：

- `store`：实际 KV 存储实现，例如 memory、redis、jdbc。
- `use case bean`：基于 KV store 组装出来的具体使用场景 Bean，例如 cache、lock、rate-limit、idempotent。
- `store 选择` 只回答“底层用哪个存储实现”。
- `use case bean 默认装配` 只回答“哪些业务场景能力默认启用、何时启用、如何被覆盖”。

### 完成标志

- 配置项清晰
- 自动配置不重复
- 业务模块更容易预测最终注入实现

### 必须做

- 盘点 `mango-infra-kv-api/core/starter` 的接口、实现、自动配置
- 统一配置前缀和术语，禁止继续混用 `dal`
- 明确 `memory` / `redis` / `jdbc` 三类实现的选择规则
- 拆分 `IKvStore` 选择逻辑和 cache/lock/rate-limit/idempotent 等 use case bean 装配逻辑
- 为自动配置增加 `@ConditionalOnMissingBean` / `@ConditionalOnProperty` 等明确条件

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P2-T1 | 盘点 `kv-api` 接口与注解 | 可并行 | 只读 |
| P2-T2 | 盘点 `kv-core` memory/redis/jdbc 实现 | 可并行 | 只读 |
| P2-T3 | 盘点 `kv-starter` 自动配置和配置项 | 可并行 | 只读 |
| P2-T4 | 设计统一配置前缀、实现选择规则、装配规则 | 不并行 | 文档 |
| P2-T5 | 修改 starter 自动配置 | 不并行 | `mango-infra-kv-starter` |
| P2-T6 | 修改 core/API 必要适配 | 不并行 | `mango-infra-kv-api/core` |
| P2-T7 | 编译验证与交付记录 | 不并行 | 交付记录 |

### 禁止做

- 禁止让业务模块直接依赖 `kv-core` 具体实现
- 禁止在业务代码里通过 if/else 选择部署形态
- 禁止默认强启所有能力

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
```

### 产出

- KV 配置说明
- 自动配置选择规则
- 编译结果

---

## Phase 3：`mango-infra-realtime`

### 目标

将 `mango-infra-sse` 与 `mango-infra-websocket` 收敛为客户端消息通信能力域。本阶段是本轮 infra 重构的必做项。

提前到 Phase 3 的原因：

- messaging 是 infra 能力域收口的一部分，应在 platform 重构前完成
- 后续 `mango-biz-notification`、`admin-app`、其它平台模块如果需要实时能力，应依赖统一 messaging 抽象，而不是继续依赖旧协议模块
- 越晚合并，越容易产生下游重复适配

### 本阶段重点

- 抽象统一的 server-client messaging 能力
- 支持 push / pull 两类交互模式
- 保留 SSE 与 WebSocket 作为协议实现，而不是对上层暴露为两个能力域
- 让平台模块依赖“消息通信能力”，而不是依赖具体协议
- 明确 `mango-infra-realtime` 与 `mango-biz-notification` 的边界

### 当前消费者清单

Phase 3 开始前以此清单作为迁移输入。

| 位置 | 当前依赖 | 迁移要求 |
|------|----------|----------|
| 根 `mango/pom.xml` | `mango-infra-sse`、`mango-infra-websocket` dependencyManagement | 删除旧 artifact，新增 `mango-infra-realtime-*` |
| `mango/mango-infra/pom.xml` | 聚合 `mango-infra-sse`、`mango-infra-websocket` | 删除旧 module，新增 `mango-infra-realtime` |
| `mango-biz-notification-core` | 直接依赖 `mango-infra-sse` / `mango-infra-websocket`，并 import `SseService` / `WebSocketHandler` | 改为依赖 `mango-infra-realtime-api` |
| `mango-biz-notification-starter` | import `WebSocketHandshakeInterceptor` | 改为 realtime starter 暴露的协议配置或 adapter |
| `mango-ai-core` | 直接使用 Spring `SseEmitter` | 评估是否迁入 messaging；如保留原生 SSE，必须说明它是 AI 流式响应例外，不属于旧 infra sse 模块依赖 |
| `mango-admin-app/pom.xml` | 直接依赖 `spring-boot-starter-websocket` | messaging 完成后复核是否由 `mango-infra-realtime-starter` 间接提供 |

当前未发现除旧 infra 模块自身外的 `WebSocketSession` 直接上层使用；发现了 `mango-biz-notification` 对旧 infra websocket handler 的直接依赖。

### 命名

采用 `mango-infra-realtime`。

原因：

- `messaging` 是行业常用词
- 能自然覆盖 SSE、WebSocket、push、pull、message dispatcher、subscription
- 借鉴 `Spring Messaging` 的能力域命名

### 和 `mango-biz-notification` 的边界

- `mango-infra-realtime` 是基础设施能力，只负责连接、会话、订阅、消息投递、SSE/WebSocket 协议适配。
- `mango-infra-realtime` 不定义业务消息模型，不负责收件人、通知状态、消息记录、站内信业务语义。
- `mango-biz-notification` 是平台业务消息/通知域，负责业务消息模型、消息存储、通知状态、业务投递规则。
- `mango-biz-notification` 可以依赖 `mango-infra-realtime` 完成实时投递，但不能把基础通信协议实现写回业务域。

### 执行顺序

1. 盘点 `mango-infra-sse` 与 `mango-infra-websocket` 的现有 API、配置项、自动配置和下游依赖。
2. 新建 `mango-infra-realtime` 聚合模块，按 `api/core/starter` 组织能力。
3. 在 `api` 中定义连接、订阅、push/pull、消息投递的最小抽象。
4. 将 SSE 与 WebSocket 迁入 `core` 的 protocol adapter，不再作为上层直接依赖入口。
5. 在 `starter` 中提供条件化自动配置，避免强行启用所有协议。
6. 替换下游对 `mango-infra-sse` / `mango-infra-websocket` 的直接依赖。
7. 删除旧模块入口，本轮不保留兼容 shim。

### 迁移路径

本阶段采用“先抽象、再适配、后替换”的迁移方式，不允许无计划地一次性删除旧入口。

1. 先冻结新 `mango-infra-realtime-api`，定义上层只允许依赖的最小接口。
2. 将 SSE/WebSocket 现有能力迁入 protocol adapter，保持行为等价。
3. 在 `mango-infra-realtime-starter` 中提供条件化自动配置。
4. 搜索所有下游对 `mango-infra-sse` / `mango-infra-websocket` / `spring-boot-starter-websocket` 的直接依赖。
5. 对下游逐个替换为 `mango-infra-realtime-api/starter`。
6. 删除旧 `mango-infra-sse` / `mango-infra-websocket` 模块目录、聚合 POM module、root dependencyManagement。
7. 搜索确认旧 artifact、旧包名、旧自动配置不再存在有效源码引用。

迁移策略：

- 默认不双写。
- 不保留旧 API shim。
- 如果存在运行时协议兼容风险，必须在新 messaging API 内提供 adapter 或 feature flag，不允许保留旧 Maven 模块。
- 任何上层模块不得在迁移后继续新增对旧 `sse/websocket` 模块的依赖。

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P3-T1 | 盘点 `mango-infra-sse` API/配置/自动配置 | 可并行 | 只读 |
| P3-T2 | 盘点 `mango-infra-websocket` API/配置/自动配置 | 可并行 | 只读 |
| P3-T3 | 搜索下游对 SSE/WebSocket 的依赖 | 可并行 | 只读 |
| P3-T4 | 设计 `mango-infra-realtime-api` 最小 API | 不并行 | 新 API/文档 |
| P3-T5 | 新建 `mango-infra-realtime` POM 与模块结构 | 不并行 | `mango-infra-realtime`、父 POM |
| P3-T6 | 迁移 SSE protocol adapter | 可与 P3-T7 并行，前提是 P3-T4/P3-T5 已完成 | `mango-infra-realtime-core.sse` |
| P3-T7 | 迁移 WebSocket protocol adapter | 可与 P3-T6 并行，前提是 P3-T4/P3-T5 已完成 | `mango-infra-realtime-core.websocket` |
| P3-T8 | 实现 starter 条件化自动配置 | 不并行 | `mango-infra-realtime-starter` |
| P3-T9 | 替换下游依赖并删除旧模块 | 不并行 | 父 POM、下游 POM、旧模块 |
| P3-T10 | 编译验证和搜索确认 | 不并行 | 交付记录 |

### 目标目录结构

```text
mango-infra-realtime/
├── pom.xml
├── mango-infra-realtime-api/
├── mango-infra-realtime-core/
└── mango-infra-realtime-starter/
```

包结构：

```text
io.mango.infra.realtime.api
io.mango.infra.realtime.core.sse
io.mango.infra.realtime.core.websocket
io.mango.infra.realtime.core.session
io.mango.infra.realtime.core.dispatcher
io.mango.infra.realtime.starter
```

最小 API：

- `RealtimePublisher`
- `RealtimePullService`
- `RealtimeSession`
- `SubscriptionManager`
- `RealtimeMessage`

### 禁止做

- 禁止把 Feign 并入 `mango-infra-realtime`
- 禁止把 `mango-biz-notification` 的业务实体迁入 `mango-infra-realtime`
- 禁止让上层继续直接依赖 `mango-infra-sse` / `mango-infra-websocket`
- 禁止一次性引入外部消息中间件，除非当前代码已有依赖
- 禁止保留旧 `mango-infra-sse` / `mango-infra-websocket` 兼容空壳

### 完成标志

- 上层业务不再依赖旧 `mango-infra-sse` / `mango-infra-websocket` 模块
- 业务消息/通知实时投递依赖 `messaging` 抽象
- `sse` / `websocket` 作为内部 protocol adapter 存在
- 旧 `mango-infra-sse` / `mango-infra-websocket` Maven 模块已删除
- 不再把 Feign、SSE、WebSocket 混为一个“通信大包”
- `mango-biz-notification` 与 `mango-infra-realtime` 的业务/基础设施边界清晰

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
rg -n "mango-infra-sse|mango-infra-websocket|io\\.mango\\.infra\\.sse|io\\.mango\\.infra\\.websocket" mango --glob '!**/target/**'
```

### 产出

- 新 `mango-infra-realtime` 模块
- SSE/WebSocket 迁移记录
- 旧模块删除记录

---

## Phase 4：`mango-infra-security` + `mango-infra-web`

### 目标

补做一次横切基础设施收口，确保 infra 不再反向感知平台业务。

### 本阶段重点

- 收口注解、上下文、请求工具、权限切面边界
- 避免业务 DTO/VO 再进入 infra
- 保证 Web / Security 仅依赖技术契约
- 冻结统一请求上下文 / 安全上下文契约，供 realtime、notification、audit、业务模块等统一消费

### 完成标志

- `infra` 不再依赖平台业务模型
- 横切能力可被平台层稳定复用
- 请求身份、租户、trace/request 元信息的读取方式统一，不再由各模块各自解析 header/token

### 必须做

- 搜索 `mango-infra-web` / `mango-infra-security` 是否依赖 `mango-platform` 下任何模块
- 将业务路径、权限、用户等领域事实抽象为 infra 接口或 provider
- Web 层只保留 HTTP、异常处理、序列化、请求上下文等基础能力
- Security 层只保留安全注解、切面、token/security 基础能力
- 设计并冻结统一请求上下文 / 安全上下文契约，至少覆盖用户 ID、租户 ID、认证状态、请求 ID、trace ID、client IP 的来源和读取方式
- 明确 `mango-infra-web`、`mango-infra-security`、`mango-infra-context` 三者分工：web 负责 HTTP 请求上下文，security 负责认证/token/权限技术契约，context 负责跨线程/跨调用上下文传播
- 为需要消费当前身份的模块提供稳定 provider/interface，例如 `CurrentUserProvider`、`RequestContextProvider`、`SecurityContextProvider` 或等价命名；具体命名以 P4-T3 设计结论为准
- 明确 realtime 等协议模块只能消费统一上下文契约或被动适配 resolver，不允许自行实现 token 解析、权限校验或业务租户规则

### 业务 DTO/VO 判定与验证

业务 DTO/VO 指满足任一条件的类：

- 包名属于 `io.mango.*.rbac`、`io.mango.*.auth`、`io.mango.*.system`、`io.mango.*.org`、`io.mango.*.message` 等平台域。
- 类名表达业务事实，例如 `SysUser`、`Role`、`Menu`、`Tenant`、`Message`、`Org`。
- 字段和方法围绕平台业务规则，而不是 HTTP、安全、序列化、请求上下文等技术契约。

技术契约 VO 可以留在 infra，但必须满足：

- 只表达技术协议或横切能力。
- 不包含平台业务事实。
- 不依赖 platform 包。

技术契约的精确定义：

- 允许依赖：JDK、Spring 基础技术包、`mango-common`、同一 infra 能力域 API。
- 禁止依赖：`mango-platform` 任意模块、platform 业务实体、platform core/starter 实现。
- 允许表达：HTTP 请求上下文、认证 token 抽象、权限检查接口、序列化/异常/安全切面技术配置。
- 禁止表达：用户资料、角色菜单、组织岗位、业务消息、租户业务规则。

验证方式：

- 自动搜索 platform 包引用。
- 自动搜索高风险业务命名。
- 人工复核命中的 DTO/VO 是否属于业务事实。

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P4-T1 | 盘点 `infra-web` 依赖与业务感知点 | 可并行 | 只读 |
| P4-T2 | 盘点 `infra-security` 依赖与业务感知点 | 可并行 | 只读 |
| P4-T3 | 统一 provider/interface 设计，冻结请求上下文 / 安全上下文契约 | 不并行 | 文档/API |
| P4-T4 | 修改 `infra-web` | 可与 P4-T5 并行，前提是 P4-T3 已完成 | `mango-infra-web` |
| P4-T5 | 修改 `infra-security` | 可与 P4-T4 并行，前提是 P4-T3 已完成 | `mango-infra-security` |
| P4-T6 | 下游被动适配；如 realtime 需要接入统一身份，只允许通过 resolver/provider 适配 | 不并行 | 受影响 infra/platform/app |
| P4-T7 | 编译和依赖方向验证 | 不并行 | 交付记录 |

### 禁止做

- 禁止 `infra` 依赖 `rbac-api`、`auth-core`、`system-core` 等平台模块
- 禁止在 `infra-web` 中硬编码平台业务路径
- 禁止在 `infra-security` 中实现 RBAC 业务规则
- 禁止在 `mango-infra-realtime`、`mango-infra-web` 或其它 infra 协议模块中各自解析 JWT、实现登录认证或执行业务权限规则
- 禁止把 `SysUser`、`Role`、`Tenant` 等平台业务模型作为统一上下文契约类型

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
rg -n "mango-platform|io\\.mango\\.rbac|io\\.mango\\.auth|io\\.mango\\.system" mango/mango-infra/mango-infra-web mango/mango-infra/mango-infra-security
rg -n "SysUser|Role|Menu|Tenant|Org|Message|.*DTO|.*VO" mango/mango-infra/mango-infra-web mango/mango-infra/mango-infra-security
```

### 产出

- infra 依赖方向检查结果
- 被动适配清单
- 统一请求上下文 / 安全上下文契约说明
- realtime 等下游模块的被动适配点清单

---

## Phase 5：`mango-gateway`

### 目标

让网关只依赖稳定认证/路径策略接口，而不是历史业务命名。

### 本阶段重点

- 清理默认 service name、旧命名配置
- 统一白名单与登录要求路径的来源
- 和 `rbac` / `auth` 的边界对齐
- 明确 gateway 对外部认证结果、可信 header 注入和 header 清洗的责任边界

说明：

- Phase 5 不等待 Phase 6/8 才开始，但只能对齐“路径策略抽象”，不能依赖 RBAC/Auth 的最终内部实现。
- Phase 5 产出的路径策略接口如果依赖后续 RBAC/Auth 补 adapter，必须标记为 provisional。
- Phase 6/8 只允许补齐 provider/adapter，不允许推翻 Phase 5 已冻结的网关语义。

### 当前错误命名清单

| 文件 | 当前命名 | 问题 | 处理要求 |
|------|----------|------|----------|
| `GatewayProperties` | `userUrl`、`userService = "mango-user-starter"` | 旧 user 服务事实已不存在 | 改为 RBAC/Auth/identity 决策后的路径策略来源，不再默认 user starter |
| `GatewayRemoteConfig` | route id `user-service`、`routes.getUserService()` | 旧 user service 命名 | 改为当前有效服务名或移入 Phase 6 adapter 待办 |
| `mango-gateway` 文档/配置 | `permission-starter`、`mango-permission` 如存在 | 旧 permission 命名 | 改为 `mango-rbac` |

默认 service name 指 gateway 在 local/remote 路由配置中内置的服务名，不是 Maven artifactId，也不是 Feign client name；但如果三者命名互相引用，必须在交付记录中一起说明。

### 完成标志

- 网关不再持有错误历史命名
- 路径策略来源明确且可替换
- gateway 与下游服务之间的可信身份/租户/header 传递约定明确

### 必须做

- 清理网关默认配置中的旧服务名，例如 `mango-user-starter`
- 明确匿名路径、登录必需路径、内部路径的来源接口
- 网关只依赖稳定路径策略接口，不直接感知 RBAC 存储模型
- 保留 local / remote 两种模式的配置入口
- 输出 Phase 6/8 需要补齐的 adapter 清单
- 明确 gateway 负责外部请求的认证结果治理、外部伪造 header 清洗、内部可信 header 注入；下游服务不得直接信任来自公网的身份/租户 header
- 冻结 gateway 注入给下游的可信 header 命名和语义，例如用户 ID、租户 ID、请求 ID、trace ID、认证类型；具体 header 名称以 P5-T2 设计结论为准
- 明确 WebSocket Upgrade、SSE、HTTP Polling 等长连接/长请求路径的 gateway 路由、超时、限流、CORS/Origin 策略归属；realtime 只消费 Phase 4 冻结的上下文契约

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P5-T1 | 搜索网关旧命名和业务耦合 | 可并行 | 只读 |
| P5-T2 | 设计路径策略来源接口，并冻结可信 header 注入/清洗规则 | 不并行 | 文档/API |
| P5-T3 | 修改 gateway 配置与默认值 | 不并行 | `mango-gateway` |
| P5-T4 | 适配路径 provider 来源 | 不并行 | `gateway` 与必要下游 |
| P5-T5 | 输出 Phase 6/8 adapter 待办清单 | 不并行 | 交付记录 |
| P5-T6 | 编译验证与旧命名搜索 | 不并行 | 交付记录 |

### 禁止做

- 禁止网关直接查询 RBAC 数据库表
- 禁止网关依赖 `rbac-core`
- 禁止在网关里写业务权限规则
- 禁止下游服务把公网传入的身份/租户 header 当作可信来源；必须由 gateway 清洗后重新注入或由统一 security/web 上下文提供
- 禁止在 gateway 中实现 RBAC 角色菜单、组织租户等业务规则；gateway 只消费稳定接口的策略结果

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
rg -n "mango-user|user-starter|permission-starter|mango-permission" mango/mango-infra/mango-gateway
```

### 产出

- 网关路径策略来源说明
- 可信 header 注入与清洗规则说明
- WebSocket/SSE/Polling 等长连接入口的 gateway 接入约定
- 旧命名搜索结果

---

## Phase 6：`mango-rbac`

### 目标

把 `mango-rbac` 从“用户权限大杂烩”收敛成“授权与访问控制中心”。

### 本阶段重点

- 明确 `SysUser` 是否继续留在 `rbac`
- 统一角色、菜单、按钮权限的事实源
- 收口 `public path` 与权限校验接口
- 消除用户事实与权限事实的混合暴露
- 减少 `auth` 与 `admin-app` 对 `rbac` 内部模型的直接感知

“直接感知内部模型”的判定：

- 依赖 `mango-rbac-core`：违规。
- import `io.mango.rbac.core.entity`、`mapper`、`service.impl`：违规。
- 在 auth/admin-app 中注入 RBAC core service、mapper、entity：违规。
- 通过 `mango-rbac-api` 的稳定接口、VO、provider/checker 获取权限或用户授权事实：允许。
- 通过 Phase 6 冻结的 `IAuthUserProvider`、`IPermissionChecker` 或等价桥接接口访问：允许。

### 完成标志

- 权限相关接口边界稳定
- 用户、角色、菜单、公共路径的职责关系可文档化说明
- `auth` 不再依赖 `rbac` 的实现细节

### 必须做

- 明确 `SysUser` 在本轮是否继续留在 RBAC；如果保留，文档写明它是“授权侧用户事实”
- 收口权限查询接口，避免 auth/admin-app 直接感知 RBAC 内部实体
- 整理 public path / login required path 的 provider 接口
- 保留角色、菜单、按钮权限、访问策略为 RBAC 核心职责
- 冻结 `rbac -> auth` 的认证用户/权限桥接契约，Phase 8 不再改变语义

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P6-T1 | 盘点 RBAC API/core/starter 暴露面 | 可并行 | 只读 |
| P6-T2 | 盘点 auth/gateway/admin-app 对 RBAC 的依赖 | 可并行 | 只读 |
| P6-T3 | 确认 RBAC 职责边界和 `SysUser` 处理口径 | 不并行 | 文档 |
| P6-T4 | 收口 RBAC API | 不并行 | `mango-rbac-api` |
| P6-T5 | 调整 RBAC core/starter 实现 | 不并行 | `mango-rbac-core/starter` |
| P6-T6 | 冻结 auth/gateway 使用的 provider/checker/path 策略契约 | 不并行 | `rbac-api`、必要 auth/gateway adapter |
| P6-T7 | 被动适配 auth/gateway/admin-app | 不并行 | 受影响模块 |
| P6-T8 | 编译验证和依赖搜索 | 不并行 | 交付记录 |

### 禁止做

- 禁止将组织域并入 RBAC
- 禁止把认证逻辑写入 RBAC
- 禁止 auth 依赖 `rbac-core`

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
rg -n "rbac-core" mango/mango-platform/mango-auth mango/mango-app
```

### 产出

- RBAC 职责边界说明
- RBAC 对 auth/gateway/app 的接口清单

---

## Phase 7：`mango-system`

### 目标

把 `mango-system` 从“系统杂项箱”收敛成可维护的系统域集合。

### 本阶段重点

- 识别强耦合子域：`dict` / `config` / `tenant` / `audit-log` / `route`
- 先做包级与 API 级收口，不急于一轮拆成多个 Maven 模块
- 让日志和路由不再继续膨胀为横向杂项能力

### 完成标志

- `system-api` 只暴露稳定系统能力
- `system-core` 中子域边界清楚
- 为后续拆分预留自然切口

### 必须做

- 在包结构中区分 `dict` / `config` / `tenant` / `audit` / `route`
- API 层只暴露稳定系统能力，不暴露内部 entity
- 操作日志只保留系统业务日志语义，不承担 infra 日志职责
- 输出每个子域的物理拆分触发条件和预留接口

### 物理拆分预备条件

Phase 7 不直接拆 Maven 模块，但包级收口必须为未来拆分留下切口。

子域满足以下任意两项时，进入后续物理拆分候选：

- 有独立对外 API，且被 2 个及以上上游模块依赖。
- 有独立数据表和生命周期。
- 有独立 starter 装配需求。
- 有独立权限、审计或租户边界。
- 频繁改动但和其它 system 子域共同变化很少。

Phase 7 包结构要求：

- 子域之间只能通过 API/service 接口调用，不直接访问彼此 entity/mapper。
- `system-api` 不能暴露 core entity。
- 每个子域必须能列出“如果未来拆模块，需要迁出的包、POM 依赖、starter 入口”。

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P7-T1 | 盘点 system 子域和 API 暴露面 | 可并行 | 只读 |
| P7-T2 | 设计 `dict/config/tenant/audit/route` 包边界 | 不并行 | 文档 |
| P7-T3 | 调整 API 层契约 | 不并行 | `mango-system-api` |
| P7-T4 | 调整 core 包结构和实现 | 不并行 | `mango-system-core` |
| P7-T5 | 调整 starter controller | 不并行 | `mango-system-starter` |
| P7-T6 | 输出子域物理拆分触发条件清单 | 不并行 | 交付记录 |
| P7-T7 | 编译验证和交付记录 | 不并行 | 交付记录 |

### 禁止做

- 禁止继续把新杂项能力塞进 `system`
- 禁止将 `message` 并入 `system`
- 禁止将基础设施 log/trace/metrics 放进 `system`

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
```

### 产出

- `system` 子域边界说明
- 后续是否物理拆分的候选清单
- 子域物理拆分触发条件清单

---

## Phase 8：`mango-auth`

### 目标

把 `mango-auth` 收敛成真正的认证域。

### 本阶段重点

- 保留登录、登出、refresh token、认证用户查询
- 将防重放、幂等、验证码拦截等治理能力迁回 infra 或独立策略组件
- 将权限查询完全改为消费稳定接口，而不是感知 `rbac` 细节

### 完成标志

- `auth-core` 只承载认证语义
- `auth-starter` 只承载认证装配
- Web 拦截治理逻辑不再在 auth 内继续增长

### 必须做

- 保留登录、登出、refresh token、token validate
- 认证用户信息通过稳定 provider 获取
- 权限能力只通过稳定 checker/provider 消费
- 识别防重放、幂等、验证码拦截的迁出位置
- 不改变 Phase 6 已冻结的认证用户/权限桥接接口语义，只清理 auth 内部实现和依赖

治理能力默认归属：

| 能力 | 默认归属 | Phase 8 处理方式 |
|------|----------|------------------|
| 防重放 | `mango-infra-security` 或基于 security 的策略组件 | 从 auth 迁出认证无关实现，auth 只消费策略 |
| 幂等 | `mango-infra-kv` 提供底层状态能力，具体 Web 拦截可在 infra web/security 策略组件 | auth 不持有通用幂等实现 |
| 验证码校验 | `mango-captcha` 是业务能力，auth 只调用 captcha API 或认证策略 | 禁止把 captcha core 实现并入 auth |
| Web MVC 拦截器治理 | `mango-infra-web` / `mango-infra-security` | auth starter 只装配认证入口 |

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P8-T1 | 盘点 auth API/core/starter 职责 | 可并行 | 只读 |
| P8-T2 | 盘点 auth 对 rbac/captcha/kv/security 的依赖 | 可并行 | 只读 |
| P8-T3 | 确认认证域保留能力和迁出能力 | 不并行 | 文档 |
| P8-T4 | 调整 auth API/core | 不并行 | `mango-auth-api/core` |
| P8-T5 | 调整 auth starter 装配 | 不并行 | `mango-auth-starter` |
| P8-T6 | 被动适配 rbac/admin-app | 不并行 | 受影响模块 |
| P8-T7 | 编译验证和依赖搜索 | 不并行 | 交付记录 |

### 禁止做

- 禁止 auth 直接依赖 `rbac-core`
- 禁止 auth 承载 RBAC 权限计算
- 禁止把验证码实现并入 auth

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
rg -n "rbac-core|captcha-core" mango/mango-platform/mango-auth
```

### 产出

- Auth 职责边界说明
- 被迁出治理能力清单

---

## Phase 9：`mango-admin-app`

### 目标

把 `mango-admin-app` 收敛成纯装配层。

### 本阶段重点

- 去掉错误历史依赖与本地特例装配
- 用能力注册 / local-vs-remote 机制统一 starter 选择
- 不在 app 层继续固化业务事实和兼容 Bean

Phase 9 与 Sprint 15 的关系：

- Phase 9 依赖 Sprint 15 的能力注册 / remote adapter 机制。
- 如果 Sprint 15 未完成，Phase 9 只能做 app 装配清单、错误依赖清理、local/remote 缺口记录，不能强行实现完整切换。
- 如果 Sprint 15 已完成，Phase 9 才执行统一 local-vs-remote starter 选择。
- Phase 9 开始前必须先确认 `./2026-04-17-sprint-15-capability-registry-remote-adapter.md` 的交付状态。

当前状态：

- `mango-admin-app` 当前 POM 使用本地 starter，例如 `mango-rbac-starter`、`mango-auth-starter`、`mango-org-starter`。
- 仓库中已存在部分 `starter-remote` 模块：`gateway`、`area`、`auth`、`i18n`、`org`、`rbac`。
- 并非所有平台模块都已经具备 remote starter。

因此 Phase 9 不默认要求一次性完成全量 remote 模式，只要求：

- 明确当前 local 模式装配清单。
- 列出现有 remote starter 覆盖范围。
- 标记缺失 remote starter 的模块。
- 给出 local / remote 切换规则和未完成项。

### 完成标志

- app 只负责部署、profile、starter 装配
- app 不再承载领域补丁
- 单体和远程两种模式切换方式一致

### 必须做

- 清理 app 中的本地兼容 Bean
- 明确 starter 装配清单
- 明确 local / remote 模式选择方式
- 更新 `mango-admin-app/README.md`
- 输出 remote starter 覆盖清单和缺口清单

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P9-T1 | 盘点 admin-app POM、启动类、配置、兼容 Bean | 可并行 | 只读 |
| P9-T2 | 确认 starter 装配清单 | 不并行 | 文档/POM |
| P9-T3 | 清理 app 中业务实现和本地补丁 | 不并行 | `mango-admin-app` |
| P9-T4 | 更新 local / remote 模式说明 | 可与 P9-T3 并行 | README |
| P9-T5 | 输出 remote starter 覆盖/缺口清单 | 不并行 | 交付记录 |
| P9-T6 | 编译验证和 app 层搜索 | 不并行 | 交付记录 |

### 禁止做

- 禁止新增业务 Service 到 app
- 禁止 app 直接访问 mapper
- 禁止 app 直接依赖 core 实现，除非当前 starter 机制无法满足且文档说明原因

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
rg -n "core\\.mapper|@Service|@Mapper" mango/mango-app/mango-admin-app/src/main/java
```

### 产出

- admin-app 装配说明
- local / remote 模式说明

---

## Phase 10：平台非主链路模块标准化

本阶段不设固定模块顺序，按需触发。

触发优先级从高到低：

1. 当前业务需求正在改动该模块
2. 该模块阻塞 `auth/rbac/system/admin-app` 主链路收口
3. 该模块存在错误依赖、循环依赖、跨层依赖
4. 该模块 API/core/starter 边界明显不一致
5. 该模块只是体量小、目录多、命名不对称

第 5 类不能单独作为重构理由。

候选模块：

- `mango-org`
- `mango-captcha`
- `mango-area`
- `mango-i18n`
- `mango-biz-notification`
- `mango-ai`

这些模块的目标不是大拆，而是统一到同一套模块标准：

- API 只放契约
- Core 只放领域行为
- Starter 只放装配与控制器
- 已存在的 remote starter 只放远程适配；缺失 remote starter 只记录是否需要，不默认补齐

说明：

- `mango-org` 是独立组织域，不是低价值长尾模块；放在本阶段是因为它不在 `rbac/system/auth` 主链路中。
- `mango-biz-notification` 是业务消息/通知域；它可以使用 `mango-infra-realtime` 投递实时消息，但不应与 infra messaging 合并。
- 本阶段的目标是标准化非主链路平台模块，不代表这些模块都要合并或降级。

### 必须做

- 先生成候选模块依赖图谱，再决定本次处理哪个模块
- 每个模块按 `api/core/starter/starter-remote` 检查一致性
- 对缺失 `starter-remote` 的模块，只记录缺口和必要性，不作为必须新增项
- API 只放契约，core 只放领域行为，starter 只放装配和控制器
- 逐个模块输出“不合并/观察/后续候选”的判断

依赖图谱生成命令：

```bash
cd mango
for m in mango-org mango-captcha mango-area mango-i18n mango-biz-notification mango-ai; do
  echo "## $m"
  rg -n "$m|io\\.mango\\.${m#mango-}" mango/mango-platform mango/mango-app mango/mango-infra --glob 'pom.xml' --glob '*.java'
done
```

说明：

- Phase 10 不再预设 `mango-org` 第一。
- 哪个模块先处理，必须由依赖图谱、当前业务需求、阻塞关系共同决定。

### Task 拆分

| Task | 内容 | 是否可并行 | 写入范围 |
|------|------|------------|----------|
| P10-T1 | 生成候选模块依赖图谱，并按触发优先级选择本次要处理的非主链路模块 | 不并行 | 交付记录 |
| P10-T2 | 标准化盘点 `mango-org` | 可并行，按需执行 | `mango-org` |
| P10-T3 | 标准化盘点 `mango-captcha` | 可并行，按需执行 | `mango-captcha` |
| P10-T4 | 标准化盘点 `mango-area` | 可并行，按需执行 | `mango-area` |
| P10-T5 | 标准化盘点 `mango-i18n` | 可并行，按需执行 | `mango-i18n` |
| P10-T6 | 标准化盘点 `mango-biz-notification` | 可并行，按需执行 | `mango-biz-notification` |
| P10-T7 | 标准化盘点 `mango-ai` | 可并行，按需执行 | `mango-ai` |
| P10-T8 | 汇总判断和统一文档 | 不并行 | 计划/交付记录 |

### 禁止做

- 禁止因代码少合并模块
- 禁止将 `org` 并入 `rbac`
- 禁止将 `message` 并入 `system`
- 禁止将 `captcha` 并入 `auth`

### 验收命令

```bash
cd mango
mvn -q -DskipTests compile
```

### 产出

- 非主链路平台模块标准化记录
- 每个模块的保留/观察/候选调整判断

---

## 7. 模块职责与最终合并方案

### 7.1 Infra 最终方案

本轮 infra 不以减少模块数量为目标，只做有明确价值的能力域收敛。

#### 保留独立

| 模块 | 定位 | 保留理由 | 借鉴 |
|------|------|----------|------|
| `mango-infra-web` | HTTP/Web MVC 基础设施 | Web 入口能力稳定，独立依赖价值高 | Spring WebMVC / Spring Boot Web |
| `mango-infra-security` | 安全基础设施 | 技术安全能力不应并入业务权限域 | Spring Security |
| `mango-infra-kv` | KV 与横切状态能力 | cache/lock/rate-limit/idempotent 属于稳定技术能力簇 | Spring Cache / Redisson / Bucket4j |
| `mango-infra-context` | 上下文传播 | 小但关键，服务 trace/tenant/request context 传播 | Context Propagation / TTL |
| `mango-gateway` | 边缘接入层 | 网关是独立边界层，不应并入普通 infra | Spring Cloud Gateway |
| `mango-infra-log` | 日志基础设施 | 当前有实际内容，先保持独立 | Logback / Spring Boot Logging |
| `mango-infra-doc` | API 文档基础设施 | OpenAPI 文档不属于日志/观测/运行诊断 | Springdoc OpenAPI |
| `mango-infra-feign` | 服务间同步调用 | Feign 是 service-to-service RPC，不属于客户端消息通信 | Spring Cloud OpenFeign |
| `mango-infra-db` | 数据库基础设施 | DB 连接、MyBatis 等数据访问底座不并入 KV | Spring JDBC / MyBatis |
| `mango-infra-orm` | ORM/Repository 抽象 | ORM 是持久化访问抽象，不等同于 Redis/KV | Repository pattern / MyBatis-Plus |
| `mango-infra-redis` | Redis 基础设施 | Redis 客户端能力可被 KV 使用，但不等同于 KV use case | Spring Data Redis / Redisson |
| `mango-infra-crypto` | 密码学基础能力 | 加密、签名、哈希是独立技术能力 | JCA / Bouncy Castle |
| `mango-infra-module` | 模块元数据与部署映射 | 支撑能力注册和装配发现，不并入业务模块 | Spring Boot auto-configuration metadata |
| `mango-infra-test` | 基础设施测试支撑 | 测试工具独立于运行时代码 | Spring Boot Test |

借鉴说明：

- “借鉴”不是照搬版本、配置或完整实现。
- 本计划只借鉴能力边界、命名、装配方式和抽象形态。
- 具体版本以当前项目 POM/BOM 为准，例如当前 Spring Boot 为 3.2.3，Spring Cloud 为 2023.0.0，Spring Security 随 Spring Boot 依赖管理进入 6.x 系列。
- 如果要引入新依赖或升级版本，必须在对应 Phase 单独决策。
- 例如 `mango-infra-kv` 借鉴 `Spring Cache` 的 cache 抽象、`Redisson` 的分布式锁/限流实现形态、`Bucket4j` 的 rate-limit 概念，不表示三者都必须被完整引入。

#### 本轮采纳的合并

| 目标模块 | 来源模块 | 定位 | 合并理由 | 借鉴 |
|----------|----------|------|----------|------|
| `mango-infra-realtime` | `mango-infra-sse` + `mango-infra-websocket` | server-client messaging，支持 push/pull、连接、订阅、消息分发 | SSE/WebSocket 都服务服务端与客户端消息通信；上层应依赖 messaging 抽象，而不是协议实现；合并后能统一连接管理、消息分发、订阅模型 | Spring Messaging |

说明：

- 本合并不是因为二者“代码少”或“名字相似”。
- 决策依据是共同上游、共同协议抽象、共同连接/会话/订阅/投递模型。
- 旧 `mango-infra-sse` / `mango-infra-websocket` 模块本轮删除，SSE/WebSocket 只作为 `mango-infra-realtime-core` 内部 protocol adapter。

#### 本轮明确不合并

| 模块组合 | 结论 | 原因 |
|----------|------|------|
| `feign` + `sse` + `websocket` | 不合并 | Feign 是服务间 RPC，SSE/WebSocket 是服务到客户端通信，变化原因不同 |
| `log` + `observability` | 不合并 | `observability` 当前只有少量占位配置，已删除；`log` 有实际内容，先独立保留 |
| `doc` + `log` | 不合并 | API 文档服务开发接入，日志服务运行排障，不是同一能力域 |
| `db` + `redis` | 不合并 | 二者都是存储相关，但数据模型和变化原因不同；后续应分别评估归入 `kv` / `orm` 的可能性 |

#### 已执行清理

- `mango-infra-observability` 当前缺少实际能力，已从源码和构建入口删除。
- 后续如果真正接入 Micrometer Observation / Tracing / Metrics，再重新规划观测能力模块；在此之前不保留空壳。

### 7.2 Platform 最终方案

本轮 platform 不以合并或拆分为目标，而以领域边界收敛为目标。

边界收敛完成标准：

- API 不暴露 core entity/mapper/service impl。
- core 不依赖 starter/app。
- starter 只做装配、controller、adapter，不持有长期领域逻辑。
- 子域之间通过 API/service 契约调用，不直接访问彼此 mapper/entity。
- POM 依赖方向符合 `app -> platform -> infra/common`。

#### 保留独立

| 模块 | 定位 | 保留理由 | 借鉴 |
|------|------|----------|------|
| `mango-auth` | 认证域 | 登录、登出、token 生命周期应保持纯粹，不继续承载安全杂项 | Spring Security / Spring Authorization Server / Keycloak |
| `mango-rbac` | 授权与访问控制域 | 角色、权限、菜单、访问策略是独立授权域，不应和组织域或认证域混合 | Casbin / Keycloak Authorization |
| `mango-system` | 系统通用能力集合 | 本轮先做内部子域化，不急物理拆分 | DDD Bounded Context / 中后台系统实践 |
| `mango-org` | 组织域 | 组织、部门、岗位是企业组织事实，不应并入 RBAC | IAM / HR domain |
| `mango-captcha` | 验证码与人机校验 | 验证码不只服务登录，不应并入 auth | reCAPTCHA 类能力 |
| `mango-biz-notification` | 业务消息/通知域 | 不应并入 system，避免 system 继续膨胀 | Notification Center |
| `mango-ai` | AI 能力域 | AI 是增长域，不应因当前体量小而被合并 | AI capability domain |
| `mango-area` | 地区事实域 | 地区、行政区划是事实数据，不等同于翻译文案 | Locale/Region data |
| `mango-i18n` | 国际化翻译域 | 语言、文案、翻译资源是独立本地化能力 | Spring MessageSource |

#### 暂不合并，只观察

| 模块组合 | 结论 | 原因 |
|----------|------|------|
| `mango-area` + `mango-i18n` | 暂不合并 | 二者都服务本地化体验，但 `area` 是地区事实，`i18n` 是语言翻译事实；当前证据不足，不因体量小而合并 |

#### Platform 本轮重点

- `rbac`：澄清用户事实、授权事实、菜单权限、公共路径的边界
- `system`：先按 `dict/config/tenant/audit-log/route` 做内部子域化
- `auth`：只保留认证职责，迁出防重放、幂等、验证码拦截等治理逻辑
- `admin-app`：只做装配，不承载领域补丁

---

## 8. 通用重构流程

### 8.1 通用流程

每个模块都按以下流程执行：

1. 盘点模块职责
2. 识别对外 API 与上游/下游
3. 判断哪些职责必须保留，哪些应迁出
4. 收敛包结构与 POM 依赖
5. 收敛 starter / starter-remote / app 装配方式
6. 修正文档、README、架构图、artifact 命名
7. 执行 `compile / test / verify / mango:check`
8. 输出交付记录与遗留问题

### 8.2 非通用流程

不同类型模块，切入点不同：

#### `common`

- 先做准入规则
- 再做迁出
- 最后做公共契约稳定化

#### `infra`

- 先做依赖方向校验
- 再做自动配置收口
- 最后做实现合并或拆分

#### `platform`

- 先做职责边界判定
- 再做 API / core / starter 收口
- 最后做上下游适配

#### `app`

- 先清理错误依赖
- 再清理临时 Bean 与兼容补丁
- 最后统一装配模式

---

## 9. 每个模块的标准执行模板

后续每个模块都按同一模板推进，避免每次重新发明流程。

### Step 1：模块盘点

- 当前职责
- 对外 API
- 上下游依赖
- 代码体量：只用于估算风险和拆分任务，不作为合并/删除依据
- 测试覆盖

### Step 2：边界判定

- 什么必须保留
- 什么应该迁出
- 什么只允许通过接口暴露

### Step 3：结构调整

- 包结构
- POM 依赖
- starter 装配：AutoConfiguration、条件注解、配置前缀、local/remote adapter、README 必须一致
- 文档与命名

### Step 4：适配与回归

- 被动适配下游模块
- 修正集成测试
- 执行 `compile / test / verify / mango:check`

### Step 5：交付记录

- 完成项
- 不做项
- 遗留问题
- 下一模块前置条件

### 交付记录模板

每个 Phase 完成后必须新增或更新交付记录，格式如下：

```md
# Phase X 交付记录

## 范围

- 本次处理模块：
- 本次未处理模块：

## 完成项

- [ ] 

## 主动不做项

- [ ] 事项：
  原因：

## 被动适配

- [ ] 

## 验证结果

- `mvn -q -DskipTests compile`：
- `mvn test`：
- 其它搜索/检查命令：

## 遗留问题

- [ ] 

## 下一 Phase 前置条件

- [ ] 
```

### Agent 执行检查表

开始前必须确认：

- [ ] 当前 Phase 编号正确
- [ ] 已阅读本 Phase 的“必须做 / 禁止做 / 验收命令 / 产出”
- [ ] 已确认不会修改后续 Phase 的主模块
- [ ] 已执行必要搜索，知道下游依赖在哪里

提交前必须确认：

- [ ] 代码、POM、README、计划文档同步
- [ ] 本 Phase 主模块 README 已完善，并覆盖职责边界、配置/装配、依赖、验证和禁止事项
- [ ] 验收命令已执行
- [ ] 搜索命令结果已记录
- [ ] 没有引入禁止事项
- [ ] 已写交付记录

---

## 10. 当前建议的最近三步

### 第一件事

先做 `Phase -1`：模块归属决策门。

必须先确认：

- `SysUser` 本轮暂留 `mango-rbac`
- `mango-common` 不接收业务实体
- `mango-infra-realtime` 是本轮必做项
- 空壳 `observability` 不保留

### 第二件事

做 `Phase 0`：事实源校准。

### 第三件事

正式进入 `Phase 1: mango-common` 重构。

`mango-common` 稳定后，按 `mango-infra-kv`、`mango-infra-realtime`、`mango-infra-security/web`、`mango-gateway` 的顺序完成 infra 层收口，再进入平台层。

---

## 11. 风险与控制

### 风险 1：边界没收紧，只有类移动

控制方式：

- 每个阶段都必须明确“保留职责 / 迁出职责 / 对外接口”

### 风险 2：app 层继续补洞

控制方式：

- `mango-admin-app` 只能做装配，不允许继续加领域兼容逻辑

### 风险 3：README、POM、源码再次分叉

控制方式：

- 每个模块交付必须包含文档和依赖清单同步更新

### 风险 4：重构顺序被长尾需求打断

控制方式：

- 长尾模块只允许做小修，不允许优先于主链路重构
- Phase 10 按需触发，不作为主链路交付阻断项
- 小修只包括 bugfix、文档修正、测试补充、配置修正；不包括新增 API、调整 POM 层级、改变模块边界、引入跨层依赖

### 风险 5：验证体系不稳定，Phase 通过标准失真

控制方式：

- `mango:check` 未稳定前不能作为唯一验收依据
- 每个 Phase 同时要求 compile/test/search/manual checklist
- blocking 与 warning 规则必须写入交付记录

---

## 12. 最终建议

当前项目目标不变的前提下，后端模块重构不要从 `auth`、`system` 或 `admin-app` 先开刀。

**正确顺序是：**

1. 先做 `Phase -1` 模块归属决策门
2. 再做 `Phase 0` 事实源校准
3. 第一个正式模块从 `mango-common` 开始
4. infra 层按 `kv -> messaging -> security/web -> gateway` 收口
5. platform 层内部按 `rbac -> system -> auth` 收口
6. 最后进入 app 层，收口 `mango-admin-app` 装配职责
7. Phase 10 按需触发，不要求固定长尾顺序
8. 不因体量小、目录多、名称不对称而合并模块

这样做更符合依赖层级，也更适合长期、模块化地逐层重构。
