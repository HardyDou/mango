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
const businessGuideMappings = [
  {
    guide: 'mango-docs/guides/business-integration/file-upload-form.md',
    matches: [
      /^mango\/mango-platform\/mango-file(\/|$)/,
      /^mango\/mango-platform\/mango-file-preview(\/|$)/,
      /^mango\/mango-infra\/mango-infra-fileproc(\/|$)/,
      /^mango-ui\/packages\/file(\/|$)/
    ]
  },
  {
    guide: 'mango-docs/guides/business-integration/workflow-business-approval.md',
    matches: [
      /^mango\/mango-platform\/mango-workflow(\/|$)/,
      /^mango-ui\/packages\/workflow(\/|$)/,
      /^mango-ui\/packages\/workflow-business-example(\/|$)/
    ]
  },
  {
    guide: 'mango-docs/guides/business-integration/rbac-menu-page-troubleshooting.md',
    matches: [
      /^mango\/mango-platform\/mango-authorization(\/|$)/,
      /^mango-ui\/packages\/rbac(\/|$)/,
      /^mango-ui\/packages\/admin-shell(\/|$)/,
      /^mango-ui\/packages\/admin(\/|$)/,
      /^mango-ui\/packages\/admin-pages(\/|$)/
    ]
  },
  {
    guide: 'mango-docs/guides/business-integration/permission-button-troubleshooting.md',
    matches: [
      /^mango\/mango-platform\/mango-access(\/|$)/,
      /^mango\/mango-platform\/mango-authorization(\/|$)/,
      /^mango-ui\/packages\/rbac(\/|$)/,
      /^mango-ui\/packages\/admin-shell(\/|$)/
    ]
  },
  {
    guide: 'mango-docs/guides/business-integration/tenant-dict-config-empty.md',
    matches: [
      /^mango\/mango-platform\/mango-identity(\/|$)/,
      /^mango\/mango-platform\/mango-org(\/|$)/,
      /^mango\/mango-platform\/mango-system(\/|$)/,
      /^mango\/mango-platform\/mango-resource(\/|$)/,
      /^mango-ui\/packages\/system(\/|$)/,
      /^mango-ui\/packages\/admin-shell(\/|$)/
    ]
  }
];

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

function assuranceSelection(prBody, measureId) {
  const value = lineValue(prBody, 'Assurance selections');
  const match = new RegExp(`(?:^|[;；,，]\\s*)${measureId}=([A-Z_]+)(?:$|[;；,，])`, 'u').exec(value);
  return match?.[1] ?? '';
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

  if (/^-[ \t]+Not applicable reason[ \t]*:/im.test(prBody)) {
    failures.push('PR body must not include the standalone "Not applicable reason" field; explain not-applicable status in each corresponding Capability Docs field.');
  }

  for (const label of ['PMO preflight', 'Role / phase', 'Task paths', 'Loaded PMO files', 'PR type']) {
    const value = lineValue(prBody, label);
    if (isPlaceholder(value)) {
      failures.push(`PR body must fill "${label}" with a concrete value`);
    }
  }

  if (assuranceSelection(prBody, 'M08') === 'ENABLE') {
    const capabilityFields = ['Affected Mango capabilities', 'Module README', 'Capability map', 'Business guide', 'PMO rules', '`mango-pmo/rules/index.json`'];
    for (const label of capabilityFields) {
      const value = lineValue(prBody, label);
      if (isPlaceholder(value)) {
        failures.push(`M08=ENABLE requires PR body field "${label}" with concrete update details`);
      }
    }
  }

  const hasEnabledValidation = Array.from({ length: 8 }, (_, index) => `M${String(index + 9).padStart(2, '0')}`)
    .some((measureId) => assuranceSelection(prBody, measureId) === 'ENABLE');
  if (hasEnabledValidation) {
    const validationSection = sectionText(prBody, '## Validation');
    const commandBlock = validationSection.match(/```(?:bash)?\s*([\s\S]*?)```/);
    if (!commandBlock || !commandBlock[1].trim()) {
      failures.push('enabled M09-M16 measures require at least one concrete validation command');
    }
    for (const label of ['Result', 'Unverified items', 'Risks']) {
      const value = lineValue(prBody, label);
      if (!value) {
        failures.push(`enabled M09-M16 measures require PR body field "${label}"`);
      }
    }
  }

  const exceptions = sectionText(prBody, '## PMO Exceptions');
  if (!exceptions || exceptions === '-' || exceptions.includes('None /')) {
    failures.push('PR body must state PMO exceptions, or "None" when there are no exceptions');
  }
}

