# Mango 文档

这里是面向业务开发者的 Mango 文档入口。Mango 是业务系统研发底座，提供后端平台能力、前端管理端能力、业务项目脚手架、PMO 规则和部署接入说明。

如果你要接入一个能力，优先看对应模块 README：模块定位、功能清单、接入方式、前端用法、配置说明和字段含义。

## 1. 开始

- [Mango 能力地图](./capabilities/README.md)：按模块查 README、接入入口和组合使用顺序。
- [业务接入场景手册](./guides/business-integration/README.md)：按业务问题查接入路径和排障入口。
- [数据初始化与停机升级治理](./designs/2026-07-01-issue-184-data-governance-design.md)：说明 Flyway、Resource、demo、`INIT_ONLY`、外部 SQL 和 baseline pack 的边界。
- [PRD 模板](../mango-pmo/templates/prd.md)：输出业务开发可读、AI 可继续设计的需求文档。
- [详细设计模板](../mango-pmo/templates/detailed-design.md)：把 PRD 转成可开发、可验证、可交付的设计文档。
- [交付契约模板](../mango-pmo/templates/delivery-contract.md)：把 PRD、设计、开发和验收项拆成逐项可核验台账。
- [PRD 模板规范](../mango-pmo/rules/product/01-prd-template.md)：约束 PRD 的业务边界、编号和验收闭环。
- [详细设计模板规范](../mango-pmo/rules/product/03-detailed-design-template.md)：约束设计文档的 PRD 追踪、接口、数据、权限、状态和验收映射。
- [文档资产归档边界](../mango-pmo/rules/06-document-assets.md)：说明哪些文档放 PMO、哪些放模块 README、哪些放 evidence。
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)：模块 README 的验收门禁。

## 1.1 模块使用文档交付

业务开发时看不到模块使用文档，按下面方式解决：

1. 在线阅读统一走 Mango 文档站，入口是 [Mango 能力地图](./capabilities/README.md)。
2. 离线或本地开发时，拉取与依赖版本匹配的 Mango 源码或文档快照，直接阅读模块 README。
3. 本地预览文档站：

```bash
npm --prefix mango-docs install
npm --prefix mango-docs run docs:dev
```

4. 静态构建文档站：

```bash
npm --prefix mango-docs run docs:build
```

后端 Maven 运行时 jar 不承载 README；jar 只包含运行所需类和资源。前端 npm 包继续保留包根 `README.md`，这是 npm 生态的标准文档入口。

## 1.2 文档版本选择

GitHub Pages 根路径 `/mango/` 永远发布当前 `main` 的最新文档，导航栏中的 `Latest` 表示最新文档。

业务开发需要锁定文档版本时，选择与后端 Maven、前端 npm 发布版本一致的 release tag。版本快照路径固定为 `/mango/versions/<release-tag>/`，例如：

```text
/mango/versions/v2026.06.30-maven-1.0.1-admin-branding-cli-release/
```

新增发布版本时，在 `mango-docs` 下生成版本快照：

```bash
npm --prefix mango-docs run docs:snapshot -- v2026.06.30-maven-1.0.1-admin-branding-cli-release
```

生成后再执行常规构建：

```bash
npm --prefix mango-docs run docs:build
```

`docs:build` 会构建 Latest，并把 `mango-docs/versions` 下的历史快照一起复制到 GitHub Pages artifact。业务开发按 release tag 选择文档版本；没有锁定版本时使用 Latest。

## 2. 示例场景

- [文件上传表单接入](./guides/business-integration/file-upload-form.md)
- [业务审批接入](./guides/business-integration/workflow-business-approval.md)
- [菜单页面打不开排障](./guides/business-integration/rbac-menu-page-troubleshooting.md)
- [按钮权限不显示排障](./guides/business-integration/permission-button-troubleshooting.md)
- [租户字典配置为空排障](./guides/business-integration/tenant-dict-config-empty.md)
- [数据初始化与停机升级治理](./designs/2026-07-01-issue-184-data-governance-design.md)
- [Workflow 业务示例前端包](../mango-ui/packages/workflow-business-example/README.md)
- [Job 部署与生产参数](../deploy/job/README.md)

## 3. 基础能力

后端基础设施：

- [Context 上下文](../mango/mango-infra/mango-infra-context/README.md)
- [Crypto 加密](../mango/mango-infra/mango-infra-crypto/README.md)
- [Doc 文档](../mango/mango-infra/mango-infra-doc/README.md)
- [Event 事件](../mango/mango-infra/mango-infra-event/README.md)
- [Feign](../mango/mango-infra/mango-infra-feign/README.md)
- [Fileproc 文件处理](../mango/mango-infra/mango-infra-fileproc/README.md)
- [Aspose License](../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md)
- [IP Location](../mango/mango-infra/mango-infra-ip-location/README.md)
- [KV](../mango/mango-infra/mango-infra-kv/README.md)
- [Log 日志](../mango/mango-infra/mango-infra-log/README.md)
- [Module 模块服务](../mango/mango-infra/mango-infra-module/README.md)
- [Persistence 持久化](../mango/mango-infra/mango-infra-persistence/README.md)
- [Realtime 实时](../mango/mango-infra/mango-infra-realtime/README.md)
- [Sensitive 敏感数据](../mango/mango-infra/mango-infra-sensitive/README.md)
- [Infra Test](../mango/mango-infra/mango-infra-test/README.md)
- [Web](../mango/mango-infra/mango-infra-web/README.md)

