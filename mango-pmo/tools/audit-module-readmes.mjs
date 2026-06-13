#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const args = process.argv.slice(2);
const selfTest = args.includes('--self-test');

const requiredSections = [
  '能力定位',
  '适用场景',
  '不适用场景',
  '模块边界',
  '接入方式',
  '配置项',
  '对外接口 / 扩展点',
  '数据库 / 初始化数据',
  '菜单 / 权限 / 租户',
  '验证方式',
  '业务接入最小闭环',
  '常见问题',
  '关联 PMO 规则',
  '历史设计 / 交付记录'
];

const frontendEntrySections = [
  '入口定位',
  '公开导出',
  '使用场景',
  '接入方式',
  'Props / 参数 / 事件',
  '后端依赖',
  '权限 / 租户 / 数据边界',
  '验证方式',
  '常见问题',
  '关联文档'
];

const moduleRoots = [
  'mango/mango-platform',
  'mango/mango-infra',
  'mango-ui/packages'
];

const topLevelReadmes = [
  'mango/mango-admin-starter/README.md',
  'mango/mango-app/README.md',
  'mango/mango-common/README.md',
  'mango/mango-extension/README.md',
  'mango/mango-parent/README.md',
  'mango/mango-tools/README.md',
  'mango-business-starter/README.md',
  'mango-business-starter/business-pmo/README.md',
  'mango-business-starter/business-pmo/mango-baseline/README.md',
  'mango-business-starter/topologies/microservice/README.md',
  'mango-business-starter/topologies/monolith/README.md'
];

const frontendEntryReadmes = [
  'mango-ui/packages/auth/src/views/README.md',
  'mango-ui/packages/file/src/components/README.md',
  'mango-ui/packages/job/src/views/README.md',
  'mango-ui/packages/rbac/src/views/README.md',
  'mango-ui/packages/system/src/components/README.md',
  'mango-ui/packages/workflow/src/components/README.md'
];

const ignoredDirs = new Set(['node_modules', 'target', 'dist', 'templates']);
const placeholderPattern = /\bTODO\b|\bTBD\b|待补充|待完善/;
const backtickedCommandPattern = /^`(?:pnpm|mvn|node|npm|npx|git|gh)\b.*`$/gm;
const packageScriptPattern = /^pnpm\s+-F\s+(@mango\/[^\s]+)\s+([a-z][\w:-]*)\b/gm;

function walk(dir, results = []) {
  if (!fs.existsSync(dir)) return results;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (ignoredDirs.has(entry.name)) {
      continue;
    }
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath, results);
    } else if (entry.name === 'README.md') {
      results.push(path.relative(root, fullPath));
    }
  }
  return results;
}

function priorityFor(readmePath) {
  if (
    readmePath.includes('mango-job') ||
    readmePath.includes('mango-workflow') ||
    readmePath.includes('mango-file') ||
    readmePath.includes('mango-auth') ||
    readmePath.includes('mango-authorization') ||
    readmePath.includes('mango-identity') ||
    readmePath.includes('mango-access') ||
    readmePath.includes('mango-infra-persistence') ||
    readmePath === 'mango-ui/packages/mango-cli/README.md' ||
    readmePath === 'mango-business-starter/README.md'
  ) {
    return 'A';
  }
  if (readmePath.startsWith('mango/mango-platform') || readmePath.startsWith('mango/mango-infra') || readmePath.startsWith('mango-ui/packages')) {
    return 'B';
  }
  return 'C';
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function headingPattern(section) {
  return new RegExp(`^##\\s+(?:\\d+\\.\\s*)?${escapeRegExp(section)}\\s*$`, 'm');
}

function hasSection(text, section) {
  return headingPattern(section).test(text);
}

function extractSection(text, section) {
  const pattern = headingPattern(section);
  const match = pattern.exec(text);
  if (!match) return '';
  const start = match.index + match[0].length;
  const rest = text.slice(start);
  const next = rest.search(/^##\s+/m);
  return (next >= 0 ? rest.slice(0, next) : rest).trim();
}

function markdownLinks(text) {
  const links = [];
  const linkPattern = /\[[^\]]+\]\(([^)]+)\)/g;
  let match;
  while ((match = linkPattern.exec(text))) {
    links.push(match[1]);
  }
  return links;
}

function isExternalLink(href) {
  return /^[a-z][a-z0-9+.-]*:/i.test(href);
}

