# Mango 能力地图

## 1. 定位

本文用于帮助 Mango 开发者、业务开发者和 AI Agent 快速定位 Mango 能力、模块 README、关联 PMO 规则和验证入口。

长期规则仍以 `mango-pmo` 为唯一来源；本文只做能力索引，不复制规范正文。

## 2. 使用方式

1. 先按任务关键词找到涉及能力。
2. 阅读能力对应的模块 README。
3. 查看关联 PMO 链接；正式交付规则以 preflight 输出和 `mango-pmo/rules/**` 为准。
4. 能力说明维护要求见 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)。

## 3. 组合接入入口

| 目标 | 阅读顺序 | 验证入口 |
|------|----------|----------|
| 登录到菜单闭环 | [Identity](../../mango/mango-platform/mango-identity/README.md) -> [Auth](../../mango/mango-platform/mango-auth/README.md) -> [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) -> [Admin Shell](../../mango-ui/packages/admin-shell/README.md) | [Auth 验证方式](../../mango/mango-platform/mango-auth/README.md#10-验证方式)、[菜单页面打不开排障](../guides/business-integration/rbac-menu-page-troubleshooting.md) |
| 按钮权限闭环 | [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) -> [RBAC Frontend](../../mango-ui/packages/rbac/README.md) -> [Admin Shell](../../mango-ui/packages/admin-shell/README.md) | [Authorization 验证方式](../../mango/mango-platform/mango-authorization/README.md#10-验证方式)、[按钮权限不显示排障](../guides/business-integration/permission-button-troubleshooting.md) |
| 文件上传到预览闭环 | [File](../../mango/mango-platform/mango-file/README.md) -> [Fileproc](../../mango/mango-infra/mango-infra-fileproc/README.md) -> [File Preview](../../mango/mango-platform/mango-file-preview/README.md) -> [Frontend File](../../mango-ui/packages/file/README.md) | [File 验证方式](../../mango/mango-platform/mango-file/README.md#10-验证方式)、[文件上传表单接入](../guides/business-integration/file-upload-form.md) |
| 业务审批闭环 | [Workflow](../../mango/mango-platform/mango-workflow/README.md) -> [Workflow Frontend](../../mango-ui/packages/workflow/README.md) -> [Workflow Example](../../mango-ui/packages/workflow-business-example/README.md) | [Workflow 验证方式](../../mango/mango-platform/mango-workflow/README.md#10-验证方式)、[业务审批接入](../guides/business-integration/workflow-business-approval.md) |
| 租户基础数据和字典闭环 | [Identity](../../mango/mango-platform/mango-identity/README.md) -> [Org](../../mango/mango-platform/mango-org/README.md) -> [System](../../mango/mango-platform/mango-system/README.md) -> [Seed](../../mango/mango-platform/mango-seed/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) | [System 验证方式](../../mango/mango-platform/mango-system/README.md#10-验证方式)、[租户字典配置为空排障](../guides/business-integration/tenant-dict-config-empty.md) |
| 定时任务闭环 | [Job](../../mango/mango-platform/mango-job/README.md) -> [Job Frontend](../../mango-ui/packages/job/README.md) -> [Notice](../../mango/mango-platform/mango-notice/README.md) | [Job 验证方式](../../mango/mango-platform/mango-job/README.md#10-验证方式)、[Job Frontend 验证方式](../../mango-ui/packages/job/README.md#10-验证方式) |
| 业务项目创建到 PR | [CLI](../../mango-ui/packages/mango-cli/README.md) -> [Business Starter](../../mango-business-starter/README.md) -> [Business PMO](../../mango-business-starter/business-pmo/README.md) -> [Topology](../../mango-business-starter/topologies/monolith/README.md) | [CLI 验证方式](../../mango-ui/packages/mango-cli/README.md#10-验证方式)、[Business Starter 验证方式](../../mango-business-starter/README.md#10-验证方式) |

## 4. 后端平台能力

| 能力 | 模块 | README | 验证入口 |
|------|------|--------|----------|
| 访问控制 Access | `mango/mango-platform/mango-access` | [README](../../mango/mango-platform/mango-access/README.md) | [验证方式](../../mango/mango-platform/mango-access/README.md#10-验证方式) |
| 认证 Auth | `mango/mango-platform/mango-auth` | [README](../../mango/mango-platform/mango-auth/README.md) | [验证方式](../../mango/mango-platform/mango-auth/README.md#10-验证方式) |
| 授权 Authorization | `mango/mango-platform/mango-authorization` | [README](../../mango/mango-platform/mango-authorization/README.md) | [验证方式](../../mango/mango-platform/mango-authorization/README.md#10-验证方式) |
| 日历 Calendar | `mango/mango-platform/mango-calendar` | [README](../../mango/mango-platform/mango-calendar/README.md) | [验证方式](../../mango/mango-platform/mango-calendar/README.md#10-验证方式) |
| 验证码 Captcha | `mango/mango-platform/mango-captcha` | [README](../../mango/mango-platform/mango-captcha/README.md) | [验证方式](../../mango/mango-platform/mango-captcha/README.md#10-验证方式) |
| 业务域 Domain | `mango/mango-platform/mango-domain` | [README](../../mango/mango-platform/mango-domain/README.md) | [验证方式](../../mango/mango-platform/mango-domain/README.md#10-验证方式) |
| 文件 File | `mango/mango-platform/mango-file` | [README](../../mango/mango-platform/mango-file/README.md) | [验证方式](../../mango/mango-platform/mango-file/README.md#10-验证方式) |
| 文件预览 File Preview | `mango/mango-platform/mango-file-preview` | [README](../../mango/mango-platform/mango-file-preview/README.md) | [验证方式](../../mango/mango-platform/mango-file-preview/README.md#10-验证方式) |
| 身份 Identity | `mango/mango-platform/mango-identity` | [README](../../mango/mango-platform/mango-identity/README.md) | [验证方式](../../mango/mango-platform/mango-identity/README.md#10-验证方式) |
| 任务调度 Job | `mango/mango-platform/mango-job` | [README](../../mango/mango-platform/mango-job/README.md) | [验证方式](../../mango/mango-platform/mango-job/README.md#10-验证方式) |
| 通知 Notice | `mango/mango-platform/mango-notice` | [README](../../mango/mango-platform/mango-notice/README.md) | [验证方式](../../mango/mango-platform/mango-notice/README.md#10-验证方式) |
| 编号生成 Numgen | `mango/mango-platform/mango-numgen` | [README](../../mango/mango-platform/mango-numgen/README.md) | [验证方式](../../mango/mango-platform/mango-numgen/README.md#10-验证方式) |
| 组织 Org | `mango/mango-platform/mango-org` | [README](../../mango/mango-platform/mango-org/README.md) | [验证方式](../../mango/mango-platform/mango-org/README.md#10-验证方式) |
| 初始化种子 Seed | `mango/mango-platform/mango-seed` | [README](../../mango/mango-platform/mango-seed/README.md) | [验证方式](../../mango/mango-platform/mango-seed/README.md#10-验证方式) |
| 系统 System | `mango/mango-platform/mango-system` | [README](../../mango/mango-platform/mango-system/README.md) | [验证方式](../../mango/mango-platform/mango-system/README.md#10-验证方式) |
| 模板 Template | `mango/mango-platform/mango-template` | [README](../../mango/mango-platform/mango-template/README.md) | [验证方式](../../mango/mango-platform/mango-template/README.md#10-验证方式) |
| 工作流 Workflow | `mango/mango-platform/mango-workflow` | [README](../../mango/mango-platform/mango-workflow/README.md) | [验证方式](../../mango/mango-platform/mango-workflow/README.md#10-验证方式) |

## 5. 后端基础设施能力

| 能力 | 模块 | README | 验证入口 |
|------|------|--------|----------|
| 上下文 Context | `mango/mango-infra/mango-infra-context` | [README](../../mango/mango-infra/mango-infra-context/README.md) | [验证方式](../../mango/mango-infra/mango-infra-context/README.md#10-验证方式) |
| 加密 Crypto | `mango/mango-infra/mango-infra-crypto` | [README](../../mango/mango-infra/mango-infra-crypto/README.md) | [验证方式](../../mango/mango-infra/mango-infra-crypto/README.md#10-验证方式) |
| 文档 Doc | `mango/mango-infra/mango-infra-doc` | [README](../../mango/mango-infra/mango-infra-doc/README.md) | [验证方式](../../mango/mango-infra/mango-infra-doc/README.md#10-验证方式) |
| 事件 Event | `mango/mango-infra/mango-infra-event` | [README](../../mango/mango-infra/mango-infra-event/README.md) | [验证方式](../../mango/mango-infra/mango-infra-event/README.md#10-验证方式) |
| Feign | `mango/mango-infra/mango-infra-feign` | [README](../../mango/mango-infra/mango-infra-feign/README.md) | [验证方式](../../mango/mango-infra/mango-infra-feign/README.md#10-验证方式) |
| 文件处理 Fileproc | `mango/mango-infra/mango-infra-fileproc` | [README](../../mango/mango-infra/mango-infra-fileproc/README.md) | [验证方式](../../mango/mango-infra/mango-infra-fileproc/README.md#10-验证方式) |
| Aspose License | `mango-infra-fileproc/resources/aspose` | [README](../../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md) | [验证方式](../../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md#10-验证方式) |
| IP 归属地 | `mango/mango-infra/mango-infra-ip-location` | [README](../../mango/mango-infra/mango-infra-ip-location/README.md) | [验证方式](../../mango/mango-infra/mango-infra-ip-location/README.md#10-验证方式) |
| KV | `mango/mango-infra/mango-infra-kv` | [README](../../mango/mango-infra/mango-infra-kv/README.md) | [验证方式](../../mango/mango-infra/mango-infra-kv/README.md#10-验证方式) |
| 日志 Log | `mango/mango-infra/mango-infra-log` | [README](../../mango/mango-infra/mango-infra-log/README.md) | [验证方式](../../mango/mango-infra/mango-infra-log/README.md#10-验证方式) |
| 模块服务 Module | `mango/mango-infra/mango-infra-module` | [README](../../mango/mango-infra/mango-infra-module/README.md) | [验证方式](../../mango/mango-infra/mango-infra-module/README.md#10-验证方式) |
| 持久化 Persistence | `mango/mango-infra/mango-infra-persistence` | [README](../../mango/mango-infra/mango-infra-persistence/README.md) | [验证方式](../../mango/mango-infra/mango-infra-persistence/README.md#10-验证方式) |
| 实时 Realtime | `mango/mango-infra/mango-infra-realtime` | [README](../../mango/mango-infra/mango-infra-realtime/README.md) | [验证方式](../../mango/mango-infra/mango-infra-realtime/README.md#10-验证方式) |
| 敏感数据 Sensitive | `mango/mango-infra/mango-infra-sensitive` | [README](../../mango/mango-infra/mango-infra-sensitive/README.md) | [验证方式](../../mango/mango-infra/mango-infra-sensitive/README.md#10-验证方式) |
| Infra Test | `mango/mango-infra/mango-infra-test` | [README](../../mango/mango-infra/mango-infra-test/README.md) | [验证方式](../../mango/mango-infra/mango-infra-test/README.md#10-验证方式) |
| Web | `mango/mango-infra/mango-infra-web` | [README](../../mango/mango-infra/mango-infra-web/README.md) | [验证方式](../../mango/mango-infra/mango-infra-web/README.md#10-验证方式) |

## 6. 前端与 CLI 能力

| 能力 | 包 | README | 验证入口 |
|------|----|--------|----------|
| 单体管理端 | `@mango/admin` | [README](../../mango-ui/packages/admin/README.md) | [验证方式](../../mango-ui/packages/admin/README.md#10-验证方式) |
| 页面注册表 | `@mango/admin-pages` | [README](../../mango-ui/packages/admin-pages/README.md) | [验证方式](../../mango-ui/packages/admin-pages/README.md#10-验证方式) |
| Shell | `@mango/admin-shell` | [README](../../mango-ui/packages/admin-shell/README.md) | [验证方式](../../mango-ui/packages/admin-shell/README.md#10-验证方式) |
| API Schema | `@mango/api-schema` | [README](../../mango-ui/packages/api-schema/README.md) | [验证方式](../../mango-ui/packages/api-schema/README.md#10-验证方式) |
| 应用运行时 | `@mango/app-runtime` | [README](../../mango-ui/packages/app-runtime/README.md) | [验证方式](../../mango-ui/packages/app-runtime/README.md#10-验证方式) |
| 认证前端 | `@mango/auth` | [README](../../mango-ui/packages/auth/README.md) | [验证方式](../../mango-ui/packages/auth/README.md#10-验证方式) |
| 日历前端 | `@mango/calendar` | [README](../../mango-ui/packages/calendar/README.md) | [验证方式](../../mango-ui/packages/calendar/README.md#10-验证方式) |
| 公共组件 | `@mango/common` | [README](../../mango-ui/packages/common/README.md) | [验证方式](../../mango-ui/packages/common/README.md#10-验证方式) |
| 文件前端 | `@mango/file` | [README](../../mango-ui/packages/file/README.md) | [验证方式](../../mango-ui/packages/file/README.md#10-验证方式) |
| 任务前端 | `@mango/job` | [README](../../mango-ui/packages/job/README.md) | [验证方式](../../mango-ui/packages/job/README.md#10-验证方式) |
| CLI | `@mango/cli` | [README](../../mango-ui/packages/mango-cli/README.md) | [验证方式](../../mango-ui/packages/mango-cli/README.md#10-验证方式) |
| 通知前端 | `@mango/notice` | [README](../../mango-ui/packages/notice/README.md) | [验证方式](../../mango-ui/packages/notice/README.md#10-验证方式) |
| 编号前端 | `@mango/numgen` | [README](../../mango-ui/packages/numgen/README.md) | [验证方式](../../mango-ui/packages/numgen/README.md#10-验证方式) |
| RBAC API | `@mango/rbac` | [README](../../mango-ui/packages/rbac/README.md) | [验证方式](../../mango-ui/packages/rbac/README.md#10-验证方式) |
| 系统前端 | `@mango/system` | [README](../../mango-ui/packages/system/README.md) | [验证方式](../../mango-ui/packages/system/README.md#10-验证方式) |
| 模板前端 | `@mango/template` | [README](../../mango-ui/packages/template/README.md) | [验证方式](../../mango-ui/packages/template/README.md#10-验证方式) |
| 工作流前端 | `@mango/workflow` | [README](../../mango-ui/packages/workflow/README.md) | [验证方式](../../mango-ui/packages/workflow/README.md#10-验证方式) |
| 工作流示例 | `@mango/workflow-business-example` | [README](../../mango-ui/packages/workflow-business-example/README.md) | [验证方式](../../mango-ui/packages/workflow-business-example/README.md#10-验证方式) |

## 7. 后端装配与工具

| 能力 | 模块 | README | 验证入口 |
|------|------|--------|----------|
| 后端聚合 Starter | `mango/mango-admin-starter` | [README](../../mango/mango-admin-starter/README.md) | [验证方式](../../mango/mango-admin-starter/README.md#10-验证方式) |
| 应用拓扑 | `mango/mango-app` | [README](../../mango/mango-app/README.md) | [验证方式](../../mango/mango-app/README.md#10-验证方式) |
| 后端公共契约 | `mango/mango-common` | [README](../../mango/mango-common/README.md) | [验证方式](../../mango/mango-common/README.md#10-验证方式) |
| 可选扩展 | `mango/mango-extension` | [README](../../mango/mango-extension/README.md) | [验证方式](../../mango/mango-extension/README.md#10-验证方式) |
| Maven Parent | `mango/mango-parent` | [README](../../mango/mango-parent/README.md) | [验证方式](../../mango/mango-parent/README.md#10-验证方式) |
| 构建工具 | `mango/mango-tools` | [README](../../mango/mango-tools/README.md) | [验证方式](../../mango/mango-tools/README.md#10-验证方式) |

## 8. 业务项目与 PMO 基线

| 能力 | 入口 | README | 验证入口 |
|------|------|--------|----------|
| Business Starter | `mango-business-starter` | [README](../../mango-business-starter/README.md) | [验证方式](../../mango-business-starter/README.md#10-验证方式) |
| Business PMO | `mango-business-starter/business-pmo` | [README](../../mango-business-starter/business-pmo/README.md) | [验证方式](../../mango-business-starter/business-pmo/README.md#10-验证方式) |
| Baseline | `mango-business-starter/business-pmo/mango-baseline` | [README](../../mango-business-starter/business-pmo/mango-baseline/README.md) | [验证方式](../../mango-business-starter/business-pmo/mango-baseline/README.md#10-验证方式) |
| 单体拓扑 | `mango-business-starter/topologies/monolith` | [README](../../mango-business-starter/topologies/monolith/README.md) | [验证方式](../../mango-business-starter/topologies/monolith/README.md#10-验证方式) |
| 微服务拓扑 | `mango-business-starter/topologies/microservice` | [README](../../mango-business-starter/topologies/microservice/README.md) | [验证方式](../../mango-business-starter/topologies/microservice/README.md#10-验证方式) |

## 9. 维护入口

能力说明维护规则见 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)。

模块 README 模板见 [module-readme.md](../../mango-pmo/templates/module-readme.md)。
