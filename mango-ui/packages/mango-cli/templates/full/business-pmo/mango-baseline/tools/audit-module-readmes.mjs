#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const args = process.argv.slice(2);
const selfTest = args.includes('--self-test');

const requiredSectionGroups = [
  { name: '概览', anyOf: ['概览'] },
  { name: '功能清单', anyOf: ['功能清单'] },
  { name: '接入方式', anyOf: ['接入方式', '后端接入', '前端接入'] },
  { name: '配置说明', anyOf: ['配置说明', 'YAML 配置字段', '运行时配置字段'] },
  { name: 'API 与扩展', anyOf: ['API 与扩展', '接口与扩展', '后端接入', '前端接入', '返回字段'] },
  { name: '数据与初始化', anyOf: ['数据与初始化'] },
  { name: '管理入口', anyOf: ['管理入口'] },
  { name: '快速开始', anyOf: ['快速开始'] },
  { name: '问题排查', anyOf: ['问题排查'] },
  { name: '相关文档', anyOf: ['相关文档'] }
];

const frontendEntrySectionGroups = [
  { name: '概览', anyOf: ['概览'] },
  { name: '功能清单', anyOf: ['功能清单'] },
  { name: '接入方式', anyOf: ['接入方式'] },
  { name: '参数与事件', anyOf: ['参数与事件'] },
  { name: '后端依赖', anyOf: ['后端依赖'] },
  { name: '权限与数据边界', anyOf: ['权限与数据边界'] },
  { name: '快速开始', anyOf: ['快速开始'] },
  { name: '问题排查', anyOf: ['问题排查'] },
  { name: '相关文档', anyOf: ['相关文档'] }
];

const managementViewSectionGroups = [
  { name: '概览', anyOf: ['概览'] },
  { name: '功能清单', anyOf: ['功能清单'] },
  { name: '页面入口', anyOf: ['页面入口'] },
  { name: '后端依赖', anyOf: ['后端依赖'] },
  { name: '管理入口', anyOf: ['管理入口'] },
  { name: '问题排查', anyOf: ['问题排查'] },
  { name: '相关文档', anyOf: ['相关文档'] }
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

const ignoredDirs = new Set(['node_modules', 'target', 'dist', 'templates']);
const ignoredReadmePathSegments = [
  '/src/test/',
  '/src/testFixtures/'
];
const placeholderPattern = /\bTODO\b|\bTBD\b|待补充|待完善/;
const backtickedCommandPattern = /^`(?:pnpm|mvn|node|npm|npx|git|gh)\b.*`$/gm;
const packageScriptPattern = /^pnpm\s+-F\s+(@mango\/[^\s]+)\s+([a-z][\w:-]*)\b/gm;
const sourceFileExtensions = new Set(['.java', '.ts', '.tsx', '.vue', '.json', '.sql', '.yml', '.yaml', '.properties', '.xml']);

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
      const relativePath = path.relative(root, fullPath).split(path.sep).join('/');
      if (!ignoredReadmePathSegments.some((segment) => relativePath.includes(segment))) {
        results.push(relativePath);
      }
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

function requiredSectionGroupsFor(readmePath) {
  if (isDetailedFrontendEntryReadme(readmePath)) {
    return frontendEntrySectionGroups;
  }
  if (isManagementViewReadme(readmePath)) {
    return managementViewSectionGroups;
  }
  return requiredSectionGroups;
}

function docTypeFor(readmePath) {
  if (isDetailedFrontendEntryReadme(readmePath)) {
    return 'frontend-entry';
  }
  if (isManagementViewReadme(readmePath)) {
    return 'management-view';
  }
  return 'module';
}

function isDetailedFrontendEntryReadme(readmePath) {
  return readmePath.startsWith('mango-ui/packages/') && readmePath.endsWith('/src/components/README.md');
}

function isManagementViewReadme(readmePath) {
  return readmePath.startsWith('mango-ui/packages/') && readmePath.endsWith('/src/views/README.md');
}

function auditText(readmePath, text) {
  const missing = requiredSectionGroupsFor(readmePath)
    .filter((group) => !group.anyOf.some((section) => hasSection(text, section)))
    .map((group) => group.name);
  const hasPlaceholder = placeholderPattern.test(text);
  const commandFormatIssues = [...text.matchAll(backtickedCommandPattern)].map((match) => match[0]);
  const missingPackageScripts = packageScriptIssues(text);
  const docType = docTypeFor(readmePath);
  const relatedDocs = extractSection(text, '相关文档');
  const emptySections = requiredSectionGroupsFor(readmePath)
    .filter((group) => group.anyOf.some((section) => hasSection(text, section)))
    .filter((group) => {
      const content = group.anyOf.map((section) => extractSection(text, section)).find(Boolean) || '';
      return isEffectivelyEmpty(content);
    })
    .map((group) => group.name);
  const relatedLinks = markdownLinks(relatedDocs).filter((href) => {
    return href.includes('mango-pmo/rules/') ||
      href.includes('mango-baseline/rules/') ||
      href.startsWith('./rules/') ||
      href.startsWith('rules/') ||
      href.includes('mango-docs/');
  });
  const missingRelatedLinks = relatedLinks.length === 0;
  const brokenLinks = markdownLinks(text).filter((href) => !linkTargetExists(readmePath, href));
  const sourceRegistrationIssues = registrationIssues(readmePath, text);
  return {
    readmePath,
    docType,
    priority: priorityFor(readmePath),
    missing,
    hasPlaceholder,
    commandFormatIssues,
    missingPackageScripts,
    emptySections,
    missingRelatedLinks,
    brokenLinks,
    sourceRegistrationIssues
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
      emptySections: [],
      missingRelatedLinks: true,
      brokenLinks: [],
      sourceRegistrationIssues: []
    };
  }
  return auditText(readmePath, read(readmePath));
}

