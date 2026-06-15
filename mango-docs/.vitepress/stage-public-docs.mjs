import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { dirname, extname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const docsRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(docsRoot, '..');
const stageRoot = resolve(docsRoot, '.vitepress/public-src');
const githubBlobBase = process.env.MANGO_DOCS_GITHUB_BLOB_BASE || 'https://github.com/HardyDou/mango/blob/main';

const publicDocs = [
  'mango-docs/index.md',
  'mango-docs/capabilities/README.md',
  'mango-docs/guides/business-integration/README.md',
  'mango-docs/guides/business-integration/file-upload-form.md',
  'mango-docs/guides/business-integration/permission-button-troubleshooting.md',
  'mango-docs/guides/business-integration/rbac-menu-page-troubleshooting.md',
  'mango-docs/guides/business-integration/tenant-dict-config-empty.md',
  'mango-docs/guides/business-integration/workflow-business-approval.md',
  'mango-docs/designs/mango-domain-event-transparent-delivery-design.md',
  'mango-docs/designs/mango-file集成file-preview方案.md',
  'mango-docs/designs/mango-multi-datasource-foundation-design.md',
  'mango-docs/designs/mango-native-job-engine-design.md',
  'mango-docs/designs/mango-notice多渠道通知中心设计说明书.md',
  'mango-docs/designs/统一支付系统设计说明书.md',
  'mango-pmo/rules/03-ai-coding-redlines.md',
  'mango-pmo/rules/05-ai-delivery-quality.md',
  'mango-pmo/rules/06-document-assets.md',
  'mango-pmo/rules/08-capability-docs.md',
  'mango-pmo/rules/backend/01-code.md',
  'mango-pmo/rules/backend/03-api.md',
  'mango-pmo/rules/frontend/01-vue-code.md',
  'mango-pmo/templates/module-readme.md',
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
  'mango/mango-platform/mango-identity/README.md',
  'mango/mango-platform/mango-job/README.md',
  'mango/mango-platform/mango-notice/README.md',
  'mango/mango-platform/mango-numgen/README.md',
  'mango/mango-platform/mango-org/README.md',
  'mango/mango-platform/mango-payment/README.md',
  'mango/mango-platform/mango-seed/README.md',
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
    text: '首页',
    collapsed: false,
    items: [
      { text: '文档首页', link: '/' },
      { text: '业务接入场景手册', link: '/mango-docs/guides/business-integration/README' },
      { text: 'Mango 能力地图', link: '/mango-docs/capabilities/README' }
    ]
  },
  {
    text: '架构设计',
    collapsed: false,
    items: [
      { text: '领域事件透明投递', link: '/mango-docs/designs/mango-domain-event-transparent-delivery-design' },
      { text: '文件预览集成', link: '/mango-docs/designs/mango-file集成file-preview方案' },
      { text: '多数据源底座', link: '/mango-docs/designs/mango-multi-datasource-foundation-design' },
      { text: '原生 Job Engine', link: '/mango-docs/designs/mango-native-job-engine-design' },
      { text: '通知中心', link: '/mango-docs/designs/mango-notice多渠道通知中心设计说明书' },
      { text: '统一支付系统', link: '/mango-docs/designs/统一支付系统设计说明书' }
    ]
  },
  {
    text: '代码规范',
    collapsed: false,
    items: [
      { text: 'AI 编码红线', link: '/mango-pmo/rules/03-ai-coding-redlines' },
      { text: 'AI 交付质量门禁', link: '/mango-pmo/rules/05-ai-delivery-quality' },
      {
        text: '后端规范',
        collapsed: false,
        items: [
          { text: '后端代码规范', link: '/mango-pmo/rules/backend/01-code' },
          { text: '后端 API 规范', link: '/mango-pmo/rules/backend/03-api' }
        ]
      },
      {
        text: '前端规范',
        collapsed: false,
        items: [
          { text: 'Vue 代码规范', link: '/mango-pmo/rules/frontend/01-vue-code' }
        ]
      },
      {
        text: '文档规范',
        collapsed: true,
        items: [
          { text: '文档资产边界', link: '/mango-pmo/rules/06-document-assets' },
          { text: '能力说明维护', link: '/mango-pmo/rules/08-capability-docs' },
          { text: '模块 README 模板', link: '/mango-pmo/templates/module-readme' }
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
          { text: 'Identity 身份', link: '/mango/mango-platform/mango-identity/README' },
          { text: 'Job 任务调度', link: '/mango/mango-platform/mango-job/README' },
          { text: 'Notice 通知', link: '/mango/mango-platform/mango-notice/README' },
          { text: 'Numgen 编号', link: '/mango/mango-platform/mango-numgen/README' },
          { text: 'Org 组织', link: '/mango/mango-platform/mango-org/README' },
          { text: 'Payment 支付', link: '/mango/mango-platform/mango-payment/README' },
          { text: 'Seed 初始化', link: '/mango/mango-platform/mango-seed/README' },
          { text: 'System 系统', link: '/mango/mango-platform/mango-system/README' },
          { text: 'Template 模板', link: '/mango/mango-platform/mango-template/README' },
          { text: 'Workflow 工作流', link: '/mango/mango-platform/mango-workflow/README' }
        ]
      },
      {
        text: '前端平台能力',
        collapsed: true,
        items: [
          { text: 'Admin 单体管理端', link: '/mango-ui/packages/admin/README' },
          { text: 'Admin Pages 页面注册表', link: '/mango-ui/packages/admin-pages/README' },
          { text: 'Admin Shell', link: '/mango-ui/packages/admin-shell/README' },
          { text: 'API Schema', link: '/mango-ui/packages/api-schema/README' },
          { text: 'App Runtime', link: '/mango-ui/packages/app-runtime/README' },
          { text: 'Auth 认证前端', link: '/mango-ui/packages/auth/README' },
          { text: 'Auth Views', link: '/mango-ui/packages/auth/src/views/README' },
          { text: 'Calendar 日历前端', link: '/mango-ui/packages/calendar/README' },
          { text: 'File 文件前端', link: '/mango-ui/packages/file/README' },
          { text: 'File Components', link: '/mango-ui/packages/file/src/components/README' },
          { text: 'Job 任务前端', link: '/mango-ui/packages/job/README' },
          { text: 'Job Views', link: '/mango-ui/packages/job/src/views/README' },
          { text: 'Notice 通知前端', link: '/mango-ui/packages/notice/README' },
          { text: 'Numgen 编号前端', link: '/mango-ui/packages/numgen/README' },
          { text: 'Payment 支付前端', link: '/mango-ui/packages/payment/README' },
          { text: 'RBAC API', link: '/mango-ui/packages/rbac/README' },
          { text: 'RBAC Views', link: '/mango-ui/packages/rbac/src/views/README' },
          { text: 'System 系统前端', link: '/mango-ui/packages/system/README' },
          { text: 'System Components', link: '/mango-ui/packages/system/src/components/README' },
          { text: 'Template 模板前端', link: '/mango-ui/packages/template/README' },
          { text: 'Workflow 工作流前端', link: '/mango-ui/packages/workflow/README' },
          { text: 'Workflow Components', link: '/mango-ui/packages/workflow/src/components/README' },
          { text: 'Workflow Business Example', link: '/mango-ui/packages/workflow-business-example/README' }
        ]
      }
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
    text: '场景示例',
    collapsed: false,
    items: [
      { text: '业务接入总览', link: '/mango-docs/guides/business-integration/README' },
      { text: '文件上传表单', link: '/mango-docs/guides/business-integration/file-upload-form' },
      { text: '业务审批接入', link: '/mango-docs/guides/business-integration/workflow-business-approval' },
      { text: '菜单页面打不开排障', link: '/mango-docs/guides/business-integration/rbac-menu-page-troubleshooting' },
      { text: '按钮权限不显示排障', link: '/mango-docs/guides/business-integration/permission-button-troubleshooting' },
      { text: '租户字典配置为空排障', link: '/mango-docs/guides/business-integration/tenant-dict-config-empty' }
    ]
  }
];

const config = `import { defineConfig } from 'vitepress';

export default defineConfig({
  title: 'Mango Docs',
  description: 'Mango business integration documentation',
  base: process.env.VITEPRESS_BASE || '/mango/',
  cleanUrls: true,
  ignoreDeadLinks: [/^https?:\\/\\//],
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '架构设计', link: '/mango-docs/designs/mango-native-job-engine-design' },
      { text: '代码规范', link: '/mango-pmo/rules/backend/01-code' },
      { text: '平台能力', link: '/mango-docs/capabilities/README' },
      { text: '场景示例', link: '/mango-docs/guides/business-integration/README' }
    ],
    sidebar: ${JSON.stringify(sidebar, null, 6)},
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

面向业务开发者的 Mango 文档入口。左侧菜单按阅读习惯组织为首页、架构设计、代码规范、平台能力、基础能力和场景示例。

## 推荐入口

| 分类 | 适合场景 | 入口 |
|------|----------|------|
| 架构设计 | 了解能力边界、模块设计和关键取舍 | [原生 Job Engine](./mango-docs/designs/mango-native-job-engine-design.md) |
| 代码规范 | 查后端、前端、API、交付和文档规范 | [后端代码规范](./mango-pmo/rules/backend/01-code.md) |
| 平台能力 | 定位认证、授权、文件、支付、工作流等能力 README | [Mango 能力地图](./mango-docs/capabilities/README.md) |
| 基础能力 | 查持久化、事件、KV、Web、装配和业务基线 | [多数据源底座](./mango-docs/designs/mango-multi-datasource-foundation-design.md) |
| 场景示例 | 按业务场景接入或排障 | [业务接入场景手册](./mango-docs/guides/business-integration/README.md) |

## 发布边界

GitHub Pages 只发布业务开发需要的公开白名单文档。历史计划、交付证据、内部配置说明和内部过程材料不作为站内文档发布。
`;

const docsIndex = `# Mango 业务开发文档

这里是 Mango 面向业务开发者的公开文档入口。

## 推荐阅读

- [业务接入场景手册](./guides/business-integration/README.md)
- [Mango 能力地图](./capabilities/README.md)

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