function filledPrBody(options = {}) {
  const forbiddenReason = options.forbiddenReason;
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
- Business guide: not applicable, no business integration scenario changed
- PMO rules: updated, capability docs gate clarified
- \`mango-pmo/rules/index.json\`: not applicable, no new PMO rule file
${forbiddenReason === undefined ? '' : `- Not applicable reason: ${forbiddenReason}\n`}

## Validation

\`\`\`bash
node mango-pmo/tools/check-capability-docs.mjs --self-test
\`\`\`

- Result: passed
- Unverified items: GitHub Actions real PR runtime not executed locally
- Risks: low, script-only governance change

## Risk / Verification

- Assurance selections: M01=CREATE; M08=ENABLE; M09=ENABLE

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
    },
    {
      name: 'no enabled validation measure does not require a command',
      body: filledPrBody()
        .replace('M09=ENABLE', 'M09=DISABLE')
        .replace(/```bash[\s\S]*?```/u, 'No validation command because no M09-M16 measure is enabled.'),
      valid: true
    },
    {
      name: 'standalone not applicable reason field fails',
      body: filledPrBody({ forbiddenReason: 'runtime behavior is unchanged' }),
      valid: false
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

  const coverageCases = [
    {
      name: 'backend source needs own README or capability map',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'mango/mango-platform/mango-job/README.md'
    },
    {
      name: 'unrelated README cannot satisfy backend source',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java', 'README.md'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'mango/mango-platform/mango-job/README.md'
    },
    {
      name: 'module README satisfies backend source',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java', 'mango/mango-platform/mango-job/README.md'],
      body: filledPrBody(),
      valid: true
    },
    {
      name: 'capability map satisfies backend source',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java', 'mango-docs/capabilities/README.md'],
      body: filledPrBody(),
      valid: true
    },
    {
      name: 'standalone not applicable reason cannot allow missing docs',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: filledPrBody({ forbiddenReason: 'runtime behavior is unchanged' }),
      valid: false,
      expectedFailures: ['Not applicable reason', 'mango/mango-platform/mango-job/README.md']
    },
    {
      name: 'frontend package source maps to package README',
      files: ['mango-ui/packages/job/src/index.ts'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'mango-ui/packages/job/README.md'
    },
    {
      name: 'frontend package README satisfies source',
      files: ['mango-ui/packages/job/src/index.ts', 'mango-ui/packages/job/README.md'],
      body: filledPrBody(),
      valid: true
    },
    {
      name: 'top-level backend source maps to top-level README',
      files: ['mango/mango-common/src/main/java/io/mango/common/R.java'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'mango/mango-common/README.md'
    },
    {
      name: 'business starter maps to root starter README',
      files: ['mango-business-starter/template/pom.xml'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'mango-business-starter/README.md'
    },
    {
      name: 'file module changes need business upload guide',
      files: ['mango/mango-platform/mango-file/mango-file-api/src/main/java/io/mango/file/api/FileApi.java', 'mango/mango-platform/mango-file/README.md'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'file-upload-form.md'
    },
    {
      name: 'file module and business upload guide pass',
      files: ['mango/mango-platform/mango-file/mango-file-api/src/main/java/io/mango/file/api/FileApi.java', 'mango/mango-platform/mango-file/README.md', 'mango-docs/guides/business-integration/file-upload-form.md'],
      body: filledPrBody(),
      valid: true
    },
    {
      name: 'package manifest dependency projection does not require a business guide',
      files: ['mango-ui/packages/admin-shell/package.json', 'mango-ui/packages/admin-shell/README.md'],
      body: filledPrBody(),
      valid: true
    },
    {
      name: 'package runtime source still requires its mapped business guides',
      files: ['mango-ui/packages/admin-shell/src/index.ts', 'mango-ui/packages/admin-shell/README.md'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'rbac-menu-page-troubleshooting.md'
    },
    {
      name: 'standalone not applicable reason does not allow missing business guide',
      files: ['mango/mango-platform/mango-file/mango-file-api/src/main/java/io/mango/file/api/FileApi.java', 'mango/mango-platform/mango-file/README.md'],
      body: filledPrBody({ forbiddenReason: 'runtime behavior is unchanged' }),
      valid: false,
      expectedFailures: ['Not applicable reason', 'file-upload-form.md']
    },
    {
      name: 'PMO rule change needs index json',
      files: ['mango-pmo/rules/09-new-rule.md'],
      body: filledPrBody(),
      valid: false,
      expectedFailure: 'rules/index.json'
    },
    {
      name: 'PMO rule and index pass',
      files: ['mango-pmo/rules/09-new-rule.md', 'mango-pmo/rules/index.json'],
      body: filledPrBody(),
      valid: true
    },
    {
      name: 'M08 disabled does not make CI add capability documents',
      files: ['mango/mango-platform/mango-job/mango-job-core/src/main/java/com/example/Job.java'],
      body: filledPrBody().replace('M08=ENABLE', 'M08=DISABLE'),
      valid: true
    }
  ];
  for (const item of coverageCases) {
    const itemFailures = [];
    validatePrBody(item.body, itemFailures);
    checkRuleIndexCoverage(item.files, itemFailures);
    if (assuranceSelection(item.body, 'M08') === 'ENABLE') {
      checkCapabilityDocCoverage(item.files, itemFailures);
    }
    const passed = itemFailures.length === 0;
    if (passed !== item.valid) {
      failures.push(`${item.name}: expected valid=${item.valid}, got valid=${passed}${itemFailures.length ? ` (${itemFailures.join('; ')})` : ''}`);
      continue;
    }
    if (item.expectedFailure && !itemFailures.some((failure) => failure.includes(item.expectedFailure))) {
      failures.push(`${item.name}: expected failure to mention ${item.expectedFailure}`);
    }
    for (const expectedFailure of item.expectedFailures ?? []) {
      if (!itemFailures.some((failure) => failure.includes(expectedFailure))) {
        failures.push(`${item.name}: expected failure to mention ${expectedFailure}`);
      }
    }
  }

  const platformFailures = [];
  checkPlatformCapabilityEntrypoints(platformFailures);
  if (platformFailures.length > 0) {
    failures.push(`platform capability entrypoint check should pass for current repository: ${platformFailures.join('; ')}`);
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

function isBusinessGuideAffectingFile(file) {
  return isCapabilityAffectingFile(file) && !/^mango-ui\/packages\/[^/]+\/package\.json$/.test(file);
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

function checkCapabilityDocCoverage(files, failures) {
  const affectedReadmes = new Set(files.filter(isCapabilityAffectingFile).map(moduleReadmeFor).filter(Boolean));
  const changedCapabilityMap = files.some((file) => file.startsWith('mango-docs/capabilities/'));
  const missingReadmes = [...affectedReadmes].filter((readme) => !files.includes(readme));
  if (affectedReadmes.size > 0 && !changedCapabilityMap && missingReadmes.length > 0) {
    const message = `Capability-affecting files changed without their module README or capability map updates: ${missingReadmes.join(', ')}`;
    failures.push(`${message}; update the required module README or capability map.`);
  }

  const affectedGuides = new Set();
  for (const file of files.filter(isBusinessGuideAffectingFile)) {
    for (const mapping of businessGuideMappings) {
      if (mapping.matches.some((pattern) => pattern.test(file))) {
        affectedGuides.add(mapping.guide);
      }
    }
  }
  const missingGuides = [...affectedGuides].filter((guide) => !files.includes(guide));
  if (missingGuides.length > 0) {
    failures.push(`Business integration scenario files may be affected but were not updated: ${missingGuides.join(', ')}; update the guide with changed usage or an explicit no-impact note for this scenario.`);
  }
}

function extractBackendPlatformCapabilityReadmes() {
  const text = read('mango-docs/capabilities/README.md');
  return uniqueMatches(
    text,
    /`(mango\/mango-platform\/[^`]+)`\s*\|\s*\[README\]\([^)]*README\.md\)/g,
    (match) => `${match[1]}/README.md`
  );
}