function rowHasFailure(row) {
  return row.missing.length > 0 ||
    row.hasPlaceholder ||
    row.commandFormatIssues.length > 0 ||
    row.missingPackageScripts.length > 0 ||
    row.emptySections.length > 0 ||
    row.missingRelatedLinks ||
    row.brokenLinks.length > 0 ||
    row.sourceRegistrationIssues.length > 0;
}

function printRows(rows) {
  console.log('| Priority | Type | README | Missing sections | Empty sections | Placeholder | Command format | Package scripts | Related links | Broken links | Source registration |');
  console.log('|----------|------|--------|------------------|----------------|-------------|----------------|-----------------|-----------|--------------|---------------------|');
  for (const row of rows) {
    console.log(`| ${row.priority} | ${row.docType} | \`${row.readmePath}\` | ${row.missing.length ? row.missing.join(', ') : 'None'} | ${row.emptySections.length ? row.emptySections.join(', ') : 'None'} | ${row.hasPlaceholder ? 'Yes' : 'No'} | ${row.commandFormatIssues.length ? row.commandFormatIssues.join('<br>') : 'OK'} | ${row.missingPackageScripts.length ? row.missingPackageScripts.join('<br>') : 'OK'} | ${row.missingRelatedLinks ? 'Missing' : 'OK'} | ${row.brokenLinks.length ? row.brokenLinks.join(', ') : 'None'} | ${row.sourceRegistrationIssues.length ? row.sourceRegistrationIssues.join('<br>') : 'OK'} |`);
  }
}

