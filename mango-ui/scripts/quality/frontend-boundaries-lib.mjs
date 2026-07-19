import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

const SOURCE_EXTENSIONS = new Set(['.css', '.js', '.jsx', '.mjs', '.scss', '.ts', '.tsx', '.vue']);
const IGNORED_DIRECTORIES = new Set([
  '.git',
  '.runtime',
  'coverage',
  'dist',
  'node_modules',
  'playwright-report',
  'test-results',
]);

function posix(value) {
  return value.split(path.sep).join('/');
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&');
}

function normalizeEvidence(value) {
  return value.replace(/\s+/gu, ' ').trim().slice(0, 240);
}

function lineNumber(source, offset) {
  return source.slice(0, offset).split('\n').length;
}

function listFiles(directory) {
  if (!fs.existsSync(directory)) return [];
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.isDirectory() && IGNORED_DIRECTORIES.has(entry.name)) continue;
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...listFiles(absolute));
    else if (entry.isFile() && SOURCE_EXTENSIONS.has(path.extname(entry.name))) files.push(absolute);
  }
  return files;
}

function addViolation(violations, occurrences, rule, file, line, evidence) {
  const normalized = normalizeEvidence(evidence);
  const occurrenceKey = `${rule}|${file}|${normalized}`;
  const ordinal = (occurrences.get(occurrenceKey) || 0) + 1;
  occurrences.set(occurrenceKey, ordinal);
  violations.push({
    identity: `${occurrenceKey}|${ordinal}`,
    rule,
    file,
    line,
    evidence: normalized,
  });
}

