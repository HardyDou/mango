# Mango 能力地图

本文只做能力索引，不复制规范正文。它介绍 Mango 提供哪些能力模块，以及每个模块负责什么；它不是任务清单、接入教程或发布日志。

正式交付规则以 preflight 输出和 `mango-pmo/rules/**` 为准。模块的完整 API、配置、权限、租户边界、失败处理和验收入口，以对应模块 README 为准。

## 1. 能力分层

| 层级 | 能力范围 | 作用 |
|---|---|---|
| 基础设施 | Context、Persistence、Event、KV、Web、Feign、Fileproc、Realtime、Sensitive、Module | 为上层模块提供上下文、数据、通信、文件处理和运行时基础。 |
| 平台模块 | Auth、Identity、Org、Authorization、Access、System、Resource、File、Workflow、Job、Notice 等 | 提供可被业务项目复用的后端业务能力。 |
| 前端能力 | Admin Shell、Admin Pages、Common、RBAC、File、Workflow、Job 等 | 提供管理端运行壳、页面注册、公共组件和领域页面。 |
| 业务项目能力 | Business Starter、Mango CLI、Business PMO | 创建业务项目、初始化环境、接入能力和执行交付流程。 |
| 文档与治理 | PMO rules、templates、designs、plans、evidence | 约束研发过程，记录设计决定和可复核交付事实。 |

## 2. 基础设施能力

| 模块 | 能做什么 | 不负责什么 | 说明入口 |
|---|---|---|---|
| Context | 保存当前请求、用户、租户、应用和登录域上下文。 | 不负责登录、授权或业务数据。 | [Context README](../../mango/mango-infra/mango-infra-context/README.md) |
| Persistence | 提供数据源、租户隔离、Flyway、Mapper 和持久化基础。 | 不负责业务实体和业务规则。 | [Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) |
| Event | 发布和消费领域事件，支持本地总线和 Outbox 传递。 | 不决定业务状态机和订阅方幂等语义。 | [Event README](../../mango/mango-infra/mango-infra-event/README.md) |
| KV | 提供 Memory、Redis、JDBC 等键值、锁和临时状态存储。 | 不替代业务数据库。 | [KV README](../../mango/mango-infra/mango-infra-kv/README.md) |
| Web | 提供 HTTP、请求上下文、错误响应和 Web 安全基础。 | 不定义具体业务 API。 | [Web README](../../mango/mango-infra/mango-infra-web/README.md) |
| Feign | 提供服务间 HTTP 调用和远程 API 适配。 | 不负责业务重试策略和数据权限。 | [Feign README](../../mango/mango-infra/mango-infra-feign/README.md) |
| Fileproc | 提供 Office、PDF、图片和文档转换处理。 | 不保存文件记录或决定文件权限。 | [Fileproc README](../../mango/mango-infra/mango-infra-fileproc/README.md) |
| Realtime | 提供 WebSocket、SSE 和跨实例实时消息传递。 | 不负责消息业务模板和接收人规则。 | [Realtime README](../../mango/mango-infra/mango-infra-realtime/README.md) |
| Sensitive | 提供敏感字段处理和安全边界。 | 不拥有业务密钥或租户权限模型。 | [Sensitive README](../../mango/mango-infra/mango-infra-sensitive/README.md) |
| Module | 提供模块元数据、装配和运行态诊断。 | 不替代模块自身健康检查。 | [Module README](../../mango/mango-infra/mango-infra-module/README.md) |

## 3. 平台后端能力

### 3.1 身份、访问和授权

| 模块 | 能做什么 | 说明入口 |
|---|---|---|
| Auth | 提供登录、认证 Provider、登录回调和登录态接入。 | [Auth README](../../mango/mango-platform/mango-auth/README.md) |
| Identity | 管理用户、租户成员、身份标识和成员生命周期。 | [Identity README](../../mango/mango-platform/mango-identity/README.md) |
| Org | 管理组织、部门、岗位、组织树和成员组织关系。 | [Org README](../../mango/mango-platform/mango-org/README.md) |
| Authorization | 管理菜单、API 资源、角色、角色数据范围和授权关系。 | [Authorization README](../../mango/mango-platform/mango-authorization/README.md) |
| Access | 提供权限检查、访问拦截和统一访问控制基础。 | [Access README](../../mango/mango-platform/mango-access/README.md) |
| Captcha | 提供验证码生成、校验和认证流程配套能力。 | [Captcha README](../../mango/mango-platform/mango-captcha/README.md) |

### 3.2 系统和资源

| 模块 | 能做什么 | 说明入口 |
|---|---|---|
| System | 管理系统字典、系统参数、国际化消息和系统基础数据。 | [System README](../../mango/mango-platform/mango-system/README.md) |
| Resource Registry | 收集资源声明，按 Handler 同步菜单、字典、流程定义、文件配置等初始化资源。 | [Resource README](../../mango/mango-platform/mango-resource/README.md) |
| Domain | 管理业务域及其基础配置。 | [Domain README](../../mango/mango-platform/mango-domain/README.md) |
| Calendar | 管理日历、工作日和时间规则。 | [Calendar README](../../mango/mango-platform/mango-calendar/README.md) |
| Template | 管理打印模板、模板渲染和模板资源。 | [Template README](../../mango/mango-platform/mango-template/README.md) |