function isEffectivelyEmpty(text) {
  const normalized = text
    .replace(/```[\s\S]*?```/g, '')
    .replace(/\|[-:\s|]+\|/g, '')
    .replace(/[*_\-\s`|:。；，,.]/g, '')
    .trim();
  return normalized.length < 4;
}

function registrationIssues(readmePath, text) {
  const issues = [];
  if (docTypeFor(readmePath) === 'management-view') {
    const capabilities = extractSection(text, '功能清单');
    const pageEntrypoints = extractSection(text, '页面入口');
    const menuPermissions = extractSection(text, '管理入口');
    if (!hasManagementCapabilityContent(capabilities)) {
      issues.push('management-capabilities');
    }
    if (!hasPageKeyContent(pageEntrypoints)) {
      issues.push('page-entrypoints');
    }
    if (!hasMenuPermissionContent(menuPermissions)) {
      issues.push('menu-permissions');
    }
    return issues;
  }

  if (!readmePath.startsWith('mango/mango-platform/') && !readmePath.startsWith('mango/mango-infra/')) {
    return issues;
  }

  const moduleRoot = moduleRootForBackendReadme(readmePath);
  if (!moduleRoot || !fs.existsSync(path.join(root, moduleRoot))) {
    return issues;
  }

  const source = collectModuleSource(moduleRoot);
  const databaseSection = extractSection(text, '数据与初始化');
  const menuSection = extractSection(text, '管理入口');
  const quickStartSection = extractSection(text, '快速开始');

  if (source.hasMigration && !hasMigrationContent(databaseSection)) {
    issues.push('migration-initialization');
  }
  if (source.hasInitializer && !hasInitializerContent(databaseSection)) {
    issues.push('runtime-initializer');
  }
  if (source.hasResourceManifest && !hasResourceManifestContent(databaseSection + '\n' + menuSection)) {
    issues.push('resource-manifest');
  }
  if ((source.hasPermissionCodes || source.hasResourceManifest) && !hasMenuPermissionContent(menuSection)) {
    issues.push('menu-permission-registry');
  }
  return issues;
}

function moduleRootForBackendReadme(readmePath) {
  const segments = readmePath.split('/');
  if (segments.length < 4 || segments[3] !== 'README.md') {
    return null;
  }
  return segments.slice(0, 3).join('/');
}

function collectModuleSource(moduleRoot) {
  const absoluteRoot = path.join(root, moduleRoot);
  const result = {
    hasMigration: false,
    hasInitializer: false,
    hasResourceManifest: false,
    hasPermissionCodes: false
  };
  for (const file of walkSourceFiles(absoluteRoot)) {
    const relativeFile = path.relative(root, file).split(path.sep).join('/');
    const text = fs.readFileSync(file, 'utf8');
    if (/\/src\/main\/resources\/db\/migration\//.test(relativeFile)) {
      result.hasMigration = true;
    }
    if (/\/META-INF\/mango\/resource-manifest(?:s\/[^/]+)?\.json$/.test(relativeFile)) {
      result.hasResourceManifest = true;
    }
    if (isRuntimeInitializer(relativeFile, text)) {
      result.hasInitializer = true;
    }
    if (/@ApiAccess\s*\([\s\S]{0,240}\bpermission\s*=|@PreAuthorize|hasAuthority|hasPermission|permissionCode|permissions"\s*:/.test(text)) {
      result.hasPermissionCodes = true;
    }
  }
  return result;
}

function isRuntimeInitializer(relativeFile, text) {
  if (/\/src\/test\//.test(relativeFile)) {
    return false;
  }
  if (/\b(ApplicationRunner|CommandLineRunner)\b|implements\s+SmartInitializingSingleton/.test(text)) {
    return true;
  }
  if (!/@PostConstruct|\bInitializingBean\b/.test(text)) {
    return false;
  }
  return /(?:Initializer|Bootstrap|Seeder|Registrar|Importer|Sync|Loader|Startup|Provision)/.test(relativeFile);
}

function walkSourceFiles(dir, results = []) {
  if (!fs.existsSync(dir)) return results;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (ignoredDirs.has(entry.name)) {
      continue;
    }
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'test') {
        continue;
      }
      walkSourceFiles(fullPath, results);
    } else if (sourceFileExtensions.has(path.extname(entry.name))) {
      results.push(fullPath);
    }
  }
  return results;
}

function hasManagementCapabilityContent(text) {
  return /管理|维护|查询|新增|编辑|删除|启用|停用|配置|审批|同步|导入|导出|上传|下载|预览|登录|认证|注销|密码|个人资料/.test(text) && text.length >= 20;
}

function hasPageKeyContent(text) {
  return /`[a-z][a-z0-9-]+\/[a-z0-9-\/]+\/index`/.test(text) ||
    /`\/[a-z][a-z0-9-\/:]*`/.test(text) ||
    /component key|页面 key|路由|页面入口/.test(text);
}

function hasMigrationContent(text) {
  return /db\/migration|Flyway|migration|V\d+__|数据表|默认数据|初始化数据|种子数据/.test(text);
}

function hasInitializerContent(text) {
  return /Runner|Importer|Initializer|启动初始化|启动时|应用启动|初始化器|幂等/.test(text);
}

function hasResourceManifestContent(text) {
  return /META-INF\/mango\/resource-manifest|resource[- ]manifest|资源清单|moduleCode|menuCode|appCode/.test(text);
}

function hasMenuPermissionContent(text) {
  return /权限码|permission|permissions|菜单|component|component key|页面 key|roleCodes|packageCodes|租户|tenant/.test(text);
}

function runSelfTest() {
  const valid = `# Demo

## 1. 概览
text
## 2. 功能清单
text
## 3. 适用场景
text
## 4. 边界说明
text
## 5. 接入方式
text
## 6. 配置说明
text
## 7. API 与扩展
text
## 8. 数据与初始化
migration 菜单 权限 SQL
## 9. 管理入口
菜单 component key 权限 permission tenant
## 10. 快速开始
migration 菜单 权限 初始化 接口 页面
## 11. 问题排查
text
## 12. 相关文档
- [能力说明维护规范](mango-pmo/rules/08-capability-docs.md)
`;
  const invalid = valid.replace('## 10. 快速开始\nmigration 菜单 权限 初始化 接口 页面', '## 10. 快速开始\n-');
  const validFrontendEntry = `# Demo Frontend Entry

## 1. 概览
text
## 2. 功能清单
text
## 3. 适用场景
text
## 4. 接入方式
text
## 5. 参数与事件
text
## 6. 后端依赖
text
## 7. 权限与数据边界
text
## 8. 快速开始
text
## 9. 问题排查
text
## 10. 相关文档
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
`;
  const validManagementView = `# Demo Views

## 1. 概览
后台管理页面入口。
## 2. 功能清单
支持用户查询、新增、编辑、删除和启用停用管理。
## 3. 页面入口
- \`demo/user/index\`
- \`/login\`
## 4. 后端依赖
- API 前缀：\`/demo/users\`。
## 5. 管理入口
- 菜单 component key 使用 \`demo/user/index\`，权限码为 \`demo:user:list\`，租户由后端校验。
## 6. 问题排查
菜单空白先查 component key。
## 7. 相关文档
- [能力说明维护规范](../../../../../mango-pmo/rules/08-capability-docs.md)
`;
  const cases = [
    { name: 'valid template passes', text: valid, valid: true },
    { name: 'valid frontend entry passes', path: 'mango-ui/packages/file/src/components/README.md', text: validFrontendEntry, valid: true },
    { name: 'valid management view passes', path: 'mango-ui/packages/demo/src/views/README.md', text: validManagementView, valid: true },
    { name: 'management view missing capability fails', path: 'mango-ui/packages/demo/src/views/README.md', text: validManagementView.replace('支持用户查询、新增、编辑、删除和启用停用管理。', 'text'), valid: false },
    { name: 'frontend entry missing section fails', path: 'mango-ui/packages/file/src/components/README.md', text: validFrontendEntry.replace('## 5. 参数与事件\ntext\n', ''), valid: false },
    { name: 'empty quick start fails', text: invalid, valid: false },
    { name: 'placeholder fails', text: `${valid}\nTODO`, valid: false },
    { name: 'backticked command fails', text: valid.replace('migration 菜单 权限 初始化 接口 页面', '`node mango-pmo/tools/audit-module-readmes.mjs`'), valid: false },
    { name: 'missing package script fails', text: valid.replace('migration 菜单 权限 初始化 接口 页面', 'pnpm -F @mango/api-schema missing-script'), valid: false },
    { name: 'missing related link fails', text: valid.replace('mango-pmo/rules/08-capability-docs.md', 'README.md'), valid: false },
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