function scanApiAndVendor(source, relative, violations, occurrences, isApiPackage = false) {
  const inApi = isApiPackage || /\/(?:api|apis)\//u.test(`/${relative}`);
  const inPresentation = /\/(?:components|pages|views)\//u.test(`/${relative}`);
  const ownsVendor = relative.startsWith('packages/app-runtime/');
  const lines = source.split('\n');

  lines.forEach((line, index) => {
    const number = index + 1;
    const typeOnlyImport = /^\s*import\s+type\b/u.test(line);
    if (inApi && /from\s+['"](?:vue|vue-router|pinia|element-plus|@element-plus\/)/u.test(line)) {
      addViolation(violations, occurrences, 'api/no-ui-framework', relative, number, line);
    }
    if (
      inApi &&
      ((!typeOnlyImport && /from\s+['"]axios['"]/u.test(line)) || /\b(?:fetch|axios\.create)\s*\(/u.test(line))
    ) {
      addViolation(violations, occurrences, 'api/no-direct-transport', relative, number, line);
    }
    if (inApi && /\b(?:import\.meta\.env|process\.env)\b/u.test(line) && !/^\s*\/\//u.test(line)) {
      addViolation(violations, occurrences, 'api/no-environment-access', relative, number, line);
    }
    if (inApi && /\b(?:get|post|put|patch|del|request)\s*(?:<[^>]+>)?\s*\(\s*['"]https?:\/\//u.test(line)) {
      addViolation(violations, occurrences, 'api/no-absolute-endpoint', relative, number, line);
    }
    if (inApi && /\btype\s+ApiId\s*=\s*number\b/u.test(line)) {
      addViolation(violations, occurrences, 'api/string-api-id', relative, number, line);
    }
    if (
      inPresentation &&
      ((!typeOnlyImport && /from\s+['"]axios['"]/u.test(line)) ||
        /from\s+['"]@mango\/common\/utils\/request['"]/u.test(line) ||
        /import\s*\{[^}]*\b(?:get|post|put|patch|del|request)\b[^}]*\}\s*from\s*['"]@mango\/common['"]/u.test(line) ||
        /\b(?:fetch|axios\.create)\s*\(/u.test(line))
    ) {
      addViolation(violations, occurrences, 'layer/presentation-no-transport', relative, number, line);
    }
    if (
      !ownsVendor &&
      (/from\s+['"]wujie['"]/u.test(line) ||
        /import\s*\(\s*['"]wujie['"]\s*\)/u.test(line) ||
        /\$(?:wujie)\b|__(?:POWERED_BY_)?WUJIE_/u.test(line))
    ) {
      addViolation(violations, occurrences, 'microfrontend/vendor-symbol-outside-adapter', relative, number, line);
    }
    if (/from\s+['"]@mango\/[^'"]+\/src\/[^'"]+\.(?:css|scss)['"]/u.test(line)) {
      addViolation(violations, occurrences, 'css/no-package-source-style-import', relative, number, line);
    }
  });
}

function scanBusinessApiPackageContracts(uiRoot, records, sourceByFile, violations, occurrences) {
  for (const record of records.filter(({ manifest }) => manifest.name?.endsWith('-api'))) {
    const packageRoot = posix(path.relative(uiRoot, record.directory));
    const combinedSource = [...sourceByFile.entries()]
      .filter(([file]) => file.startsWith(`${packageRoot}/`))
      .map(([, source]) => source)
      .join('\n');
    const relativeManifest = `${packageRoot}/package.json`;
    if (!/\bHttpClient\b/u.test(combinedSource)) {
      addViolation(
        violations,
        occurrences,
        'api/package-must-use-http-client',
        relativeManifest,
        1,
        `${record.manifest.name} must accept the vendor-neutral HttpClient contract`,
      );
    }
    if (!/export\s+function\s+create[A-Z][A-Za-z0-9]*Api\s*\([^)]*\bHttpClient\b/u.test(combinedSource)) {
      addViolation(
        violations,
        occurrences,
        'api/package-must-export-factory',
        relativeManifest,
        1,
        `${record.manifest.name} must export createXxxApi(client: HttpClient)`,
      );
    }
    const forbiddenDependencies = ['axios', 'element-plus', 'pinia', 'vue', 'vue-router'].filter(
      (dependency) => record.manifest.dependencies?.[dependency] || record.manifest.peerDependencies?.[dependency],
    );
    if (forbiddenDependencies.length > 0) {
      addViolation(
        violations,
        occurrences,
        'api/package-no-ui-transport-dependency',
        relativeManifest,
        1,
        `${record.manifest.name} declares forbidden dependencies: ${forbiddenDependencies.join(', ')}`,
      );
    }
  }
}

function scanVueStyles(source, relative, violations, occurrences) {
  if (!relative.endsWith('.vue') || !/\/(?:pages|views)\//u.test(`/${relative}`)) return;
  const stylePattern = /<style\b([^>]*)>([\s\S]*?)<\/style>/gu;
  for (const match of source.matchAll(stylePattern)) {
    const attributes = match[1] || '';
    if (/\b(?:scoped|module)\b/u.test(attributes)) continue;
    const number = lineNumber(source, match.index || 0);
    addViolation(
      violations,
      occurrences,
      'css/page-style-must-be-scoped-or-module',
      relative,
      number,
      match[0].split('\n')[0],
    );
    if (/\.el-[\w-]+/u.test(match[2])) {
      addViolation(
        violations,
        occurrences,
        'css/no-global-element-plus-page-override',
        relative,
        number,
        match[0].split('\n')[0],
      );
    }
  }
}

function readWorkspaceManifests(uiRoot) {
  const records = [];
  for (const kind of ['apps', 'packages']) {
    const parent = path.join(uiRoot, kind);
    if (!fs.existsSync(parent)) continue;
    for (const entry of fs.readdirSync(parent, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const file = path.join(parent, entry.name, 'package.json');
      if (!fs.existsSync(file)) continue;
      records.push({ kind, directory: path.dirname(file), manifest: JSON.parse(fs.readFileSync(file, 'utf8')) });
    }
  }
  return records;
}

function scanMicroStyleContracts(uiRoot, records, sourceByFile, violations, occurrences) {
  const stylePackages = new Map(
    records.flatMap(({ manifest }) => {
      const styleExports = Object.keys(manifest.exports || {}).filter((key) => /\.(?:css|scss)$/u.test(key));
      return styleExports.length > 0 ? [[manifest.name, styleExports]] : [];
    }),
  );
  for (const record of records.filter(
    ({ kind, manifest }) =>
      kind === 'apps' &&
      manifest.mangoArchitecture?.role === 'app' &&
      typeof manifest.dependencies?.['@mango/app-runtime'] === 'string' &&
      manifest.name.endsWith('-app'),
  )) {
    const combinedSource = [...sourceByFile.entries()]
      .filter(([file]) => file.startsWith(`${posix(path.relative(uiRoot, record.directory))}/`))
      .map(([, source]) => source)
      .join('\n');
    for (const [dependency, styleExports] of stylePackages) {
      const codeImport = new RegExp(
        `(?:from\\s+|import\\s*)["']${escapeRegExp(dependency)}(?:["']|\\/(?![^"']+\\.(?:css|scss)["']))`,
        'u',
      );
      if (!codeImport.test(combinedSource)) continue;
      const importsStyle = styleExports.some((key) => {
        const specifier = `${dependency}/${key.replace(/^\.\//u, '')}`;
        return combinedSource.includes(`'${specifier}'`) || combinedSource.includes(`"${specifier}"`);
      });
      if (!importsStyle) {
        const relativeManifest = `${posix(path.relative(uiRoot, record.directory))}/package.json`;
        addViolation(
          violations,
          occurrences,
          'css/micro-app-explicit-package-style',
          relativeManifest,
          1,
          `${record.manifest.name} must import one of ${dependency} ${styleExports.join(',')}`,
        );
      }
    }
  }
}

export function analyzeFrontendBoundaries(uiRoot) {
  const roots = ['apps', 'packages'].map((item) => path.join(uiRoot, item));
  const files = roots.flatMap(listFiles).sort();
  if (files.length === 0) throw new Error('frontend boundary scan input is empty');
  const records = readWorkspaceManifests(uiRoot);
  if (records.length === 0) throw new Error('frontend workspace manifest input is empty');
  const apiPackageRoots = records
    .filter(({ manifest }) => manifest.name?.endsWith('-api'))
    .map(({ directory }) => posix(path.relative(uiRoot, directory)));
  const violations = [];
  const occurrences = new Map();
  const sourceByFile = new Map();
  let vendorAdapterReferenceCount = 0;
  for (const file of files) {
    const relative = posix(path.relative(uiRoot, file));
    const source = fs.readFileSync(file, 'utf8');
    sourceByFile.set(relative, source);
    if (
      relative.startsWith('packages/app-runtime/') &&
      /(?:from\s+['"]wujie['"]|import\s*\(\s*['"]wujie['"]\s*\)|\$(?:wujie)\b|__WUJIE_)/u.test(source)
    ) {
      vendorAdapterReferenceCount += 1;
    }
    scanApiAndVendor(
      source,
      relative,
      violations,
      occurrences,
      apiPackageRoots.some((root) => relative.startsWith(`${root}/`)),
    );
    scanVueStyles(source, relative, violations, occurrences);
  }
  scanBusinessApiPackageContracts(uiRoot, records, sourceByFile, violations, occurrences);
  scanMicroStyleContracts(uiRoot, records, sourceByFile, violations, occurrences);
  if (vendorAdapterReferenceCount === 0)
    throw new Error('microfrontend vendor adapter owner is missing or has no implementation');
  violations.sort((left, right) => left.identity.localeCompare(right.identity));
  const byDomain = { api: 0, css: 0, layer: 0, microfrontend: 0 };
  for (const violation of violations) byDomain[violation.rule.split('/')[0]] += 1;
  const report = {
    schemaVersion: 1,
    scannedFileCount: files.length,
    workspaceCount: records.length,
    vendorAdapterReferenceCount,
    violations,
    summary: { violationCount: violations.length, byDomain },
  };
  report.reportSha256 = crypto.createHash('sha256').update(JSON.stringify(report)).digest('hex');
  return report;
}

export function createFrontendBoundaryBaseline(report) {
  return {
    schemaVersion: 1,
    ownerRole: 'Frontend Standards Owner',
    policy: 'exact-identities-only-decrease',
    violations: report.violations.map(({ identity, rule, file, evidence }) => ({ identity, rule, file, evidence })),
  };
}

export function validateFrontendBoundaryBaseline(baseline) {
  const failures = [];
  if (baseline?.schemaVersion !== 1) failures.push('frontend boundary baseline schemaVersion must be 1');
  if (baseline?.ownerRole !== 'Frontend Standards Owner')
    failures.push('frontend boundary baseline ownerRole is invalid');
  if (baseline?.policy !== 'exact-identities-only-decrease')
    failures.push('frontend boundary baseline policy is invalid');
  if (!Array.isArray(baseline?.violations)) failures.push('frontend boundary baseline violations must be an array');
  const identities = new Set();
  for (const item of baseline?.violations || []) {
    if (!item.identity || !item.rule || !item.file || !item.evidence)
      failures.push('frontend boundary baseline entry is incomplete');
    if (identities.has(item.identity)) failures.push(`duplicate frontend boundary baseline identity: ${item.identity}`);
    identities.add(item.identity);
  }
  return failures;
}

export function compareFrontendBoundaryReport(report, baseline) {
  const allowed = new Set((baseline.violations || []).map((item) => item.identity));
  return report.violations
    .filter((item) => !allowed.has(item.identity))
    .map((item) => `new frontend boundary violation ${item.identity}`);
}

export function compareFrontendBoundaryBaselines(current, base) {
  const baseIdentities = new Set((base.violations || []).map((item) => item.identity));
  return (current.violations || [])
    .filter((item) => !baseIdentities.has(item.identity))
    .map((item) => `frontend boundary baseline debt may not increase: ${item.identity}`);
}
