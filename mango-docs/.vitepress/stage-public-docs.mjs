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
const docsVersionLabel = formatVersionLabel(process.env.MANGO_DOCS_VERSION_LABEL || latestDocsLabel);

const publicDocs = [
  'mango-docs/index.md',
  'mango-docs/README.md',
  'mango-docs/capabilities/README.md',
  'mango-docs/mango-architecture-design.md',
  'mango-docs/mango-backend-architecture-boundary-refactor-master-plan.md',
  'mango-docs/guides/business-integration/README.md',
  'mango-docs/guides/business-integration/file-upload-form.md',
  'mango-docs/guides/business-integration/permission-button-display-rule.md',
  'mango-docs/guides/business-integration/workflow-business-approval.md',
  'mango-docs/guides/faq/README.md',
  'mango-docs/guides/faq/permission-button-troubleshooting.md',
  'mango-docs/guides/faq/rbac-menu-page-troubleshooting.md',
  'mango-docs/guides/faq/tenant-dict-config-empty.md',
  'mango-docs/guides/operations/README.md',
  'mango-docs/guides/operations/build-time-cold-baseline.md',
  'mango-docs/guides/operations/ci-cd-release-practices.md',
  'mango-docs/guides/operations/history-debt-remediation.md',
  'mango-docs/guides/operations/mango-1.0.30-to-1.0.31-upgrade.md',
  'mango-docs/guides/operations/maven-1.0.21-to-1.0.22-java-api-upgrade.md',
  'mango-docs/guides/operations/resource-reset-incremental-release.md',
  'mango-docs/guides/operations/workflow-assignee-identity-upgrade.md',
  'mango-docs/changelog/README.md',
  'mango-docs/changelog/business-integration/file-upload-form.md',
  'mango-docs/changelog/business-integration/permission-button-troubleshooting.md',
  'mango-docs/changelog/business-integration/rbac-menu-page-troubleshooting.md',
  'mango-docs/changelog/business-integration/tenant-dict-config-empty.md',
  'mango-docs/changelog/business-integration/workflow-business-approval.md',
  'mango-docs/guides/understanding-first/README.md',
  'mango-docs/guides/understanding-first/resource-registry-quickstart.md',
  'mango-docs/guides/understanding-first/file-upload-preview-quickstart.md',
  'mango-docs/guides/understanding-first/workflow-approval-quickstart.md',
  'mango-docs/designs/business-project-development-guide.md',
  'mango-docs/designs/mango-capability-usage-guide-for-ai.md',
  'mango-docs/designs/2026-07-13-module-architecture-debt-governance-design.md',
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
  'mango-pmo/rules/13-agent-text-output.md',
  'mango-pmo/rules/14-decision-expert-review.md',
  'mango-pmo/baselines/architecture/README.md',
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
  'mango-pmo/rules/product/02-sprint.md',
  'mango-pmo/rules/product/03-detailed-design-template.md',
  'mango-pmo/rules/product/05-document-lifecycle.md',
  'mango-pmo/templates/acceptance-evidence.md',
  'mango-pmo/templates/README.md',
  'mango-pmo/templates/business-requirements.md',
  'mango-pmo/templates/delivery-contract.md',
  'mango-pmo/templates/decision-review.md',
  'mango-pmo/templates/implementation-plan.md',
  'mango-pmo/templates/retrospective.md',
  'mango-pmo/templates/system-requirements.md',
  'mango-pmo/templates/technical-design.md',
  'mango-pmo/templates/detailed-design.md',
  'mango-pmo/templates/frontend-entry-readme.md',
  'mango-pmo/templates/module-readme.md',
  'mango-pmo/tools/check-comprehension-evaluation.mjs',
  'mango-pmo/tools/check-decision-review.mjs',
  'mango-pmo/tools/check-document-reading-entry.mjs',
  'mango-pmo/tools/check-readable-module-readmes.mjs',
  'mango-pmo/tools/check-retrospective.mjs',
  'mango-pmo/tools/create-comprehension-evaluation-packet.mjs',
  'mango-pmo/tools/render-implementation-plan-graph.mjs',
  'mango-pmo/tools/render-rule-action-card.mjs',
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
  'mango/mango-platform/mango-cms/README.md',
  'mango/mango-platform/mango-domain/README.md',
  'mango/mango-platform/mango-file/README.md',
  'mango/mango-platform/mango-file-preview/README.md',
  'mango/mango-platform/mango-grid-layout/README.md',
  'mango/mango-platform/mango-home/README.md',
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
      { text: 'Mango 能力地图', link: '/mango-docs/capabilities/README' },
      { text: '业务接入场景', link: '/mango-docs/guides/business-integration/README' },
      { text: '常见问题与排障', link: '/mango-docs/guides/faq/README' },
      { text: '运维、升级与交付', link: '/mango-docs/guides/operations/README' },
      { text: '历史变更索引', link: '/mango-docs/changelog/README' }
    ]
  },
  {
    text: '业务接入',
    collapsed: false,
    items: [
      { text: '接入总览', link: '/mango-docs/guides/business-integration/README' },
      { text: '文件上传表单', link: '/mango-docs/guides/business-integration/file-upload-form' },
      { text: '业务审批接入', link: '/mango-docs/guides/business-integration/workflow-business-approval' },
      { text: '按钮展示规则', link: '/mango-docs/guides/business-integration/permission-button-display-rule' }
    ]
  },
  {
    text: '常见问题与排障',
    collapsed: false,
    items: [
      { text: '排障总览', link: '/mango-docs/guides/faq/README' },
      { text: '菜单页面打不开', link: '/mango-docs/guides/faq/rbac-menu-page-troubleshooting' },
      { text: '按钮权限不显示', link: '/mango-docs/guides/faq/permission-button-troubleshooting' },
      { text: '租户字典配置为空', link: '/mango-docs/guides/faq/tenant-dict-config-empty' }
    ]
  },
  {
    text: '运维、升级与交付',
    collapsed: true,
    items: [
      { text: '运维总览', link: '/mango-docs/guides/operations/README' },
      { text: 'CI/CD 发布实践', link: '/mango-docs/guides/operations/ci-cd-release-practices' },
      { text: '构建期 cold baseline', link: '/mango-docs/guides/operations/build-time-cold-baseline' },
      { text: 'Resource 重置与增量发布', link: '/mango-docs/guides/operations/resource-reset-incremental-release' },
      { text: 'Mango 1.0.31 升级', link: '/mango-docs/guides/operations/mango-1.0.30-to-1.0.31-upgrade' },
      { text: 'Maven Java API 升级', link: '/mango-docs/guides/operations/maven-1.0.21-to-1.0.22-java-api-upgrade' },
      { text: 'Workflow 办理人升级', link: '/mango-docs/guides/operations/workflow-assignee-identity-upgrade' },
      { text: '历史债务修复', link: '/mango-docs/guides/operations/history-debt-remediation' }
    ]
  },
  {
    text: '历史变更',
    collapsed: true,
    items: [
      { text: '业务接入变更索引', link: '/mango-docs/changelog/README' },
      { text: '文件上传变更', link: '/mango-docs/changelog/business-integration/file-upload-form' },
      { text: '业务审批变更', link: '/mango-docs/changelog/business-integration/workflow-business-approval' }
    ]
  },
  {
    text: '产品文档输出',
    collapsed: false,
    items: [
      { text: 'Understanding-first 能力入口', link: '/mango-docs/guides/understanding-first/README' },
      { text: '模板选择页', link: '/mango-pmo/templates/README' },
      { text: 'BRD 业务需求模板', link: '/mango-pmo/templates/business-requirements' },
      { text: 'SRS 系统需求模板', link: '/mango-pmo/templates/system-requirements' },
      { text: 'TDD 技术设计模板', link: '/mango-pmo/templates/technical-design' },
      { text: 'Implementation Plan 实施计划', link: '/mango-pmo/templates/implementation-plan' },
      { text: '文档生命周期规则', link: '/mango-pmo/rules/product/05-document-lifecycle' },
      { text: '交付契约模板', link: '/mango-pmo/templates/delivery-contract' },
      { text: '交付复盘模板', link: '/mango-pmo/templates/retrospective' }
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
          { text: 'CMS 内容管理', link: '/mango/mango-platform/mango-cms/README' },
          { text: 'Domain 业务域', link: '/mango/mango-platform/mango-domain/README' },
          { text: 'File 文件', link: '/mango/mango-platform/mango-file/README' },
          { text: 'File Preview 文件预览', link: '/mango/mango-platform/mango-file-preview/README' },
          { text: 'Grid Layout 自定义栅格布局', link: '/mango/mango-platform/mango-grid-layout/README' },
          { text: 'Home 用户首页工作台', link: '/mango/mango-platform/mango-home/README' },
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
      { text: '业务项目开发说明', link: '/mango-docs/designs/business-project-development-guide' },
      { text: 'Mango 能力使用指南', link: '/mango-docs/designs/mango-capability-usage-guide-for-ai' },
      { text: '模块架构债务治理', link: '/mango-docs/designs/2026-07-13-module-architecture-debt-governance-design' },
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
          { text: '架构债务递减预算', link: '/mango-pmo/baselines/architecture/README' },
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
          { text: '需求文档生命周期', link: '/mango-pmo/rules/product/05-document-lifecycle' },
          { text: 'Sprint 规范', link: '/mango-pmo/rules/product/02-sprint' },
          { text: '详细设计模板规范', link: '/mango-pmo/rules/product/03-detailed-design-template' }
        ]
      },
      {
        text: '模板',
        collapsed: true,
        items: [
          { text: '模板选择页', link: '/mango-pmo/templates/README' },
          { text: 'BRD 业务需求模板', link: '/mango-pmo/templates/business-requirements' },
          { text: 'SRS 系统需求模板', link: '/mango-pmo/templates/system-requirements' },
          { text: 'TDD 技术设计模板', link: '/mango-pmo/templates/technical-design' },
          { text: '实施计划模板', link: '/mango-pmo/templates/implementation-plan' },
          { text: '验收证据模板', link: '/mango-pmo/templates/acceptance-evidence' },
          { text: '交付契约模板', link: '/mango-pmo/templates/delivery-contract' },
          { text: '详细设计模板', link: '/mango-pmo/templates/detailed-design' },
          { text: '前端入口 README 模板', link: '/mango-pmo/templates/frontend-entry-readme' },
          { text: '模块 README 模板', link: '/mango-pmo/templates/module-readme' }
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
  '业务接入',
  '常见问题与排障',
  '运维、升级与交付',
  '历史变更',
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
  { text: '业务接入', link: '/mango-docs/guides/business-integration/README' },
  { text: 'FAQ', link: '/mango-docs/guides/faq/README' },
  { text: '运维交付', link: '/mango-docs/guides/operations/README' },
  { text: '产品文档输出', link: '/mango-docs/guides/understanding-first/README' },
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

Mango 是面向业务开发者、架构师和交付人员的业务系统研发底座。

先按任务进入文档：

| 目标 | 入口 |
|---|---|
| 了解 Mango 有哪些能力模块 | [能力地图](./mango-docs/capabilities/README.md) |
| 接入文件、审批或按钮能力 | [业务接入场景](./mango-docs/guides/business-integration/README.md) |
| 菜单、按钮或租户数据异常 | [常见问题与排障](./mango-docs/guides/faq/README.md) |
| 构建、升级、发布业务项目 | [运维、升级与交付](./mango-docs/guides/operations/README.md) |
| 选择 BRD、SRS、TDD 或 Plan | [产品文档模板选择](./mango-pmo/templates/README.md) |
| 创建业务项目 | [Business Starter](./mango-business-starter/README.md) |
| 了解架构边界 | [Mango 架构设计](./mango-docs/mango-architecture-design.md) |
| 执行研发流程和门禁 | [PMO 总流程](./mango-pmo/rules/00-dev-flow.md) |

当前入口负责现在怎么做；模块 README 负责能力事实；设计、计划、证据、CHANGELOG 和历史索引负责追溯。
`;

const docsIndex = `# Mango 文档目录

这里按读者任务和文档职责组织入口，不按源码目录堆叠内容。

| 章节 | 内容 |
|---|---|
| 开始 | 定位、阅读顺序和入口选择 |
| 能力地图 | 模块职责、边界、源码和最小使用路径 |
| 业务接入 | 文件、审批和按钮能力的当前接入步骤 |
| FAQ 与排障 | 菜单、按钮、租户和基础数据异常定位 |
| 运维、升级与交付 | 构建、发布、升级、回滚和债务治理 |
| 产品文档输出 | BRD、SRS、TDD、Plan 和交付证据 |
| 架构设计 | 分层、边界和设计决策 |
| 历史变更 | CHANGELOG、设计、计划和验收证据索引 |
| PMO 规范与模板 | 研发规则、角色、模板和检查工具 |

常用入口：

- [能力地图](./capabilities/README.md)
- [业务接入场景](./guides/business-integration/README.md)
- [常见问题与排障](./guides/faq/README.md)
- [运维、升级与交付](./guides/operations/README.md)
- [业务接入历史变更](./changelog/README.md)
- [产品文档模板选择](../mango-pmo/templates/README.md)
- [Understanding-first 能力入口](./guides/understanding-first/README.md)
- [Mango 架构设计](./mango-architecture-design.md)
- [PMO 总流程](../mango-pmo/rules/00-dev-flow.md)

模块 README 是能力事实来源；当前指南是任务入口；历史计划、变更和证据只用于追溯。
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
    const staged = sanitizeInternalRepositoryAddresses(rewriteMarkdownLinks(original, doc));
    assertNoInternalRepositoryAddress(staged, doc);
    await writeFile(target, staged);
  }
}

