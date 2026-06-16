# 2026-06-15 Module README Usage Rewrite Ledger

## 1. 目标

按 `mango-pmo/rules/08-capability-docs.md` 重写有效模块 README，让业务开发者读完后能判断模块是什么、有什么能力、如何接入、配置在哪里、配置字段含义、菜单和权限如何初始化、在哪里确认生效或排障。

## 2. 范围

- 包含仓库内有效 `README.md`。
- 排除 `mango-docs/evidence/**` 历史证据快照。
- 纯管理页面 README 只写具体管理能力、页面 key、依赖接口、菜单和权限关系。
- 公共组件、公开 API、运行时扩展点、starter、CLI、后端能力模块必须写到字段和使用闭环。

## 3. 重写清单

| 状态 | README | 类型 | 说明 |
|------|--------|------|------|
| DONE | `README.md` | 入口 | 根入口、文档导航 |
| DONE | `deploy/job/README.md` | 部署 | job 部署 |
| DONE | `mango-business-starter/README.md` | starter | 业务项目模板 |
| DONE | `mango-business-starter/business-pmo/README.md` | starter | 业务 PMO |
| DONE | `mango-business-starter/business-pmo/mango-baseline/README.md` | starter | 基线 |
| DONE | `mango-business-starter/topologies/microservice/README.md` | starter | 微服务拓扑 |
| DONE | `mango-business-starter/topologies/monolith/README.md` | starter | 单体拓扑 |
| DONE | `mango-docs/README.md` | 文档入口 | Pages 导航 |
| DONE | `mango-docs/capabilities/README.md` | 能力地图 | 能力索引 |
| DONE | `mango-docs/guides/business-integration/README.md` | 业务指南 | 业务接入 |
| DONE | `mango-ui/README.md` | 前端入口 | workspace |
| DONE | `mango-ui/packages/admin-pages/README.md` | 前端公共能力 | 页面注册 |
| DONE | `mango-ui/packages/admin-shell/README.md` | 前端公共能力 | 管理壳 |
| DONE | `mango-ui/packages/admin/README.md` | 前端聚合 | admin 包 |
| DONE | `mango-ui/packages/api-schema/README.md` | 前端公共能力 | API schema |
| DONE | `mango-ui/packages/app-runtime/README.md` | 前端公共能力 | runtime |
| DONE | `mango-ui/packages/auth/README.md` | 前端能力 | auth 包 |
| DONE | `mango-ui/packages/auth/src/views/README.md` | 管理页面 | auth 页面 |
| DONE | `mango-ui/packages/calendar/README.md` | 前端能力 | calendar |
| DONE | `mango-ui/packages/common/README.md` | 前端公共能力 | common |
| DONE | `mango-ui/packages/file/README.md` | 前端能力 | file API、admin-pages、组件 |
| DONE | `mango-ui/packages/file/src/components/README.md` | 前端公共组件 | MUpload、FilePreviewPanel |
| DONE | `mango-ui/packages/job/README.md` | 前端能力 | job |
| DONE | `mango-ui/packages/job/src/views/README.md` | 管理页面 | job 页面 |
| DONE | `mango-ui/packages/mango-cli/README.md` | CLI | CLI 命令 |
| DONE | `mango-ui/packages/mango-cli/templates/full/README.md` | 模板 | full 模板 |
| DONE | `mango-ui/packages/mango-cli/templates/full/business-pmo/README.md` | 模板 | PMO 模板 |
| DONE | `mango-ui/packages/mango-cli/templates/full/business-pmo/mango-baseline/README.md` | 模板 | 基线模板 |
| DONE | `mango-ui/packages/mango-cli/templates/full/topologies/microservice/README.md` | 模板 | 微服务模板 |
| DONE | `mango-ui/packages/mango-cli/templates/full/topologies/monolith/README.md` | 模板 | 单体模板 |
| DONE | `mango-ui/packages/notice/README.md` | 前端能力 | notice |
| DONE | `mango-ui/packages/numgen/README.md` | 前端能力 | numgen |
| DONE | `mango-ui/packages/payment/README.md` | 前端能力 | payment |
| DONE | `mango-ui/packages/rbac/README.md` | 前端能力 | rbac |
| DONE | `mango-ui/packages/rbac/src/views/README.md` | 管理页面 | rbac 页面 |
| DONE | `mango-ui/packages/system/README.md` | 前端能力 | system |
| DONE | `mango-ui/packages/system/src/components/README.md` | 前端公共组件 | system 组件 |
| DONE | `mango-ui/packages/template/README.md` | 前端能力 | template |
| DONE | `mango-ui/packages/workflow-business-example/README.md` | 示例 | workflow example |
| DONE | `mango-ui/packages/workflow/README.md` | 前端能力 | workflow |
| DONE | `mango-ui/packages/workflow/src/components/README.md` | 前端公共组件 | workflow 组件 |
| DONE | `mango/README.md` | 后端入口 | Maven 聚合 |
| DONE | `mango/config/ip-location/README.md` | 配置资产 | IP 数据 |
| DONE | `mango/mango-admin-starter/README.md` | 后端 starter | admin starter |
| DONE | `mango/mango-app/README.md` | 应用入口 | app 拓扑 |
| DONE | `mango/mango-app/microservice/README.md` | 应用入口 | 微服务 app |
| DONE | `mango/mango-app/monolith/mango-monolith-app/README.md` | 应用入口 | 单体 app |
| DONE | `mango/mango-common/README.md` | 后端公共能力 | common |
| DONE | `mango/mango-extension/README.md` | 后端扩展 | extension |
| DONE | `mango/mango-infra/mango-infra-context/README.md` | 后端公共能力 | 上下文 |
| DONE | `mango/mango-infra/mango-infra-crypto/README.md` | 后端公共能力 | 加解密 |
| DONE | `mango/mango-infra/mango-infra-doc/README.md` | 后端公共能力 | doc |
| DONE | `mango/mango-infra/mango-infra-event/README.md` | 后端公共能力 | event |
| DONE | `mango/mango-infra/mango-infra-feign/README.md` | 后端公共能力 | feign |
| DONE | `mango/mango-infra/mango-infra-fileproc/README.md` | 后端公共能力 | 文件处理 |
| DONE | `mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md` | 资源说明 | Aspose |
| DONE | `mango/mango-infra/mango-infra-ip-location/README.md` | 后端公共能力 | IP 定位 |
| DONE | `mango/mango-infra/mango-infra-kv/README.md` | 后端公共能力 | KV |
| DONE | `mango/mango-infra/mango-infra-log/README.md` | 后端公共能力 | 日志 |
| DONE | `mango/mango-infra/mango-infra-module/README.md` | 后端公共能力 | 模块资源 |
| DONE | `mango/mango-infra/mango-infra-persistence/README.md` | 后端公共能力 | 持久化 |
| DONE | `mango/mango-infra/mango-infra-realtime/README.md` | 后端公共能力 | 实时 |
| DONE | `mango/mango-infra/mango-infra-sensitive/README.md` | 后端公共能力 | 敏感词 |
| DONE | `mango/mango-infra/mango-infra-test/README.md` | 后端测试能力 | test |
| DONE | `mango/mango-infra/mango-infra-web/README.md` | 后端公共能力 | web |
| DONE | `mango/mango-parent/README.md` | Maven 管理 | parent |
| DONE | `mango/mango-platform/mango-access/README.md` | 后端能力 | access |
| DONE | `mango/mango-platform/mango-auth/README.md` | 后端能力 | auth |
| DONE | `mango/mango-platform/mango-authorization/README.md` | 后端能力 | authorization |
| DONE | `mango/mango-platform/mango-calendar/README.md` | 后端能力 | calendar |
| DONE | `mango/mango-platform/mango-captcha/README.md` | 后端能力 | captcha |
| DONE | `mango/mango-platform/mango-domain/README.md` | 后端能力 | domain |
| DONE | `mango/mango-platform/mango-file-preview/README.md` | 后端能力 | file preview |
| DONE | `mango/mango-platform/mango-file/README.md` | 后端能力 | file 配置/API/权限/迁移 |
| DONE | `mango/mango-platform/mango-identity/README.md` | 后端能力 | identity |
| DONE | `mango/mango-platform/mango-job/README.md` | 后端能力 | job |
| DONE | `mango/mango-platform/mango-notice/README.md` | 后端能力 | notice |
| DONE | `mango/mango-platform/mango-numgen/README.md` | 后端能力 | numgen |
| DONE | `mango/mango-platform/mango-org/README.md` | 后端能力 | org |
| DONE | `mango/mango-platform/mango-payment/README.md` | 后端能力 | payment |
| DONE | `mango/mango-platform/mango-seed/README.md` | 后端能力 | seed |
| DONE | `mango/mango-platform/mango-system/README.md` | 后端能力 | system |
| DONE | `mango/mango-platform/mango-template/README.md` | 后端能力 | template |
| DONE | `mango/mango-platform/mango-workflow/README.md` | 后端能力 | workflow |
| DONE | `mango/mango-tools/README.md` | 工具 | Maven plugin |

## 4. 验收命令

```bash
node mango-pmo/tools/audit-module-readmes.mjs
node mango-pmo/tools/audit-readme-source-facts.mjs
git diff --check
(cd mango-docs && npm run docs:build)
```