function extractDocsReadmeBackendPlatformLinks() {
  const text = read('mango-docs/README.md');
  return uniqueMatches(
    text,
    /\]\(\.\.\/(mango\/mango-platform\/[^)]+\/README\.md)\)/g,
    (match) => match[1]
  );
}

function extractPublicDocsBackendPlatformReadmes() {
  const text = read('mango-docs/.vitepress/stage-public-docs.mjs');
  const publicDocsMatch = text.match(/const publicDocs = \[([\s\S]*?)\];/);
  if (!publicDocsMatch) {
    return [];
  }
  return uniqueMatches(
    publicDocsMatch[1],
    /'([^']+)'/g,
    (match) => match[1]
  ).filter((item) => /^mango\/mango-platform\/[^/]+\/README\.md$/.test(item));
}

function extractSidebarBackendPlatformLinks() {
  const text = read('mango-docs/.vitepress/stage-public-docs.mjs');
  return uniqueMatches(
    text,
    /link: '\/(mango\/mango-platform\/[^']+\/README)'/g,
    (match) => `${match[1]}.md`
  );
}

function uniqueMatches(text, pattern, mapper) {
  return [...new Set([...text.matchAll(pattern)].map(mapper))].sort();
}

function missingItems(expected, actual) {
  const actualSet = new Set(actual);
  return expected.filter((item) => !actualSet.has(item));
}

