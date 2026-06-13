#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const args = process.argv.slice(2);
const baseArgIndex = args.indexOf('--base');
const headArgIndex = args.indexOf('--head');
const baseRef = baseArgIndex >= 0 ? args[baseArgIndex + 1] : process.env.GITHUB_BASE_REF || 'HEAD~1';
const headRef = headArgIndex >= 0 ? args[headArgIndex + 1] : process.env.GITHUB_SHA || 'HEAD';
const prBodyPath = process.env.PR_BODY_FILE || '';
const requiredPrSections = ['## PMO / Scope', '## Capability Docs', '## Validation', '## PMO Exceptions'];
const selfTest = args.includes('--self-test');
const topLevelBackendModules = new Set([
  'mango-admin-starter',
  'mango-app',
  'mango-common',
  'mango-extension',
  'mango-parent',
  'mango-tools'
]);

function fileExists(relativePath) {
  return fs.existsSync(path.join(root, relativePath));
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function gitOutput(args) {
  return execFileSync('git', args, {
    cwd: root,
    encoding: 'utf8'
  });
}

function changedFiles() {
  const names = new Set();
  try {
    const output = gitOutput(['diff', '--name-only', `${baseRef}...${headRef}`]);
    for (const line of output.split('\n').map((item) => item.trim()).filter(Boolean)) {
      names.add(line);
    }
  } catch {
    if (process.env.GITHUB_ACTIONS) {
      throw new Error(`Unable to diff ${baseRef}...${headRef}; check workflow base/head refs`);
    }
    const output = gitOutput(['diff', '--name-only', 'HEAD']);
    for (const line of output.split('\n').map((item) => item.trim()).filter(Boolean)) {
      names.add(line);
    }
  }
  if (!process.env.GITHUB_ACTIONS) {
    const output = gitOutput(['diff', '--name-only', 'HEAD']);
    for (const line of output.split('\n').map((item) => item.trim()).filter(Boolean)) {
      names.add(line);
    }
    const untracked = gitOutput(['ls-files', '--others', '--exclude-standard']);
    for (const line of untracked.split('\n').map((item) => item.trim()).filter(Boolean)) {
      names.add(line);
    }
  }
  if (process.env.GITHUB_ACTIONS && names.size === 0) {
    throw new Error(`No changed files detected for ${baseRef}...${headRef}; check workflow base/head refs`);
  }
  return [...names].sort();
}

function isTracked(relativePath) {
  const result = execFileSync('git', ['ls-files', '--error-unmatch', relativePath], {
    cwd: root,
    encoding: 'utf8',
    stdio: 'pipe'
  });
  return result.trim().length > 0;
}

function readPrBody() {
  if (prBodyPath && fs.existsSync(prBodyPath)) {
    return fs.readFileSync(prBodyPath, 'utf8');
  }
  return '';
}

function sectionText(markdown, heading) {
  const start = markdown.indexOf(heading);
  if (start < 0) {
    return '';
  }
  const rest = markdown.slice(start + heading.length);
  const next = rest.search(/\n##\s+/);
  return (next >= 0 ? rest.slice(0, next) : rest).trim();
}

function lineValue(markdown, label) {
  const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = markdown.match(new RegExp(`^-[ \\t]+${escaped}:[ \\t]*(.*)$`, 'm'));
  return match ? match[1].trim() : '';
}

function isPlaceholder(value) {
  if (!value) {
    return true;
  }
  const normalized = value.toLowerCase();
  const collapsed = normalized.replace(/\s+/g, ' ').trim();
  return (
    collapsed === 'executed / not applicable' ||
    collapsed === 'feature / fix / refactor / docs / release / governance / sync' ||
    collapsed === 'updated / not applicable' ||
    normalized === 'none' ||
    normalized === 'n/a' ||
    normalized === 'na' ||
    normalized === 'todo' ||
    normalized === 'tbd' ||
    normalized === 'not applicable'
  );
}

function hasExplicitNotApplicableReason(prBody) {
  const reason = lineValue(prBody, 'Not applicable reason');
  if (!reason || isPlaceholder(reason)) {
    return false;
  }
  const normalized = reason.toLowerCase();
  if (
    normalized.includes('state the concrete impact judgment') ||
    normalized.includes('for example unchanged') ||
    normalized.includes('public api/configuration/menu/permission') ||
    normalized.includes('例如') ||
    normalized.length < 20
  ) {
    return false;
  }
  return [
    'unchanged',
    'no change',
    'does not change',
    '不改变',
    '未改变',
    'public api',
    '公开 api',
    'configuration',
    '配置',
    'menu',
    '菜单',
    'permission',
    '权限',
    'tenant',
    '租户',
    'page',
    '页面',
    'startup',
    '启动',
    'validation',
    '验收',
    'runtime behavior',
    'capability',
    '运行时行为',
    '能力'
  ].some((keyword) => normalized.includes(keyword));
}

function validatePrBody(prBody, failures) {
  for (const section of requiredPrSections) {
    if (!prBody.includes(section)) {
      failures.push(`PR body is missing section: ${section}`);
    }
  }
  if (failures.length > 0) {
    return;
  }

  for (const label of ['PMO preflight', 'Role / phase', 'Task paths', 'Loaded PMO files', 'PR type']) {
    const value = lineValue(prBody, label);
    if (isPlaceholder(value)) {
      failures.push(`PR body must fill "${label}" with a concrete value`);
    }
  }

  const capabilityFields = ['Affected Mango capabilities', 'Module README', 'Capability map', 'PMO rules', '`mango-pmo/rules/index.json`'];
  for (const label of capabilityFields) {
    const value = lineValue(prBody, label);
    if (isPlaceholder(value)) {
      failures.push(`PR body must fill "${label}" with updated/not-applicable status and details`);
    }
  }

  const validationSection = sectionText(prBody, '## Validation');
  const commandBlock = validationSection.match(/```(?:bash)?\s*([\s\S]*?)```/);
  if (!commandBlock || !commandBlock[1].trim()) {
    failures.push('PR body must include at least one concrete validation command');
  }
  for (const label of ['Result', 'Unverified items', 'Risks']) {
    const value = lineValue(prBody, label);
    if (!value) {
      failures.push(`PR body must fill "${label}" with a concrete value`);
    }
  }

  const exceptions = sectionText(prBody, '## PMO Exceptions');
  if (!exceptions || exceptions === '-' || exceptions.includes('None /')) {
    failures.push('PR body must state PMO exceptions, or "None" when there are no exceptions');
  }
}

function filledPrBody(options = {}) {
  const notApplicableReason = options.notApplicableReason ?? 'this PR only changes governance checks and does not alter a runtime module capability.';
  return `## Summary

- Tighten capability docs gate.

## PMO / Scope

- PMO preflight: executed
- Role / phase: pmo / governance
- Task paths: mango-pmo/tools/check-capability-docs.mjs
- Loaded PMO files: rules/00-dev-flow.md, rules/08-capability-docs.md
- PR type: governance

## Capability Docs

- Affected Mango capabilities: capability docs governance
- Module README: not applicable, no module behavior changed
- Capability map: not applicable, no capability index changed
- PMO rules: updated, capability docs gate clarified
- \`mango-pmo/rules/index.json\`: not applicable, no new PMO rule file
- Not applicable reason: ${notApplicableReason}

## Validation

\`\`\`bash
node mango-pmo/tools/check-capability-docs.mjs --self-test
\`\`\`

- Result: passed
- Unverified items: GitHub Actions real PR runtime not executed locally
- Risks: low, script-only governance change

## PMO Exceptions

- None
`;
}

function runSelfTest() {
  const prBodyCases = [
    {
      name: 'empty body fails',
      body: '',
      valid: false
    },
    {
      name: 'template placeholders fail',
      body: read('.github/pull_request_template.md'),
      valid: false
    },
    {
      name: 'filled body passes',
      body: filledPrBody(),
      valid: true
    }
  ];
  const failures = [];
  for (const item of prBodyCases) {
    const itemFailures = [];
    if (!item.body.trim()) {
      itemFailures.push('PR body is empty; use the repository pull request template and fill required fields.');
    } else {
      validatePrBody(item.body, itemFailures);
    }
    const passed = itemFailures.length === 0;
    if (passed !== item.valid) {
      failures.push(`${item.name}: expected valid=${item.valid}, got valid=${passed}${itemFailures.length ? ` (${itemFailures.join('; ')})` : ''}`);
    }
  }

  const noReasonBody = filledPrBody({ notApplicableReason: '' });
  const coverageCases = [
    {
      name: 'backend source needs own README or capability map',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: noReasonBody,
      valid: false,
      expectedFailure: 'mango/mango-platform/mango-job/README.md'
    },
    {
      name: 'unrelated README cannot satisfy backend source',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java', 'README.md'],
      body: noReasonBody,
      valid: false,
      expectedFailure: 'mango/mango-platform/mango-job/README.md'
    },
    {
      name: 'module README satisfies backend source',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java', 'mango/mango-platform/mango-job/README.md'],
      body: noReasonBody,
      valid: true
    },
    {
      name: 'capability map satisfies backend source',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java', 'mango-docs/capabilities/README.md'],
      body: noReasonBody,
      valid: true
    },
    {
      name: 'explicit not applicable reason allows missing docs',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: filledPrBody(),
      valid: true,
      warning: true
    },
    {
      name: 'template not applicable reason does not allow missing docs',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: filledPrBody({ notApplicableReason: 'state the concrete impact judgment, for example unchanged public API/configuration/menu/permission/tenant/page/startup/validation/runtime behavior.' }),
      valid: false,
      expectedFailure: 'Not applicable reason'
    },
    {
      name: 'short vague not applicable reason does not allow missing docs',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: filledPrBody({ notApplicableReason: 'no change' }),
      valid: false,
      expectedFailure: 'Not applicable reason'
    },
    {
      name: 'frontend package source maps to package README',
      files: ['mango-ui/packages/job/src/index.ts'],
      body: noReasonBody,
      valid: false,
      expectedFailure: 'mango-ui/packages/job/README.md'
    },
    {
      name: 'frontend package README satisfies source',
      files: ['mango-ui/packages/job/src/index.ts', 'mango-ui/packages/job/README.md'],
      body: noReasonBody,
      valid: true
    },
    {
      name: 'top-level backend source maps to top-level README',
      files: ['mango/mango-common/src/main/java/io/mango/common/R.java'],
      body: noReasonBody,
      valid: false,
      expectedFailure: 'mango/mango-common/README.md'
    },
    {
      name: 'business starter maps to root starter README',
      files: ['mango-business-starter/template/pom.xml'],
      body: noReasonBody,
      valid: false,
      expectedFailure: 'mango-business-starter/README.md'
    },
    {
      name: 'PMO rule change needs index json',
      files: ['mango-pmo/rules/09-new-rule.md'],
      body: noReasonBody,
      valid: false,
      expectedFailure: 'rules/index.json'
    },
    {
      name: 'PMO rule and index pass',
      files: ['mango-pmo/rules/09-new-rule.md', 'mango-pmo/rules/index.json'],
      body: noReasonBody,
      valid: true
    }
  ];
  for (const item of coverageCases) {
    const itemFailures = [];
    const itemWarnings = [];
    checkRuleIndexCoverage(item.files, itemFailures);
    checkCapabilityDocCoverage(item.files, item.body, itemFailures, itemWarnings);
    const passed = itemFailures.length === 0;
    if (passed !== item.valid) {
      failures.push(`${item.name}: expected valid=${item.valid}, got valid=${passed}${itemFailures.length ? ` (${itemFailures.join('; ')})` : ''}`);
      continue;
    }
    if (item.expectedFailure && !itemFailures.some((failure) => failure.includes(item.expectedFailure))) {
      failures.push(`${item.name}: expected failure to mention ${item.expectedFailure}`);
    }
    if (item.warning && itemWarnings.length === 0) {
      failures.push(`${item.name}: expected warning when using explicit not-applicable reason`);
    }
  }

  if (failures.length > 0) {
    console.error(`Capability docs self-test failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
    process.exit(1);
  }
  console.log(`Capability docs self-test passed: ${prBodyCases.length + coverageCases.length} cases`);
  process.exit(0);
}

function hasAny(files, predicate) {
  return files.some(predicate);
}

function checkLinks(relativePath, failures) {
  const text = read(relativePath);
  checkLinksInText(relativePath, text, failures);
}

function headingAnchors(markdown) {
  const anchors = new Set();
  const headingPattern = /^#{1,6}\s+(.+?)\s*#*\s*$/gm;
  let match;
  while ((match = headingPattern.exec(markdown))) {
    const base = anchorSlug(match[1]);
    anchors.add(base);
  }
  return anchors;
}

function anchorSlug(heading) {
  return heading
    .toLowerCase()
    .trim()
    .replace(/[`~!@#$%^&*()+=\[\]{}\\|;:'",.<>/?，。！？；：“”‘’（）【】《》、]/g, '')
    .replace(/\s+/g, '-');
}

function checkLinksInText(relativePath, text, failures) {
  const dir = path.dirname(path.join(root, relativePath));
  const linkPattern = /\]\(([^)]+)\)/g;
  let match;
  while ((match = linkPattern.exec(text))) {
    const [rawHref, rawAnchor = ''] = match[1].split('#');
    const href = rawHref;
    if (/^[a-z][a-z0-9+.-]*:/i.test(href)) {
      continue;
    }
    const target = href ? path.resolve(dir, href) : path.join(root, relativePath);
    if (!fs.existsSync(target)) {
      failures.push(`${relativePath}: missing link target ${match[1]}`);
      continue;
    }
    if (rawAnchor) {
      const targetRelative = path.relative(root, target);
      const targetText = fs.readFileSync(target, 'utf8');
      const anchor = decodeURIComponent(rawAnchor);
      if (!headingAnchors(targetText).has(anchor)) {
        failures.push(`${relativePath}: missing link anchor ${match[1]} in ${targetRelative}`);
      }
    }
  }
}

function addedMarkdownLines(relativePath) {
  if (!fileExists(relativePath)) {
    return '';
  }
  let added = [];
  try {
    const diff = gitOutput(['diff', '--unified=0', `${baseRef}...${headRef}`, '--', relativePath]);
    added = diff
      .split('\n')
      .filter((line) => line.startsWith('+') && !line.startsWith('+++'))
      .map((line) => line.slice(1));
    if (added.length > 0) {
      return added.join('\n');
    }
  } catch {
    if (process.env.GITHUB_ACTIONS) {
      throw new Error(`Unable to diff links in ${relativePath} for ${baseRef}...${headRef}`);
    }
  }
  try {
    const tracked = isTracked(relativePath);
    if (tracked) {
      const diff = gitOutput(['diff', '--unified=0', 'HEAD', '--', relativePath]);
      added = diff
        .split('\n')
        .filter((line) => line.startsWith('+') && !line.startsWith('+++'))
        .map((line) => line.slice(1));
      return added.join('\n');
    }
  } catch {
    try {
      return read(relativePath);
    } catch {
      return '';
    }
  }
  return '';
}

function checkAddedLinks(relativePath, failures) {
  const added = addedMarkdownLines(relativePath);
  if (added.trim()) {
    checkLinksInText(relativePath, added, failures);
  }
}

function isCapabilityAffectingFile(file) {
  if (file.endsWith('/README.md') || file.startsWith('mango-docs/capabilities/')) {
    return false;
  }
  return (
    /^mango\/.+\/src\//.test(file) ||
    /^mango\/.+\/db\/migration\//.test(file) ||
    /^mango\/.+\/META-INF\/mango\//.test(file) ||
    /^mango\/.+\/pom\.xml$/.test(file) ||
    /^mango\/.+\/module\.properties$/.test(file) ||
    /^mango\/.+\/src\/main\/resources\/.*\.(ya?ml|properties|json)$/.test(file) ||
    /^mango-ui\/packages\/[^/]+\/src\//.test(file) ||
    /^mango-ui\/packages\/[^/]+\/package\.json$/.test(file) ||
    /^mango-ui\/packages\/mango-cli\//.test(file) ||
    /^mango-business-starter\//.test(file) ||
    /^deploy\/.+/.test(file)
  );
}

function moduleReadmeFor(file) {
  const uiPackage = file.match(/^(mango-ui\/packages\/[^/]+)\//);
  if (uiPackage) {
    return `${uiPackage[1]}/README.md`;
  }
  if (file.startsWith('mango-business-starter/')) {
    return 'mango-business-starter/README.md';
  }
  const topLevelBackend = file.match(/^mango\/([^/]+)\//);
  if (topLevelBackend && topLevelBackendModules.has(topLevelBackend[1])) {
    return `mango/${topLevelBackend[1]}/README.md`;
  }
  const backendModule = file.match(/^(mango\/[^/]+\/[^/]+)\//);
  if (backendModule) {
    return `${backendModule[1]}/README.md`;
  }
  const deployModule = file.match(/^(deploy\/[^/]+)\//);
  if (deployModule) {
    return `${deployModule[1]}/README.md`;
  }
  return '';
}

function checkRuleIndexCoverage(files, failures) {
  const changedRules = hasAny(files, (file) => /^mango-pmo\/rules\/.+\.md$/.test(file));
  const changedRuleIndex = files.includes('mango-pmo/rules/index.json');
  if (changedRules && !changedRuleIndex) {
    failures.push('PMO rule files changed without updating mango-pmo/rules/index.json');
  }
}

function checkCapabilityDocCoverage(files, prBody, failures, warnings) {
  const affectedReadmes = new Set(files.filter(isCapabilityAffectingFile).map(moduleReadmeFor).filter(Boolean));
  const changedCapabilityMap = files.some((file) => file.startsWith('mango-docs/capabilities/'));
  const missingReadmes = [...affectedReadmes].filter((readme) => !files.includes(readme));
  if (affectedReadmes.size > 0 && !changedCapabilityMap && missingReadmes.length > 0) {
    const message = `Capability-affecting files changed without their module README or capability map updates: ${missingReadmes.join(', ')}`;
    if (hasExplicitNotApplicableReason(prBody)) {
      warnings.push(`${message}; accepted because PR body includes an explicit not-applicable reason.`);
    } else {
      failures.push(`${message}; update docs or fill "Not applicable reason" with a concrete impact judgment, for example unchanged public API/configuration/menu/permission/startup/validation.`);
    }
  }
}

if (selfTest) {
  runSelfTest();
}

const failures = [];
const warnings = [];
let files = [];
try {
  files = changedFiles();
} catch (error) {
  failures.push(error.message);
}
const prBody = readPrBody();
const shouldValidatePrBody = Boolean(prBodyPath) || Boolean(process.env.GITHUB_ACTIONS);

if (fileExists('mango-pmo/rules/index.json')) {
  const index = JSON.parse(read('mango-pmo/rules/index.json'));
  for (const [id, entry] of Object.entries(index.rules || {})) {
    if (!entry.path || !fileExists(path.join('mango-pmo', entry.path))) {
      failures.push(`mango-pmo/rules/index.json: rule ${id} points to missing path ${entry.path || '<missing>'}`);
    }
  }
}

for (const linkCheckedFile of files.filter((file) => file.endsWith('.md') && fileExists(file))) {
  checkAddedLinks(linkCheckedFile, failures);
}

checkRuleIndexCoverage(files, failures);

if (shouldValidatePrBody) {
  if (!prBody.trim()) {
    failures.push('PR body is empty; use the repository pull request template and fill required fields.');
  } else {
    validatePrBody(prBody, failures);
  }
}

checkCapabilityDocCoverage(files, prBody, failures, warnings);

const capabilityMap = fileExists('mango-docs/capabilities/README.md')
  ? read('mango-docs/capabilities/README.md')
  : '';
if (capabilityMap && /必须|禁止|不得|不允许|只允许/.test(capabilityMap)) {
  failures.push('Capability map contains strong rule words; keep long-term rules in mango-pmo/rules and link them instead.');
}

if (warnings.length > 0) {
  console.warn(`Capability docs warnings:\n${warnings.map((warning) => `- ${warning}`).join('\n')}`);
}

if (failures.length > 0) {
  console.error(`Capability docs check failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  process.exit(1);
}

console.log(`Capability docs checks passed: ${files.length} changed files inspected`);