### 3.3 文件、流程和作业

| 模块 | 能做什么 | 说明入口 |
|---|---|---|
| File | 管理文件记录、存储配置、上传、下载、访问和归档。 | [File README](../../mango/mango-platform/mango-file/README.md) |
| File Preview | 管理文档预览入口、转换状态和预览产物。 | [File Preview README](../../mango/mango-platform/mango-file-preview/README.md) |
| Workflow | 管理流程定义、业务申请、审批任务、办理人和审批进度。 | [Workflow README](../../mango/mango-platform/mango-workflow/README.md) |
| Job | 管理定时任务、任务执行、调度状态和执行记录。 | [Job README](../../mango/mango-platform/mango-job/README.md) |
| Notice | 管理站内消息、公告、通知渠道、消息模板和投递记录。 | [Notice README](../../mango/mango-platform/mango-notice/README.md) |
| Numgen | 管理编号规则和业务编号生成。 | [Numgen README](../../mango/mango-platform/mango-numgen/README.md) |

### 3.4 业务扩展模块

| 模块 | 能做什么 | 说明入口 |
|---|---|---|
| CMS | 提供内容、栏目和内容运营管理能力。 | [CMS README](../../mango/mango-platform/mango-cms/README.md) |
| Home | 提供用户首页、首页模板和工作台能力。 | [Home README](../../mango/mango-platform/mango-home/README.md) |
| Link | 提供网址导航、链接分类和个人收藏。 | [Link README](../../mango/mango-platform/mango-link/README.md) |
| Payment | 提供支付订单、渠道、签名和回调处理。 | [Payment README](../../mango/mango-platform/mango-payment/README.md) |
| Grid Layout | 提供可配置的页面栅格布局能力。 | [Grid Layout README](../../mango/mango-platform/mango-grid-layout/README.md) |

## 4. 前端能力

| 包 | 能做什么 | 说明入口 |
|---|---|---|
| Admin Shell | 提供管理端运行壳、菜单装配、登录后初始化和页面承载。 | [Admin Shell README](../../mango-ui/packages/admin-shell/README.md) |
| Admin Pages | 提供管理页面注册表和页面插件接入。 | [Admin Pages README](../../mango-ui/packages/admin-pages/README.md) |
| Common | 提供公共组件、主题变量、API 契约和前端工具。 | [Common README](../../mango-ui/packages/common/README.md) |
| RBAC | 提供菜单、角色、权限和数据范围管理页面。 | [RBAC README](../../mango-ui/packages/rbac/README.md) |
| `@mango/file` | 提供上传、附件列表、文件预览和文件管理页面。 | [File Frontend README](../../mango-ui/packages/file/README.md) |
| `@mango/workflow` | 提供流程管理页面、审批组件和 Workflow API。 | [Workflow Frontend README](../../mango-ui/packages/workflow/README.md) |
| `@mango/job` | 提供任务管理和调度页面。 | [Job Frontend README](../../mango-ui/packages/job/README.md) |
| `@mango/notice` | 提供消息中心、通知管理和通知展示组件。 | [Notice Frontend README](../../mango-ui/packages/notice/README.md) |
| Mango CLI | 提供项目创建、workspace、开发、PMO 和交付命令。 | [CLI README](../../mango-ui/packages/mango-cli/README.md) |

## 5. 业务项目和治理能力

| 能力 | 能做什么 | 说明入口 |
|---|---|---|
| Business Starter | 提供业务项目目录、后端/前端拓扑和初始化模板。 | [Business Starter](../../mango-business-starter/README.md) |
| Business PMO | 为业务仓提供版本化规则、模板、Agent 和检查工具。 | [Business PMO](../../mango-business-starter/business-pmo/README.md) |
| PMO Rules | 定义研发流程、开发规范、测试、交付和文档门禁。 | [PMO 总流程](../../mango-pmo/rules/00-dev-flow.md) |
| PMO Templates | 提供 BRD、SRS、TDD、Plan、验收和交付记录模板。 | [模板选择页](../../mango-pmo/templates/README.md) |

## 6. 模块协作关系

### 登录到菜单闭环

登录后菜单和页面入口由多个模块协作完成：

1. **Identity** 说明账号和租户成员是谁。
2. **Auth** 负责认证、登录回调和登录上下文。
3. **Authorization** 负责菜单、按钮、API 资源、角色和权限事实。
4. **Access** 负责访问检查和拦截；**Admin Shell** 负责把可用菜单装配为页面入口。

完整责任、权限校验和失败边界仍以各模块 README 为准；前端显示菜单不是最终授权决定。

## 7. 排障入口

运行时失败先看对应模块 README 的配置、权限、租户和失败边界；页面问题再看对应前端包 README；跨模块问题进入[业务接入场景手册](../guides/business-integration/README.md)。

## 8. 如何使用本页

1. 先按模块名称和职责判断能力归属。
2. 进入对应 README，查看完整 API、配置、权限、租户和失败边界。
3. 需要多个模块协同时，再查看[业务接入场景手册](../guides/business-integration/README.md)。
4. 需要长期约束时查看 `mango-pmo/rules/**`；需要历史原因时查看 `mango-docs/designs/**`、`plans/**` 和 `evidence/**`。

能力地图不复制模块 README 的详细内容，也不承载 Issue、Release 或交付历史。
