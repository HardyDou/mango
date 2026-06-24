# Mango 能力地图

## 1. 定位

本文用于帮助 Mango 开发者、业务开发者和 AI Agent 快速定位 Mango 能力、模块 README、关联 PMO 规则和排障入口。

长期规则仍以 `mango-pmo` 为唯一来源；本文只做能力索引，不复制规范正文。

## 2. 使用方式

1. 先按任务关键词找到涉及能力。
2. 阅读能力对应的模块 README。
3. 查看关联 PMO 链接；正式交付规则以 preflight 输出和 `mango-pmo/rules/**` 为准。
4. 能力说明维护要求见 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)。

## 3. 组合接入入口

| 目标 | 阅读顺序 | 排障入口 |
|------|----------|----------|
| 登录到菜单闭环 | [Identity](../../mango/mango-platform/mango-identity/README.md) -> [Auth](../../mango/mango-platform/mango-auth/README.md) -> [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) -> [Admin Shell](../../mango-ui/packages/admin-shell/README.md) | [Auth README](../../mango/mango-platform/mango-auth/README.md)、[菜单页面打不开排障](../guides/business-integration/rbac-menu-page-troubleshooting.md) |
| 按钮权限闭环 | [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) -> [RBAC Frontend](../../mango-ui/packages/rbac/README.md) -> [Admin Shell](../../mango-ui/packages/admin-shell/README.md) | [Authorization README](../../mango/mango-platform/mango-authorization/README.md)、[按钮权限不显示排障](../guides/business-integration/permission-button-troubleshooting.md) |
| 数据权限闭环 | [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Persistence](../../mango/mango-infra/mango-infra-persistence/README.md) -> [RBAC Frontend](../../mango-ui/packages/rbac/README.md) | [Authorization README](../../mango/mango-platform/mango-authorization/README.md)、[Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) |
| 文件上传到预览闭环 | [File](../../mango/mango-platform/mango-file/README.md) -> [Fileproc](../../mango/mango-infra/mango-infra-fileproc/README.md) -> [File Preview](../../mango/mango-platform/mango-file-preview/README.md) -> [Frontend File](../../mango-ui/packages/file/README.md) | [File README](../../mango/mango-platform/mango-file/README.md)、[文件上传表单接入](../guides/business-integration/file-upload-form.md) |
| 业务审批闭环 | [Workflow](../../mango/mango-platform/mango-workflow/README.md) -> [Workflow Frontend](../../mango-ui/packages/workflow/README.md) -> [Workflow Example](../../mango-ui/packages/workflow-business-example/README.md) | [Workflow README](../../mango/mango-platform/mango-workflow/README.md)、[业务审批接入](../guides/business-integration/workflow-business-approval.md) |
| 租户基础数据和字典闭环 | [Identity](../../mango/mango-platform/mango-identity/README.md) -> [Org](../../mango/mango-platform/mango-org/README.md) -> [System](../../mango/mango-platform/mango-system/README.md) -> [Seed](../../mango/mango-platform/mango-seed/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) | [System README](../../mango/mango-platform/mango-system/README.md)、[租户字典配置为空排障](../guides/business-integration/tenant-dict-config-empty.md) |
| 定时任务闭环 | [Job](../../mango/mango-platform/mango-job/README.md) -> [Job Frontend](../../mango-ui/packages/job/README.md) -> [Notice](../../mango/mango-platform/mango-notice/README.md) | [Job README](../../mango/mango-platform/mango-job/README.md)、[Job Frontend README](../../mango-ui/packages/job/README.md) |
| 业务项目创建到 PR | [CLI](../../mango-ui/packages/mango-cli/README.md) -> [Business Starter](../../mango-business-starter/README.md) -> [Business PMO](../../mango-business-starter/business-pmo/README.md) -> [Topology](../../mango-business-starter/topologies/monolith/README.md) | [CLI README](../../mango-ui/packages/mango-cli/README.md)、[Business Starter README](../../mango-business-starter/README.md) |
| 业务配置资源注入 | [Resource Registry](../../mango/mango-platform/mango-resource/README.md) -> 目标模块 README | [Resource README](../../mango/mango-platform/mango-resource/README.md) |