function linkTargetExists(readmePath, href) {
  const [cleanHref, rawAnchor = ''] = href.split('#');
  if (isExternalLink(cleanHref)) {
    return true;
  }
  const target = cleanHref
    ? path.resolve(path.dirname(path.join(root, readmePath)), cleanHref)
    : path.join(root, readmePath);
  if (!fs.existsSync(target)) {
    return false;
  }
  if (!rawAnchor) {
    return true;
  }
  return headingAnchors(fs.readFileSync(target, 'utf8')).has(decodeURIComponent(rawAnchor));
}

function headingAnchors(markdown) {
  const anchors = new Set();
  const headingPattern = /^#{1,6}\s+(.+?)\s*#*\s*$/gm;
  let match;
  while ((match = headingPattern.exec(markdown))) {
    anchors.add(anchorSlug(match[1]));
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

function packageScriptExists(packageName, scriptName) {
  const packagesRoot = path.join(root, 'mango-ui/packages');
  if (!fs.existsSync(packagesRoot)) {
    return true;
  }
  for (const entry of fs.readdirSync(packagesRoot, { withFileTypes: true })) {
    if (!entry.isDirectory()) {
      continue;
    }
    const packageJson = path.join(packagesRoot, entry.name, 'package.json');
    if (!fs.existsSync(packageJson)) {
      continue;
    }
    const pkg = JSON.parse(fs.readFileSync(packageJson, 'utf8'));
    if (pkg.name === packageName) {
      return Boolean(pkg.scripts?.[scriptName]);
    }
  }
  return false;
}

function packageScriptIssues(text) {
  const issues = [];
  for (const match of text.matchAll(packageScriptPattern)) {
    const [, packageName, scriptName] = match;
    if (!packageScriptExists(packageName, scriptName)) {
      issues.push(`${packageName} ${scriptName}`);
    }
  }
  return issues;
}

function requiredSectionsFor(readmePath) {
  return frontendEntryReadmes.includes(readmePath) ? frontendEntrySections : requiredSections;
}

function docTypeFor(readmePath) {
  return frontendEntryReadmes.includes(readmePath) ? 'frontend-entry' : 'module';
}

function auditText(readmePath, text) {
  const missing = requiredSectionsFor(readmePath).filter((section) => !hasSection(text, section));
  const hasPlaceholder = placeholderPattern.test(text);
  const commandFormatIssues = [...text.matchAll(backtickedCommandPattern)].map((match) => match[0]);
  const missingPackageScripts = packageScriptIssues(text);
  const validation = extractSection(text, '验证方式');
  const pmoRules = extractSection(text, docTypeFor(readmePath) === 'frontend-entry' ? '关联文档' : '关联 PMO 规则');
  const validationEmpty = !validation || validation === '-' || validation.length < 8;
  const pmoRuleLinks = markdownLinks(pmoRules).filter((href) => {
    return href.includes('mango-pmo/rules/') ||
      href.includes('mango-baseline/rules/') ||
      href.startsWith('./rules/') ||
      href.startsWith('rules/');
  });
  const missingPmoRuleLinks = pmoRuleLinks.length === 0;
  const brokenLinks = markdownLinks(text).filter((href) => !linkTargetExists(readmePath, href));
  return {
    readmePath,
    docType: docTypeFor(readmePath),
    priority: priorityFor(readmePath),
    missing,
    hasPlaceholder,
    commandFormatIssues,
    missingPackageScripts,
    validationEmpty,
    missingPmoRuleLinks,
    brokenLinks
  };
}

function auditReadme(readmePath) {
  if (!fs.existsSync(path.join(root, readmePath))) {
    return {
      readmePath,
      docType: docTypeFor(readmePath),
      priority: priorityFor(readmePath),
      missing: ['README file'],
      hasPlaceholder: false,
      commandFormatIssues: [],
      missingPackageScripts: [],
      validationEmpty: true,
      missingPmoRuleLinks: true,
      brokenLinks: []
    };
  }
  return auditText(readmePath, read(readmePath));
}

function rowHasFailure(row) {
  return row.missing.length > 0 || row.hasPlaceholder || row.commandFormatIssues.length > 0 || row.missingPackageScripts.length > 0 || row.validationEmpty || row.missingPmoRuleLinks || row.brokenLinks.length > 0;
}

function printRows(rows) {
  console.log('| Priority | Type | README | Missing sections | Placeholder | Command format | Package scripts | Validation | PMO links | Broken links |');
  console.log('|----------|------|--------|------------------|-------------|----------------|-----------------|------------|-----------|--------------|');
  for (const row of rows) {
    console.log(`| ${row.priority} | ${row.docType} | \`${row.readmePath}\` | ${row.missing.length ? row.missing.join(', ') : 'None'} | ${row.hasPlaceholder ? 'Yes' : 'No'} | ${row.commandFormatIssues.length ? row.commandFormatIssues.join('<br>') : 'OK'} | ${row.missingPackageScripts.length ? row.missingPackageScripts.join('<br>') : 'OK'} | ${row.validationEmpty ? 'Empty' : 'OK'} | ${row.missingPmoRuleLinks ? 'Missing' : 'OK'} | ${row.brokenLinks.length ? row.brokenLinks.join(', ') : 'None'} |`);
  }
}

function runSelfTest() {
  const valid = `# Demo

## 1. 能力定位
text
## 2. 适用场景
text
## 3. 不适用场景
text
## 4. 模块边界
text
## 5. 接入方式
text
## 6. 配置项
text
## 7. 对外接口 / 扩展点
text
## 8. 数据库 / 初始化数据
text
## 9. 菜单 / 权限 / 租户
text
## 10. 验证方式
\`\`\`bash
node mango-pmo/tools/audit-module-readmes.mjs
\`\`\`
## 业务接入最小闭环
text
## 11. 常见问题
text
## 12. 关联 PMO 规则
- [能力说明维护规范](mango-pmo/rules/08-capability-docs.md)
## 13. 历史设计 / 交付记录
text
`;
  const invalid = valid.replace('## 10. 验证方式\n```bash\nnode mango-pmo/tools/audit-module-readmes.mjs\n```', '## 10. 验证方式\n-');
  const validFrontendEntry = `# Demo Frontend Entry

## 1. 入口定位
text
## 2. 公开导出
text
## 3. 使用场景
text
## 4. 接入方式
text
## 5. Props / 参数 / 事件
text
## 6. 后端依赖
text
## 7. 权限 / 租户 / 数据边界
text
## 8. 验证方式
\`\`\`bash
node mango-pmo/tools/audit-module-readmes.mjs
\`\`\`
## 9. 常见问题
text
## 10. 关联文档
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
`;
  const cases = [
    { name: 'valid template passes', text: valid, valid: true },
    { name: 'valid frontend entry passes', path: 'mango-ui/packages/file/src/components/README.md', text: validFrontendEntry, valid: true },
    { name: 'frontend entry missing section fails', path: 'mango-ui/packages/file/src/components/README.md', text: validFrontendEntry.replace('## 5. Props / 参数 / 事件\ntext\n', ''), valid: false },
    { name: 'empty validation fails', text: invalid, valid: false },
    { name: 'placeholder fails', text: `${valid}\nTODO`, valid: false },
    { name: 'backticked command fails', text: valid.replace('node mango-pmo/tools/audit-module-readmes.mjs', '`node mango-pmo/tools/audit-module-readmes.mjs`'), valid: false },
    { name: 'missing package script fails', text: valid.replace('node mango-pmo/tools/audit-module-readmes.mjs', 'pnpm -F @mango/api-schema build'), valid: false },
    { name: 'missing PMO link fails', text: valid.replace('mango-pmo/rules/08-capability-docs.md', 'README.md'), valid: false },
    { name: 'broken link fails', text: valid.replace('mango-pmo/rules/08-capability-docs.md', 'missing.md'), valid: false }
  ];
  const failures = [];
  for (const item of cases) {
    const result = auditText(item.path || 'SELF_TEST.md', item.text);
    const passed = !rowHasFailure(result);
    if (passed !== item.valid) {
      failures.push(`${item.name}: expected valid=${item.valid}, got valid=${passed}`);
    }
  }
  if (failures.length > 0) {
    console.error(`Module README audit self-test failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
    process.exit(1);
  }
  console.log(`Module README audit self-test passed: ${cases.length} cases`);
  process.exit(0);
}

if (selfTest) {
  runSelfTest();
}

function managedModuleReadmes() {
  const readmes = new Set(topLevelReadmes);
  for (const entryReadme of frontendEntryReadmes) {
    readmes.add(entryReadme);
  }
  for (const moduleRoot of moduleRoots) {
    if (!fs.existsSync(path.join(root, moduleRoot))) {
      continue;
    }
    for (const entry of fs.readdirSync(path.join(root, moduleRoot), { withFileTypes: true })) {
      if (entry.isDirectory() && !ignoredDirs.has(entry.name)) {
        readmes.add(path.join(moduleRoot, entry.name, 'README.md'));
      }
    }
  }
  for (const discovered of moduleRoots.flatMap((moduleRoot) => walk(path.join(root, moduleRoot)))) {
    readmes.add(discovered);
  }
  return [...readmes].sort();
}

const readmes = managedModuleReadmes();
const rows = readmes.map(auditReadme);
printRows(rows);

const failures = rows.filter(rowHasFailure);
if (failures.length > 0) {
  console.error(`Module README audit failed: ${failures.length} file(s) need attention`);
  process.exit(1);
}
