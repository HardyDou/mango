#!/usr/bin/env node
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const args = process.argv.slice(2);
const selfTest = args.includes('--self-test');

const ignoredDirs = new Set(['.git', 'node_modules', 'target', 'dist', 'coverage', '.turbo']);
const sourceExtensions = new Set(['.java', '.kt', '.ts', '.tsx', '.js', '.jsx', '.vue', '.json', '.yml', '.yaml', '.properties', '.xml', '.sql']);

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

const docsReadmes = [
  'mango-docs/capabilities/README.md'
];

function walkFiles(dir, predicate, results = []) {
  if (!fs.existsSync(dir)) return results;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (ignoredDirs.has(entry.name)) continue;
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkFiles(fullPath, predicate, results);
    } else if (predicate(fullPath)) {
      results.push(fullPath);
    }
  }
  return results;
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function relative(file) {
  return path.relative(root, file).split(path.sep).join('/');
}

function managedReadmes(base = root) {
  const readmes = new Set([...topLevelReadmes, ...frontendEntryReadmes, ...docsReadmes]);
  for (const moduleRoot of moduleRoots) {
    const absoluteRoot = path.join(base, moduleRoot);
    if (!fs.existsSync(absoluteRoot)) continue;
    for (const entry of fs.readdirSync(absoluteRoot, { withFileTypes: true })) {
      if (entry.isDirectory() && !ignoredDirs.has(entry.name)) {
        readmes.add(path.join(moduleRoot, entry.name, 'README.md').split(path.sep).join('/'));
      }
    }
    for (const file of walkFiles(absoluteRoot, (candidate) => path.basename(candidate) === 'README.md')) {
      readmes.add(path.relative(base, file).split(path.sep).join('/'));
    }
  }
  return [...readmes].filter((file) => fs.existsSync(path.join(base, file))).sort();
}

function textAround(text, index, length = 120) {
  return text.slice(Math.max(0, index - length), Math.min(text.length, index + length));
}

function unique(values) {
  return [...new Set(values)].sort();
}