## 4. 后端平台能力

| 能力 | 模块 | README | 排障入口 |
|------|------|--------|----------|
| 访问控制 Access | `mango/mango-platform/mango-access` | [README](../../mango/mango-platform/mango-access/README.md) | [README](../../mango/mango-platform/mango-access/README.md) |
| 认证 Auth | `mango/mango-platform/mango-auth` | [README](../../mango/mango-platform/mango-auth/README.md) | [README](../../mango/mango-platform/mango-auth/README.md) |
| 授权 Authorization | `mango/mango-platform/mango-authorization` | [README](../../mango/mango-platform/mango-authorization/README.md) | [README](../../mango/mango-platform/mango-authorization/README.md) |
| 日历 Calendar | `mango/mango-platform/mango-calendar` | [README](../../mango/mango-platform/mango-calendar/README.md) | [README](../../mango/mango-platform/mango-calendar/README.md) |
| 验证码 Captcha | `mango/mango-platform/mango-captcha` | [README](../../mango/mango-platform/mango-captcha/README.md) | [README](../../mango/mango-platform/mango-captcha/README.md) |
| 业务域 Domain | `mango/mango-platform/mango-domain` | [README](../../mango/mango-platform/mango-domain/README.md) | [README](../../mango/mango-platform/mango-domain/README.md) |
| 文件 File | `mango/mango-platform/mango-file` | [README](../../mango/mango-platform/mango-file/README.md) | [README](../../mango/mango-platform/mango-file/README.md) |
| 文件预览 File Preview | `mango/mango-platform/mango-file-preview` | [README](../../mango/mango-platform/mango-file-preview/README.md) | [README](../../mango/mango-platform/mango-file-preview/README.md) |
| 自定义栅格布局 Grid Layout | `mango/mango-platform/mango-grid-layout` | [README](../../mango/mango-platform/mango-grid-layout/README.md) | [README](../../mango/mango-platform/mango-grid-layout/README.md) |
| 身份 Identity | `mango/mango-platform/mango-identity` | [README](../../mango/mango-platform/mango-identity/README.md) | [README](../../mango/mango-platform/mango-identity/README.md) |
| 任务调度 Job | `mango/mango-platform/mango-job` | [README](../../mango/mango-platform/mango-job/README.md) | [README](../../mango/mango-platform/mango-job/README.md) |
| 通知 Notice | `mango/mango-platform/mango-notice` | [README](../../mango/mango-platform/mango-notice/README.md) | [README](../../mango/mango-platform/mango-notice/README.md) |
| 编号生成 Numgen | `mango/mango-platform/mango-numgen` | [README](../../mango/mango-platform/mango-numgen/README.md) | [README](../../mango/mango-platform/mango-numgen/README.md) |
| 组织 Org | `mango/mango-platform/mango-org` | [README](../../mango/mango-platform/mango-org/README.md) | [README](../../mango/mango-platform/mango-org/README.md) |
| 支付 Payment | `mango/mango-platform/mango-payment` | [README](../../mango/mango-platform/mango-payment/README.md) | [README](../../mango/mango-platform/mango-payment/README.md) |
| 资源注册中心 Resource Registry | `mango/mango-platform/mango-resource` | [README](../../mango/mango-platform/mango-resource/README.md) | [README](../../mango/mango-platform/mango-resource/README.md) |
| 初始化种子 Seed | `mango/mango-platform/mango-seed` | [README](../../mango/mango-platform/mango-seed/README.md) | [README](../../mango/mango-platform/mango-seed/README.md) |
| 系统 System | `mango/mango-platform/mango-system` | [README](../../mango/mango-platform/mango-system/README.md) | [README](../../mango/mango-platform/mango-system/README.md) |
| 模板 Template | `mango/mango-platform/mango-template` | [README](../../mango/mango-platform/mango-template/README.md) | [README](../../mango/mango-platform/mango-template/README.md) |
| 工作流 Workflow | `mango/mango-platform/mango-workflow` | [README](../../mango/mango-platform/mango-workflow/README.md) | [README](../../mango/mango-platform/mango-workflow/README.md) |