公共装配和工具：

- [Admin Starter](../mango/mango-admin-starter/README.md)
- [Common](../mango/mango-common/README.md)
- [Extension](../mango/mango-extension/README.md)
- [Maven Parent](../mango/mango-parent/README.md)
- [Mango Tools](../mango/mango-tools/README.md)
- [Mango CLI](../mango-ui/packages/mango-cli/README.md)
- [API Schema](../mango-ui/packages/api-schema/README.md)
- [Common 前端公共组件](../mango-ui/packages/common/README.md)

## 4. 平台能力

后端平台能力：

- [Access 访问控制](../mango/mango-platform/mango-access/README.md)
- [Auth 认证](../mango/mango-platform/mango-auth/README.md)
- [Authorization 授权](../mango/mango-platform/mango-authorization/README.md)
- [Calendar 日历](../mango/mango-platform/mango-calendar/README.md)
- [Captcha 验证码](../mango/mango-platform/mango-captcha/README.md)
- [Domain 业务域](../mango/mango-platform/mango-domain/README.md)
- [File 文件](../mango/mango-platform/mango-file/README.md)
- [File Preview 文件预览](../mango/mango-platform/mango-file-preview/README.md)
- [Grid Layout 自定义栅格布局](../mango/mango-platform/mango-grid-layout/README.md)
- [Identity 身份](../mango/mango-platform/mango-identity/README.md)
- [Job 任务调度](../mango/mango-platform/mango-job/README.md)
- [Link 网址导航](../mango/mango-platform/mango-link/README.md)
- [Notice 通知](../mango/mango-platform/mango-notice/README.md)
- [Numgen 编号生成](../mango/mango-platform/mango-numgen/README.md)
- [Org 组织](../mango/mango-platform/mango-org/README.md)
- [Payment 支付](../mango/mango-platform/mango-payment/README.md)
- [Resource Registry 资源注册中心](../mango/mango-platform/mango-resource/README.md)
- [System 系统](../mango/mango-platform/mango-system/README.md)
- [Template 模板](../mango/mango-platform/mango-template/README.md)
- [Workflow 工作流](../mango/mango-platform/mango-workflow/README.md)

前端平台能力主要服务管理后台，详见：

- [前端能力索引](./capabilities/README.md#6-前端与-cli-能力)
- [Admin Pages](../mango-ui/packages/admin-pages/README.md)
- [RBAC 前端](../mango-ui/packages/rbac/README.md)
- [Payment 前端](../mango-ui/packages/payment/README.md)
- [Workflow 前端](../mango-ui/packages/workflow/README.md)

## 5. 代码规范

- [PMO 总流程](../mango-pmo/rules/00-dev-flow.md)
- [AI 编码红线](../mango-pmo/rules/03-ai-coding-redlines.md)
- [AI 交付质量门禁](../mango-pmo/rules/05-ai-delivery-quality.md)
- [后端模块规范](../mango-pmo/rules/backend/05-module.md)
- [后端安全规范](../mango-pmo/rules/backend/06-security.md)
- [模块菜单规范](../mango-pmo/rules/backend/11-module-menu.md)
- [前端 Vue 代码规范](../mango-pmo/rules/frontend/01-vue-code.md)
- [前端 Monorepo 架构规范](../mango-pmo/rules/frontend/06-monorepo-architecture.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)
- [模块 README 模板](../mango-pmo/templates/module-readme.md)

## 6. 架构设计

- [Mango 后端聚合](../mango/README.md)
- [应用拓扑](../mango/mango-app/README.md)
- [单体应用](../mango/mango-app/monolith/mango-monolith-app/README.md)
- [微服务拓扑](../mango/mango-app/microservice/README.md)
- [前端 workspace](../mango-ui/README.md)
- [Admin Shell](../mango-ui/packages/admin-shell/README.md)
- [App Runtime](../mango-ui/packages/app-runtime/README.md)
- [业务项目模板](../mango-business-starter/README.md)
- [单体业务拓扑模板](../mango-business-starter/topologies/monolith/README.md)
- [微服务业务拓扑模板](../mango-business-starter/topologies/microservice/README.md)

## 7. PMO

PMO 内容统一维护在 `mango-pmo`，这里登记入口：

- [PMO 总流程](../mango-pmo/rules/00-dev-flow.md)
- [PRD 模板规范](../mango-pmo/rules/product/01-prd-template.md)
- [PRD 模板](../mango-pmo/templates/prd.md)
- [详细设计模板规范](../mango-pmo/rules/product/03-detailed-design-template.md)
- [详细设计模板](../mango-pmo/templates/detailed-design.md)
- [交付契约模板](../mango-pmo/templates/delivery-contract.md)
- [文档资产归档边界](../mango-pmo/rules/06-document-assets.md)
- [能力说明维护规范](../mango-pmo/rules/08-capability-docs.md)
- [规则索引](../mango-pmo/rules/index.json)
- [Business PMO 模板](../mango-business-starter/business-pmo/README.md)
- [Business PMO Baseline](../mango-business-starter/business-pmo/mango-baseline/README.md)
