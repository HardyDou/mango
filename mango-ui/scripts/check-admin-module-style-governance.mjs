#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(fileURLToPath(new URL('..', import.meta.url)));
const failures = [];

const adminPackageJson = readJson('packages/admin/package.json');
const adminModules = readJson('packages/admin/admin-modules.json');
const adminManifest = readJson('packages/admin/admin-packages.json');
const adminBuildStyleDeps = readText('packages/admin/build-style-deps.mjs');
const generatedStyles = readText('packages/admin/generated-package-styles.css');
const styleFull = readText('packages/admin/style-full.css');
const fullEntry = readText('packages/admin/src/full.ts');
const fullTypes = readText('packages/admin/src/full.d.ts');
const cliSource = readText('packages/mango-cli/src/index.mjs');
const cliAdminModules = readJson('packages/mango-cli/admin-modules.json');
const cliPackageJson = readJson('packages/mango-cli/package.json');
const paymentStyle = readText('packages/payment/style.css');
const mangoAliases = readText('build-config/mangoAliases.ts');

const defaultModules = normalizeModules(adminModules.defaultPackages);
const fullModules = normalizeModules(adminModules.fullPackages);
const officialModules = [...defaultModules, ...fullModules].filter((module) => module.registrars.length > 0);

assertNoDuplicateModules([...defaultModules, ...fullModules]);
assertGeneratedDefaultManifest(defaultModules);
assertFullStyleIncludesDefaultEntry();
assertPaymentIsFullOnly();
assertPaymentStyleScoped();
assertWorkspaceAliasesUseAdminModules();
assertBuildStyleDepsGenerated();
assertCliAdminModulesPackaged();

for (const module of [...defaultModules, ...fullModules]) {
  assertPackageStyleExport(module);
  assertAdminDependency(module);
  assertStyleAggregation(module);
}

for (const module of officialModules) {
  assertFullEntry(module);
  assertCliModule(module);
}

if (failures.length > 0) {
  console.error(`Admin module style governance check failed:\n${failures.map((item) => `- ${item}`).join('\n')}`);
  process.exit(1);
}

console.log(`Admin module style governance check passed: ${officialModules.length} official modules.`);

function normalizeModules(items) {
  if (!Array.isArray(items)) {
    return [];
  }
  return items.map((item) => ({
    ...item,
    packageName: item.packageName || item.name,
    registrars: item.registrars || [],
  }));
}

function assertNoDuplicateModules(modules) {
  const seen = new Set();
  for (const module of modules) {
    if (!module.packageName || !module.style) {
      failures.push('admin-modules.json module entries must include packageName and style');
      continue;
    }
    if (seen.has(module.packageName)) {
      failures.push(`admin-modules.json must not declare ${module.packageName} more than once`);
    }
    seen.add(module.packageName);
  }
}

function assertGeneratedDefaultManifest(defaultModules) {
  const expectedPackages = defaultModules.map((module) => ({
    name: module.packageName,
    style: module.style,
  }));
  if (JSON.stringify(adminManifest.packages) !== JSON.stringify(expectedPackages)) {
    failures.push('admin-packages.json must be generated from admin-modules.json defaultPackages');
  }
}

function assertFullStyleIncludesDefaultEntry() {
  if (!styleFull.includes("@import './style.css';")) {
    failures.push("style-full.css must import './style.css' for default admin styles");
  }
}

function assertPaymentIsFullOnly() {
  if (defaultModules.some((module) => module.packageName === '@mango/payment')) {
    failures.push('@mango/payment must stay out of default @mango/admin/style.css aggregation');
  }
}

function assertPaymentStyleScoped() {
  const forbiddenPatterns = [
    "body [class^='payment-']",
    'body .payment-',
    "[class^='payment-'][class$='__toolbar'] .el-form-item .el-select",
  ];
  for (const pattern of forbiddenPatterns) {
    if (paymentStyle.includes(pattern)) {
      failures.push(`@mango/payment style must not use cross-page selector: ${pattern}`);
    }
  }
}