## 5. 后端基础设施能力

| 能力 | 模块 | README | 排障入口 |
|------|------|--------|----------|
| 上下文 Context | `mango/mango-infra/mango-infra-context` | [README](../../mango/mango-infra/mango-infra-context/README.md) | [README](../../mango/mango-infra/mango-infra-context/README.md) |
| 加密 Crypto | `mango/mango-infra/mango-infra-crypto` | [README](../../mango/mango-infra/mango-infra-crypto/README.md) | [README](../../mango/mango-infra/mango-infra-crypto/README.md) |
| 文档 Doc | `mango/mango-infra/mango-infra-doc` | [README](../../mango/mango-infra/mango-infra-doc/README.md) | [README](../../mango/mango-infra/mango-infra-doc/README.md) |
| 事件 Event | `mango/mango-infra/mango-infra-event` | [README](../../mango/mango-infra/mango-infra-event/README.md) | [README](../../mango/mango-infra/mango-infra-event/README.md) |
| Feign | `mango/mango-infra/mango-infra-feign` | [README](../../mango/mango-infra/mango-infra-feign/README.md) | [README](../../mango/mango-infra/mango-infra-feign/README.md) |
| 文件处理 Fileproc | `mango/mango-infra/mango-infra-fileproc` | [README](../../mango/mango-infra/mango-infra-fileproc/README.md) | [README](../../mango/mango-infra/mango-infra-fileproc/README.md) |
| Aspose License | `mango-infra-fileproc/resources/aspose` | [README](../../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md) | [README](../../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md) |
| IP 归属地 | `mango/mango-infra/mango-infra-ip-location` | [README](../../mango/mango-infra/mango-infra-ip-location/README.md) | [README](../../mango/mango-infra/mango-infra-ip-location/README.md) |
| KV | `mango/mango-infra/mango-infra-kv` | [README](../../mango/mango-infra/mango-infra-kv/README.md) | [README](../../mango/mango-infra/mango-infra-kv/README.md) |
| 日志 Log | `mango/mango-infra/mango-infra-log` | [README](../../mango/mango-infra/mango-infra-log/README.md) | [README](../../mango/mango-infra/mango-infra-log/README.md) |
| 模块服务 Module | `mango/mango-infra/mango-infra-module` | [README](../../mango/mango-infra/mango-infra-module/README.md) | [README](../../mango/mango-infra/mango-infra-module/README.md) |
| 持久化 Persistence | `mango/mango-infra/mango-infra-persistence` | [README](../../mango/mango-infra/mango-infra-persistence/README.md) | [README](../../mango/mango-infra/mango-infra-persistence/README.md) |
| 实时 Realtime | `mango/mango-infra/mango-infra-realtime` | [README](../../mango/mango-infra/mango-infra-realtime/README.md) | [README](../../mango/mango-infra/mango-infra-realtime/README.md) |
| 敏感数据 Sensitive | `mango/mango-infra/mango-infra-sensitive` | [README](../../mango/mango-infra/mango-infra-sensitive/README.md) | [README](../../mango/mango-infra/mango-infra-sensitive/README.md) |
| Infra Test | `mango/mango-infra/mango-infra-test` | [README](../../mango/mango-infra/mango-infra-test/README.md) | [README](../../mango/mango-infra/mango-infra-test/README.md) |
| Web | `mango/mango-infra/mango-infra-web` | [README](../../mango/mango-infra/mango-infra-web/README.md) | [README](../../mango/mango-infra/mango-infra-web/README.md) |

## 6. 前端与 CLI 能力

Mango 前端包默认服务管理后台。标记为 `Admin Shell` 或 `Admin Pages` 的包不适合作为官网、营销站、C 端门户的页面组件直接集成；这类站点只应评估 `通用能力`、`混合能力` 或 CLI，并单独确认样式、依赖、接口和权限边界。

