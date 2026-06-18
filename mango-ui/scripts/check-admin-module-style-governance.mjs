#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(new URL('..', import.meta.url).pathname);
const failures = [];

const officialModules = [
  {
    code: 'job',
    packageName: '@mango/job',
    style: '@mango/job/style.css',
    registrar: 'registerMangoJobAdminPages',
    registrarImport: '@mango/job/admin-pages',
    cliVersionKey: 'mangoJob',
    cliOptional: false,
  },
  {
    code: 'payment',
    packageName: '@mango/payment',
    style: '@mango/payment/style.css',
    registrar: 'registerMangoPaymentAdminPages',
    registrarImport: '@mango/payment/admin-pages',
    cliVersionKey: 'mangoPayment',
  },
  {
    code: 'workflow',
    packageName: '@mango/workflow',
    style: '@mango/workflow/style.css',
    registrar: 'registerMangoWorkflowAdminPages',
    registrarImport: '@mango/workflow/admin-pages',
    cliVersionKey: 'mangoWorkflow',
  },
  {
    code: 'workflow-example',
    packageName: '@mango/workflow-business-example',
    style: '@mango/workflow-business-example/style.css',
    registrar: 'registerMangoWorkflowBusinessExampleAdminPages',
    registrarImport: '@mango/workflow-business-example/admin-pages',
    cliVersionKey: 'mangoWorkflowBusinessExample',
  },
  {
    code: 'file',
    packageName: '@mango/file',
    style: '@mango/file/style.css',
    registrar: 'registerMangoFileAdminPages',
    registrarImport: '@mango/file/admin-pages',
    cliVersionKey: 'mangoFile',
  },
  {
    code: 'template',
    packageName: '@mango/template',
    style: '@mango/template/style.css',
    registrar: 'registerMangoTemplateAdminPages',
    registrarImport: '@mango/template/admin-pages',
    cliVersionKey: 'mangoTemplate',
  },
  {
    code: 'notice',
    packageName: '@mango/notice',
    style: '@mango/notice/style.css',
    registrar: 'registerMangoNoticeAdminPages',
    registrarImport: '@mango/notice/admin-pages',
    cliVersionKey: 'mangoNotice',
  },
  {
    code: 'numgen',
    packageName: '@mango/numgen',
    style: '@mango/numgen/style.css',
    registrar: 'registerMangoNumgenAdminPages',
    registrarImport: '@mango/numgen/admin-pages',
    cliVersionKey: 'mangoNumgen',
  },
  {
    code: 'calendar',
    packageName: '@mango/calendar',
    style: '@mango/calendar/style.css',
    registrar: 'registerMangoCalendarAdminPages',
    registrarImport: '@mango/calendar/admin-pages',
    cliVersionKey: 'mangoCalendar',
  },
];

const adminPackageJson = readJson('packages/admin/package.json');
const adminManifest = readJson('packages/admin/admin-packages.json');
const generatedStyles = readText('packages/admin/generated-package-styles.css');
const styleFull = readText('packages/admin/style-full.css');
const fullEntry = readText('packages/admin/src/full.ts');
const cliSource = readText('packages/mango-cli/src/index.mjs');

for (const module of officialModules) {
  assertPackageStyleExport(module);
  assertFullEntry(module);
  assertAdminDependency(module);
  assertCliModule(module);
  assertStyleAggregation(module);
}

if (failures.length > 0) {
  console.error(`Admin module style governance check failed:\n${failures.map((item) => `- ${item}`).join('\n')}`);
  process.exit(1);
}

console.log(`Admin module style governance check passed: ${officialModules.length} official modules.`);

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
  if (!fullEntry.includes(`from '${module.registrarImport}'`)) {
    failures.push(`@mango/admin/full must import/export ${module.registrarImport}`);
  }
  if (!fullEntry.includes(module.registrar)) {
    failures.push(`@mango/admin/full must register ${module.registrar}`);
  }
}

function assertAdminDependency(module) {
  const inDefaultStyle = adminManifest.packages.some((item) => item.name === module.packageName);
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

  if (!hasOptionalPeer) {
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
  for (const expected of [module.packageName, module.style, module.registrar, module.registrarImport, module.cliVersionKey]) {
    if (!cliSource.includes(expected)) {
      failures.push(`mango-cli optional module ${module.code} must include ${expected}`);
    }
  }
}

function assertStyleAggregation(module) {
  const manifestItem = adminManifest.packages.find((item) => item.name === module.packageName);
  const inGeneratedStyles = generatedStyles.includes(`@import '${module.style}';`);
  const inStyleFull = styleFull.includes(`@import '${module.style}';`);
  if (manifestItem && manifestItem.style !== module.style) {
    failures.push(`admin-packages.json ${module.packageName} style must be ${module.style}`);
  }
  if (!manifestItem && !inStyleFull) {
    failures.push(`${module.packageName} must appear in admin-packages.json or style-full.css`);
  }
  if (manifestItem && !inGeneratedStyles) {
    failures.push(`generated-package-styles.css must include ${module.style}`);
  }
}

function packageFolder(packageName) {
  return packageName.replace('@mango/', '');
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