function assertWorkspaceAliasesUseAdminModules() {
  const requiredFragments = [
    'packages/admin/admin-modules.json',
    'getConfiguredStylePackages(repoRoot)',
    'getSourcePackageEntries(repoRoot)',
    'readAdminModulesManifest',
  ];
  for (const fragment of requiredFragments) {
    if (!mangoAliases.includes(fragment)) {
      failures.push(`mangoAliases.ts package style aliases must be derived from admin-modules.json: missing ${fragment}`);
    }
  }

  const forbiddenFragments = [
    'const STYLE_PACKAGES',
    'const PACKAGE_ENTRIES',
    "'@mango/admin-shell/style.css'",
    '"@mango/admin-shell/style.css"',
  ];
  for (const fragment of forbiddenFragments) {
    if (mangoAliases.includes(fragment)) {
      failures.push(`mangoAliases.ts must not hardcode package style alias list or package style exceptions: ${fragment}`);
    }
  }

  for (const module of [...defaultModules, ...fullModules]) {
    const styleAlias = `${module.packageName}/style.css`;
    if (styleAlias !== '@mango/admin/style.css' && mangoAliases.includes(`find: '${styleAlias}'`)) {
      failures.push(`mangoAliases.ts must not hardcode style alias ${styleAlias}; edit admin-modules.json instead`);
    }

    for (const registrar of module.registrars) {
      const entryName = registrar.import.replace(`${module.packageName}/`, '');
      if (mangoAliases.includes(`'${entryName}': 'src/${entryName}.ts'`)) {
        failures.push(`mangoAliases.ts must not hardcode official module entry ${registrar.import}; edit admin-modules.json instead`);
      }
    }
  }
}

function assertBuildStyleDepsGenerated() {
  if (!adminBuildStyleDeps.includes('Generated from packages/admin/admin-modules.json')) {
    failures.push('packages/admin/build-style-deps.mjs must be generated from admin-modules.json');
  }
  if (adminPackageJson.scripts?.['build:style-deps'] !== 'node ./build-style-deps.mjs') {
    failures.push('@mango/admin build:style-deps must call the generated build-style-deps.mjs script');
  }
  if (!adminPackageJson.scripts?.['generate:styles']?.includes('--build-deps-script-out')) {
    failures.push('@mango/admin generate:styles must regenerate build-style-deps.mjs');
  }
  if (!adminPackageJson.scripts?.['check:styles']?.includes('--build-deps-script-out')) {
    failures.push('@mango/admin check:styles must verify build-style-deps.mjs');
  }
  for (const module of [...defaultModules, ...fullModules]) {
    if (!adminBuildStyleDeps.includes(`'${module.packageName}'`)) {
      failures.push(`build-style-deps.mjs must build ${module.packageName}`);
    }
  }
  for (const dependencyName of Object.keys(adminPackageJson.dependencies || {})) {
    if (
      dependencyName.startsWith('@mango/') &&
      hasWorkspaceBuildScript(dependencyName) &&
      !adminBuildStyleDeps.includes(`'${dependencyName}'`)
    ) {
      failures.push(`build-style-deps.mjs must build admin dependency ${dependencyName}`);
    }
  }
}

function assertPackageStyleExport(module) {
  const packageJson = readJson(`packages/${packageFolder(module.packageName)}/package.json`);
  if (!packageJson.exports?.['./style.css']) {
    failures.push(`${module.packageName} must export ./style.css`);
  }
  const styleExport = readStyleExportTarget(packageJson);
  if (!styleExport) {
    failures.push(`${module.packageName} must keep a resolvable ./style.css export target`);
  }
  const packageSourceStyle = resolve(root, `packages/${packageFolder(module.packageName)}/style.css`);
  if (styleExport === './style.css' && !existsSync(packageSourceStyle)) {
    failures.push(`${module.packageName} ./style.css export must point to an existing package-level file`);
  }
}

function assertFullEntry(module) {
  for (const registrar of module.registrars) {
    if (!fullEntry.includes(`from '${registrar.import}'`)) {
      failures.push(`@mango/admin/full must import/export ${registrar.import}`);
    }
    if (!fullTypes.includes(`from '${registrar.import}'`)) {
      failures.push(`@mango/admin/full types must export ${registrar.import}`);
    }
    if (!fullEntry.includes(registrar.name)) {
      failures.push(`@mango/admin/full must register ${registrar.name}`);
    }
  }
}

function assertAdminDependency(module) {
  const inDefaultStyle = adminManifest.packages.some((item) => item.name === module.packageName);
  const inFullOnly = fullModules.some((item) => item.packageName === module.packageName);
  const hasDirectDependency = Boolean(adminPackageJson.dependencies?.[module.packageName]);
  const hasOptionalPeer = Boolean(
    adminPackageJson.peerDependencies?.[module.packageName] &&
      adminPackageJson.peerDependenciesMeta?.[module.packageName]?.optional,
  );

  if (inDefaultStyle) {
    if (!hasDirectDependency) {
      failures.push(`@mango/admin dependencies must include default style package ${module.packageName}`);
    }
    if (adminPackageJson.peerDependencies?.[module.packageName]) {
      failures.push(`@mango/admin peerDependencies must not duplicate default style package ${module.packageName}`);
    }
    return;
  }

  if (inFullOnly && hasDirectDependency) {
    failures.push(`@mango/admin dependencies must not include full-only package ${module.packageName}`);
  }

  if (inFullOnly && !hasOptionalPeer) {
    failures.push(`@mango/admin peerDependenciesMeta must mark full-only package ${module.packageName} optional`);
  }
}

