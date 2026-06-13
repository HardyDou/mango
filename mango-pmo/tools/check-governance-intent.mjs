#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assertIncludes(file, patterns, failures) {
  const text = read(file);
  for (const pattern of patterns) {
    if (!text.includes(pattern)) {
      failures.push(`${file}: missing expected text "${pattern}"`);
    }
  }
}

function assertNotIncludes(file, patterns, failures) {
  const text = read(file);
  for (const pattern of patterns) {
    if (text.includes(pattern)) {
      failures.push(`${file}: should not contain "${pattern}"`);
    }
  }
}

function assertNoLongTermRuleLanguage(file, failures) {
  const allowedLines = [
    '长期规范只维护在 `mango-pmo/rules/**`',
    '长期前端规则只维护在 `mango-pmo/rules/frontend/**`',
    '长期规则仍以 `mango-pmo` 为唯一来源；本文只做能力索引，不复制规范正文。'
  ];
  const ruleTerms = ['必须', '禁止', '不允许', '不得', '不应', '只允许'];
  const lines = read(file).split(/\r?\n/);
  lines.forEach((line, index) => {
    if (allowedLines.includes(line.trim())) {
      return;
    }
    for (const term of ruleTerms) {
      if (line.includes(term)) {
        failures.push(`${file}:${index + 1}: entry README should link PMO rules instead of carrying long-term rule wording "${term}"`);
      }
    }
  });
}

const failures = [];

assertIncludes('AGENTS.md', [
  'PMO preflight 的首要目的不是审批命令',
  '完整触发边界见 [Mango PMO 总流程]',
  'git pull --ff-only',
  '低风险操作过程中需要人工改文件、解决冲突、修复问题或形成交付结论'
], failures);

assertIncludes('mango-pmo/rules/00-dev-flow.md', [
  'Agent 必须先识别用户意图和规范遗漏风险',
  '纯同步仓库',
  '执行纯仓库同步命令，例如 `git fetch`、`git pull --ff-only`、`git remote update`',
  '处理 `git pull`、`rebase`、`merge` 产生的冲突'
], failures);

assertIncludes('mango-pmo/rules/08-capability-docs.md', [
  '改变 API、配置项、注解、事件、菜单、权限、租户、数据源或初始化数据',
  '模块具体用法更新到对应模块 `README.md`',
  '能力索引更新到 `mango-docs/capabilities/README.md`',
  '未更新时说明原因',
  '禁止把能力地图写成第二套规范源'
], failures);

assertIncludes('mango-docs/capabilities/README.md', [
  '本文只做能力索引，不复制规范正文',
  '正式交付规则以 preflight 输出和 `mango-pmo/rules/**` 为准',
  '[验证方式](../../mango/mango-platform/mango-auth/README.md#10-验证方式)'
], failures);
assertNotIncludes('mango-docs/capabilities/README.md', ['必须', '禁止'], failures);

for (const entryReadme of ['README.md', 'mango/README.md', 'mango-ui/README.md', 'mango-business-starter/README.md']) {
  assertNoLongTermRuleLanguage(entryReadme, failures);
}

for (const frontendEntryReadme of [
  'mango-ui/packages/auth/src/views/README.md',
  'mango-ui/packages/file/src/components/README.md',
  'mango-ui/packages/job/src/views/README.md',
  'mango-ui/packages/rbac/src/views/README.md',
  'mango-ui/packages/system/src/components/README.md',
  'mango-ui/packages/workflow/src/components/README.md'
]) {
  assertNoLongTermRuleLanguage(frontendEntryReadme, failures);
}

assertIncludes('mango-pmo/templates/module-readme.md', [
  '只写本模块的具体表、数据、资源和入口',
  '只写本模块的资源归属、默认授权和租户边界事实',
  '只写本模块可执行的验证命令或验收入口',
  '链接相关 `mango-pmo/rules/**` 规则源，不复制规则正文'
], failures);

assertIncludes('.github/pull_request_template.md', [
  '## PMO / Scope',
  '## Capability Docs',
  'Not applicable reason',
  '## Validation',
  '## PMO Exceptions'
], failures);

assertIncludes('.github/workflows/pmo-doc-check.yml', [
  'node mango-pmo/tools/check-governance-intent.mjs',
  'node mango-pmo/tools/audit-module-readmes.mjs',
  'github.event.pull_request.base.sha',
  'github.event.pull_request.head.sha',
  'PR_BODY_FILE'
], failures);

const index = JSON.parse(read('mango-pmo/rules/index.json'));
if (!index.rules?.['process.capabilityDocs']) {
  failures.push('mango-pmo/rules/index.json: missing process.capabilityDocs');
}
if (!index.roles?.pmo?.includes('process.capabilityDocs')) {
  failures.push('mango-pmo/rules/index.json: pmo role must include process.capabilityDocs');
}
if (!index.phases?.governance?.includes('process.capabilityDocs')) {
  failures.push('mango-pmo/rules/index.json: governance phase must include process.capabilityDocs');
}
const capabilityBundle = index.bundles?.capabilityDocs;
if (!capabilityBundle) {
  failures.push('mango-pmo/rules/index.json: missing capabilityDocs bundle');
} else {
  for (const expectedPath of ['mango-docs/capabilities/**', 'mango/**/README.md', 'mango/**/src/**', 'mango-ui/packages/**/src/**', 'mango-business-starter/**']) {
    if (!capabilityBundle.paths?.includes(expectedPath)) {
      failures.push(`mango-pmo/rules/index.json: capabilityDocs bundle missing path ${expectedPath}`);
    }
  }
  for (const expectedRule of ['process.capabilityDocs', 'process.documentAssets']) {
    if (!capabilityBundle.include?.includes(expectedRule)) {
      failures.push(`mango-pmo/rules/index.json: capabilityDocs bundle missing include ${expectedRule}`);
    }
  }
}

if (failures.length > 0) {
  console.error(`Governance intent check failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  process.exit(1);
}

console.log('Governance intent checks passed');