// Preserve these published URLs without duplicating FAQ content or adding old pages to navigation.
// Relative targets keep both project Pages bases and version snapshot bases intact.
const legacyFaqSlugs = [
  'permission-button-troubleshooting',
  'rbac-menu-page-troubleshooting',
  'tenant-dict-config-empty'
];
for (const slug of legacyFaqSlugs) {
  const currentDoc = `mango-docs/guides/faq/${slug}.md`;
  if (!publicDocSet.has(currentDoc)) throw new Error(`FAQ redirect target is not public: ${currentDoc}`);
  const targetHref = `../faq/${slug}.html`;
  const redirectFile = resolve(stageRoot, 'public/mango-docs/guides/business-integration', `${slug}.html`);
  await mkdir(dirname(redirectFile), { recursive: true });
  await writeFile(redirectFile, `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>排障文档已迁移</title>
<meta name="robots" content="noindex">
<link rel="canonical" href="${targetHref}">
<meta http-equiv="refresh" content="0;url=${targetHref}">
<script>
const target = new URL(${JSON.stringify(targetHref)}, window.location.href);
target.search = window.location.search;
target.hash = window.location.hash;
window.location.replace(target.href);
</script>
</head>
<body><p>本页已迁移至 FAQ。<a href="${targetHref}">打开当前排障文档</a></p></body>
</html>
`);
}