| 能力 | 包 | 适用端 / 集成形态 | 官网类站点建议 | README | 排障入口 |
|------|----|-------------------|----------------|--------|----------|
| 单体管理端 | `@mango/admin` | Admin Shell，后台应用聚合入口 | 不使用 | [README](../../mango-ui/packages/admin/README.md) | [README](../../mango-ui/packages/admin/README.md) |
| 后台 Shell | `@mango/admin-shell` | Admin Shell，后台布局、菜单、路由和运行时 | 不使用，除非官网就是内部后台 | [README](../../mango-ui/packages/admin-shell/README.md) | [README](../../mango-ui/packages/admin-shell/README.md) |
| 页面注册表 | `@mango/admin-pages` | Admin Pages，后台页面注册和 component key 映射 | 不使用 | [README](../../mango-ui/packages/admin-pages/README.md) | [README](../../mango-ui/packages/admin-pages/README.md) |
| 认证前端 | `@mango/auth` | Admin Pages，后台登录、用户与认证页面 | 不直接复用官网登录页 | [README](../../mango-ui/packages/auth/README.md) | [README](../../mango-ui/packages/auth/README.md) |
| 日历前端 | `@mango/calendar` | Admin Pages，后台日历管理页面 | 不直接复用整页 | [README](../../mango-ui/packages/calendar/README.md) | [README](../../mango-ui/packages/calendar/README.md) |
| 任务前端 | `@mango/job` | Admin Pages，后台任务管理页面 | 不使用 | [README](../../mango-ui/packages/job/README.md) | [README](../../mango-ui/packages/job/README.md) |
| 通知前端 | `@mango/notice` | Admin Pages，后台通知管理页面 | 不直接复用整页 | [README](../../mango-ui/packages/notice/README.md) | [README](../../mango-ui/packages/notice/README.md) |
| 编号前端 | `@mango/numgen` | Admin Pages，后台编号规则管理页面 | 不使用 | [README](../../mango-ui/packages/numgen/README.md) | [README](../../mango-ui/packages/numgen/README.md) |
| 支付前端 | `@mango/payment` | Admin Pages，后台支付配置、订单和对账页面 | 不直接复用后台管理页；收银台另按业务评估 | [README](../../mango-ui/packages/payment/README.md) | [README](../../mango-ui/packages/payment/README.md) |
| RBAC API | `@mango/rbac` | Admin Pages/API，后台菜单、权限和页面注册辅助 | 不使用后台页面；API 封装需按权限模型评估 | [README](../../mango-ui/packages/rbac/README.md) | [README](../../mango-ui/packages/rbac/README.md) |
| 系统前端 | `@mango/system` | Admin Pages，后台系统配置页面与组件 | 不直接复用整页 | [README](../../mango-ui/packages/system/README.md) | [README](../../mango-ui/packages/system/README.md) |
| 模板前端 | `@mango/template` | Admin Pages，后台模板管理页面 | 不使用 | [README](../../mango-ui/packages/template/README.md) | [README](../../mango-ui/packages/template/README.md) |
| 工作流前端 | `@mango/workflow` | Admin Pages，后台流程设计、审批和运行页面 | 不直接复用整页；表单/流程组件需单独评估 | [README](../../mango-ui/packages/workflow/README.md) | [README](../../mango-ui/packages/workflow/README.md) |
| 工作流示例 | `@mango/workflow-business-example` | Example，后台业务审批示例 | 不作为生产站点依赖 | [README](../../mango-ui/packages/workflow-business-example/README.md) | [README](../../mango-ui/packages/workflow-business-example/README.md) |
| API Schema | `@mango/api-schema` | 通用能力，接口类型和 schema | 可评估使用 | [README](../../mango-ui/packages/api-schema/README.md) | [README](../../mango-ui/packages/api-schema/README.md) |
| 应用运行时 | `@mango/app-runtime` | 通用/运行时能力，应用装配基础 | 可评估使用，但需确认是否绑定后台运行模型 | [README](../../mango-ui/packages/app-runtime/README.md) | [README](../../mango-ui/packages/app-runtime/README.md) |
| 公共组件 | `@mango/common` | 通用能力，请求、消息、选择器、编辑器等 | 可评估使用，需核对 Element Plus、主题和后台依赖 | [README](../../mango-ui/packages/common/README.md) | [README](../../mango-ui/packages/common/README.md) |
| 文件前端 | `@mango/file` | 混合能力，包含后台页面和上传/预览组件 | 只评估组件级能力，不直接复用后台页面 | [README](../../mango-ui/packages/file/README.md) | [README](../../mango-ui/packages/file/README.md) |
| 自定义栅格布局前端 | `@mango/grid-layout` | 通用能力，自定义栅格展示与编辑器 | 可评估使用，需确认 Element Plus、主题和个人布局接口边界 | [README](../../mango-ui/packages/grid-layout/README.md) | [README](../../mango-ui/packages/grid-layout/README.md) |
| 栅格系统小组件 | `@mango/grid-widgets` | 通用能力，系统小组件集合、用户信息、快捷入口、消息中心与业务小组件注册聚合 | 可评估使用，需确认运行时用户、菜单、跳转适配和小组件数据权限边界 | [README](../../mango-ui/packages/grid-widgets/README.md) | [README](../../mango-ui/packages/grid-widgets/README.md) |
| CLI | `@mango/cli` | 开发工具，项目生成、模块追加和 PMO baseline 同步 | 可用于生成项目，不是运行时组件 | [README](../../mango-ui/packages/mango-cli/README.md) | [README](../../mango-ui/packages/mango-cli/README.md) |

