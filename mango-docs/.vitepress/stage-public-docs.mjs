import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { dirname, extname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const docsRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(docsRoot, '..');
const stageRoot = resolve(docsRoot, '.vitepress/public-src');
const versionsRoot = resolve(docsRoot, 'versions');
const versionsManifestPath = resolve(versionsRoot, 'manifest.json');
const githubBlobBase = process.env.MANGO_DOCS_GITHUB_BLOB_BASE || 'https://github.com/HardyDou/mango/blob/main';
const docsPublicBase = normalizePublicBase(process.env.MANGO_DOCS_PUBLIC_BASE || 'https://hardydou.github.io/mango/');
const latestDocsLabel = process.env.MANGO_DOCS_LATEST_LABEL || 'Latest';
const docsVersionLabel = process.env.MANGO_DOCS_VERSION_LABEL || latestDocsLabel;

const publicDocs = [
  'mango-docs/index.md',
  'mango-docs/README.md',
  'mango-docs/capabilities/README.md',
  'mango-docs/mango-architecture-design.md',
  'mango-docs/mango-backend-architecture-boundary-refactor-master-plan.md',
  'mango-docs/tenant-model-research-guarantee-business.md',
  'mango-docs/guides/business-integration/README.md',
  'mango-docs/guides/business-integration/file-upload-form.md',
  'mango-docs/guides/business-integration/permission-button-troubleshooting.md',
  'mango-docs/guides/business-integration/rbac-menu-page-troubleshooting.md',
  'mango-docs/guides/business-integration/tenant-dict-config-empty.md',
  'mango-docs/guides/business-integration/workflow-business-approval.md',
  'mango-docs/designs/business-project-development-guide.md',
  'mango-docs/designs/mango-capability-usage-guide-for-ai.md',
  'mango-docs/designs/mango-domain-event-transparent-delivery-design.md',
  'mango-docs/designs/mango-file集成file-preview方案.md',
  'mango-docs/designs/mango-job-design.md',
  'mango-docs/designs/mango-multi-datasource-foundation-design.md',
  'mango-docs/designs/mango-native-job-engine-design.md',
  'mango-docs/designs/mango-notice多渠道通知中心设计说明书.md',
  'mango-docs/designs/统一支付系统设计说明书.md',
  'mango-pmo/AGENTS.md',
  'mango-pmo/agents/01-pm-agent.md',
  'mango-pmo/agents/02-tech-lead-agent.md',
  'mango-pmo/agents/03-dev-agent.md',
  'mango-pmo/agents/04-qa-agent.md',
  'mango-pmo/agents/05-pmo-agent.md',
  'mango-pmo/rules/00-dev-flow.md',
  'mango-pmo/rules/01-delivery-contract.md',
  'mango-pmo/rules/02-dev-environment.md',
  'mango-pmo/rules/03-ai-coding-redlines.md',
  'mango-pmo/rules/04-test-assets.md',
  'mango-pmo/rules/05-ai-delivery-quality.md',
  'mango-pmo/rules/06-document-assets.md',
  'mango-pmo/rules/07-mango-issue-runbook.md',
  'mango-pmo/rules/08-capability-docs.md',
  'mango-pmo/rules/backend/01-code.md',
  'mango-pmo/rules/backend/02-naming.md',
  'mango-pmo/rules/backend/03-api.md',
  'mango-pmo/rules/backend/04-db.md',
  'mango-pmo/rules/backend/05-module.md',
  'mango-pmo/rules/backend/06-security.md',
  'mango-pmo/rules/backend/07-persistence.md',
  'mango-pmo/rules/backend/08-test.md',
  'mango-pmo/rules/backend/09-versioning.md',
  'mango-pmo/rules/backend/10-dev-flow.md',
  'mango-pmo/rules/backend/11-module-menu.md',
  'mango-pmo/rules/frontend/01-vue-code.md',
  'mango-pmo/rules/frontend/02-element-plus-ui.md',
  'mango-pmo/rules/frontend/03-component-development.md',
  'mango-pmo/rules/frontend/04-test.md',
  'mango-pmo/rules/frontend/05-dev-flow.md',
  'mango-pmo/rules/frontend/06-monorepo-architecture.md',
  'mango-pmo/rules/frontend/07-admin-ui-common.md',
  'mango-pmo/rules/frontend/08-list-page.md',
  'mango-pmo/rules/frontend/09-detail-page.md',
  'mango-pmo/rules/frontend/10-form-page.md',
  'mango-pmo/rules/frontend/11-dialog-drawer.md',
  'mango-pmo/rules/index.json',
  'mango-pmo/rules/product/01-prd-template.md',
  'mango-pmo/rules/product/02-sprint.md',
  'mango-pmo/rules/product/03-detailed-design-template.md',
  'mango-pmo/templates/acceptance-evidence.md',
  'mango-pmo/templates/delivery-contract.md',
  'mango-pmo/templates/detailed-design.md',
  'mango-pmo/templates/frontend-entry-readme.md',
  'mango-pmo/templates/module-readme.md',
  'mango-pmo/templates/prd.md',
  'mango-pmo/tmp/2026-05-28-codex-history-governance-review.md',
  'mango-business-starter/README.md',
  'mango-business-starter/business-pmo/README.md',
  'mango-business-starter/business-pmo/mango-baseline/README.md',
  'mango-business-starter/topologies/monolith/README.md',
  'mango-business-starter/topologies/microservice/README.md',
  'mango/mango-admin-starter/README.md',
  'mango/mango-app/README.md',
  'mango/mango-common/README.md',
  'mango/mango-extension/README.md',
  'mango/mango-parent/README.md',
  'mango/mango-tools/README.md',
  'mango/mango-infra/mango-infra-context/README.md',
  'mango/mango-infra/mango-infra-crypto/README.md',
  'mango/mango-infra/mango-infra-doc/README.md',
  'mango/mango-infra/mango-infra-event/README.md',
  'mango/mango-infra/mango-infra-feign/README.md',
  'mango/mango-infra/mango-infra-fileproc/README.md',
  'mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md',
  'mango/mango-infra/mango-infra-ip-location/README.md',
  'mango/mango-infra/mango-infra-kv/README.md',
  'mango/mango-infra/mango-infra-log/README.md',
  'mango/mango-infra/mango-infra-module/README.md',
  'mango/mango-infra/mango-infra-persistence/README.md',
  'mango/mango-infra/mango-infra-realtime/README.md',
  'mango/mango-infra/mango-infra-sensitive/README.md',
  'mango/mango-infra/mango-infra-test/README.md',
  'mango/mango-infra/mango-infra-web/README.md',
  'mango/mango-platform/mango-access/README.md',
  'mango/mango-platform/mango-auth/README.md',
  'mango/mango-platform/mango-authorization/README.md',
  'mango/mango-platform/mango-calendar/README.md',
  'mango/mango-platform/mango-captcha/README.md',
  'mango/mango-platform/mango-domain/README.md',
  'mango/mango-platform/mango-file/README.md',
  'mango/mango-platform/mango-file-preview/README.md',
  'mango/mango-platform/mango-grid-layout/README.md',
  'mango/mango-platform/mango-identity/README.md',
  'mango/mango-platform/mango-job/README.md',
  'mango/mango-platform/mango-link/README.md',
  'mango/mango-platform/mango-notice/README.md',
  'mango/mango-platform/mango-numgen/README.md',
  'mango/mango-platform/mango-org/README.md',
  'mango/mango-platform/mango-payment/README.md',
  'mango/mango-platform/mango-resource/README.md',
  'mango/mango-platform/mango-system/README.md',
  'mango/mango-platform/mango-template/README.md',
  'mango/mango-platform/mango-workflow/README.md',
  'mango-ui/packages/admin/README.md',
  'mango-ui/packages/admin-pages/README.md',
  'mango-ui/packages/admin-shell/README.md',
  'mango-ui/packages/api-schema/README.md',
  'mango-ui/packages/app-runtime/README.md',
  'mango-ui/packages/auth/README.md',
  'mango-ui/packages/auth/src/views/README.md',
  'mango-ui/packages/calendar/README.md',
  'mango-ui/packages/common/README.md',
  'mango-ui/packages/file/README.md',
  'mango-ui/packages/file/src/components/README.md',
  'mango-ui/packages/job/README.md',
  'mango-ui/packages/job/src/views/README.md',
  'mango-ui/packages/mango-cli/README.md',
  'mango-ui/packages/notice/README.md',
  'mango-ui/packages/numgen/README.md',
  'mango-ui/packages/payment/README.md',
  'mango-ui/packages/rbac/README.md',
  'mango-ui/packages/rbac/src/views/README.md',
  'mango-ui/packages/system/README.md',
  'mango-ui/packages/system/src/components/README.md',
  'mango-ui/packages/template/README.md',
  'mango-ui/packages/workflow/README.md',
  'mango-ui/packages/workflow/src/components/README.md',
  'mango-ui/packages/workflow-business-example/README.md'
];

const publicDocSet = new Set(publicDocs.map(normalizePath));

const sidebar = [
  {
    text: '开始',
    collapsed: false,
    items: [
      { text: '文档首页', link: '/' },
      { text: 'Mango 文档目录', link: '/mango-docs/README' },
      { text: '业务接入场景手册', link: '/mango-docs/guides/business-integration/README' },
      { text: 'Mango 能力地图', link: '/mango-docs/capabilities/README' }
    ]
  },
  {
    text: '示例场景',
    collapsed: false,
    items: [
      { text: '业务接入总览', link: '/mango-docs/guides/business-integration/README' },
      { text: '文件上传表单', link: '/mango-docs/guides/business-integration/file-upload-form' },
      { text: '业务审批接入', link: '/mango-docs/guides/business-integration/workflow-business-approval' },
      { text: '菜单页面打不开排障', link: '/mango-docs/guides/business-integration/rbac-menu-page-troubleshooting' },
      { text: '按钮权限不显示排障', link: '/mango-docs/guides/business-integration/permission-button-troubleshooting' },
      { text: '租户字典配置为空排障', link: '/mango-docs/guides/business-integration/tenant-dict-config-empty' }
    ]
  },
  {
    text: '产品文档输出',
    collapsed: false,
    items: [
      { text: 'PRD 模板', link: '/mango-pmo/templates/prd' },
      { text: 'PRD 模板规范', link: '/mango-pmo/rules/product/01-prd-template' },
      { text: '详细设计模板', link: '/mango-pmo/templates/detailed-design' },
      { text: '详细设计模板规范', link: '/mango-pmo/rules/product/03-detailed-design-template' },
      { text: '交付契约模板', link: '/mango-pmo/templates/delivery-contract' },
      { text: '交付契约规则', link: '/mango-pmo/rules/01-delivery-contract' },
      { text: 'Sprint 规范', link: '/mango-pmo/rules/product/02-sprint' }
    ]
  },
  {
    text: '基础能力',
    collapsed: true,
    items: [
      {
        text: '基础设施',
        collapsed: false,
        items: [
          { text: 'Context 上下文', link: '/mango/mango-infra/mango-infra-context/README' },
          { text: 'Crypto 加密', link: '/mango/mango-infra/mango-infra-crypto/README' },
          { text: 'Doc 文档', link: '/mango/mango-infra/mango-infra-doc/README' },
          { text: 'Event 事件', link: '/mango/mango-infra/mango-infra-event/README' },
          { text: 'Feign', link: '/mango/mango-infra/mango-infra-feign/README' },
          { text: 'Fileproc 文件处理', link: '/mango/mango-infra/mango-infra-fileproc/README' },
          { text: 'Aspose License', link: '/mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README' },
          { text: 'IP Location', link: '/mango/mango-infra/mango-infra-ip-location/README' },
          { text: 'KV', link: '/mango/mango-infra/mango-infra-kv/README' },
          { text: 'Log 日志', link: '/mango/mango-infra/mango-infra-log/README' },
          { text: 'Module 模块服务', link: '/mango/mango-infra/mango-infra-module/README' },
          { text: 'Persistence 持久化', link: '/mango/mango-infra/mango-infra-persistence/README' },
          { text: 'Realtime 实时', link: '/mango/mango-infra/mango-infra-realtime/README' },
          { text: 'Sensitive 敏感数据', link: '/mango/mango-infra/mango-infra-sensitive/README' },
          { text: 'Infra Test', link: '/mango/mango-infra/mango-infra-test/README' },
          { text: 'Web', link: '/mango/mango-infra/mango-infra-web/README' }
        ]
      },
      {
        text: '公共与装配',
        collapsed: true,
        items: [
          { text: 'Common 前端公共组件', link: '/mango-ui/packages/common/README' },
          { text: 'Admin Starter', link: '/mango/mango-admin-starter/README' },
          { text: 'App 应用拓扑', link: '/mango/mango-app/README' },
          { text: 'Common 后端公共契约', link: '/mango/mango-common/README' },
          { text: 'Extension 可选扩展', link: '/mango/mango-extension/README' },
          { text: 'Parent Maven Parent', link: '/mango/mango-parent/README' },
          { text: 'Tools 构建工具', link: '/mango/mango-tools/README' }
        ]
      },
      {
        text: '业务项目基线',
        collapsed: true,
        items: [
          { text: 'Business Starter', link: '/mango-business-starter/README' },
          { text: 'Business PMO', link: '/mango-business-starter/business-pmo/README' },
          { text: 'Baseline', link: '/mango-business-starter/business-pmo/mango-baseline/README' },
          { text: '单体拓扑', link: '/mango-business-starter/topologies/monolith/README' },
          { text: '微服务拓扑', link: '/mango-business-starter/topologies/microservice/README' }
        ]
      }
    ]
  },
  {
    text: '平台能力',
    collapsed: false,
    items: [
      {
        text: '后端平台能力',
        collapsed: false,
        items: [
          { text: 'Access 访问控制', link: '/mango/mango-platform/mango-access/README' },
          { text: 'Auth 认证', link: '/mango/mango-platform/mango-auth/README' },
          { text: 'Authorization 授权', link: '/mango/mango-platform/mango-authorization/README' },
          { text: 'Calendar 日历', link: '/mango/mango-platform/mango-calendar/README' },
          { text: 'Captcha 验证码', link: '/mango/mango-platform/mango-captcha/README' },
          { text: 'Domain 业务域', link: '/mango/mango-platform/mango-domain/README' },
          { text: 'File 文件', link: '/mango/mango-platform/mango-file/README' },
          { text: 'File Preview 文件预览', link: '/mango/mango-platform/mango-file-preview/README' },
          { text: 'Grid Layout 自定义栅格布局', link: '/mango/mango-platform/mango-grid-layout/README' },
          { text: 'Identity 身份', link: '/mango/mango-platform/mango-identity/README' },
          { text: 'Job 任务调度', link: '/mango/mango-platform/mango-job/README' },
          { text: 'Link 网址导航', link: '/mango/mango-platform/mango-link/README' },
          { text: 'Notice 通知', link: '/mango/mango-platform/mango-notice/README' },
          { text: 'Numgen 编号', link: '/mango/mango-platform/mango-numgen/README' },
          { text: 'Org 组织', link: '/mango/mango-platform/mango-org/README' },
          { text: 'Payment 支付', link: '/mango/mango-platform/mango-payment/README' },
          { text: 'Resource Registry 资源注册中心', link: '/mango/mango-platform/mango-resource/README' },
          { text: 'System 系统', link: '/mango/mango-platform/mango-system/README' },
          { text: 'Template 模板', link: '/mango/mango-platform/mango-template/README' },
          { text: 'Workflow 工作流', link: '/mango/mango-platform/mango-workflow/README' }
        ]
      },
      {
        text: '前端能力分层',
        collapsed: true,
        items: [
          {
            text: 'Admin Shell / 后台壳层',
            collapsed: false,
            items: [
              { text: 'Admin Shell: 单体管理端', link: '/mango-ui/packages/admin/README' },
              { text: 'Admin Shell: 后台运行壳层', link: '/mango-ui/packages/admin-shell/README' }
            ]
          },
          {
            text: 'Admin Pages / 后台页面插件',
            collapsed: false,
            items: [
              { text: 'Admin Pages: 页面注册表', link: '/mango-ui/packages/admin-pages/README' },
              { text: 'Admin Pages: Auth 认证前端', link: '/mango-ui/packages/auth/README' },
              { text: 'Admin Pages: Auth Views', link: '/mango-ui/packages/auth/src/views/README' },
              { text: 'Admin Pages: Calendar 日历前端', link: '/mango-ui/packages/calendar/README' },
              { text: 'Admin Pages: Job 任务前端', link: '/mango-ui/packages/job/README' },
              { text: 'Admin Pages: Job Views', link: '/mango-ui/packages/job/src/views/README' },
              { text: 'Admin Pages: Notice 通知前端', link: '/mango-ui/packages/notice/README' },
              { text: 'Admin Pages: Numgen 编号前端', link: '/mango-ui/packages/numgen/README' },
              { text: 'Admin Pages: Payment 支付前端', link: '/mango-ui/packages/payment/README' },
              { text: 'Admin Pages: RBAC API 与菜单', link: '/mango-ui/packages/rbac/README' },
              { text: 'Admin Pages: RBAC Views', link: '/mango-ui/packages/rbac/src/views/README' },
              { text: 'Admin Pages: System 系统前端', link: '/mango-ui/packages/system/README' },
              { text: 'Admin Pages: System Components', link: '/mango-ui/packages/system/src/components/README' },
              { text: 'Admin Pages: Template 模板前端', link: '/mango-ui/packages/template/README' },
              { text: 'Admin Pages: Workflow 工作流前端', link: '/mango-ui/packages/workflow/README' },
              { text: 'Admin Pages: Workflow Components', link: '/mango-ui/packages/workflow/src/components/README' },
              { text: 'Admin Pages: Workflow Example', link: '/mango-ui/packages/workflow-business-example/README' }
            ]
          },
          {
            text: '通用能力 / 可独立评估',
            collapsed: false,
            items: [
              { text: '通用 API Schema', link: '/mango-ui/packages/api-schema/README' },
              { text: '通用 App Runtime', link: '/mango-ui/packages/app-runtime/README' },
              { text: '通用 Common 组件与工具', link: '/mango-ui/packages/common/README' },
              { text: '混合 File 文件前端', link: '/mango-ui/packages/file/README' },
              { text: '通用 File Components', link: '/mango-ui/packages/file/src/components/README' },
              { text: 'CLI 项目生成工具', link: '/mango-ui/packages/mango-cli/README' }
            ]
          }
        ]
      }
    ]
  },
  {
    text: '架构设计',
    collapsed: false,
    items: [
      { text: 'Mango 整体架构', link: '/mango-docs/mango-architecture-design' },
      { text: '后端架构边界总计划', link: '/mango-docs/mango-backend-architecture-boundary-refactor-master-plan' },
      { text: '租户模型调研', link: '/mango-docs/tenant-model-research-guarantee-business' },
      { text: '业务项目开发说明', link: '/mango-docs/designs/business-project-development-guide' },
      { text: 'Mango 能力使用指南', link: '/mango-docs/designs/mango-capability-usage-guide-for-ai' },
      { text: '领域事件透明投递', link: '/mango-docs/designs/mango-domain-event-transparent-delivery-design' },
      { text: '文件预览集成', link: '/mango-docs/designs/mango-file集成file-preview方案' },
      { text: '多数据源底座', link: '/mango-docs/designs/mango-multi-datasource-foundation-design' },
      { text: '原生 Job Engine', link: '/mango-docs/designs/mango-native-job-engine-design' },
      { text: '历史 Job 设计', link: '/mango-docs/designs/mango-job-design' },
      { text: '通知中心', link: '/mango-docs/designs/mango-notice多渠道通知中心设计说明书' },
      { text: '统一支付系统', link: '/mango-docs/designs/统一支付系统设计说明书' }
    ]
  },
  {
    text: 'PMO 规范与模板',
    collapsed: true,
    items: [
      {
        text: 'PMO 入口与角色',
        collapsed: true,
        items: [
          { text: 'PMO 入口说明', link: '/mango-pmo/AGENTS' },
          { text: 'PM Agent', link: '/mango-pmo/agents/01-pm-agent' },
          { text: 'Tech Lead Agent', link: '/mango-pmo/agents/02-tech-lead-agent' },
          { text: 'Dev Agent', link: '/mango-pmo/agents/03-dev-agent' },
          { text: 'QA Agent', link: '/mango-pmo/agents/04-qa-agent' },
          { text: 'PMO Agent', link: '/mango-pmo/agents/05-pmo-agent' }
        ]
      },
      {
        text: '总流程与质量门禁',
        collapsed: true,
        items: [
          { text: 'Mango PMO 总流程', link: '/mango-pmo/rules/00-dev-flow' },
          { text: '交付契约与设计说明', link: '/mango-pmo/rules/01-delivery-contract' },
          { text: '开发环境规范', link: '/mango-pmo/rules/02-dev-environment' },
          { text: 'AI 编码红线', link: '/mango-pmo/rules/03-ai-coding-redlines' },
          { text: '测试资产目录规范', link: '/mango-pmo/rules/04-test-assets' },
          { text: 'AI 交付质量门禁', link: '/mango-pmo/rules/05-ai-delivery-quality' },
          { text: '文档资产边界', link: '/mango-pmo/rules/06-document-assets' },
          { text: 'Mango Issue Runbook', link: '/mango-pmo/rules/07-mango-issue-runbook' },
          { text: '能力说明维护', link: '/mango-pmo/rules/08-capability-docs' },
          { text: '规则索引 JSON', link: '/mango-pmo/rules/index.json' }
        ]
      },
      {
        text: '后端规范',
        collapsed: true,
        items: [
          { text: '后端代码规范', link: '/mango-pmo/rules/backend/01-code' },
          { text: '后端命名规范', link: '/mango-pmo/rules/backend/02-naming' },
          { text: '后端 API 规范', link: '/mango-pmo/rules/backend/03-api' },
          { text: '数据库规范', link: '/mango-pmo/rules/backend/04-db' },
          { text: '后端模块规范', link: '/mango-pmo/rules/backend/05-module' },
          { text: '后端安全规范', link: '/mango-pmo/rules/backend/06-security' },
          { text: '持久化规范', link: '/mango-pmo/rules/backend/07-persistence' },
          { text: '后端测试规范', link: '/mango-pmo/rules/backend/08-test' },
          { text: '版本化发布规范', link: '/mango-pmo/rules/backend/09-versioning' },
          { text: '后端开发流程', link: '/mango-pmo/rules/backend/10-dev-flow' },
          { text: '模块菜单初始化归口', link: '/mango-pmo/rules/backend/11-module-menu' }
        ]
      },
      {
        text: '前端与 UI 规范',
        collapsed: true,
        items: [
          { text: 'Vue 代码规范', link: '/mango-pmo/rules/frontend/01-vue-code' },
          { text: 'Element Plus UI 规范', link: '/mango-pmo/rules/frontend/02-element-plus-ui' },
          { text: '前端组件开发规范', link: '/mango-pmo/rules/frontend/03-component-development' },
          { text: '前端测试规范', link: '/mango-pmo/rules/frontend/04-test' },
          { text: '前端开发流程', link: '/mango-pmo/rules/frontend/05-dev-flow' },
          { text: '前端 Monorepo 架构', link: '/mango-pmo/rules/frontend/06-monorepo-architecture' },
          { text: 'Admin UI 通用规范', link: '/mango-pmo/rules/frontend/07-admin-ui-common' },
          { text: '列表页 UI 规范', link: '/mango-pmo/rules/frontend/08-list-page' },
          { text: '详情页 UI 规范', link: '/mango-pmo/rules/frontend/09-detail-page' },
          { text: '表单页 UI 规范', link: '/mango-pmo/rules/frontend/10-form-page' },
          { text: '弹框与抽屉 UI 规范', link: '/mango-pmo/rules/frontend/11-dialog-drawer' }
        ]
      },
      {
        text: '产品规范',
        collapsed: true,
        items: [
          { text: 'PRD 模板规范', link: '/mango-pmo/rules/product/01-prd-template' },
          { text: 'Sprint 规范', link: '/mango-pmo/rules/product/02-sprint' },
          { text: '详细设计模板规范', link: '/mango-pmo/rules/product/03-detailed-design-template' }
        ]
      },
      {
        text: '模板',
        collapsed: true,
        items: [
          { text: '验收证据模板', link: '/mango-pmo/templates/acceptance-evidence' },
          { text: '交付契约模板', link: '/mango-pmo/templates/delivery-contract' },
          { text: 'PRD 模板', link: '/mango-pmo/templates/prd' },
          { text: '详细设计模板', link: '/mango-pmo/templates/detailed-design' },
          { text: '前端入口 README 模板', link: '/mango-pmo/templates/frontend-entry-readme' },
          { text: '模块 README 模板', link: '/mango-pmo/templates/module-readme' }
        ]
      },
      {
        text: 'PMO 归档',
        collapsed: true,
        items: [
          { text: 'Codex 协作复盘', link: '/mango-pmo/tmp/2026-05-28-codex-history-governance-review' }
        ]
      },
      {
        text: 'PMO 工具源码',
        collapsed: true,
        items: [
          { text: 'pmo-preflight', link: `${githubBlobBase}/mango-pmo/tools/pmo-preflight.mjs` },
          { text: 'check-pmo-preflight', link: `${githubBlobBase}/mango-pmo/tools/check-pmo-preflight.mjs` },
          { text: 'check-governance-intent', link: `${githubBlobBase}/mango-pmo/tools/check-governance-intent.mjs` },
          { text: 'check-business-guides', link: `${githubBlobBase}/mango-pmo/tools/check-business-guides.mjs` },
          { text: 'check-capability-docs', link: `${githubBlobBase}/mango-pmo/tools/check-capability-docs.mjs` },
          { text: 'audit-module-readmes', link: `${githubBlobBase}/mango-pmo/tools/audit-module-readmes.mjs` },
          { text: 'audit-readme-source-facts', link: `${githubBlobBase}/mango-pmo/tools/audit-readme-source-facts.mjs` },
          { text: 'delivery-contract-check', link: `${githubBlobBase}/mango-pmo/tools/delivery-contract-check.mjs` },
          { text: 'acceptance-evidence-check', link: `${githubBlobBase}/mango-pmo/tools/acceptance-evidence-check.mjs` }
        ]
      }
    ]
  }
];

const sidebarOrder = [
  '开始',
  '示例场景',
  '产品文档输出',
  '基础能力',
  '平台能力',
  '架构设计',
  'PMO 规范与模板'
];

const orderedSidebar = sidebar
  .map((item, sourceIndex) => ({
    item,
    sourceIndex,
    order: sidebarOrder.indexOf(item.text)
  }))
  .sort((left, right) => {
    const leftOrder = left.order === -1 ? sidebarOrder.length : left.order;
    const rightOrder = right.order === -1 ? sidebarOrder.length : right.order;
    return leftOrder === rightOrder ? left.sourceIndex - right.sourceIndex : leftOrder - rightOrder;
  })
  .map(({ item }) => item);

const versionNavItems = await loadVersionNavItems();
const nav = [
  { text: '开始', link: '/' },
  { text: '示例场景', link: '/mango-docs/guides/business-integration/README' },
  { text: '产品文档输出', link: '/mango-pmo/templates/prd' },
  { text: '基础能力', link: '/mango/mango-infra/mango-infra-context/README' },
  { text: '平台能力', link: '/mango-docs/capabilities/README' },
  { text: '架构设计', link: '/mango-docs/mango-architecture-design' },
  { text: 'PMO 规范', link: '/mango-pmo/rules/00-dev-flow' },
  ...(versionNavItems.length > 0 ? [{ text: docsVersionLabel, items: versionNavItems }] : [])
];

const config = `import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'Mango Docs',
  description: 'Mango business integration documentation',
  base: process.env.VITEPRESS_BASE || '/mango/',
  cleanUrls: true,
  ignoreDeadLinks: [/^https?:\\/\\//],
  themeConfig: {
    nav: ${JSON.stringify(nav, null, 6)},
    sidebar: ${JSON.stringify(orderedSidebar, null, 6)},
    search: {
      provider: 'local'
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/HardyDou/mango' }
    ],
    footer: {
      message: 'Public documentation is generated from a curated whitelist of repository docs.'
    }
  }
});
`;

const index = `# Mango Docs

Mango 是面向 AI Agent 和业务开发者的 Java Spring Boot 业务开发底座，目标是把后端模块、前端后台、权限菜单、初始化数据、PMO 流程和验证规则沉淀成可复用能力，让业务需求可以按统一边界快速落地。

这份公开文档用于说明 Mango 的能力边界、接入方式、基础能力和架构设计。左侧菜单按阅读习惯组织为开始、示例场景、基础能力、平台能力、架构设计和 PMO 规范。

## 推荐入口

| 分类 | 适合场景 | 入口 |
|------|----------|------|
| 示例场景 | 按业务场景接入或排障 | [业务接入场景手册](./mango-docs/guides/business-integration/README.md) |
| 产品文档输出 | 给业务开发输出 PRD、详细设计、交付契约和规范依据 | [PRD 模板](./mango-pmo/templates/prd.md) |
| 基础能力 | 查持久化、事件、KV、Web、装配和业务基线 | [Context 上下文](./mango/mango-infra/mango-infra-context/README.md) |
| 平台能力 | 定位认证、授权、文件、支付、工作流等能力 README | [Mango 能力地图](./mango-docs/capabilities/README.md) |
| 架构设计 | 了解 Mango 定位、架构边界、模块设计和关键取舍 | [Mango 整体架构](./mango-docs/mango-architecture-design.md) |
| PMO 规范 | 查后端、前端、API、UI、交付和文档规范 | [Mango PMO 总流程](./mango-pmo/rules/00-dev-flow.md) |

## 发布边界

GitHub Pages 只发布业务开发需要的公开白名单文档。历史计划、交付证据、内部配置说明和内部过程材料不作为站内文档发布。
`;

const docsIndex = `# Mango 业务开发文档

Mango 是面向 AI Agent 和业务开发者的 Java Spring Boot 业务开发底座，提供后端平台模块、前端后台能力、业务项目基线和 PMO 交付规范。

这里是 Mango 面向业务开发者的公开文档入口，用于快速定位使用说明、业务接入场景、基础能力、平台能力和架构设计。

## 推荐阅读

- [业务接入场景手册](./guides/business-integration/README.md)
- [Mango 能力地图](./capabilities/README.md)
- [PRD 模板](../mango-pmo/templates/prd.md)
- [详细设计模板](../mango-pmo/templates/detailed-design.md)
- [交付契约模板](../mango-pmo/templates/delivery-contract.md)

## 模块使用文档交付

业务开发时看不到模块使用文档，按下面方式解决：

1. 在线阅读统一走 Mango 文档站，入口是 [Mango 能力地图](./capabilities/README.md)。
2. 离线或本地开发时，拉取与依赖版本匹配的 Mango 源码或文档快照，直接阅读模块 README。
3. 本地预览文档站：

\`\`\`bash
npm --prefix mango-docs install
npm --prefix mango-docs run docs:dev
\`\`\`

4. 静态构建文档站：

\`\`\`bash
npm --prefix mango-docs run docs:build
\`\`\`

后端 Maven 运行时 jar 不承载 README；jar 只包含运行所需类和资源。前端 npm 包继续保留包根 \`README.md\`，这是 npm 生态的标准文档入口。

## 发布边界

GitHub Pages 只发布业务开发需要的公开白名单文档。历史计划、交付证据、内部配置说明和内部过程材料不作为站内文档发布。
`;

await rm(stageRoot, { recursive: true, force: true });
await mkdir(resolve(stageRoot, '.vitepress'), { recursive: true });

for (const doc of publicDocs) {
  const source = resolve(repoRoot, doc);
  const target = resolve(stageRoot, doc);
  await mkdir(dirname(target), { recursive: true });
  await cp(source, target);
  if (target.endsWith('.md')) {
    const original = await readFile(target, 'utf8');
    await writeFile(target, rewriteMarkdownLinks(original, doc));
  }
}

await writeFile(resolve(stageRoot, 'index.md'), index);
await writeFile(resolve(stageRoot, 'mango-docs/index.md'), docsIndex);
await writeFile(resolve(stageRoot, '.vitepress/config.mts'), config);

console.log(`Staged ${publicDocs.length} public docs in ${relative(repoRoot, stageRoot)}`);

function rewriteMarkdownLinks(markdown, sourceDoc) {
  return markdown.replace(/(!?)\[([^\]]+)\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g, (match, marker, text, rawHref) => {
    if (isExternalOrAnchor(rawHref)) {
      return match;
    }

    const [hrefPath, hash = ''] = rawHref.split('#');
    if (!hrefPath) {
      return match;
    }

    const resolved = resolveMarkdownTarget(sourceDoc, hrefPath);
    if (!resolved) {
      return match;
    }

    const stagedTarget = findStagedDocTarget(resolved.repoPath);
    if (stagedTarget) {
      const nextHref = relativeLink(dirname(sourceDoc), stagedTarget, hash);
      return `${marker}[${text}](${nextHref})`;
    }

    const sourceHref = `${githubBlobBase}/${encodeURI(resolved.repoPath)}${hash ? `#${hash}` : ''}`;
    return `${marker}[${text}](${sourceHref})`;
  });
}

function resolveMarkdownTarget(sourceDoc, hrefPath) {
  const absolute = resolve(repoRoot, dirname(sourceDoc), hrefPath);
  const repoPath = normalizePath(relative(repoRoot, absolute));

  if (repoPath.startsWith('../')) {
    return null;
  }

  return { repoPath };
}

function findStagedDocTarget(repoPath) {
  const candidates = candidateDocPaths(repoPath);
  return candidates.find((candidate) => publicDocSet.has(candidate));
}

function candidateDocPaths(repoPath) {
  const clean = repoPath.replace(/\/$/, '');
  const ext = extname(clean);

  if (ext) {
    return [normalizePath(clean)];
  }

  return [
    `${normalizePath(clean)}.md`,
    `${normalizePath(clean)}/README.md`,
    `${normalizePath(clean)}/index.md`
  ];
}

function relativeLink(fromDir, toDoc, hash) {
  let link = normalizePath(relative(fromDir, toDoc));
  if (!link.startsWith('.')) {
    link = `./${link}`;
  }
  return `${link}${hash ? `#${hash}` : ''}`;
}

function isExternalOrAnchor(href) {
  return /^(https?:|mailto:|tel:|#)/.test(href);
}

function normalizePath(path) {
  return path.split('\\').join('/');
}

async function loadVersionNavItems() {
  try {
    const manifest = JSON.parse(await readFile(versionsManifestPath, 'utf8'));
    const versions = Array.isArray(manifest.versions) ? manifest.versions : [];
    const items = [
      {
        text: manifest.latest?.label || latestDocsLabel,
        link: toPublicDocsLink(manifest.latest?.path || '/')
      },
      ...versions
        .filter((version) => version && typeof version.version === 'string')
        .map((version) => ({
          text: version.label || version.version,
          link: toPublicDocsLink(version.path || `/versions/${version.version}/`)
        }))
    ];

    return items.length > 1 ? items : [];
  } catch (error) {
    if (error?.code === 'ENOENT') {
      return [];
    }
    throw error;
  }
}

function toPublicDocsLink(path) {
  return new URL(path.replace(/^\/+/, ''), docsPublicBase).toString();
}

function normalizePublicBase(base) {
  const normalized = base.endsWith('/') ? base : `${base}/`;
  return new URL(normalized).toString();
}