await writeFile(resolve(stageRoot, 'index.md'), index);
await writeFile(resolve(stageRoot, 'mango-docs/index.md'), docsIndex);
await writeFile(resolve(stageRoot, '.vitepress/config.mts'), config);

console.log(`Staged ${publicDocs.length} public docs and ${legacyFaqSlugs.length} FAQ redirects in ${relative(repoRoot, stageRoot)}`);

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

function sanitizeInternalRepositoryAddresses(markdown) {
  return markdown.replace(
    /https?:\/\/[^/\s)\]}>"']*\.inner\.[^/\s)\]}>"']*(\/repository\/[^\s)\]}>"']*)/giu,
    'https://registry.example.invalid$1'
  );
}

function assertNoInternalRepositoryAddress(markdown, sourceDoc) {
  if (/https?:\/\/[^/\s)\]}>"']*\.inner\.[^/\s)\]}>"']*\/repository\//iu.test(markdown)) {
    throw new Error(`Public documentation still contains an internal repository address: ${sourceDoc}`);
  }
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
          text: formatVersionLabel(version.label || version.version),
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

function formatVersionLabel(label) {
  const match = String(label).match(/(?:^|-)maven-(\d+\.\d+\.\d+)(?:-|$)/);
  return match ? match[1] : label;
}

function normalizePublicBase(base) {
  const normalized = base.endsWith('/') ? base : `${base}/`;
  return new URL(normalized).toString();
}
