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
      { text: '业务接入', link: '/mango-docs/guides/business-integration/README' },
      { text: '能力地图', link: '/mango-docs/capabilities/README' }
    ],
    sidebar: [
      {
        text: '业务开发入口',
        items: [
          { text: '文档首页', link: '/' },
          { text: '业务接入场景手册', link: '/mango-docs/guides/business-integration/README' },
          { text: 'Mango 能力地图', link: '/mango-docs/capabilities/README' }
        ]
      },
      {
        text: '场景手册',
        items: [
          { text: '文件上传表单', link: '/mango-docs/guides/business-integration/file-upload-form' },
          { text: '业务审批接入', link: '/mango-docs/guides/business-integration/workflow-business-approval' },
          { text: '菜单页面打不开排障', link: '/mango-docs/guides/business-integration/rbac-menu-page-troubleshooting' },
          { text: '按钮权限不显示排障', link: '/mango-docs/guides/business-integration/permission-button-troubleshooting' },
          { text: '租户字典配置为空排障', link: '/mango-docs/guides/business-integration/tenant-dict-config-empty' }
        ]
      }
    ],
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

const index = `---
layout: home

hero:
  name: Mango Docs
  text: 业务开发文档入口
  tagline: 面向业务开发者的 Mango 接入、能力定位和排障手册。
  actions:
    - theme: brand
      text: 业务接入场景
      link: /mango-docs/guides/business-integration/README
    - theme: alt
      text: 能力地图
      link: /mango-docs/capabilities/README

features:
  - title: 接入场景
    details: 文件上传、审批、菜单、按钮权限、租户基础数据等业务开发常见路径。
  - title: 能力定位
    details: 从业务目标定位后端模块、前端包、README 和验证入口。
  - title: 公开白名单
    details: GitHub Pages 只发布业务开发需要的文档，不发布 evidence、plans 和内部历史材料。
---
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