function checkPlatformCapabilityEntrypoints(failures) {
  if (
    !fileExists('mango-docs/capabilities/README.md') ||
    !fileExists('mango-docs/README.md') ||
    !fileExists('mango-docs/.vitepress/stage-public-docs.mjs')
  ) {
    return;
  }

  const capabilityReadmes = extractBackendPlatformCapabilityReadmes();
  const docsReadmeLinks = extractDocsReadmeBackendPlatformLinks();
  const publicDocsReadmes = extractPublicDocsBackendPlatformReadmes();
  const sidebarLinks = extractSidebarBackendPlatformLinks();

  const missingDocsReadme = missingItems(capabilityReadmes, docsReadmeLinks);
  const missingPublicDocs = missingItems(capabilityReadmes, publicDocsReadmes);
  const missingSidebar = missingItems(capabilityReadmes, sidebarLinks);

  if (missingDocsReadme.length > 0) {
    failures.push(`mango-docs/README.md is missing backend platform capability links from capability map: ${missingDocsReadme.join(', ')}`);
  }
  if (missingPublicDocs.length > 0) {
    failures.push(`mango-docs/.vitepress/stage-public-docs.mjs publicDocs is missing backend platform capability README files from capability map: ${missingPublicDocs.join(', ')}`);
  }
  if (missingSidebar.length > 0) {
    failures.push(`mango-docs/.vitepress/stage-public-docs.mjs sidebar is missing backend platform capability links from capability map: ${missingSidebar.join(', ')}`);
  }
}

if (selfTest) {
  runSelfTest();
}

const failures = [];
let files = [];
try {
  files = changedFiles();
} catch (error) {
  failures.push(error.message);
}
const prBody = readPrBody();
const shouldValidatePrBody = Boolean(prBodyPath);

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

if (assuranceSelection(prBody, 'M08') === 'ENABLE') {
  checkCapabilityDocCoverage(files, failures);
}
checkPlatformCapabilityEntrypoints(failures);

const capabilityMap = fileExists('mango-docs/capabilities/README.md')
  ? read('mango-docs/capabilities/README.md')
  : '';
if (capabilityMap && /必须|禁止|不得|不允许|只允许/.test(capabilityMap)) {
  failures.push('Capability map contains strong rule words; keep long-term rules in mango-pmo/rules and link them instead.');
}

if (failures.length > 0) {
  console.error(`Capability docs check failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  process.exit(1);
}

console.log(`Capability docs checks passed: ${files.length} changed files inspected`);