function markdownCodeSpans(text) {
  return [...text.matchAll(/`([^`\n]+)`/g)].map((match) => match[1]);
}

function stripMarkdownLinks(text) {
  return text.replace(/\[[^\]]+\]\([^)]+\)/g, '');
}

function extractArtifactIdsFromPom(text) {
  return [...text.matchAll(/<artifactId>\s*([^<\s]+)\s*<\/artifactId>/g)]
    .map((match) => match[1])
    .filter((artifactId) => artifactId.startsWith('mango-'));
}

function sourceIndex(base = root) {
  const artifactIds = new Set();
  const packageNames = new Set();
  const endpointPaths = new Set();
  const configKeys = new Set();
  const sourceTextChunks = [];

  const pomFiles = walkFiles(base, (file) => path.basename(file) === 'pom.xml');
  for (const file of pomFiles) {
    const text = fs.readFileSync(file, 'utf8');
    sourceTextChunks.push(text);
    for (const artifactId of extractArtifactIdsFromPom(text)) {
      artifactIds.add(artifactId);
    }
  }

  const packageJsonFiles = walkFiles(path.join(base, 'mango-ui/packages'), (file) => path.basename(file) === 'package.json');
  for (const file of packageJsonFiles) {
    const text = fs.readFileSync(file, 'utf8');
    sourceTextChunks.push(text);
    try {
      const parsed = JSON.parse(text);
      if (typeof parsed.name === 'string' && parsed.name.startsWith('@mango/')) {
        packageNames.add(parsed.name);
      }
    } catch {
      // package JSON validity is checked by package tooling; this audit only indexes usable metadata.
    }
  }

  const sourceRoots = ['mango', 'mango-ui/packages', 'mango-business-starter'];
  for (const sourceRoot of sourceRoots) {
    for (const file of walkFiles(path.join(base, sourceRoot), (candidate) => sourceExtensions.has(path.extname(candidate)))) {
      const text = fs.readFileSync(file, 'utf8');
      sourceTextChunks.push(text);
      indexJavaMappings(text, endpointPaths);
      indexConfigKeys(text, configKeys);
    }
  }

  const sourceText = sourceTextChunks.join('\n');
  for (const match of sourceText.matchAll(/mango(?:\.[a-z][a-z0-9-]*){1,8}/g)) {
    configKeys.add(match[0]);
  }

  return { artifactIds, packageNames, endpointPaths, configKeys, sourceText };
}

function indexJavaMappings(text, endpointPaths) {
  const classMatch = text.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["'][\s\S]{0,800}?\bclass\s+\w+/);
  const requestMappingPrefix = classMatch ? normalizeApiPath(classMatch[1]) : '';
  const feignMatch = text.match(/@FeignClient\s*\([\s\S]{0,500}?\bpath\s*=\s*["']([^"']+)["']/);
  const feignPrefix = feignMatch ? normalizeApiPath(feignMatch[1]) : '';
  const classPrefixes = [requestMappingPrefix, feignPrefix].filter(Boolean);
  for (const classPrefix of classPrefixes) {
    endpointPaths.add(classPrefix);
  }

  const mappingPattern = /@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\s*(?:\(\s*(?:(?:value|path)\s*=\s*)?["']([^"']*)["']|$)/g;
  for (const match of text.matchAll(mappingPattern)) {
    const raw = match[2];
    if (raw === undefined) continue;
    const pathValue = normalizeApiPath(raw);
    if (pathValue) endpointPaths.add(pathValue);
    for (const classPrefix of classPrefixes) {
      endpointPaths.add(joinApiPath(classPrefix, pathValue));
    }
  }
}

function indexConfigKeys(text, configKeys) {
  const patterns = [
    /@ConfigurationProperties\s*\(\s*prefix\s*=\s*["']([^"']+)["']/g,
    /@ConditionalOnProperty\s*\(\s*prefix\s*=\s*["']([^"']+)["']/g,
    /@Value\s*\(\s*["']\$\{([^}:]+)[^"']*["']\s*\)/g
  ];
  for (const pattern of patterns) {
    for (const match of text.matchAll(pattern)) {
      if (match[1].startsWith('mango.')) configKeys.add(match[1]);
    }
  }
}

function normalizeApiPath(value) {
  if (!value || value === '/') return '';
  return value.startsWith('/') ? value : `/${value}`;
}

function joinApiPath(prefix, child) {
  if (!prefix && !child) return '';
  if (!child) return prefix;
  if (!prefix) return child;
  return `${prefix.replace(/\/$/, '')}/${child.replace(/^\//, '')}`;
}

function extractArtifacts(text) {
  const values = [];
  for (const span of markdownCodeSpans(text)) {
    for (const match of span.matchAll(/\b(mango-[a-z0-9][a-z0-9-]*(?:-(?:api|core|starter|starter-remote|support|web-starter|gateway-starter|resource-[a-z0-9-]+|channel-[a-z0-9-]+|engine)))\b/g)) {
      values.push(match[1]);
    }
  }
  for (const match of text.matchAll(/<artifactId>\s*(mango-[^<\s]+)\s*<\/artifactId>/g)) {
    values.push(match[1]);
  }
  values.push(...extractMavenContextArtifacts(text));
  return unique(values);
}

function artifactLooksLikeRepoPath(value) {
  return fs.existsSync(path.join(root, value)) || fs.existsSync(path.join(root, 'mango', value));
}

function extractMavenContextArtifacts(text) {
  const values = [];
  for (const match of text.matchAll(/\b(mango-[a-z0-9][a-z0-9-]*(?:-(?:api|core|starter|starter-remote|support|web-starter|gateway-starter|resource-[a-z0-9-]+|channel-[a-z0-9-]+|engine)))\b/g)) {
    const around = textAround(text, match.index, 80).toLowerCase();
    if (around.includes('<artifactid') || around.includes('artifact') || around.includes('依赖') || around.includes('引入') || around.includes('pom') || around.includes('starter')) {
      values.push(match[1]);
    }
  }
  return unique(values);
}

function extractApiPaths(text) {
  const cleaned = stripMarkdownLinks(text);
  const values = [];
  for (const span of markdownCodeSpans(cleaned)) {
    for (const match of span.matchAll(/(?:GET|POST|PUT|DELETE|PATCH)?\s*(\/[a-z][a-z0-9-]*(?:\/[a-z0-9:_{}.-]+)+)/gi)) {
      if (!shouldSkipApiPath(match[1])) values.push(match[1]);
    }
  }
  for (const match of cleaned.matchAll(/\b(?:GET|POST|PUT|DELETE|PATCH)\s+(\/[a-z][a-z0-9-]*(?:\/[a-z0-9:_{}.-]+)+)/gi)) {
    if (!shouldSkipApiPath(match[1])) values.push(match[1]);
  }
  return unique(values);
}

function shouldSkipApiPath(value) {
  return value.includes('.') ||
    value.includes('{') ||
    value.includes('}') ||
    value.startsWith('/api/') ||
    value.startsWith('/src/') ||
    value.startsWith('/packages/') ||
    value.startsWith('/scripts/') ||
    value.startsWith('/tools/') ||
    value.startsWith('/mango-platform/') ||
    value.startsWith('/mango-infra/') ||
    value.startsWith('/topologies/') ||
    value.startsWith('/business-pmo/') ||
    value.startsWith('/spring/') ||
    value.startsWith('/migration/') ||
    value.startsWith('/mango-baseline/') ||
    value.startsWith('/core/starter') ||
    value.includes('module') ||
    value.includes('example') ||
    value.endsWith('.md') ||
    value.includes('/README') ||
    value.includes('/rules/');
}

function extractConfigKeys(text) {
  return unique([...text.matchAll(/\bmango(?:\.[a-z][a-z0-9-]*){1,8}\b/g)]
    .map((match) => match[0])
    .filter((value) => !value.startsWith('mango.docs') && !value.startsWith('mango.pmo'))
    .filter((value) => !/\.(json|yml|yaml|xml|md|ts|tsx|java|sql)$/.test(value)));
}

function extractPackageNames(text) {
  return unique([...text.matchAll(/@mango\/[a-z0-9][a-z0-9-]*/g)].map((match) => match[0]));
}

function extractPageKeys(text) {
  const values = [];
  for (const span of markdownCodeSpans(text)) {
    for (const match of span.matchAll(/\b([a-z][a-z0-9-]+\/[a-z][a-z0-9-]+\/index)\b/g)) {
      if (match[1].startsWith('mango-baseline/')) continue;
      values.push(match[1]);
    }
  }
  return unique(values);
}

function hasConfigFact(index, value) {
  if (index.sourceText.includes(value)) return true;
  for (const sourceValue of index.configKeys) {
    if (value === sourceValue || value.startsWith(`${sourceValue}.`) || sourceValue.startsWith(`${value}.`)) {
      return true;
    }
  }
  return false;
}

function hasApiPath(index, value) {
  return index.sourceText.includes(value) || index.endpointPaths.has(value);
}

function hasPageKey(readmePath, index, value) {
  if (index.sourceText.includes(value)) return true;
  if (!readmePath.startsWith('mango-ui/packages/')) return false;
  const segments = readmePath.split('/');
  const packageRoot = path.join(root, segments.slice(0, 3).join('/'));
  const viewPath = path.join(packageRoot, 'src/views', value.replace(/^[^/]+\//, ''), '.vue');
  const normalizedViewPath = viewPath.replace('/.vue', '.vue');
  return fs.existsSync(normalizedViewPath);
}

function auditText(readmePath, text, index) {
  const artifacts = extractArtifacts(text);
  const apiPaths = extractApiPaths(text);
  const configKeys = extractConfigKeys(text);
  const packageNames = extractPackageNames(text);
  const pageKeys = extractPageKeys(text);

  const missingArtifacts = artifacts.filter((value) => !index.artifactIds.has(value));
  const missingApiPaths = apiPaths.filter((value) => !hasApiPath(index, value));
  const missingConfigKeys = configKeys.filter((value) => !hasConfigFact(index, value));
  const missingPackageNames = packageNames.filter((value) => !index.packageNames.has(value));
  const missingPageKeys = pageKeys.filter((value) => !hasPageKey(readmePath, index, value));

  return {
    readmePath,
    artifacts,
    apiPaths,
    configKeys,
    packageNames,
    pageKeys,
    issues: [
      ...missingArtifacts.filter((value) => !artifactLooksLikeRepoPath(value)).map((value) => `artifact:${value}`),
      ...missingApiPaths.map((value) => `api:${value}`),
      ...missingConfigKeys.map((value) => `config:${value}`),
      ...missingPackageNames.map((value) => `package:${value}`),
      ...missingPageKeys.map((value) => `page:${value}`)
    ]
  };
}

function printRows(rows) {
  console.log('| README | Artifacts | API paths | Config keys | Packages | Page keys | Issues |');
  console.log('|--------|-----------|-----------|-------------|----------|-----------|--------|');
  for (const row of rows) {
    console.log(`| \`${row.readmePath}\` | ${row.artifacts.length} | ${row.apiPaths.length} | ${row.configKeys.length} | ${row.packageNames.length} | ${row.pageKeys.length} | ${row.issues.length ? row.issues.join('<br>') : 'OK'} |`);
  }
}

function runSelfTest() {
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-readme-facts-'));
  try {
    fs.mkdirSync(path.join(temp, 'mango/mango-platform/demo/demo-starter/src/main/java/demo'), { recursive: true });
    fs.mkdirSync(path.join(temp, 'mango-ui/packages/demo/src'), { recursive: true });
    fs.mkdirSync(path.join(temp, 'mango-docs/capabilities'), { recursive: true });
    fs.writeFileSync(path.join(temp, 'mango/mango-platform/demo/demo-starter/pom.xml'), '<project><artifactId>mango-demo-starter</artifactId></project>');
    fs.writeFileSync(path.join(temp, 'mango-ui/packages/demo/package.json'), JSON.stringify({ name: '@mango/demo' }));
    fs.writeFileSync(path.join(temp, 'mango-docs/capabilities/README.md'), '能力地图引用 `mango-demo-starter`。');
    fs.writeFileSync(path.join(temp, 'mango/mango-platform/demo/demo-starter/src/main/java/demo/DemoController.java'), `
@ConfigurationProperties(prefix = "mango.demo")
@RequestMapping("/demo/items")
class DemoController {
  @GetMapping("/page")
  void page() {}
}
`);
    fs.writeFileSync(path.join(temp, 'mango-ui/packages/demo/src/pages.ts'), "export const page = 'demo/items/index';");
    const index = sourceIndex(temp);
    const managed = managedReadmes(temp);
    const valid = auditText('README.md', `
依赖 \`mango-demo-starter\`
接口 \`GET /demo/items/page\`
配置 \`mango.demo.enabled\`
前端包 \`@mango/demo\`
页面 \`demo/items/index\`
`, index);
    const validNaturalLanguageArtifact = auditText('README.md', `
业务服务需要引入 mango-demo-starter 依赖。
`, index);
    const invalid = auditText('README.md', `
依赖 \`mango-missing-starter\`
接口 \`GET /missing/items/page\`
配置 \`mango.missing.enabled\`
前端包 \`@mango/missing\`
页面 \`missing/items/index\`
`, index);
    const failures = [];
    if (!managed.includes('mango-docs/capabilities/README.md')) failures.push('capability map README should be audited');
    if (valid.issues.length > 0) failures.push(`valid facts should pass: ${valid.issues.join(', ')}`);
    if (validNaturalLanguageArtifact.issues.length > 0) failures.push(`natural language artifact should pass: ${validNaturalLanguageArtifact.issues.join(', ')}`);
    if (invalid.issues.length !== 5) failures.push(`invalid facts should report 5 issues, got ${invalid.issues.length}: ${invalid.issues.join(', ')}`);
    if (failures.length > 0) {
      console.error(`README source facts audit self-test failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
      process.exit(1);
    }
    console.log('README source facts audit self-test passed: 4 cases');
  } finally {
    fs.rmSync(temp, { recursive: true, force: true });
  }
}

if (selfTest) {
  runSelfTest();
  process.exit(0);
}

const index = sourceIndex(root);
const rows = managedReadmes(root).map((readmePath) => auditText(readmePath, read(readmePath), index));
printRows(rows);

const failures = rows.filter((row) => row.issues.length > 0);
if (failures.length > 0) {
  console.error(`README source facts audit failed: ${failures.length} file(s) contain facts that were not found in source`);
  process.exit(1);
}