function assertCliModule(module) {
  if (module.cliOptional === false) {
    return;
  }
  if (!cliSource.includes(`code: '${module.code}'`)) {
    failures.push(`mango-cli optional modules must include ${module.code}`);
  }
}

function assertCliAdminModulesPackaged() {
  if (JSON.stringify(cliAdminModules) !== JSON.stringify(adminModules)) {
    failures.push('packages/mango-cli/admin-modules.json must match packages/admin/admin-modules.json');
  }
  if (!cliPackageJson.files?.includes('admin-modules.json')) {
    failures.push('@mango/cli package files must include admin-modules.json');
  }
  for (const requiredFragment of [
    'readAdminModulesManifest',
    'admin-modules.json',
    'buildOptionalModules(ADMIN_FULL_MODULES, OPTIONAL_MODULE_OVERLAYS)',
  ]) {
    if (!cliSource.includes(requiredFragment)) {
      failures.push(`mango-cli must derive optional module metadata from packaged admin-modules.json: missing ${requiredFragment}`);
    }
  }
  const overlayBlock = readCliOverlayBlock();
  for (const forbidden of [
    'frontendPackage',
    'versionKey',
    'styleImport',
    'registrarImport',
    'registrar:',
  ]) {
    if (overlayBlock.includes(forbidden)) {
      failures.push(`mango-cli optional module overlay must not duplicate admin module metadata field ${forbidden}`);
    }
  }
  for (const module of [...defaultModules, ...fullModules]) {
    if (module.cliVersionKey && !cliSource.includes(module.cliVersionKey)) {
      failures.push(`mango-cli default versions must include ${module.cliVersionKey}`);
    }
  }
}

function readCliOverlayBlock() {
  const start = cliSource.indexOf('const OPTIONAL_MODULE_OVERLAYS = [');
  const end = cliSource.indexOf('const OPTIONAL_MODULES = buildOptionalModules', start);
  if (start < 0 || end < 0) {
    failures.push('mango-cli must keep optional CLI-only metadata in OPTIONAL_MODULE_OVERLAYS');
    return '';
  }
  return cliSource.slice(start, end);
}

function assertStyleAggregation(module) {
  const manifestItem = adminManifest.packages.find((item) => item.name === module.packageName);
  const inGeneratedStyles = generatedStyles.includes(`@import '${module.style}';`);
  const inStyleFull = styleFull.includes(`@import '${module.style}';`);
  const inDefault = defaultModules.some((item) => item.packageName === module.packageName);
  const inFullOnly = fullModules.some((item) => item.packageName === module.packageName);
  if (manifestItem && manifestItem.style !== module.style) {
    failures.push(`admin-packages.json ${module.packageName} style must be ${module.style}`);
  }
  if (inDefault && !manifestItem) {
    failures.push(`${module.packageName} must appear in admin-packages.json`);
  }
  if (inFullOnly && manifestItem) {
    failures.push(`${module.packageName} must not appear in default admin-packages.json`);
  }
  if (inFullOnly && inGeneratedStyles) {
    failures.push(`${module.packageName} must not appear in generated-package-styles.css`);
  }
  if (inFullOnly && !inStyleFull) {
    failures.push(`${module.packageName} must appear in style-full.css`);
  }
  if (inDefault && !inGeneratedStyles) {
    failures.push(`generated-package-styles.css must include ${module.style}`);
  }
}

function packageFolder(packageName) {
  return packageName.replace('@mango/', '');
}

function hasWorkspaceBuildScript(packageName) {
  const packageJsonPath = `packages/${packageFolder(packageName)}/package.json`;
  if (!existsSync(resolve(root, packageJsonPath))) {
    return false;
  }
  return Boolean(readJson(packageJsonPath).scripts?.build);
}

function readJson(path) {
  return JSON.parse(readText(path));
}

function readText(path) {
  return readFileSync(resolve(root, path), 'utf8');
}

function readStyleExportTarget(packageJson) {
  const styleExport = packageJson.exports?.['./style.css'];
  if (typeof styleExport === 'string') {
    return styleExport;
  }
  if (styleExport && typeof styleExport.import === 'string') {
    return styleExport.import;
  }
  if (styleExport && typeof styleExport.default === 'string') {
    return styleExport.default;
  }
  return '';
}