## 7. 后端装配与工具

| 能力 | 模块 | README | 排障入口 |
|------|------|--------|----------|
| 后端聚合 Starter | `mango/mango-admin-starter` | [README](../../mango/mango-admin-starter/README.md) | [README](../../mango/mango-admin-starter/README.md) |
| 应用拓扑 | `mango/mango-app` | [README](../../mango/mango-app/README.md) | [README](../../mango/mango-app/README.md) |
| 后端公共契约 | `mango/mango-common` | [README](../../mango/mango-common/README.md) | [README](../../mango/mango-common/README.md) |
| 可选扩展 | `mango/mango-extension` | [README](../../mango/mango-extension/README.md) | [README](../../mango/mango-extension/README.md) |
| Maven Parent | `mango/mango-parent` | [README](../../mango/mango-parent/README.md) | [README](../../mango/mango-parent/README.md) |
| 构建工具 | `mango/mango-tools` | [README](../../mango/mango-tools/README.md) | [README](../../mango/mango-tools/README.md) |

## 8. 业务项目与 PMO 基线

| 能力 | 入口 | README | 排障入口 |
|------|------|--------|----------|
| Business Starter | `mango-business-starter` | [README](../../mango-business-starter/README.md) | [README](../../mango-business-starter/README.md) |
| Business PMO | `mango-business-starter/business-pmo` | [README](../../mango-business-starter/business-pmo/README.md) | [README](../../mango-business-starter/business-pmo/README.md) |
| Baseline | `mango-business-starter/business-pmo/mango-baseline` | [README](../../mango-business-starter/business-pmo/mango-baseline/README.md) | [README](../../mango-business-starter/business-pmo/mango-baseline/README.md) |
| 单体拓扑 | `mango-business-starter/topologies/monolith` | [README](../../mango-business-starter/topologies/monolith/README.md) | [README](../../mango-business-starter/topologies/monolith/README.md) |
| 微服务拓扑 | `mango-business-starter/topologies/microservice` | [README](../../mango-business-starter/topologies/microservice/README.md) | [README](../../mango-business-starter/topologies/microservice/README.md) |

## 9. 维护入口

能力说明维护规则见 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)。

模块 README 模板见 [module-readme.md](../../mango-pmo/templates/module-readme.md)。
